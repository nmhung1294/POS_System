package test.performance;

import org.junit.jupiter.api.*;
import util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance tests for database operations.
 * Measures query execution times and verifies optimization effectiveness.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DatabasePerformanceTest extends PerformanceTestBase {

    private static final int WARMUP_ITERATIONS = 5;
    private static final int TEST_ITERATIONS = 100;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    @Test
    @Order(1)
    @DisplayName("Verify Database Connection")
    void testDatabaseConnection() throws Exception {
        verifyDatabaseConnection();
        logger.info(" Database connection test passed");
    }
    
    @Test
    @Order(2)
    @DisplayName("Verify Critical Indices Exist")
    void testCriticalIndicesExist() throws Exception {
        // Test critical indices
        assertTrue(checkIndexExists("invoice", "idx_invoice_date_time"), 
            "Index idx_invoice_date_time should exist on invoice table");
        assertTrue(checkIndexExists("grn", "idx_grn_date_time"), 
            "Index idx_grn_date_time should exist on grn table");
        assertTrue(checkIndexExists("stock", "idx_stock_product_price"), 
            "Index idx_stock_product_price should exist on stock table");
        assertTrue(checkIndexExists("customer", "idx_customer_name"), 
            "Index idx_customer_name should exist on customer table");
        
        logger.info(" All critical indices verified");
    }
    
    @Test
    @Order(3)
    @DisplayName("Performance: Invoice Date Range Query")
    void testInvoiceDateRangePerformance() {
        LocalDate startDate = LocalDate.now().minusMonths(1);
        LocalDate endDate = LocalDate.now();
        
        PerformanceMetrics metrics = measurePerformance(
            "Invoice Date Range Query",
            () -> executeInvoiceDateRangeQuery(startDate, endDate),
            TEST_ITERATIONS
        );
        
        printPerformanceReport(metrics);
        
        // Assert performance target: < 50ms average per query
        double avgTimeMs = (double) metrics.executionTimeMs / TEST_ITERATIONS;
        assertTrue(avgTimeMs < 50, 
            String.format("Query should complete in < 50ms, actual: %.2f ms", avgTimeMs));
    }
    
    @Test
    @Order(4)
    @DisplayName("Performance: Invoice Count and Sum Query")
    void testInvoiceCountAndSumPerformance() {
        LocalDate startDate = LocalDate.now().minusMonths(1);
        LocalDate endDate = LocalDate.now();
        
        PerformanceMetrics metrics = measurePerformance(
            "Invoice Count + Sum Query",
            () -> executeInvoiceCountAndSum(startDate, endDate),
            TEST_ITERATIONS
        );
        
        printPerformanceReport(metrics);
        
        double avgTimeMs = (double) metrics.executionTimeMs / TEST_ITERATIONS;
        assertTrue(avgTimeMs < 50, 
            String.format("Aggregate query should complete in < 50ms, actual: %.2f ms", avgTimeMs));
    }
    
    @Test
    @Order(5)
    @DisplayName("Performance: Customer Search by Name")
    void testCustomerSearchPerformance() {
        String[] searchTerms = {"S%", "A%", "M%", "K%", "L%"};
        
        PerformanceMetrics[] allMetrics = new PerformanceMetrics[searchTerms.length];
        
        for (int i = 0; i < searchTerms.length; i++) {
            final String term = searchTerms[i];
            allMetrics[i] = measurePerformance(
                "Customer Search: " + term,
                () -> executeCustomerSearch(term),
                TEST_ITERATIONS / 5 // Fewer iterations for variety
            );
        }
        
        printPerformanceReport(allMetrics);
        
        // Average should be < 20ms
        double totalAvg = 0;
        for (PerformanceMetrics m : allMetrics) {
            totalAvg += (double) m.executionTimeMs / m.queryCount;
        }
        totalAvg /= allMetrics.length;
        
        assertTrue(totalAvg < 20, 
            String.format("Customer search should average < 20ms, actual: %.2f ms", totalAvg));
    }
    
    @Test
    @Order(6)
    @DisplayName("Performance: Stock Lookup by Product")
    void testStockLookupPerformance() {
        PerformanceMetrics metrics = measurePerformance(
            "Stock Lookup Query",
            () -> executeStockLookup("1", 100.00),
            TEST_ITERATIONS
        );
        
        printPerformanceReport(metrics);
        
        double avgTimeMs = (double) metrics.executionTimeMs / TEST_ITERATIONS;
        assertTrue(avgTimeMs < 10, 
            String.format("Stock lookup should complete in < 10ms, actual: %.2f ms", avgTimeMs));
    }
    
    @Test
    @Order(7)
    @DisplayName("Performance: GRN Date Range Query")
    void testGrnDateRangePerformance() {
        LocalDate startDate = LocalDate.now().minusMonths(1);
        LocalDate endDate = LocalDate.now();
        
        PerformanceMetrics metrics = measurePerformance(
            "GRN Date Range Query",
            () -> executeGrnDateRangeQuery(startDate, endDate),
            TEST_ITERATIONS
        );
        
        printPerformanceReport(metrics);
        
        double avgTimeMs = (double) metrics.executionTimeMs / TEST_ITERATIONS;
        assertTrue(avgTimeMs < 30, 
            String.format("GRN query should complete in < 30ms, actual: %.2f ms", avgTimeMs));
    }
    
    @Test
    @Order(8)
    @DisplayName("Performance: Connection Pool Stress Test")
    void testConnectionPoolPerformance() {
        PerformanceMetrics metrics = measurePerformance(
            "Connection Pool Stress (50 connections)",
            () -> {
                try (Connection conn = DBUtil.getConnection()) {
                    // Just get and release connection
                    assertNotNull(conn);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            },
            50 // Test connection pool size
        );
        
        printPerformanceReport(metrics);
        
        // All 50 connections should be obtained within 5 seconds
        assertTrue(metrics.executionTimeMs < 5000,
            String.format("Connection pool should handle 50 connections in < 5s, actual: %d ms", 
                metrics.executionTimeMs));
    }
    
    // Helper methods for queries
    
    private void executeInvoiceDateRangeQuery(LocalDate start, LocalDate end) {
        String sql = "SELECT id, date_time, paid_amount FROM invoice " +
                    "WHERE date_time >= ? AND date_time < ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, start.format(DATE_FORMATTER));
            ps.setString(2, end.format(DATE_FORMATTER));
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Process results
                    rs.getString("id");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Query failed", e);
        }
    }
    
    private void executeInvoiceCountAndSum(LocalDate start, LocalDate end) {
        String sql = "SELECT COUNT(*) as cnt, SUM(paid_amount) as total FROM invoice " +
                    "WHERE date_time >= ? AND date_time < ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, start.format(DATE_FORMATTER));
            ps.setString(2, end.format(DATE_FORMATTER));
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    rs.getInt("cnt");
                    rs.getBigDecimal("total");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Query failed", e);
        }
    }
    
    private void executeCustomerSearch(String searchTerm) {
        String sql = "SELECT mobile, name, points FROM customer " +
                    "WHERE name LIKE ? ORDER BY name ASC LIMIT 10";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, searchTerm);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rs.getString("mobile");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Query failed", e);
        }
    }
    
    private void executeStockLookup(String productId, double sellingPrice) {
        String sql = "SELECT id, qty FROM stock " +
                    "WHERE product_id = ? AND selling_price = ? LIMIT 1";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, productId);
            ps.setDouble(2, sellingPrice);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    rs.getString("id");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Query failed", e);
        }
    }
    
    private void executeGrnDateRangeQuery(LocalDate start, LocalDate end) {
        String sql = "SELECT id, date_time, paid_amount FROM grn " +
                    "WHERE date_time >= ? AND date_time < ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, start.format(DATE_FORMATTER));
            ps.setString(2, end.format(DATE_FORMATTER));
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rs.getString("id");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Query failed", e);
        }
    }
}
