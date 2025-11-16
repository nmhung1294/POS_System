package repository;

import util.DBUtil;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of StockRepository.
 */
public class StockRepositoryImpl implements StockRepository {

    private static final Logger logger = Logger.getLogger(StockRepositoryImpl.class.getName());

    @Override
    public Optional<String> findStockId(String productId, BigDecimal sellingPrice, LocalDate mfg, LocalDate exp) throws Exception {
        String sql = "SELECT id FROM stock WHERE product_id = ? AND selling_price = ? AND mfg = ? AND exp = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, productId);
            ps.setBigDecimal(2, sellingPrice);
            ps.setDate(3, Date.valueOf(mfg));
            ps.setDate(4, Date.valueOf(exp));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getString("id"));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error finding stock", e);
            throw new Exception("Failed to find stock", e);
        }
    }

    @Override
    public double getStockQuantity(String stockId) throws Exception {
        String sql = "SELECT qty FROM stock WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, stockId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("qty");
                }
                return 0.0;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error getting stock quantity", e);
            throw new Exception("Failed to get stock quantity", e);
        }
    }

    @Override
    public void updateStockQuantity(String stockId, double newQuantity) throws Exception {
        String sql = "UPDATE stock SET qty = ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, newQuantity);
            ps.setString(2, stockId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error updating stock quantity", e);
            throw new Exception("Failed to update stock quantity", e);
        }
    }

    @Override
    public String createStock(String productId, double quantity, BigDecimal sellingPrice,
                             LocalDate mfg, LocalDate exp) throws Exception {
        String sql = "INSERT INTO stock (product_id, qty, selling_price, mfg, exp) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, productId);
            ps.setDouble(2, quantity);
            ps.setBigDecimal(3, sellingPrice);
            ps.setDate(4, Date.valueOf(mfg));
            ps.setDate(5, Date.valueOf(exp));
            ps.executeUpdate();

            // Get generated stock ID
            Optional<String> stockId = findStockId(productId, sellingPrice, mfg, exp);
            return stockId.orElseThrow(() -> new Exception("Failed to retrieve created stock ID"));
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error creating stock", e);
            throw new Exception("Failed to create stock", e);
        }
    }

    @Override
    public void decreaseStockQuantity(String stockId, double quantity) throws Exception {
        String sql = "UPDATE stock SET qty = qty - ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, quantity);
            ps.setString(2, stockId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error decreasing stock quantity", e);
            throw new Exception("Failed to decrease stock quantity", e);
        }
    }
}
