package repository;

import model.Invoice;
import util.DBUtil;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of InvoiceRepository.
 */
public class InvoiceRepositoryImpl implements InvoiceRepository {

    private static final Logger logger = Logger.getLogger(InvoiceRepositoryImpl.class.getName());

    @Override
    public void createInvoice(String id, String customerMobile, BigDecimal discount,
                             BigDecimal paidAmount, String paymentMethodId, BigDecimal balance,
                             LocalDateTime dateTime) throws Exception {
        String sql = "INSERT INTO invoice (id, customer_mobile, discount, paid_amount, payment_method_id, balance, date_time) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, customerMobile);
            ps.setBigDecimal(3, discount);
            ps.setBigDecimal(4, paidAmount);
            ps.setString(5, paymentMethodId);
            ps.setBigDecimal(6, balance);
            ps.setTimestamp(7, Timestamp.valueOf(dateTime));
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error creating invoice: " + id, e);
            throw new Exception("Failed to create invoice", e);
        }
    }

    @Override
    public int countByMonth(String yyyyMM) throws Exception {
        String sql = "SELECT COUNT(*) AS cnt FROM invoice WHERE date_time >= ? AND date_time < ?";
        String startDate = yyyyMM + "-01";
        String endDate = calculateNextMonthFirstDay(yyyyMM);

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, startDate);
            ps.setString(2, endDate);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("cnt");
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error counting invoices by month: " + yyyyMM, e);
            throw new Exception("Failed to count invoices", e);
        }
        return 0;
    }

    @Override
    public double sumPaidByMonth(String yyyyMM) throws Exception {
        String sql = "SELECT SUM(paid_amount) AS total FROM invoice WHERE date_time >= ? AND date_time < ?";
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
    public List<Invoice> findByMonth(String yyyyMM) throws Exception {
        String sql = "SELECT id, date_time, paid_amount FROM invoice WHERE date_time >= ? AND date_time < ?";
        List<Invoice> invoices = new ArrayList<>();
        String startDate = yyyyMM + "-01";
        String endDate = calculateNextMonthFirstDay(yyyyMM);

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, startDate);
            ps.setString(2, endDate);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Invoice invoice = new Invoice();
                    invoice.setId(rs.getInt("id"));
                    invoice.setDateTime(rs.getTimestamp("date_time").toLocalDateTime());
                    invoice.setPaidAmount(rs.getBigDecimal("paid_amount"));
                    invoices.add(invoice);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error finding invoices by month: " + yyyyMM, e);
            throw new Exception("Failed to find invoices", e);
        }
        return invoices;
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