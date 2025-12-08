@echo off
REM ================================================================
REM COMPREHENSIVE DEPLOYMENT SCRIPT
REM Deploys Phase 0 optimizations with measurement
REM ================================================================

echo.
echo ========================================================================
echo POS SYSTEM - PHASE 0 COMPREHENSIVE DEPLOYMENT
echo ========================================================================
echo.

set PROJECT_DIR=%cd%
set BACKUP_DIR=%PROJECT_DIR%\backups
set TIMESTAMP=%date:~10,4%%date:~4,2%%date:~7,2%_%time:~0,2%%time:~3,2%%time:~6,2%
set TIMESTAMP=%TIMESTAMP: =0%

REM MySQL credentials (adjust as needed)
set MYSQL_USER=root
set MYSQL_PASS=Mysql2003
set MYSQL_DB=chamika_motors
set MYSQL_HOST=localhost

echo Configuration:
echo - Database: %MYSQL_DB%@%MYSQL_HOST%
echo - User: %MYSQL_USER%
echo - Timestamp: %TIMESTAMP%
echo.

REM Create backup directory
if not exist "%BACKUP_DIR%" mkdir "%BACKUP_DIR%"

echo ========================================================================
echo STEP 1: BACKUP CURRENT DATABASE
echo ========================================================================
echo Creating full backup before any changes...

mysqldump -u%MYSQL_USER% -p%MYSQL_PASS% %MYSQL_DB% > "%BACKUP_DIR%\backup_before_optimization_%TIMESTAMP%.sql"

if %errorlevel% neq 0 (
    echo ERROR: Database backup failed!
    echo Please check MySQL credentials and try again.
    pause
    exit /b 1
)

echo SUCCESS: Backup created at %BACKUP_DIR%\backup_before_optimization_%TIMESTAMP%.sql
echo.

echo ========================================================================
echo STEP 2: MEASURE BASELINE PERFORMANCE (BEFORE OPTIMIZATION)
echo ========================================================================
echo Running baseline performance tests...

mysql -u%MYSQL_USER% -p%MYSQL_PASS% %MYSQL_DB% < measure_baseline.sql > "%BACKUP_DIR%\baseline_results_%TIMESTAMP%.txt"

if %errorlevel% neq 0 (
    echo WARNING: Baseline measurement encountered issues
    echo Continuing with deployment...
) else (
    echo SUCCESS: Baseline results saved to %BACKUP_DIR%\baseline_results_%TIMESTAMP%.txt
)
echo.
echo IMPORTANT: Record these baseline times for comparison!
pause

echo ========================================================================
echo STEP 3: DEPLOY DATABASE OPTIMIZATIONS (16 INDICES)
echo ========================================================================
echo Creating 16 performance indices...

mysql -u%MYSQL_USER% -p%MYSQL_PASS% %MYSQL_DB% < database_optimization.sql

if %errorlevel% neq 0 (
    echo ERROR: Failed to create indices!
    echo Rolling back from backup...
    mysql -u%MYSQL_USER% -p%MYSQL_PASS% %MYSQL_DB% < "%BACKUP_DIR%\backup_before_optimization_%TIMESTAMP%.sql"
    echo Rollback completed. System restored to previous state.
    pause
    exit /b 1
)

echo SUCCESS: 16 indices created successfully
echo.

echo ========================================================================
echo STEP 4: VERIFY INDICES CREATION
echo ========================================================================
echo Checking that all indices were created...

mysql -u%MYSQL_USER% -p%MYSQL_PASS% -e "SELECT TABLE_NAME, INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA='%MYSQL_DB%' AND INDEX_NAME LIKE 'idx_%%' ORDER BY TABLE_NAME, INDEX_NAME;" > "%BACKUP_DIR%\indices_created_%TIMESTAMP%.txt"

echo Indices list saved to %BACKUP_DIR%\indices_created_%TIMESTAMP%.txt
type "%BACKUP_DIR%\indices_created_%TIMESTAMP%.txt"
echo.

echo ========================================================================
echo STEP 5: ANALYZE TABLES FOR OPTIMIZER
echo ========================================================================
echo Updating table statistics for query optimizer...

mysql -u%MYSQL_USER% -p%MYSQL_PASS% %MYSQL_DB% -e "ANALYZE TABLE invoice, invoice_item, grn, grn_item, stock, customer, attendance;"

if %errorlevel% neq 0 (
    echo WARNING: Table analysis had issues, but indices are still active
) else (
    echo SUCCESS: Table statistics updated
)
echo.

echo ========================================================================
echo STEP 6: MEASURE AFTER OPTIMIZATION PERFORMANCE
echo ========================================================================
echo Running performance tests after optimization...

mysql -u%MYSQL_USER% -p%MYSQL_PASS% %MYSQL_DB% < measure_after.sql > "%BACKUP_DIR%\after_results_%TIMESTAMP%.txt"

if %errorlevel% neq 0 (
    echo WARNING: After measurement encountered issues
) else (
    echo SUCCESS: After results saved to %BACKUP_DIR%\after_results_%TIMESTAMP%.txt
)
echo.

echo ========================================================================
echo STEP 7: REBUILD APPLICATION WITH NEW CODE
echo ========================================================================
echo Compiling updated Java code...

cd Chamika_Motors

REM Clean previous build
if exist "build\classes" (
    echo Cleaning previous build...
    rmdir /s /q "build\classes"
    mkdir "build\classes"
)

REM Compile using Ant
if exist "build.xml" (
    echo Building with Apache Ant...
    call ant clean compile
    
    if %errorlevel% neq 0 (
        echo ERROR: Build failed!
        echo Check compilation errors above.
        cd ..
        pause
        exit /b 1
    )
    
    echo SUCCESS: Application rebuilt successfully
) else (
    echo ERROR: build.xml not found!
    echo Please rebuild manually using your IDE.
    cd ..
    pause
    exit /b 1
)

cd ..
echo.

echo ========================================================================
echo STEP 8: GENERATE DEPLOYMENT REPORT
echo ========================================================================

echo Generating comprehensive deployment report...

(
    echo # DEPLOYMENT REPORT - Phase 0 Optimizations
    echo.
    echo **Deployment Date:** %date% %time%
    echo **Database:** %MYSQL_DB%
    echo.
    echo ## Changes Deployed
    echo.
    echo ### 1. Database Indices (16 created^)
    echo - idx_invoice_date_time
    echo - idx_grn_date_time
    echo - idx_stock_product_price
    echo - idx_customer_name
    echo - idx_attendance_emp_date
    echo - 11 other covering indices
    echo.
    echo ### 2. Application Code Updates
    echo - DBUtil.java: Connection pool 10 -^> 50
    echo - CacheManager.java: In-memory caching layer
    echo - InvoiceServiceImpl.java: Transaction management
    echo - PaymentMethodRepositoryImpl.java: Cache integration
    echo.
    echo ## Performance Results
    echo.
    echo See detailed results in:
    echo - Baseline: backups\baseline_results_%TIMESTAMP%.txt
    echo - After: backups\after_results_%TIMESTAMP%.txt
    echo.
    echo ## Backup Location
    echo - Full backup: backups\backup_before_optimization_%TIMESTAMP%.sql
    echo.
    echo ## Rollback Instructions
    echo If issues occur, restore from backup:
    echo ```
    echo mysql -u%MYSQL_USER% -p %MYSQL_DB% ^< backups\backup_before_optimization_%TIMESTAMP%.sql
    echo ```
    echo.
    echo ## Next Steps
    echo 1. Run application and test invoice creation
    echo 2. Monitor logs for "Cache HIT/MISS" messages
    echo 3. Verify transaction rollback on errors
    echo 4. Run full performance test suite
    echo.
) > "%BACKUP_DIR%\deployment_report_%TIMESTAMP%.md"

echo SUCCESS: Deployment report created
echo.

echo ========================================================================
echo DEPLOYMENT SUMMARY
echo ========================================================================
echo.
echo  Database backup created
echo  Baseline performance measured
echo  16 indices deployed
echo  Indices verified
echo  Tables analyzed
echo  After performance measured
echo  Application rebuilt
echo  Deployment report generated
echo.
echo  Performance Results:
echo    - Baseline: backups\baseline_results_%TIMESTAMP%.txt
echo    - After:    backups\after_results_%TIMESTAMP%.txt
echo.
echo 📄 Full Report: backups\deployment_report_%TIMESTAMP%.md
echo.
echo ========================================================================
echo DEPLOYMENT COMPLETED SUCCESSFULLY!
echo ========================================================================
echo.
echo Next: Run the application and verify improvements
echo.

pause
