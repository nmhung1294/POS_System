-- ================================================================
-- PERFORMANCE TESTING SCRIPT
-- Purpose: Measure performance improvements after optimization
-- ================================================================

USE chamika_motors;

-- ================================================================
-- SECTION 1: BEFORE/AFTER QUERY ANALYSIS
-- Run EXPLAIN to see execution plan
-- ================================================================

DELIMITER $$

DROP PROCEDURE IF EXISTS test_query_performance$$

CREATE PROCEDURE test_query_performance()
BEGIN
    DECLARE start_time BIGINT;
    DECLARE end_time BIGINT;
    DECLARE elapsed_ms INT;
    
    -- Test 1: Monthly invoice summary
    SELECT 'Test 1: Monthly Invoice Summary' AS test_name;
    SET start_time = UNIX_TIMESTAMP(NOW(6)) * 1000000 + MICROSECOND(NOW(6));
    
    SELECT COUNT(*) AS cnt, SUM(paid_amount) AS total
    FROM invoice 
    WHERE date_time >= '2024-01-01' AND date_time < '2024-02-01';
    
    SET end_time = UNIX_TIMESTAMP(NOW(6)) * 1000000 + MICROSECOND(NOW(6));
    SET elapsed_ms = (end_time - start_time) DIV 1000;
    SELECT CONCAT('Elapsed time: ', elapsed_ms, ' ms') AS result;
    
    -- Show execution plan
    EXPLAIN SELECT COUNT(*) AS cnt, SUM(paid_amount) AS total
    FROM invoice 
    WHERE date_time >= '2024-01-01' AND date_time < '2024-02-01';
    
    SELECT '---' AS separator;
    
    -- Test 2: Customer search
    SELECT 'Test 2: Customer Search' AS test_name;
    SET start_time = UNIX_TIMESTAMP(NOW(6)) * 1000000 + MICROSECOND(NOW(6));
    
    SELECT mobile, name, points 
    FROM customer 
    WHERE name LIKE 'S%' 
    ORDER BY name ASC
    LIMIT 10;
    
    SET end_time = UNIX_TIMESTAMP(NOW(6)) * 1000000 + MICROSECOND(NOW(6));
    SET elapsed_ms = (end_time - start_time) DIV 1000;
    SELECT CONCAT('Elapsed time: ', elapsed_ms, ' ms') AS result;
    
    EXPLAIN SELECT mobile, name, points 
    FROM customer 
    WHERE name LIKE 'S%' 
    ORDER BY name ASC
    LIMIT 10;
    
    SELECT '---' AS separator;
    
    -- Test 3: Stock lookup
    SELECT 'Test 3: Stock Lookup' AS test_name;
    SET start_time = UNIX_TIMESTAMP(NOW(6)) * 1000000 + MICROSECOND(NOW(6));
    
    SELECT id, qty 
    FROM stock 
    WHERE product_id = '1' 
      AND selling_price = 100.00
    LIMIT 1;
    
    SET end_time = UNIX_TIMESTAMP(NOW(6)) * 1000000 + MICROSECOND(NOW(6));
    SET elapsed_ms = (end_time - start_time) DIV 1000;
    SELECT CONCAT('Elapsed time: ', elapsed_ms, ' ms') AS result;
    
    EXPLAIN SELECT id, qty 
    FROM stock 
    WHERE product_id = '1' 
      AND selling_price = 100.00;
    
    SELECT '---' AS separator;
    
    -- Test 4: GRN summary
    SELECT 'Test 4: GRN Monthly Summary' AS test_name;
    SET start_time = UNIX_TIMESTAMP(NOW(6)) * 1000000 + MICROSECOND(NOW(6));
    
    SELECT SUM(paid_amount) AS total
    FROM grn 
    WHERE date_time >= '2024-01-01' AND date_time < '2024-02-01';
    
    SET end_time = UNIX_TIMESTAMP(NOW(6)) * 1000000 + MICROSECOND(NOW(6));
    SET elapsed_ms = (end_time - start_time) DIV 1000;
    SELECT CONCAT('Elapsed time: ', elapsed_ms, ' ms') AS result;
    
    EXPLAIN SELECT SUM(paid_amount) AS total
    FROM grn 
    WHERE date_time >= '2024-01-01' AND date_time < '2024-02-01';
    
END$$

DELIMITER ;

-- ================================================================
-- SECTION 2: INDEX VERIFICATION
-- Verify all indices were created successfully
-- ================================================================

SELECT 
    'Index Verification Report' AS report_title,
    COUNT(*) AS total_indices
FROM information_schema.STATISTICS 
WHERE TABLE_SCHEMA = 'chamika_motors'
  AND INDEX_NAME LIKE 'idx_%';

SELECT 
    TABLE_NAME,
    INDEX_NAME,
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS columns,
    INDEX_TYPE,
    NON_UNIQUE
FROM information_schema.STATISTICS 
WHERE TABLE_SCHEMA = 'chamika_motors'
  AND INDEX_NAME LIKE 'idx_%'
GROUP BY TABLE_NAME, INDEX_NAME, INDEX_TYPE, NON_UNIQUE
ORDER BY TABLE_NAME, INDEX_NAME;

-- ================================================================
-- SECTION 3: TABLE STATISTICS
-- Check cardinality and data distribution
-- ================================================================

SELECT 
    TABLE_NAME,
    TABLE_ROWS AS estimated_rows,
    ROUND(DATA_LENGTH / 1024 / 1024, 2) AS data_mb,
    ROUND(INDEX_LENGTH / 1024 / 1024, 2) AS index_mb,
    ROUND((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024, 2) AS total_mb
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'chamika_motors'
  AND TABLE_TYPE = 'BASE TABLE'
ORDER BY (DATA_LENGTH + INDEX_LENGTH) DESC;

-- ================================================================
-- SECTION 4: RUN PERFORMANCE TESTS
-- ================================================================

-- Run the test procedure
CALL test_query_performance();

-- ================================================================
-- SECTION 5: SLOW QUERY MONITORING
-- Enable slow query log to track performance
-- ================================================================

-- Check current slow query settings
SHOW VARIABLES LIKE 'slow_query%';
SHOW VARIABLES LIKE 'long_query_time';

-- To enable slow query log (requires SUPER privilege):
-- SET GLOBAL slow_query_log = 'ON';
-- SET GLOBAL long_query_time = 1;
-- SET GLOBAL slow_query_log_file = '/var/log/mysql/slow.log';

-- ================================================================
-- SECTION 6: CONNECTION POOL MONITORING
-- ================================================================

-- Show current connections
SHOW STATUS LIKE 'Threads_connected';
SHOW STATUS LIKE 'Max_used_connections';
SHOW VARIABLES LIKE 'max_connections';

-- Show connection details
SELECT 
    ID,
    USER,
    HOST,
    DB,
    COMMAND,
    TIME,
    STATE,
    LEFT(INFO, 50) AS QUERY_PREVIEW
FROM information_schema.PROCESSLIST
WHERE DB = 'chamika_motors'
ORDER BY TIME DESC;

-- ================================================================
-- EXPECTED RESULTS AFTER OPTIMIZATION
-- ================================================================

/*
EXPLAIN Output Interpretation:

GOOD SIGNS (After optimization):
- type: const, eq_ref, ref, range (using index)
- key: idx_invoice_date_time, idx_grn_date_time, etc.
- rows: Small number (< 1000)
- Extra: "Using index" or "Using where; Using index"

BAD SIGNS (Before optimization):
- type: ALL (full table scan)
- key: NULL (no index used)
- rows: Large number (> 10000)
- Extra: "Using filesort" or "Using temporary"

PERFORMANCE TARGETS:
- Invoice monthly summary: < 50ms
- Customer search: < 20ms
- Stock lookup: < 10ms
- GRN summary: < 30ms
*/

-- ================================================================
-- SECTION 7: BENCHMARK COMPARISON
-- ================================================================

-- Simple benchmark for repeated queries
DELIMITER $$

DROP PROCEDURE IF EXISTS benchmark_queries$$

CREATE PROCEDURE benchmark_queries(IN iterations INT)
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE start_time BIGINT;
    DECLARE end_time BIGINT;
    DECLARE total_ms BIGINT DEFAULT 0;
    
    SELECT CONCAT('Running ', iterations, ' iterations...') AS status;
    
    SET start_time = UNIX_TIMESTAMP(NOW(6)) * 1000000 + MICROSECOND(NOW(6));
    
    WHILE i < iterations DO
        -- Run test query
        SELECT COUNT(*) INTO @dummy
        FROM invoice 
        WHERE date_time >= DATE_SUB(CURDATE(), INTERVAL 30 DAY);
        
        SET i = i + 1;
    END WHILE;
    
    SET end_time = UNIX_TIMESTAMP(NOW(6)) * 1000000 + MICROSECOND(NOW(6));
    SET total_ms = (end_time - start_time) DIV 1000;
    
    SELECT 
        iterations AS total_queries,
        total_ms AS total_time_ms,
        ROUND(total_ms / iterations, 2) AS avg_time_ms,
        ROUND(1000 / (total_ms / iterations), 2) AS queries_per_second;
END$$

DELIMITER ;

-- Run benchmark (100 iterations)
CALL benchmark_queries(100);

-- ================================================================
-- SECTION 8: CLEANUP
-- ================================================================

DROP PROCEDURE IF EXISTS test_query_performance;
DROP PROCEDURE IF EXISTS benchmark_queries;

-- ================================================================
-- END OF TESTING SCRIPT
-- ================================================================

SELECT 'Performance testing complete!' AS status;
SELECT 'Review the results above to verify improvements.' AS next_steps;
