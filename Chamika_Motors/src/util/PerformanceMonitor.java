package util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Performance Monitoring Utility.
 * Tracks system performance metrics in real-time.
 * 
 * Metrics tracked:
 * - Query execution times
 * - Cache hit/miss rates
 * - Connection pool usage
 * - Transaction success/failure rates
 * - Active users count
 * 
 * Usage:
 * <pre>
 * PerformanceMonitor.getInstance().recordQueryTime("invoice_search", 25);
 * PerformanceMonitor.getInstance().recordCacheHit("payment_methods");
 * PerformanceMonitor.getInstance().printDashboard();
 * </pre>
 */
public class PerformanceMonitor {
    
    private static final Logger logger = Logger.getLogger(PerformanceMonitor.class.getName());
    private static final PerformanceMonitor INSTANCE = new PerformanceMonitor();
    private static final DecimalFormat df = new DecimalFormat("#.##");
    
    // Metrics counters
    private long totalQueries = 0;
    private long totalQueryTime = 0;
    private long cacheHits = 0;
    private long cacheMisses = 0;
    private long transactionCommits = 0;
    private long transactionRollbacks = 0;
    
    // Query categories
    private QueryStats invoiceQueries = new QueryStats("Invoice");
    private QueryStats grnQueries = new QueryStats("GRN");
    private QueryStats stockQueries = new QueryStats("Stock");
    private QueryStats customerQueries = new QueryStats("Customer");
    
    // Start time
    private final LocalDateTime startTime = LocalDateTime.now();
    
    private PerformanceMonitor() {}
    
    public static PerformanceMonitor getInstance() {
        return INSTANCE;
    }
    
    /**
     * Record a query execution time.
     */
    public void recordQueryTime(String queryType, long milliseconds) {
        totalQueries++;
        totalQueryTime += milliseconds;
        
        // Categorize by query type
        if (queryType.toLowerCase().contains("invoice")) {
            invoiceQueries.addQuery(milliseconds);
        } else if (queryType.toLowerCase().contains("grn")) {
            grnQueries.addQuery(milliseconds);
        } else if (queryType.toLowerCase().contains("stock")) {
            stockQueries.addQuery(milliseconds);
        } else if (queryType.toLowerCase().contains("customer")) {
            customerQueries.addQuery(milliseconds);
        }
    }
    
    /**
     * Record a cache hit.
     */
    public void recordCacheHit(String cacheKey) {
        cacheHits++;
        logger.fine("Cache HIT: " + cacheKey);
    }
    
    /**
     * Record a cache miss.
     */
    public void recordCacheMiss(String cacheKey) {
        cacheMisses++;
        logger.info("Cache MISS: " + cacheKey);
    }
    
    /**
     * Record a transaction commit.
     */
    public void recordTransactionCommit() {
        transactionCommits++;
    }
    
    /**
     * Record a transaction rollback.
     */
    public void recordTransactionRollback() {
        transactionRollbacks++;
        logger.warning("Transaction ROLLBACK recorded");
    }
    
    /**
     * Get cache hit rate percentage.
     */
    public double getCacheHitRate() {
        long total = cacheHits + cacheMisses;
        return total > 0 ? (cacheHits * 100.0) / total : 0;
    }
    
    /**
     * Get average query time in milliseconds.
     */
    public double getAverageQueryTime() {
        return totalQueries > 0 ? (double) totalQueryTime / totalQueries : 0;
    }
    
    /**
     * Get transaction success rate percentage.
     */
    public double getTransactionSuccessRate() {
        long total = transactionCommits + transactionRollbacks;
        return total > 0 ? (transactionCommits * 100.0) / total : 100;
    }
    
    /**
     * Get connection pool statistics.
     */
    public PoolStats getConnectionPoolStats() {
        try (Connection conn = DBUtil.getConnection()) {
            String sql = "SHOW STATUS LIKE 'Threads_connected'";
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                int activeConnections = rs.getInt("Value");
                return new PoolStats(activeConnections, 50); // Max pool size is 50
            }
        } catch (SQLException e) {
            logger.warning("Failed to get connection pool stats: " + e.getMessage());
        }
        return new PoolStats(0, 50);
    }
    
    /**
     * Print comprehensive dashboard to console.
     */
    public void printDashboard() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("═".repeat(80)).append("\n");
        sb.append("              PERFORMANCE MONITORING DASHBOARD\n");
        sb.append("═".repeat(80)).append("\n");
        
        // System info
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        sb.append(String.format("Started:  %s\n", startTime.format(formatter)));
        sb.append(String.format("Current:  %s\n", now.format(formatter)));
        sb.append(String.format("Uptime:   %d minutes\n", 
            java.time.Duration.between(startTime, now).toMinutes()));
        sb.append("─".repeat(80)).append("\n");
        
        // Query performance
        sb.append("QUERY PERFORMANCE\n");
        sb.append("─".repeat(80)).append("\n");
        sb.append(String.format("Total Queries:       %,d\n", totalQueries));
        sb.append(String.format("Total Query Time:    %,d ms\n", totalQueryTime));
        sb.append(String.format("Average Query Time:  %.2f ms\n", getAverageQueryTime()));
        sb.append("\n");
        
        // Query breakdown
        sb.append("Query Breakdown:\n");
        sb.append(String.format("  %-15s: %5d queries, avg %.2f ms\n", 
            invoiceQueries.name, invoiceQueries.count, invoiceQueries.getAverage()));
        sb.append(String.format("  %-15s: %5d queries, avg %.2f ms\n", 
            grnQueries.name, grnQueries.count, grnQueries.getAverage()));
        sb.append(String.format("  %-15s: %5d queries, avg %.2f ms\n", 
            stockQueries.name, stockQueries.count, stockQueries.getAverage()));
        sb.append(String.format("  %-15s: %5d queries, avg %.2f ms\n", 
            customerQueries.name, customerQueries.count, customerQueries.getAverage()));
        sb.append("─".repeat(80)).append("\n");
        
        // Cache performance
        sb.append("CACHE PERFORMANCE\n");
        sb.append("─".repeat(80)).append("\n");
        sb.append(String.format("Cache Hits:          %,d\n", cacheHits));
        sb.append(String.format("Cache Misses:        %,d\n", cacheMisses));
        sb.append(String.format("Cache Hit Rate:      %.1f%%\n", getCacheHitRate()));
        sb.append("─".repeat(80)).append("\n");
        
        // Transaction stats
        sb.append("TRANSACTION STATISTICS\n");
        sb.append("─".repeat(80)).append("\n");
        sb.append(String.format("Commits:             %,d\n", transactionCommits));
        sb.append(String.format("Rollbacks:           %,d\n", transactionRollbacks));
        sb.append(String.format("Success Rate:        %.1f%%\n", getTransactionSuccessRate()));
        sb.append("─".repeat(80)).append("\n");
        
        // Connection pool
        sb.append("CONNECTION POOL\n");
        sb.append("─".repeat(80)).append("\n");
        PoolStats poolStats = getConnectionPoolStats();
        sb.append(String.format("Active Connections:  %d / %d\n", 
            poolStats.active, poolStats.max));
        sb.append(String.format("Pool Utilization:    %.1f%%\n", 
            poolStats.getUtilization()));
        sb.append("─".repeat(80)).append("\n");
        
        // Cache details
        sb.append("CACHE DETAILS\n");
        sb.append("─".repeat(80)).append("\n");
        CacheManager.CacheStats cacheStats = CacheManager.getInstance().getStats();
        sb.append(cacheStats.toString()).append("\n");
        sb.append("═".repeat(80)).append("\n");
        
        System.out.println(sb.toString());
        logger.info("Performance dashboard printed");
    }
    
    /**
     * Get summary as HTML for web display.
     */
    public String getHtmlSummary() {
        StringBuilder html = new StringBuilder();
        html.append("<html><body>");
        html.append("<h2>Performance Dashboard</h2>");
        html.append("<h3>Query Performance</h3>");
        html.append("<ul>");
        html.append(String.format("<li>Total Queries: %,d</li>", totalQueries));
        html.append(String.format("<li>Average Time: %.2f ms</li>", getAverageQueryTime()));
        html.append("</ul>");
        html.append("<h3>Cache Performance</h3>");
        html.append("<ul>");
        html.append(String.format("<li>Hit Rate: %.1f%%</li>", getCacheHitRate()));
        html.append("</ul>");
        html.append("<h3>Transactions</h3>");
        html.append("<ul>");
        html.append(String.format("<li>Success Rate: %.1f%%</li>", getTransactionSuccessRate()));
        html.append("</ul>");
        html.append("</body></html>");
        return html.toString();
    }
    
    /**
     * Reset all metrics.
     */
    public void reset() {
        totalQueries = 0;
        totalQueryTime = 0;
        cacheHits = 0;
        cacheMisses = 0;
        transactionCommits = 0;
        transactionRollbacks = 0;
        invoiceQueries = new QueryStats("Invoice");
        grnQueries = new QueryStats("GRN");
        stockQueries = new QueryStats("Stock");
        customerQueries = new QueryStats("Customer");
        logger.info("Performance metrics reset");
    }
    
    /**
     * Inner class for query statistics.
     */
    private static class QueryStats {
        String name;
        long count = 0;
        long totalTime = 0;
        
        QueryStats(String name) {
            this.name = name;
        }
        
        void addQuery(long milliseconds) {
            count++;
            totalTime += milliseconds;
        }
        
        double getAverage() {
            return count > 0 ? (double) totalTime / count : 0;
        }
    }
    
    /**
     * Inner class for connection pool statistics.
     */
    public static class PoolStats {
        int active;
        int max;
        
        PoolStats(int active, int max) {
            this.active = active;
            this.max = max;
        }
        
        double getUtilization() {
            return max > 0 ? (active * 100.0) / max : 0;
        }
    }
}
