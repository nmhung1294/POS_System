@echo off
REM ================================================================
REM DEPLOYMENT SCRIPT FOR PERFORMANCE IMPROVEMENTS
REM Windows PowerShell version
REM ================================================================

echo.
echo ========================================
echo POS System Performance Improvements
echo Deployment Script v1.0
echo ========================================
echo.

REM Configuration
set DB_HOST=localhost
set DB_PORT=3306
set DB_NAME=chamika_motors
set DB_USER=root
set /p DB_PASS="Enter MySQL password: "

echo.
echo Step 1/5: Backing up current database...
echo ========================================

set BACKUP_FILE=backup_before_optimization_%date:~-4,4%%date:~-10,2%%date:~-7,2%_%time:~0,2%%time:~3,2%%time:~6,2%.sql
set BACKUP_FILE=%BACKUP_FILE: =0%

"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqldump.exe" -h %DB_HOST% -P %DB_PORT% -u %DB_USER% -p%DB_PASS% %DB_NAME% > %BACKUP_FILE%

if %errorlevel% neq 0 (
    echo ERROR: Backup failed!
    pause
    exit /b 1
)

echo SUCCESS: Backup created: %BACKUP_FILE%
echo.

echo Step 2/5: Applying database optimizations...
echo ========================================

"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -h %DB_HOST% -P %DB_PORT% -u %DB_USER% -p%DB_PASS% %DB_NAME% < database_optimization.sql

if %errorlevel% neq 0 (
    echo ERROR: Database optimization failed!
    echo Rolling back from backup...
    "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -h %DB_HOST% -P %DB_PORT% -u %DB_USER% -p%DB_PASS% %DB_NAME% < %BACKUP_FILE%
    pause
    exit /b 1
)

echo SUCCESS: Database optimizations applied
echo.

echo Step 3/5: Running performance tests...
echo ========================================

"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -h %DB_HOST% -P %DB_PORT% -u %DB_USER% -p%DB_PASS% %DB_NAME% < test_performance.sql > test_results.txt

if %errorlevel% neq 0 (
    echo WARNING: Performance tests had issues
    echo Check test_results.txt for details
) else (
    echo SUCCESS: Performance tests completed
    echo Results saved to: test_results.txt
)

echo.

echo Step 4/5: Rebuilding application...
echo ========================================

cd Chamika_Motors

if exist build.xml (
    echo Using Ant build...
    call ant clean compile
) else (
    echo Using javac build...
    if not exist build\classes mkdir build\classes
    
    dir /s /b src\*.java > sources.txt
    javac -cp "lib/*" -d build/classes @sources.txt
    del sources.txt
)

if %errorlevel% neq 0 (
    echo ERROR: Build failed!
    cd ..
    pause
    exit /b 1
)

echo SUCCESS: Application rebuilt
cd ..
echo.

echo Step 5/5: Verification...
echo ========================================

echo Verifying indices created...
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -h %DB_HOST% -P %DB_PORT% -u %DB_USER% -p%DB_PASS% -e "SELECT COUNT(*) AS total_indices FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = '%DB_NAME%' AND INDEX_NAME LIKE 'idx_%%';" %DB_NAME%

echo.
echo ========================================
echo DEPLOYMENT COMPLETED SUCCESSFULLY!
echo ========================================
echo.
echo Next steps:
echo 1. Review test_results.txt for performance metrics
echo 2. Start the application and monitor logs
echo 3. Test with multiple concurrent users
echo.
echo Backup location: %BACKUP_FILE%
echo.
echo Press any key to exit...
pause > nul
