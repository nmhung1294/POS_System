package repository;

import model.Grn;
import util.DBUtil;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of GrnRepository.
 */
public class GrnRepositoryImpl implements GrnRepository {

    private static final Logger logger = Logger.getLogger(GrnRepositoryImpl.class.getName());

    @Override
    public void createGrn(String id, String supplierId, String employeeMobile,
                         LocalDateTime dateTime, BigDecimal paidAmount) throws Exception {
        String sql = "INSERT INTO grn (id, supplier_id, employee_mobile, date_time, paid_amount) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, supplierId);
            ps.setString(3, employeeMobile);
            ps.setTimestamp(4, Timestamp.valueOf(dateTime));
            ps.setBigDecimal(5, paidAmount);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error creating GRN: " + id, e);
            throw new Exception("Failed to create GRN", e);
        }
    }

    @Override
    public double sumPaidByMonth(String yyyyMM) throws Exception {
        String sql = "SELECT SUM(paid_amount) AS total FROM grn WHERE date_time >= ? AND date_time < ?";
        String startDate = yyyyMM + "-01";
        String endDate = calculateNextMonthFirstDay(yyyyMM);

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, startDate);
            ps.setString(2, endDate);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal total = rs.getBigDecimal("total");
                    return total != null ? total.doubleValue() : 0.0;
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error summing paid amounts by month: " + yyyyMM, e);
            throw new Exception("Failed to sum paid amounts", e);
        }
        return 0.0;
    }

    @Override
    public List<Grn> findByMonth(String yyyyMM) throws Exception {
        String sql = "SELECT id, date_time, paid_amount FROM grn WHERE date_time >= ? AND date_time < ?";
        List<Grn> grns = new ArrayList<>();
        String startDate = yyyyMM + "-01";
        String endDate = calculateNextMonthFirstDay(yyyyMM);

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, startDate);
            ps.setString(2, endDate);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Grn grn = new Grn();
                    grn.setId(rs.getInt("id"));
                    grn.setDateTime(rs.getTimestamp("date_time").toLocalDateTime());
                    grn.setPaidAmount(rs.getBigDecimal("paid_amount"));
                    grns.add(grn);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error finding GRNs by month: " + yyyyMM, e);
            throw new Exception("Failed to find GRNs", e);
        }
        return grns;
    }

    /**
     * Calculates the first day of the next month from a yyyy-MM format string.
     * For example: "2025-01" returns "2025-02-01", "2025-12" returns "2026-01-01"
     */
    private String calculateNextMonthFirstDay(String yyyyMM) {
        String[] parts = yyyyMM.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);

        if (month == 12) {
            return (year + 1) + "-01-01";
        } else {
            return year + "-" + String.format("%02d", month + 1) + "-01";
        }
    }
}