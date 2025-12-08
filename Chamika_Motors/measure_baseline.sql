-- ================================================================
-- BASELINE PERFORMANCE MEASUREMENT
-- Run this BEFORE applying optimizations
-- ================================================================

USE chamika_motors;

SET @start_time = NOW();

-- ================================================================
-- 1. CHECK CURRENT INDICES
-- ================================================================
SELECT 
    '1. CURRENT INDICES' as test_section,
    TABLE_NAME, 
    INDEX_NAME, 
    COLUMN_NAME,
    SEQ_IN_INDEX
FROM information_schema.STATISTICS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME IN ('invoice', 'grn', 'stock', 'customer', 'attendance')
ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;

-- ================================================================
-- 2. BASELINE QUERY PERFORMANCE (BEFORE OPTIMIZATION)
-- ================================================================

-- Test 1: Invoice Date Range (Last 30 days)
SELECT '2.1 Invoice Date Range Query' as test_name;
SET @test_start = NOW(6);
SELECT COUNT(*), SUM(paid_amount) 
FROM invoice 
WHERE date_time >= DATE_SUB(CURDATE(), INTERVAL 30 DAY);
SET @test_end = NOW(6);
SELECT TIMESTAMPDIFF(MICROSECOND, @test_start, @test_end) / 1000 as execution_time_ms;

-- Test 2: Customer Search by Name
SELECT '2.2 Customer Search Query' as test_name;
SET @test_start = NOW(6);
SELECT mobile, name, points 
FROM customer 
WHERE name LIKE 'S%' 
ORDER BY name ASC 
LIMIT 10;
SET @test_end = NOW(6);
SELECT TIMESTAMPDIFF(MICROSECOND, @test_start, @test_end) / 1000 as execution_time_ms;

-- Test 3: Stock Lookup by Product and Price
SELECT '2.3 Stock Lookup Query' as test_name;
SET @test_start = NOW(6);
SELECT id, qty 
FROM stock 
WHERE product_id = (SELECT id FROM product LIMIT 1) 
  AND selling_price > 0 
LIMIT 1;
SET @test_end = NOW(6);
SELECT TIMESTAMPDIFF(MICROSECOND, @test_start, @test_end) / 1000 as execution_time_ms;

-- Test 4: GRN Date Range
SELECT '2.4 GRN Date Range Query' as test_name;
SET @test_start = NOW(6);
SELECT COUNT(*), SUM(paid_amount) 
FROM grn 
WHERE date_time >= DATE_SUB(CURDATE(), INTERVAL 30 DAY);
SET @test_end = NOW(6);
SELECT TIMESTAMPDIFF(MICROSECOND, @test_start, @test_end) / 1000 as execution_time_ms;

-- Test 5: Attendance by Employee and Date
SELECT '2.5 Attendance Query' as test_name;
SET @test_start = NOW(6);
SELECT id, in_time, out_time 
FROM attendance 
WHERE employee_id = (SELECT id FROM employee LIMIT 1)
  AND date >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)
ORDER BY date DESC;
SET @test_end = NOW(6);
SELECT TIMESTAMPDIFF(MICROSECOND, @test_start, @test_end) / 1000 as execution_time_ms;

-- ================================================================
-- 3. EXPLAIN ANALYSIS (Query Execution Plans)
-- ================================================================

SELECT '3. QUERY EXECUTION PLANS (BEFORE OPTIMIZATION)' as test_section;

-- Invoice date range EXPLAIN
EXPLAIN FORMAT=JSON
SELECT COUNT(*), SUM(paid_amount) 
FROM invoice 
WHERE date_time >= DATE_SUB(CURDATE(), INTERVAL 30 DAY);

-- Customer search EXPLAIN
EXPLAIN FORMAT=JSON
SELECT mobile, name, points 
FROM customer 
WHERE name LIKE 'S%' 
ORDER BY name ASC 
LIMIT 10;

-- Stock lookup EXPLAIN
EXPLAIN FORMAT=JSON
SELECT id, qty 
FROM stock 
WHERE product_id = 1 AND selling_price > 0 
LIMIT 1;

-- ================================================================
-- 4. TABLE STATISTICS
-- ================================================================

SELECT 
    '4. TABLE STATISTICS' as test_section,
    TABLE_NAME,
    TABLE_ROWS as estimated_rows,
    ROUND((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024, 2) as size_mb,
    ROUND(DATA_LENGTH / 1024 / 1024, 2) as data_mb,
    ROUND(INDEX_LENGTH / 1024 / 1024, 2) as index_mb
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('invoice', 'grn', 'stock', 'customer', 'attendance', 'invoice_item', 'grn_item')
ORDER BY TABLE_ROWS DESC;

-- ================================================================
-- 5. CONNECTION POOL TEST
-- ================================================================

SELECT 
    '5. CURRENT CONNECTION INFO' as test_section,
    COUNT(*) as active_connections,
    MAX(TIME) as longest_query_seconds
FROM information_schema.PROCESSLIST 
WHERE DB = DATABASE();

-- Show max connections setting
SHOW VARIABLES LIKE 'max_connections';

-- ================================================================
-- BASELINE MEASUREMENT COMPLETE
-- ================================================================

SET @end_time = NOW();
SELECT 
    'BASELINE MEASUREMENT COMPLETED' as status,
    TIMESTAMPDIFF(SECOND, @start_time, @end_time) as total_time_seconds;

-- SAVE RESULTS TO TABLE FOR COMPARISON
CREATE TABLE IF NOT EXISTS performance_baseline (
    id INT AUTO_INCREMENT PRIMARY KEY,
    test_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    test_name VARCHAR(100),
    execution_time_ms DECIMAL(10,2),
    notes TEXT
);

-- Insert baseline results (adjust values based on your actual results)
-- Run this after manually recording the times above
-- INSERT INTO performance_baseline (test_name, execution_time_ms, notes) VALUES
-- ('Invoice Date Range (BEFORE)', XXX, 'No indices, full table scan'),
-- ('Customer Search (BEFORE)', XXX, 'No index on name column'),
-- ('Stock Lookup (BEFORE)', XXX, 'No composite index'),
-- ('GRN Date Range (BEFORE)', XXX, 'No indices'),
-- ('Attendance Query (BEFORE)', XXX, 'No composite index');

SELECT 'Save the execution times above to compare after optimization!' as reminder;
