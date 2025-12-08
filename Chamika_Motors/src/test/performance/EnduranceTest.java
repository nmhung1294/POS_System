package test.performance;

import org.junit.jupiter.api.*;
import repository.*;
import util.DBUtil;

import java.sql.Connection;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Endurance Testing for Test Lần 4.
 * Tests system stability over extended periods (1-24 hours).
 * 
 * Purpose:
 * - Detect memory leaks
 * - Verify no performance degradation over time
 * - Test connection pool resilience
 * - Monitor JVM health
 * 
 * NOTE: Full 24h test should be run in production-like environment.
 * This implementation runs shorter tests for demonstration.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EnduranceTest extends PerformanceTestBase {

    private CustomerRepository customerRepository;
    private PaymentMethodRepository paymentMethodRepository;
    
    // Metrics
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicLong totalResponseTime = new AtomicLong(0);
    
    @BeforeEach
    void setUp() {
        customerRepository = new CustomerRepositoryImpl();
        paymentMethodRepository = new PaymentMethodRepositoryImpl();
        
        // Reset metrics
        successCount.set(0);
        failureCount.set(0);
        totalResponseTime.set(0);
    }
    
    @Test
    @Order(1)
    @DisplayName("Endurance Test: 1 Minute Continuous Load")
    void testOneMinuteEndurance() throws Exception {
        int numUsers = 20;
        int durationSeconds = 60;
        
        logger.info("=".repeat(80));
        logger.info("ENDURANCE TEST: 1 MINUTE CONTINUOUS LOAD");
        logger.info("=".repeat(80));
        logger.info(String.format("Configuration: %d users, %d seconds duration", numUsers, durationSeconds));
        logger.info("Purpose: Quick endurance check for CI/CD pipeline");
        
        long startTime = System.currentTimeMillis();
        long memoryBefore = getUsedMemory();
        
        runEnduranceTest(numUsers, durationSeconds, 10); // Report every 10 seconds
        
        long endTime = System.currentTimeMillis();
        long memoryAfter = getUsedMemory();
        long duration = endTime - startTime;
        
        // Calculate metrics
        int totalOps = successCount.get();
        double avgResponseTime = totalOps > 0 ? (double) totalResponseTime.get() / totalOps : 0;
        double throughput = totalOps > 0 ? (totalOps * 1000.0) / duration : 0;
        double errorRate = (failureCount.get() * 100.0) / (successCount.get() + failureCount.get());
        long memoryGrowth = memoryAfter - memoryBefore;
        
        logger.info("-".repeat(80));
        logger.info("1-MINUTE ENDURANCE RESULTS:");
        logger.info(String.format("Duration: %d ms (%.1f seconds)", duration, duration / 1000.0));
        logger.info(String.format("Total operations: %d", totalOps));
        logger.info(String.format("Successful: %d", successCount.get()));
        logger.info(String.format("Failed: %d", failureCount.get()));
        logger.info(String.format("Average response time: %.2f ms", avgResponseTime));
        logger.info(String.format("Throughput: %.2f operations/second", throughput));
        logger.info(String.format("Error rate: %.2f%%", errorRate));
        logger.info(String.format("Memory before: %.2f MB", memoryBefore / 1024.0 / 1024.0));
        logger.info(String.format("Memory after: %.2f MB", memoryAfter / 1024.0 / 1024.0));
        logger.info(String.format("Memory growth: %.2f MB", memoryGrowth / 1024.0 / 1024.0));
        logger.info("=".repeat(80));
        
        // Assertions
        assertTrue(totalOps > 500, "Should complete many operations in 1 minute");
        assertTrue(errorRate < 1.0, String.format("Error rate should be < 1%%, actual: %.2f%%", errorRate));
        assertTrue(avgResponseTime < 100, 
            String.format("Avg response should be < 100ms, actual: %.2f ms", avgResponseTime));
        assertTrue(memoryGrowth < 100 * 1024 * 1024, 
            String.format("Memory growth should be < 100MB, actual: %.2f MB", memoryGrowth / 1024.0 / 1024.0));
    }
    
    @Test
    @Order(2)
    @DisplayName("Endurance Test: 5 Minutes Continuous Load")
    void testFiveMinuteEndurance() throws Exception {
        int numUsers = 20;
        int durationSeconds = 300; // 5 minutes
        
        logger.info("=".repeat(80));
        logger.info("ENDURANCE TEST: 5 MINUTES CONTINUOUS LOAD");
        logger.info("=".repeat(80));
        logger.info(String.format("Configuration: %d users, %d seconds duration", numUsers, durationSeconds));
        logger.info("Purpose: Detect early memory leaks and connection issues");
        
        long startTime = System.currentTimeMillis();
        long memoryBefore = getUsedMemory();
        
        runEnduranceTest(numUsers, durationSeconds, 30); // Report every 30 seconds
        
        long endTime = System.currentTimeMillis();
        long memoryAfter = getUsedMemory();
        long duration = endTime - startTime;
        
        // Calculate metrics
        int totalOps = successCount.get();
        double avgResponseTime = totalOps > 0 ? (double) totalResponseTime.get() / totalOps : 0;
        double throughput = totalOps > 0 ? (totalOps * 1000.0) / duration : 0;
        double errorRate = (failureCount.get() * 100.0) / (successCount.get() + failureCount.get());
        long memoryGrowth = memoryAfter - memoryBefore;
        
        logger.info("-".repeat(80));
        logger.info("5-MINUTE ENDURANCE RESULTS:");
        logger.info(String.format("Duration: %d ms (%.1f minutes)", duration, duration / 60000.0));
        logger.info(String.format("Total operations: %d", totalOps));
        logger.info(String.format("Successful: %d", successCount.get()));
        logger.info(String.format("Failed: %d", failureCount.get()));
        logger.info(String.format("Average response time: %.2f ms", avgResponseTime));
        logger.info(String.format("Throughput: %.2f operations/second", throughput));
        logger.info(String.format("Error rate: %.2f%%", errorRate));
        logger.info(String.format("Memory before: %.2f MB", memoryBefore / 1024.0 / 1024.0));
        logger.info(String.format("Memory after: %.2f MB", memoryAfter / 1024.0 / 1024.0));
        logger.info(String.format("Memory growth: %.2f MB", memoryGrowth / 1024.0 / 1024.0));
        logger.info("=".repeat(80));
        
        // Assertions
        assertTrue(totalOps > 2500, "Should complete many operations in 5 minutes");
        assertTrue(errorRate < 1.0, String.format("Error rate should be < 1%%, actual: %.2f%%", errorRate));
        assertTrue(avgResponseTime < 100, 
            String.format("Avg response should be < 100ms, actual: %.2f ms", avgResponseTime));
        assertTrue(memoryGrowth < 200 * 1024 * 1024, 
            String.format("Memory growth should be < 200MB, actual: %.2f MB", memoryGrowth / 1024.0 / 1024.0));
    }
    
    @Test
    @Order(3)
    @DisplayName("Memory Leak Detection Test")
    void testMemoryLeakDetection() throws Exception {
        logger.info("=".repeat(80));
        logger.info("MEMORY LEAK DETECTION TEST");
        logger.info("=".repeat(80));
        logger.info("Running 5 cycles of load to detect memory leaks");
        
        int cyclesCount = 5;
        int operationsPerCycle = 1000;
        long[] memorySnapshots = new long[cyclesCount + 1];
        
        // Initial memory
        System.gc();
        Thread.sleep(1000);
        memorySnapshots[0] = getUsedMemory();
        logger.info(String.format("Initial memory: %.2f MB", memorySnapshots[0] / 1024.0 / 1024.0));
        
        // Run cycles
        for (int cycle = 0; cycle < cyclesCount; cycle++) {
            logger.info(String.format("\nCycle %d/%d - Performing %d operations...", 
                cycle + 1, cyclesCount, operationsPerCycle));
            
            // Perform operations
            for (int i = 0; i < operationsPerCycle; i++) {
                try {
                    if (i % 2 == 0) {
                        customerRepository.searchByName("A%");
                    } else {
                        paymentMethodRepository.findAllPaymentMethods();
                    }
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                }
            }
            
            // Force GC and measure memory
            System.gc();
            Thread.sleep(1000);
            memorySnapshots[cycle + 1] = getUsedMemory();
            
            long growth = memorySnapshots[cycle + 1] - memorySnapshots[cycle];
            logger.info(String.format("After cycle %d: %.2f MB (growth: %.2f MB)", 
                cycle + 1, 
                memorySnapshots[cycle + 1] / 1024.0 / 1024.0,
                growth / 1024.0 / 1024.0));
        }
        
        // Analyze memory growth pattern
        long totalGrowth = memorySnapshots[cyclesCount] - memorySnapshots[0];
        double avgGrowthPerCycle = totalGrowth / (double) cyclesCount;
        
        logger.info("-".repeat(80));
        logger.info("MEMORY LEAK ANALYSIS:");
        logger.info(String.format("Initial memory: %.2f MB", memorySnapshots[0] / 1024.0 / 1024.0));
        logger.info(String.format("Final memory: %.2f MB", memorySnapshots[cyclesCount] / 1024.0 / 1024.0));
        logger.info(String.format("Total growth: %.2f MB", totalGrowth / 1024.0 / 1024.0));
        logger.info(String.format("Average growth per cycle: %.2f MB", avgGrowthPerCycle / 1024.0 / 1024.0));
        logger.info(String.format("Total operations: %d", successCount.get()));
        logger.info("=".repeat(80));
        
        // Assertions
        assertTrue(totalGrowth < 50 * 1024 * 1024, 
            String.format("Total memory growth should be < 50MB, actual: %.2f MB", 
                totalGrowth / 1024.0 / 1024.0));
        assertTrue(avgGrowthPerCycle < 15 * 1024 * 1024, 
            String.format("Avg growth per cycle should be < 15MB, actual: %.2f MB", 
                avgGrowthPerCycle / 1024.0 / 1024.0));
    }
    
    @Test
    @Order(4)
    @DisplayName("Connection Pool Endurance Test")
    void testConnectionPoolEndurance() throws Exception {
        logger.info("=".repeat(80));
        logger.info("CONNECTION POOL ENDURANCE TEST");
        logger.info("=".repeat(80));
        logger.info("Testing connection pool stability over 2 minutes");
        
        int numThreads = 30;
        int durationSeconds = 120; // 2 minutes
        AtomicInteger connectionAcquisitions = new AtomicInteger(0);
        AtomicInteger connectionFailures = new AtomicInteger(0);
        AtomicLong totalAcquireTime = new AtomicLong(0);
        
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        final AtomicInteger running = new AtomicInteger(1);
        
        long startTime = System.currentTimeMillis();
        long endTime = startTime + (durationSeconds * 1000);
        
        // Start worker threads
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                while (running.get() == 1 && System.currentTimeMillis() < endTime) {
                    try {
                        long acquireStart = System.nanoTime();
                        Connection conn = DBUtil.getConnection();
                        long acquireEnd = System.nanoTime();
                        long acquireTimeMs = (acquireEnd - acquireStart) / 1_000_000;
                        
                        totalAcquireTime.addAndGet(acquireTimeMs);
                        connectionAcquisitions.incrementAndGet();
                        
                        // Perform quick query
                        var stmt = conn.createStatement();
                        var rs = stmt.executeQuery("SELECT 1");
                        rs.next();
                        rs.close();
                        stmt.close();
                        conn.close();
                        
                        // Small delay
                        Thread.sleep(ThreadLocalRandom.current().nextInt(10, 50));
                        
                    } catch (Exception e) {
                        connectionFailures.incrementAndGet();
                        logger.warning("Connection acquisition failed: " + e.getMessage());
                    }
                }
            });
        }
        
        // Monitor every 20 seconds
        for (int i = 0; i < durationSeconds / 20; i++) {
            Thread.sleep(20000);
            logger.info(String.format("Progress: %d/%d seconds | Acquisitions: %d | Failures: %d", 
                (i + 1) * 20, durationSeconds, 
                connectionAcquisitions.get(), connectionFailures.get()));
        }
        
        running.set(0);
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        
        long actualDuration = System.currentTimeMillis() - startTime;
        int totalAcquisitions = connectionAcquisitions.get();
        double avgAcquireTime = totalAcquisitions > 0 ? 
            (double) totalAcquireTime.get() / totalAcquisitions : 0;
        double failureRate = (connectionFailures.get() * 100.0) / 
            (connectionAcquisitions.get() + connectionFailures.get());
        
        logger.info("-".repeat(80));
        logger.info("CONNECTION POOL ENDURANCE RESULTS:");
        logger.info(String.format("Test duration: %.1f seconds", actualDuration / 1000.0));
        logger.info(String.format("Total acquisitions: %d", totalAcquisitions));
        logger.info(String.format("Successful: %d", connectionAcquisitions.get()));
        logger.info(String.format("Failed: %d", connectionFailures.get()));
        logger.info(String.format("Average acquire time: %.2f ms", avgAcquireTime));
        logger.info(String.format("Failure rate: %.2f%%", failureRate));
        logger.info("=".repeat(80));
        
        // Assertions
        assertTrue(totalAcquisitions > 1000, "Should handle many connections");
        assertTrue(failureRate < 1.0, 
            String.format("Failure rate should be < 1%%, actual: %.2f%%", failureRate));
        assertTrue(avgAcquireTime < 50, 
            String.format("Avg acquire time should be < 50ms, actual: %.2f ms", avgAcquireTime));
    }
    
    /**
     * Helper method to run endurance test with specified parameters.
     */
    private void runEnduranceTest(int numUsers, int durationSeconds, int reportIntervalSeconds) 
            throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(numUsers);
        final AtomicInteger running = new AtomicInteger(1);
        
        long startTime = System.currentTimeMillis();
        long endTime = startTime + (durationSeconds * 1000);
        
        // Start users
        for (int i = 0; i < numUsers; i++) {
            executor.submit(() -> {
                while (running.get() == 1 && System.currentTimeMillis() < endTime) {
                    try {
                        long opStart = System.nanoTime();
                        
                        // Random operation
                        if (ThreadLocalRandom.current().nextBoolean()) {
                            customerRepository.searchByName("A%");
                        } else {
                            paymentMethodRepository.findAllPaymentMethods();
                        }
                        
                        long opEnd = System.nanoTime();
                        totalResponseTime.addAndGet((opEnd - opStart) / 1_000_000);
                        successCount.incrementAndGet();
                        
                        // User think time
                        Thread.sleep(ThreadLocalRandom.current().nextInt(50, 200));
                        
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                    }
                }
            });
        }
        
        // Monitor progress
        int intervals = durationSeconds / reportIntervalSeconds;
        for (int i = 0; i < intervals; i++) {
            Thread.sleep(reportIntervalSeconds * 1000);
            logger.info(String.format("Progress: %d/%d seconds | Operations: %d | Success: %d | Failed: %d", 
                (i + 1) * reportIntervalSeconds, durationSeconds, 
                successCount.get() + failureCount.get(),
                successCount.get(), failureCount.get()));
        }
        
        running.set(0);
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
    }
    
    /**
     * Get current used memory in bytes.
     */
    private long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
}
