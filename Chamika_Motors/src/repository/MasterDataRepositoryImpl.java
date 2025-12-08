package repository;

import util.DBUtil;
import util.CacheManager;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of MasterDataRepository with comprehensive caching.
 * All master data queries are cached for 24 hours (configurable).
 * 
 * Cache Keys:
 * - "brands" - All brands
 * - "product_types" - All product types
 * - "employee_types" - All employee types
 * - "genders" - All genders
 * - "cities" - All cities
 * - "companies" - All companies
 */
public class MasterDataRepositoryImpl implements MasterDataRepository {

    private static final Logger logger = Logger.getLogger(MasterDataRepositoryImpl.class.getName());
    
    // Cache keys
    private static final String CACHE_BRANDS = "brands";
    private static final String CACHE_PRODUCT_TYPES = "product_types";
    private static final String CACHE_EMPLOYEE_TYPES = "employee_types";
    private static final String CACHE_GENDERS = "genders";
    private static final String CACHE_CITIES = "cities";
    private static final String CACHE_COMPANIES = "companies";

    @Override
    public Map<String, String> findAllBrands() throws Exception {
        return CacheManager.getInstance().get(CACHE_BRANDS, () -> {
            logger.info("Cache MISS: Loading brands from database");
            return loadFromDatabase("SELECT id, name FROM brand", "brands");
        });
    }

    @Override
    public Map<String, String> findAllProductTypes() throws Exception {
        return CacheManager.getInstance().get(CACHE_PRODUCT_TYPES, () -> {
            logger.info("Cache MISS: Loading product types from database");
            return loadFromDatabase("SELECT id, name FROM product_type", "product types");
        });
    }

    @Override
    public Map<String, String> findAllEmployeeTypes() throws Exception {
        return CacheManager.getInstance().get(CACHE_EMPLOYEE_TYPES, () -> {
            logger.info("Cache MISS: Loading employee types from database");
            return loadFromDatabase("SELECT id, name FROM employee_type", "employee types");
        });
    }

    @Override
    public Map<String, String> findAllGenders() throws Exception {
        return CacheManager.getInstance().get(CACHE_GENDERS, () -> {
            logger.info("Cache MISS: Loading genders from database");
            return loadFromDatabase("SELECT id, name FROM gender", "genders");
        });
    }

    @Override
    public Map<String, String> findAllCities() throws Exception {
        return CacheManager.getInstance().get(CACHE_CITIES, () -> {
            logger.info("Cache MISS: Loading cities from database");
            return loadFromDatabase("SELECT id, name FROM city", "cities");
        });
    }

    @Override
    public Map<String, String> findAllCompanies() throws Exception {
        return CacheManager.getInstance().get(CACHE_COMPANIES, () -> {
            logger.info("Cache MISS: Loading companies from database");
            return loadFromDatabase("SELECT id, name FROM company", "companies");
        });
    }

    @Override
    public void invalidateCache(String cacheKey) {
        CacheManager.getInstance().invalidate(cacheKey);
        logger.info("Cache invalidated: " + cacheKey);
    }

    @Override
    public void invalidateAllCaches() {
        CacheManager.getInstance().invalidate(CACHE_BRANDS);
        CacheManager.getInstance().invalidate(CACHE_PRODUCT_TYPES);
        CacheManager.getInstance().invalidate(CACHE_EMPLOYEE_TYPES);
        CacheManager.getInstance().invalidate(CACHE_GENDERS);
        CacheManager.getInstance().invalidate(CACHE_CITIES);
        CacheManager.getInstance().invalidate(CACHE_COMPANIES);
        logger.info("All master data caches invalidated");
    }

    /**
     * Generic method to load data from database.
     * Returns Map<name, id> for easy lookup.
     */
    private Map<String, String> loadFromDatabase(String sql, String dataType) throws Exception {
        Map<String, String> result = new HashMap<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DBUtil.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            
            while (rs.next()) {
                String id = rs.getString("id");
                String name = rs.getString("name");
                result.put(name, id);
            }
            
            logger.info(String.format("Loaded %d %s from database", result.size(), dataType));
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error loading " + dataType + " from database", e);
            throw new Exception("Failed to load " + dataType, e);
        } finally {
            DBUtil.closeQuietly(rs, ps, conn);
        }
        
        return result;
    }

    /**
     * Create a new brand.
     * Invalidates cache after successful creation.
     */
    public void createBrand(String name) throws Exception {
        String sql = "INSERT INTO brand (name) VALUES (?)";
        executeUpdate(sql, name, CACHE_BRANDS);
    }

    /**
     * Create a new product type.
     * Invalidates cache after successful creation.
     */
    public void createProductType(String name) throws Exception {
        String sql = "INSERT INTO product_type (name) VALUES (?)";
        executeUpdate(sql, name, CACHE_PRODUCT_TYPES);
    }

    /**
     * Create a new company.
     * Invalidates cache after successful creation.
     */
    public void createCompany(String name, String hotline) throws Exception {
        String sql = "INSERT INTO company (name, hotline) VALUES (?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = DBUtil.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, name);
            ps.setString(2, hotline);
            
            int affected = ps.executeUpdate();
            if (affected > 0) {
                invalidateCache(CACHE_COMPANIES);
                logger.info("Company created and cache invalidated: " + name);
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error creating company: " + name, e);
            throw new Exception("Failed to create company", e);
        } finally {
            DBUtil.closeQuietly(null, ps, conn);
        }
    }

    /**
     * Update a company.
     * Invalidates cache after successful update.
     */
    public void updateCompany(String id, String name, String hotline) throws Exception {
        String sql = "UPDATE company SET name = ?, hotline = ? WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = DBUtil.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, name);
            ps.setString(2, hotline);
            ps.setString(3, id);
            
            int affected = ps.executeUpdate();
            if (affected > 0) {
                invalidateCache(CACHE_COMPANIES);
                logger.info("Company updated and cache invalidated: " + id);
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error updating company: " + id, e);
            throw new Exception("Failed to update company", e);
        } finally {
            DBUtil.closeQuietly(null, ps, conn);
        }
    }

    /**
     * Generic method for simple INSERT operations.
     */
    private void executeUpdate(String sql, String value, String cacheKey) throws Exception {
        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = DBUtil.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, value);
            
            int affected = ps.executeUpdate();
            if (affected > 0) {
                invalidateCache(cacheKey);
                logger.info("Record created and cache invalidated: " + value);
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error executing update: " + sql, e);
            throw new Exception("Failed to execute update", e);
        } finally {
            DBUtil.closeQuietly(null, ps, conn);
        }
    }

    /**
     * Check if a brand exists.
     * Uses cached data for fast lookup.
     */
    public boolean brandExists(String name) throws Exception {
        Map<String, String> brands = findAllBrands();
        return brands.containsKey(name);
    }

    /**
     * Check if a company exists.
     * Uses cached data for fast lookup.
     */
    public boolean companyExists(String name) throws Exception {
        Map<String, String> companies = findAllCompanies();
        return companies.containsKey(name);
    }

    /**
     * Get brand ID by name.
     * Uses cached data for fast lookup.
     */
    public String getBrandId(String name) throws Exception {
        Map<String, String> brands = findAllBrands();
        return brands.get(name);
    }

    /**
     * Get company ID by name.
     * Uses cached data for fast lookup.
     */
    public String getCompanyId(String name) throws Exception {
        Map<String, String> companies = findAllCompanies();
        return companies.get(name);
    }
}
