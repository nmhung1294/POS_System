package repository;

import java.util.Map;

/**
 * Repository interface for master data operations.
 * Includes common CRUD operations with caching support.
 */
public interface MasterDataRepository {
    
    /**
     * Find all brands (cached).
     * @return Map of brand name to ID
     */
    Map<String, String> findAllBrands() throws Exception;
    
    /**
     * Find all product types (cached).
     * @return Map of type name to ID
     */
    Map<String, String> findAllProductTypes() throws Exception;
    
    /**
     * Find all employee types (cached).
     * @return Map of type name to ID
     */
    Map<String, String> findAllEmployeeTypes() throws Exception;
    
    /**
     * Find all genders (cached).
     * @return Map of gender name to ID
     */
    Map<String, String> findAllGenders() throws Exception;
    
    /**
     * Find all cities (cached).
     * @return Map of city name to ID
     */
    Map<String, String> findAllCities() throws Exception;
    
    /**
     * Find all companies (cached).
     * @return Map of company name to ID
     */
    Map<String, String> findAllCompanies() throws Exception;
    
    /**
     * Invalidate specific cache.
     * Call after INSERT/UPDATE/DELETE operations.
     */
    void invalidateCache(String cacheKey);
    
    /**
     * Invalidate all master data caches.
     * Use when multiple master data tables are updated.
     */
    void invalidateAllCaches();
}
