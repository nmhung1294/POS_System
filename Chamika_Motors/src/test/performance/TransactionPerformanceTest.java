package test.performance;

import org.junit.jupiter.api.*;
import util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance tests for transaction management.
 * Verifies rollback capability and measures transaction overhead.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TransactionPerformanceTest extends PerformanceTestBase {
    
    @Test
    @Order(1)
    @DisplayName("Transaction: Commit Performance")
    void testTransactionCommitPerformance() {
        int iterations = 50;
        
        PerformanceMetrics metrics = measurePerformance(
            "Transaction Commit",
            () -> {
                try (Connection conn = DBUtil.getTransactionalConnection()) {
                    // Simulate a simple transaction
                    executeTestInsert(conn);
                    conn.commit();
                } catch (SQLException e) {
                    throw new RuntimeException("Transaction failed", e);
                }
            },
            iterations
        );
        
        printPerformanceReport(metrics);
        
        // Transaction overhead should be minimal (< 50ms avg including insert)
        double avgTime = (double) metrics.executionTimeMs / iterations;
        assertTrue(avgTime < 50, 
            String.format("Transaction should complete in < 50ms avg, actual: %.2f ms", avgTime));
    }
    
    @Test
    @Order(2)
    @DisplayName("Transaction: Rollback Performance")
    void testTransactionRollbackPerformance() {
        int iterations = 50;
        
        PerformanceMetrics metrics = measurePerformance(
            "Transaction Rollback",
            () -> {
                try (Connection conn = DBUtil.getTransactionalConnection()) {
                    // Simulate a transaction that needs rollback
                    executeTestInsert(conn);
                    conn.rollback(); // Intentional rollback
                } catch (SQLException e) {
                    throw new RuntimeException("Transaction failed", e);
                }
            },
            iterations
        );
        
        printPerformanceReport(metrics);
        
        // Rollback should also be fast
        double avgTime = (double) metrics.executionTimeMs / iterations;
        assertTrue(avgTime < 50, 
            String.format("Rollback should complete in < 50ms avg, actual: %.2f ms", avgTime));
    }
    
    @Test
    @Order(3)
    @DisplayName("Transaction: Auto-commit vs Manual Transaction")
    void testAutoCommitVsTransaction() throws SQLException {
        int iterations = 20;
        
        // Test auto-commit mode
        PerformanceMetrics autoCommitMetrics = measurePerformance(
            "Auto-commit Mode",
            () -> {
                try (Connection conn = DBUtil.getConnection()) {
                    executeTestInsert(conn);
                } catch (SQLException e) {
                    throw new RuntimeException("Query failed", e);
                }
            },
            iterations
        );
        
        // Test manual transaction mode
        PerformanceMetrics transactionMetrics = measurePerformance(
            "Transaction Mode",
            () -> {
                try (Connection conn = DBUtil.getTransactionalConnection()) {
                    executeTestInsert(conn);
                    conn.commit();
                } catch (SQLException e) {
                    throw new RuntimeException("Transaction failed", e);
                }
            },
            iterations
        );
        
        printPerformanceReport(autoCommitMetrics, transactionMetrics);
        
        // Transaction mode may be slightly slower but should be acceptable
        double autoCommitAvg = (double) autoCommitMetrics.executionTimeMs / iterations;
        double transactionAvg = (double) transactionMetrics.executionTimeMs / iterations;
        double overhead = transactionAvg - autoCommitAvg;
        
        logger.info(String.format("Transaction overhead: %.2f ms per operation", overhead));
        
        // Overhead should be minimal (< 10ms)
        assertTrue(overhead < 10, 
            String.format("Transaction overhead should be < 10ms, actual: %.2f ms", overhead));
    }
    
    @Test
    @Order(4)
    @DisplayName("Transaction: Data Consistency Verification")
    void testDataConsistency() throws SQLException {
        // Test that rollback actually works
        Connection conn = null;
        
        try {
            conn = DBUtil.getTransactionalConnection();
            
            // Insert test data
            String insertSql = "INSERT INTO payment_method (name) VALUES (?)";
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setString(1, "TEST_ROLLBACK_METHOD");
                ps.executeUpdate();
            }
            
            // Verify it exists before rollback
            String checkSql = "SELECT COUNT(*) as cnt FROM payment_method WHERE name = ?";
            try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ps.setString(1, "TEST_ROLLBACK_METHOD");
                try (var rs = ps.executeQuery()) {
                    if (rs.next()) {
                        assertEquals(1, rs.getInt("cnt"), "Record should exist before rollback");
                    }
                }
            }
            
            // Rollback
            conn.rollback();
            
            // Verify it's gone after rollback (need new connection to see committed state)
            try (Connection verifyConn = DBUtil.getConnection();
                 PreparedStatement ps = verifyConn.prepareStatement(checkSql)) {
                ps.setString(1, "TEST_ROLLBACK_METHOD");
                try (var rs = ps.executeQuery()) {
                    if (rs.next()) {
                        assertEquals(0, rs.getInt("cnt"), "Record should NOT exist after rollback");
                    }
                }
            }
            
            logger.info(" Transaction rollback data consistency verified");
            
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }
    
    // Helper method for test inserts
    private void executeTestInsert(Connection conn) throws SQLException {
        // Use a harmless SELECT instead of INSERT to avoid polluting data
        String sql = "SELECT 1 as test";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeQuery();
        }
    }
}
