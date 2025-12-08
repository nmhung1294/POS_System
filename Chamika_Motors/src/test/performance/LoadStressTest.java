package test.performance;

import org.junit.jupiter.api.*;

import repository.*;
import util.DBUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Load and Stress Testing for Test Lần 3.
 * Simulates 50 concurrent users performing realistic operations.
 * 
 * Concurrent User Simulation Strategy:
 * - Uses ExecutorService with fixed thread pool of 50 threads
 * - Each thread represents one concurrent user
 * - Users perform realistic operations: search, view, create
 * - Measures: throughput, response time, error rate, connection pool stress
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LoadStressTest extends PerformanceTestBase {

    private CustomerRepository customerRepository;
    private PaymentMethodRepository paymentMethodRepository;
    
    // Concurrent test metrics
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
    @DisplayName("Load Test: 50 Concurrent Users - Customer Search")
    void testConcurrentCustomerSearch() throws Exception {
        int numUsers = 50;
        int operationsPerUser = 10;
        
        logger.info("=".repeat(80));
        logger.info("LOAD TEST: 50 CONCURRENT USERS - CUSTOMER SEARCH");
        logger.info("=".repeat(80));
        logger.info(String.format("Configuration: %d users, %d operations per user", numUsers, operationsPerUser));
        
        // Create fixed thread pool with 50 threads (represents 50 concurrent users)
        ExecutorService executor = Executors.newFixedThreadPool(numUsers);
        CountDownLatch startLatch = new CountDownLatch(1); // Synchronize start
        CountDownLatch endLatch = new CountDownLatch(numUsers);
        
        long testStartTime = System.currentTimeMillis();
        
        // Submit 50 concurrent user tasks
        for (int i = 0; i < numUsers; i++) {
            final int userId = i;
            executor.submit(() -> {
                try {
                    // Wait for all threads to be ready
                    startLatch.await();
                    
                    // Each user performs multiple search operations
                    String[] searchPrefixes = {"A", "B", "S", "M", "K"};
                    for (int j = 0; j < operationsPerUser; j++) {
                        long opStart = System.nanoTime();
                        
                        String prefix = searchPrefixes[j % searchPrefixes.length];
                        List<?> results = customerRepository.searchByName(prefix + "%");
                        
                        long opEnd = System.nanoTime();
                        long responseTimeMs = (opEnd - opStart) / 1_000_000;
                        
                        totalResponseTime.addAndGet(responseTimeMs);
                        successCount.incrementAndGet();
                        
                        assertNotNull(results);
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    logger.severe(String.format("User %d failed: %s", userId, e.getMessage()));
                } finally {
                    endLatch.countDown();
                }
            });
        }
        
        // Start all threads simultaneously
        startLatch.countDown();
        
        // Wait for all to complete (max 30 seconds timeout)
        boolean completed = endLatch.await(30, TimeUnit.SECONDS);
        long testEndTime = System.currentTimeMillis();
        long totalTestTime = testEndTime - testStartTime;
        
        executor.shutdown();
        
        // Calculate metrics
        int totalOperations = successCount.get();
        double avgResponseTime = totalOperations > 0 ? 
            (double) totalResponseTime.get() / totalOperations : 0;
        double throughput = totalOperations > 0 ? 
            (totalOperations * 1000.0) / totalTestTime : 0;
        double errorRate = (failureCount.get() * 100.0) / (successCount.get() + failureCount.get());
        
        // Log results
        logger.info("-".repeat(80));
        logger.info("TEST RESULTS:");
        logger.info(String.format("Total operations: %d", totalOperations));
        logger.info(String.format("Successful: %d", successCount.get()));
        logger.info(String.format("Failed: %d", failureCount.get()));
        logger.info(String.format("Total time: %d ms", totalTestTime));
        logger.info(String.format("Average response time: %.2f ms", avgResponseTime));
        logger.info(String.format("Throughput: %.2f operations/second", throughput));
        logger.info(String.format("Error rate: %.2f%%", errorRate));
        logger.info("=".repeat(80));
        
        // Assertions
        assertTrue(completed, "All operations should complete within timeout");
        assertEquals(numUsers * operationsPerUser, successCount.get(), 
            "All operations should succeed");
        assertEquals(0, failureCount.get(), "Should have zero failures");
        assertTrue(avgResponseTime < 100, 
            String.format("Avg response time should be < 100ms, actual: %.2f ms", avgResponseTime));
        assertTrue(errorRate == 0.0, "Error rate should be 0%");
    }
    
    @Test
    @Order(2)
    @DisplayName("Load Test: 50 Concurrent Users - Mixed Operations")
    void testConcurrentMixedOperations() throws Exception {
        int numUsers = 50;
        int operationsPerUser = 5;
        
        logger.info("=".repeat(80));
        logger.info("LOAD TEST: 50 CONCURRENT USERS - MIXED OPERATIONS");
        logger.info("=".repeat(80));
        logger.info("Operations: Customer search, Payment methods load");
        
        ExecutorService executor = Executors.newFixedThreadPool(numUsers);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numUsers);
        
        long testStartTime = System.currentTimeMillis();
        
        for (int i = 0; i < numUsers; i++) {
            final int userId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    for (int j = 0; j < operationsPerUser; j++) {
                        long opStart = System.nanoTime();
                        
                        // Simulate realistic user behavior: mix of operations
                        switch (j % 2) {
                            case 0:
                                // Search customer
                                customerRepository.searchByName("A%");
                                break;
                            case 1:
                                // Load payment methods (cached)
                                paymentMethodRepository.findAllPaymentMethods();
                                break;
                        }
                        
                        long opEnd = System.nanoTime();
                        long responseTimeMs = (opEnd - opStart) / 1_000_000;
                        
                        totalResponseTime.addAndGet(responseTimeMs);
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    logger.severe(String.format("User %d failed: %s", userId, e.getMessage()));
                } finally {
                    endLatch.countDown();
                }
            });
        }
        
        startLatch.countDown();
        boolean completed = endLatch.await(30, TimeUnit.SECONDS);
        long testEndTime = System.currentTimeMillis();
        long totalTestTime = testEndTime - testStartTime;
        
        executor.shutdown();
        
        // Calculate metrics
        int totalOperations = successCount.get();
        double avgResponseTime = totalOperations > 0 ? 
            (double) totalResponseTime.get() / totalOperations : 0;
        double throughput = totalOperations > 0 ? 
            (totalOperations * 1000.0) / totalTestTime : 0;
        double errorRate = (failureCount.get() * 100.0) / (successCount.get() + failureCount.get());
        
        logger.info("-".repeat(80));
        logger.info("MIXED OPERATIONS RESULTS:");
        logger.info(String.format("Total operations: %d", totalOperations));
        logger.info(String.format("Successful: %d", successCount.get()));
        logger.info(String.format("Failed: %d", failureCount.get()));
        logger.info(String.format("Total time: %d ms", totalTestTime));
        logger.info(String.format("Average response time: %.2f ms", avgResponseTime));
        logger.info(String.format("Throughput: %.2f operations/second", throughput));
        logger.info(String.format("Error rate: %.2f%%", errorRate));
        logger.info("=".repeat(80));
        
        // Assertions
        assertTrue(completed, "All operations should complete within timeout");
        assertTrue(successCount.get() >= numUsers * operationsPerUser * 0.95, 
            "At least 95% operations should succeed");
        assertTrue(avgResponseTime < 150, 
            String.format("Avg response time should be < 150ms, actual: %.2f ms", avgResponseTime));
        assertTrue(errorRate < 5.0, 
            String.format("Error rate should be < 5%%, actual: %.2f%%", errorRate));
    }
    
    @Test
    @Order(3)
    @DisplayName("Stress Test: Connection Pool Under Load")
    void testConnectionPoolStress() throws Exception {
        int numConcurrentConnections = 50;
        
        logger.info("=".repeat(80));
        logger.info("STRESS TEST: CONNECTION POOL (50 CONCURRENT CONNECTIONS)");
        logger.info("=".repeat(80));
        logger.info("Testing HikariCP pool with 50 simultaneous connections");
        
        ExecutorService executor = Executors.newFixedThreadPool(numConcurrentConnections);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numConcurrentConnections);
        
        List<Future<Long>> futures = new ArrayList<>();
        
        long testStartTime = System.currentTimeMillis();
        
        // Request 50 connections simultaneously
        for (int i = 0; i < numConcurrentConnections; i++) {
            Future<Long> future = executor.submit(() -> {
                long acquireTime = 0;
                try {
                    startLatch.await();
                    
                    long start = System.nanoTime();
                    Connection conn = DBUtil.getConnection();
                    long end = System.nanoTime();
                    acquireTime = (end - start) / 1_000_000; // ms
                    
                    // Hold connection briefly and perform a query
                    var stmt = conn.createStatement();
                    var rs = stmt.executeQuery("SELECT 1");
                    assertTrue(rs.next());
                    rs.close();
                    stmt.close();
                    conn.close();
                    
                    successCount.incrementAndGet();
                    return acquireTime;
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    logger.severe("Connection failed: " + e.getMessage());
                    return acquireTime;
                } finally {
                    endLatch.countDown();
                }
            });
            futures.add(future);
        }
        
        startLatch.countDown();
        boolean completed = endLatch.await(10, TimeUnit.SECONDS);
        long testEndTime = System.currentTimeMillis();
        long totalTestTime = testEndTime - testStartTime;
        
        executor.shutdown();
        
        // Analyze connection acquisition times
        long maxAcquireTime = 0;
        long totalAcquireTime = 0;
        int successfulAcquisitions = 0;
        
        for (Future<Long> future : futures) {
            try {
                Long acquireTime = future.get(1, TimeUnit.SECONDS);
                if (acquireTime > 0) {
                    totalAcquireTime += acquireTime;
                    maxAcquireTime = Math.max(maxAcquireTime, acquireTime);
                    successfulAcquisitions++;
                }
            } catch (Exception e) {
                // Skip failed acquisitions
            }
        }
        
        double avgAcquireTime = successfulAcquisitions > 0 ? 
            (double) totalAcquireTime / successfulAcquisitions : 0;
        
        logger.info("-".repeat(80));
        logger.info("CONNECTION POOL STRESS RESULTS:");
        logger.info(String.format("Concurrent connection requests: %d", numConcurrentConnections));
        logger.info(String.format("Successful: %d", successCount.get()));
        logger.info(String.format("Failed: %d", failureCount.get()));
        logger.info(String.format("Total time: %d ms", totalTestTime));
        logger.info(String.format("Avg connection acquire time: %.2f ms", avgAcquireTime));
        logger.info(String.format("Max connection acquire time: %d ms", maxAcquireTime));
        logger.info("=".repeat(80));
        
        // Assertions
        assertTrue(completed, "All connections should be acquired within timeout");
        assertEquals(numConcurrentConnections, successCount.get(), 
            "All 50 concurrent connections should succeed");
        assertEquals(0, failureCount.get(), "Should have zero connection failures");
        assertTrue(avgAcquireTime < 50, 
            String.format("Avg acquire time should be < 50ms, actual: %.2f ms", avgAcquireTime));
        assertTrue(maxAcquireTime < 200, 
            String.format("Max acquire time should be < 200ms, actual: %d ms", maxAcquireTime));
    }
    
    @Test
    @Order(4)
    @DisplayName("Stress Test: Sustained Load (30 seconds)")
    void testSustainedLoad() throws Exception {
        int numUsers = 50;
        int durationSeconds = 30;
        
        logger.info("=".repeat(80));
        logger.info("STRESS TEST: SUSTAINED LOAD");
        logger.info("=".repeat(80));
        logger.info(String.format("Configuration: %d concurrent users, %d seconds duration", 
            numUsers, durationSeconds));
        
        ExecutorService executor = Executors.newFixedThreadPool(numUsers);
        AtomicInteger activeUsers = new AtomicInteger(0);
        final AtomicInteger testRunning = new AtomicInteger(1);
        
        long testStartTime = System.currentTimeMillis();
        long testEndTimeTarget = testStartTime + (durationSeconds * 1000);
        
        // Start concurrent users
        for (int i = 0; i < numUsers; i++) {
            final int userId = i;
            executor.submit(() -> {
                activeUsers.incrementAndGet();
                try {
                    while (testRunning.get() == 1 && System.currentTimeMillis() < testEndTimeTarget) {
                        try {
                            long opStart = System.nanoTime();
                            
                            // Perform random operation
                            int operation = ThreadLocalRandom.current().nextInt(2);
                            switch (operation) {
                                case 0:
                                    customerRepository.searchByName("A%");
                                    break;
                                case 1:
                                    paymentMethodRepository.findAllPaymentMethods();
                                    break;
                            }
                            
                            long opEnd = System.nanoTime();
                            long responseTimeMs = (opEnd - opStart) / 1_000_000;
                            
                            totalResponseTime.addAndGet(responseTimeMs);
                            successCount.incrementAndGet();
                            
                            // Small delay to simulate user think time
                            Thread.sleep(ThreadLocalRandom.current().nextInt(50, 200));
                            
                        } catch (Exception e) {
                            failureCount.incrementAndGet();
                        }
                    }
                } finally {
                    activeUsers.decrementAndGet();
                }
            });
        }
        
        // Monitor progress every 5 seconds
        int intervals = durationSeconds / 5;
        for (int i = 0; i < intervals; i++) {
            Thread.sleep(5000);
            logger.info(String.format("Progress: %d/%d seconds | Operations: %d | Active users: %d", 
                (i + 1) * 5, durationSeconds, successCount.get(), activeUsers.get()));
        }
        
        // Stop test
        testRunning.set(0);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        
        long actualTestTime = System.currentTimeMillis() - testStartTime;
        
        // Calculate metrics
        int totalOperations = successCount.get();
        double avgResponseTime = totalOperations > 0 ? 
            (double) totalResponseTime.get() / totalOperations : 0;
        double throughput = totalOperations > 0 ? 
            (totalOperations * 1000.0) / actualTestTime : 0;
        double errorRate = (failureCount.get() * 100.0) / (successCount.get() + failureCount.get());
        
        logger.info("-".repeat(80));
        logger.info("SUSTAINED LOAD RESULTS:");
        logger.info(String.format("Test duration: %d ms (%.1f seconds)", actualTestTime, actualTestTime / 1000.0));
        logger.info(String.format("Total operations: %d", totalOperations));
        logger.info(String.format("Successful: %d", successCount.get()));
        logger.info(String.format("Failed: %d", failureCount.get()));
        logger.info(String.format("Average response time: %.2f ms", avgResponseTime));
        logger.info(String.format("Throughput: %.2f operations/second", throughput));
        logger.info(String.format("Error rate: %.2f%%", errorRate));
        logger.info("=".repeat(80));
        
        // Assertions
        assertTrue(totalOperations > 0, "Should complete many operations");
        assertTrue(errorRate < 5.0, 
            String.format("Error rate should be < 5%%, actual: %.2f%%", errorRate));
        assertTrue(avgResponseTime < 200, 
            String.format("Avg response time should be < 200ms under sustained load, actual: %.2f ms", 
                avgResponseTime));
    }
}
