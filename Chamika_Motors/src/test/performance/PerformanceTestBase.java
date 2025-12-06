package test.performance;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import util.DBUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Base class for performance tests.
 * Provides utilities for measuring and reporting performance metrics.
 */
public abstract class PerformanceTestBase {
    
    protected static final Logger logger = Logger.getLogger(PerformanceTestBase.class.getName());
    
    protected static class PerformanceMetrics {
        public String testName;
        public long executionTimeMs;
        public long queryCount;
        public double queriesPerSecond;
        public String status;
        
        @Override
        public String toString() {
            return String.format(
                "%-40s | %8d ms | %5d queries | %8.2f qps | %s",
                testName, executionTimeMs, queryCount, queriesPerSecond, status
            );
        }
        
        public String toMarkdown() {
            return String.format("| %-40s | %8d ms | %5d | %8.2f | %s |",
                testName, executionTimeMs, queryCount, queriesPerSecond, status);
        }
    }
    
    @BeforeAll
    static void setupPerformanceTests() {
        logger.info("=".repeat(80));
        logger.info("PERFORMANCE TEST SUITE - Started");
        logger.info("=".repeat(80));
    }
    
    @AfterAll
    static void teardownPerformanceTests() {
        logger.info("=".repeat(80));
        logger.info("PERFORMANCE TEST SUITE - Completed");
        logger.info("=".repeat(80));
    }
    
    /**
     * Measure execution time of a runnable task.
     */
    protected PerformanceMetrics measurePerformance(String testName, Runnable task, int iterations) {
        PerformanceMetrics metrics = new PerformanceMetrics();
        metrics.testName = testName;
        metrics.queryCount = iterations;
        
        // Warm up
        try {
            task.run();
        } catch (Exception e) {
            // Ignore warm-up errors
        }
        
        // Actual measurement
        long startTime = System.currentTimeMillis();
        
        try {
            for (int i = 0; i < iterations; i++) {
                task.run();
            }
            metrics.status = "✅ PASS";
        } catch (Exception e) {
            metrics.status = "❌ FAIL: " + e.getMessage();
            logger.severe("Test failed: " + testName + " - " + e.getMessage());
        }
        
        long endTime = System.currentTimeMillis();
        metrics.executionTimeMs = endTime - startTime;
        metrics.queriesPerSecond = (iterations * 1000.0) / metrics.executionTimeMs;
        
        return metrics;
    }
    
    /**
     * Verify database connection is working.
     */
    protected void verifyDatabaseConnection() throws SQLException {
        try (Connection conn = DBUtil.getConnection()) {
            if (conn == null || conn.isClosed()) {
                throw new SQLException("Database connection is not available");
            }
            logger.info("✅ Database connection verified");
        }
    }
    
    /**
     * Check if indices exist on a table.
     */
    protected boolean checkIndexExists(String tableName, String indexName) throws SQLException {
        String sql = "SELECT COUNT(*) as cnt FROM information_schema.STATISTICS " +
                    "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?";
        
        try (Connection conn = DBUtil.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableName);
            ps.setString(2, indexName);
            
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("cnt") > 0;
                }
            }
        }
        return false;
    }
    
    /**
     * Print performance report.
     */
    protected void printPerformanceReport(PerformanceMetrics... metricsArray) {
        logger.info("\n" + "=".repeat(120));
        logger.info("PERFORMANCE REPORT");
        logger.info("=".repeat(120));
        logger.info(String.format("%-40s | %12s | %11s | %13s | %s",
            "Test Name", "Time", "Queries", "QPS", "Status"));
        logger.info("-".repeat(120));
        
        long totalTime = 0;
        long totalQueries = 0;
        
        for (PerformanceMetrics metrics : metricsArray) {
            logger.info(metrics.toString());
            totalTime += metrics.executionTimeMs;
            totalQueries += metrics.queryCount;
        }
        
        logger.info("-".repeat(120));
        logger.info(String.format("TOTAL: %d queries in %d ms (%.2f qps average)",
            totalQueries, totalTime, (totalQueries * 1000.0) / totalTime));
        logger.info("=".repeat(120) + "\n");
    }
}
