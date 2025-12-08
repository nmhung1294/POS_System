-- ================================================================
-- AFTER OPTIMIZATION PERFORMANCE MEASUREMENT
-- Run this AFTER applying optimizations
-- ================================================================

USE chamika_motors;

SET @start_time = NOW();

-- ================================================================
-- 1. VERIFY INDICES ARE CREATED
-- ================================================================
SELECT 
    '1. VERIFY INDICES (AFTER OPTIMIZATION)' as test_section,
    TABLE_NAME, 
    INDEX_NAME, 
    COLUMN_NAME,
    SEQ_IN_INDEX
FROM information_schema.STATISTICS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME IN ('invoice', 'grn', 'stock', 'customer', 'attendance')
ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;

-- Count indices per table
SELECT 
    TABLE_NAME,
    COUNT(DISTINCT INDEX_NAME) as index_count
FROM information_schema.STATISTICS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME IN ('invoice', 'grn', 'stock', 'customer', 'attendance')
GROUP BY TABLE_NAME;

-- ================================================================
-- 2. PERFORMANCE MEASUREMENT (AFTER OPTIMIZATION)
-- ================================================================

-- Test 1: Invoice Date Range (Last 30 days)
SELECT '2.1 Invoice Date Range Query (AFTER)' as test_name;
SET @test_start = NOW(6);
SELECT COUNT(*), SUM(paid_amount) 
FROM invoice 
WHERE date_time >= DATE_SUB(CURDATE(), INTERVAL 30 DAY);
SET @test_end = NOW(6);
SELECT TIMESTAMPDIFF(MICROSECOND, @test_start, @test_end) / 1000 as execution_time_ms;

-- Test 2: Customer Search by Name
SELECT '2.2 Customer Search Query (AFTER)' as test_name;
SET @test_start = NOW(6);
SELECT mobile, name, points 
FROM customer 
WHERE name LIKE 'S%' 
ORDER BY name ASC 
LIMIT 10;
SET @test_end = NOW(6);
SELECT TIMESTAMPDIFF(MICROSECOND, @test_start, @test_end) / 1000 as execution_time_ms;

-- Test 3: Stock Lookup by Product and Price
SELECT '2.3 Stock Lookup Query (AFTER)' as test_name;
SET @test_start = NOW(6);
SELECT id, qty 
FROM stock 
WHERE product_id = (SELECT id FROM product LIMIT 1) 
  AND selling_price > 0 
LIMIT 1;
SET @test_end = NOW(6);
SELECT TIMESTAMPDIFF(MICROSECOND, @test_start, @test_end) / 1000 as execution_time_ms;

-- Test 4: GRN Date Range
SELECT '2.4 GRN Date Range Query (AFTER)' as test_name;
SET @test_start = NOW(6);
SELECT COUNT(*), SUM(paid_amount) 
FROM grn 
WHERE date_time >= DATE_SUB(CURDATE(), INTERVAL 30 DAY);
SET @test_end = NOW(6);
SELECT TIMESTAMPDIFF(MICROSECOND, @test_start, @test_end) / 1000 as execution_time_ms;

-- Test 5: Attendance by Employee and Date
SELECT '2.5 Attendance Query (AFTER)' as test_name;
SET @test_start = NOW(6);
SELECT id, in_time, out_time 
FROM attendance 
WHERE employee_id = (SELECT id FROM employee LIMIT 1)
  AND date >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)
ORDER BY date DESC;
SET @test_end = NOW(6);
SELECT TIMESTAMPDIFF(MICROSECOND, @test_start, @test_end) / 1000 as execution_time_ms;

-- ================================================================
-- 3. EXPLAIN ANALYSIS (Query Execution Plans AFTER)
-- ================================================================

SELECT '3. QUERY EXECUTION PLANS (AFTER OPTIMIZATION)' as test_section;

-- Invoice date range EXPLAIN (should use idx_invoice_date_time)
EXPLAIN FORMAT=JSON
SELECT COUNT(*), SUM(paid_amount) 
FROM invoice 
WHERE date_time >= DATE_SUB(CURDATE(), INTERVAL 30 DAY);

-- Customer search EXPLAIN (should use idx_customer_name)
EXPLAIN FORMAT=JSON
SELECT mobile, name, points 
FROM customer 
WHERE name LIKE 'S%' 
ORDER BY name ASC 
LIMIT 10;

-- Stock lookup EXPLAIN (should use idx_stock_product_price)
EXPLAIN FORMAT=JSON
SELECT id, qty 
FROM stock 
WHERE product_id = 1 AND selling_price > 0 
LIMIT 1;

-- ================================================================
-- 4. INDEX USAGE ANALYSIS
-- ================================================================

SELECT 
    '4. INDEX USAGE ANALYSIS' as test_section,
    TABLE_NAME,
    INDEX_NAME,
    CARDINALITY,
    INDEX_TYPE
FROM information_schema.STATISTICS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND INDEX_NAME LIKE 'idx_%'
ORDER BY TABLE_NAME, INDEX_NAME;

-- ================================================================
-- 5. PERFORMANCE COMPARISON
-- ================================================================

-- Insert after optimization results
INSERT INTO performance_baseline (test_name, execution_time_ms, notes) VALUES
('Invoice Date Range (AFTER)', 0, 'With idx_invoice_date_time - UPDATE THIS VALUE'),
('Customer Search (AFTER)', 0, 'With idx_customer_name - UPDATE THIS VALUE'),
('Stock Lookup (AFTER)', 0, 'With idx_stock_product_price - UPDATE THIS VALUE'),
('GRN Date Range (AFTER)', 0, 'With idx_grn_date_time - UPDATE THIS VALUE'),
('Attendance Query (AFTER)', 0, 'With idx_attendance_emp_date - UPDATE THIS VALUE');

-- Compare BEFORE vs AFTER
SELECT 
    '5. BEFORE vs AFTER COMPARISON' as test_section;

SELECT 
    SUBSTRING_INDEX(test_name, ' (', 1) as operation,
    MAX(CASE WHEN test_name LIKE '%(BEFORE)%' THEN execution_time_ms END) as before_ms,
    MAX(CASE WHEN test_name LIKE '%(AFTER)%' THEN execution_time_ms END) as after_ms,
    ROUND(
        MAX(CASE WHEN test_name LIKE '%(BEFORE)%' THEN execution_time_ms END) / 
        NULLIF(MAX(CASE WHEN test_name LIKE '%(AFTER)%' THEN execution_time_ms END), 0),
        1
    ) as improvement_factor,
    ROUND(
        (MAX(CASE WHEN test_name LIKE '%(BEFORE)%' THEN execution_time_ms END) - 
         MAX(CASE WHEN test_name LIKE '%(AFTER)%' THEN execution_time_ms END)) /
        MAX(CASE WHEN test_name LIKE '%(BEFORE)%' THEN execution_time_ms END) * 100,
        1
    ) as improvement_percentage
FROM performance_baseline
WHERE test_date >= DATE_SUB(NOW(), INTERVAL 1 HOUR)
GROUP BY SUBSTRING_INDEX(test_name, ' (', 1)
ORDER BY improvement_factor DESC;

-- ================================================================
-- MEASUREMENT COMPLETE
-- ================================================================

SET @end_time = NOW();
SELECT 
    'AFTER OPTIMIZATION MEASUREMENT COMPLETED' as status,
    TIMESTAMPDIFF(SECOND, @start_time, @end_time) as total_time_seconds;
