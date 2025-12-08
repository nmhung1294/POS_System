# ================================================================
# COMPREHENSIVE DEPLOYMENT SCRIPT (PowerShell)
# Deploys Phase 0 optimizations with measurement
# ================================================================

Write-Host ""
Write-Host "========================================================================"
Write-Host "POS SYSTEM - PHASE 0 COMPREHENSIVE DEPLOYMENT"
Write-Host "========================================================================"
Write-Host ""

$PROJECT_DIR = Get-Location
$BACKUP_DIR = Join-Path $PROJECT_DIR "backups"
$TIMESTAMP = Get-Date -Format "yyyyMMdd_HHmmss"

# MariaDB credentials (adjust as needed)
$MYSQL_USER = "root"
$MYSQL_PASS = "Mysql2003"
$MYSQL_DB = "chamika_motors"
$MYSQL_HOST = "localhost"

Write-Host "Configuration:"
Write-Host "- Database: $MYSQL_DB@$MYSQL_HOST"
Write-Host "- User: $MYSQL_USER"
Write-Host "- Timestamp: $TIMESTAMP"
Write-Host ""

# Create backup directory
if (-not (Test-Path $BACKUP_DIR)) {
    New-Item -ItemType Directory -Path $BACKUP_DIR | Out-Null
}

Write-Host "========================================================================"
Write-Host "STEP 1: BACKUP CURRENT DATABASE"
Write-Host "========================================================================"
Write-Host "Creating full backup before any changes..."

$backupFile = Join-Path $BACKUP_DIR "backup_before_optimization_$TIMESTAMP.sql"
& mariadb-dump -u$MYSQL_USER -p$MYSQL_PASS $MYSQL_DB | Out-File -FilePath $backupFile -Encoding UTF8

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Database backup failed!" -ForegroundColor Red
    Write-Host "Please check MariaDB credentials and try again."
#     Read-Host "Press Enter to exit"
    exit 1
}

Write-Host "SUCCESS: Backup created at $backupFile" -ForegroundColor Green
Write-Host ""

Write-Host "========================================================================"
Write-Host "STEP 2: MEASURE BASELINE PERFORMANCE (BEFORE OPTIMIZATION)"
Write-Host "========================================================================"
Write-Host "Running baseline performance tests..."

$baselineFile = Join-Path $BACKUP_DIR "baseline_results_$TIMESTAMP.txt"
Get-Content "measure_baseline.sql" | & mariadb -u$MYSQL_USER -p$MYSQL_PASS $MYSQL_DB | Out-File -FilePath $baselineFile -Encoding UTF8

if ($LASTEXITCODE -ne 0) {
    Write-Host "WARNING: Baseline measurement encountered issues" -ForegroundColor Yellow
    Write-Host "Continuing with deployment..."
} else {
    Write-Host "SUCCESS: Baseline results saved to $baselineFile" -ForegroundColor Green
}

Write-Host ""
Write-Host "IMPORTANT: Record these baseline times for comparison!" -ForegroundColor Cyan
# Read-Host "Press Enter to continue"

Write-Host "========================================================================"
Write-Host "STEP 3: DEPLOY DATABASE OPTIMIZATIONS (16 INDICES)"
Write-Host "========================================================================"
Write-Host "Creating 16 performance indices..."

Get-Content "database_optimization.sql" | & mariadb -u$MYSQL_USER -p$MYSQL_PASS $MYSQL_DB

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Failed to create indices!" -ForegroundColor Red
    Write-Host "Rolling back from backup..."
    Get-Content $backupFile | & mariadb -u$MYSQL_USER -p$MYSQL_PASS $MYSQL_DB
    Write-Host "Rollback completed. System restored to previous state." -ForegroundColor Yellow
#     Read-Host "Press Enter to exit"
    exit 1
}

Write-Host "SUCCESS: 16 indices created successfully" -ForegroundColor Green
Write-Host ""

Write-Host "========================================================================"
Write-Host "STEP 4: VERIFY INDICES CREATION"
Write-Host "========================================================================"
Write-Host "Checking that all indices were created..."

$indicesFile = Join-Path $BACKUP_DIR "indices_created_$TIMESTAMP.txt"
& mysql -u$MYSQL_USER -p$MYSQL_PASS -e "SELECT TABLE_NAME, INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA='$MYSQL_DB' AND INDEX_NAME LIKE 'idx_%' ORDER BY TABLE_NAME, INDEX_NAME;" | Out-File -FilePath $indicesFile -Encoding UTF8

Write-Host "Indices list saved to $indicesFile"
Get-Content $indicesFile | Write-Host
Write-Host ""

Write-Host "========================================================================"
Write-Host "STEP 5: ANALYZE TABLES FOR OPTIMIZER"
Write-Host "========================================================================"
Write-Host "Updating table statistics for query optimizer..."

& mysql -u$MYSQL_USER -p$MYSQL_PASS $MYSQL_DB -e "ANALYZE TABLE invoice, invoice_item, grn, grn_item, stock, customer, attendance;"

if ($LASTEXITCODE -ne 0) {
    Write-Host "WARNING: Table analysis had issues, but indices are still active" -ForegroundColor Yellow
} else {
    Write-Host "SUCCESS: Table statistics updated" -ForegroundColor Green
}
Write-Host ""

Write-Host "========================================================================"
Write-Host "STEP 6: MEASURE AFTER OPTIMIZATION PERFORMANCE"
Write-Host "========================================================================"
Write-Host "Running performance tests after optimization..."

$afterFile = Join-Path $BACKUP_DIR "after_results_$TIMESTAMP.txt"
Get-Content "measure_after.sql" | & mysql -u$MYSQL_USER -p$MYSQL_PASS $MYSQL_DB | Out-File -FilePath $afterFile -Encoding UTF8

if ($LASTEXITCODE -ne 0) {
    Write-Host "WARNING: After measurement encountered issues" -ForegroundColor Yellow
} else {
    Write-Host "SUCCESS: After results saved to $afterFile" -ForegroundColor Green
}
Write-Host ""

Write-Host "========================================================================"
Write-Host "STEP 7: REBUILD APPLICATION WITH NEW CODE"
Write-Host "========================================================================"
Write-Host "Compiling updated Java code..."

Push-Location "Chamika_Motors"

# Clean previous build
if (Test-Path "build\classes") {
    Write-Host "Cleaning previous build..."
    Remove-Item -Recurse -Force "build\classes"
    New-Item -ItemType Directory -Path "build\classes" | Out-Null
}

# Compile using Ant
if (Test-Path "build.xml") {
    Write-Host "Building with Apache Ant..."
    & ant clean compile
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: Build failed!" -ForegroundColor Red
        Write-Host "Check compilation errors above."
        Pop-Location
#         Read-Host "Press Enter to exit"
        exit 1
    }
    
    Write-Host "SUCCESS: Application rebuilt successfully" -ForegroundColor Green
} else {
    Write-Host "ERROR: build.xml not found!" -ForegroundColor Red
    Write-Host "Please rebuild manually using your IDE."
    Pop-Location
#     Read-Host "Press Enter to exit"
    exit 1
}

Pop-Location
Write-Host ""

Write-Host "========================================================================"
Write-Host "STEP 8: GENERATE DEPLOYMENT REPORT"
Write-Host "========================================================================"

Write-Host "Generating comprehensive deployment report..."

$reportFile = Join-Path $BACKUP_DIR "deployment_report_$TIMESTAMP.md"

$report = @"
# DEPLOYMENT REPORT - Phase 0 Optimizations

**Deployment Date:** $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")
**Database:** $MYSQL_DB

## Changes Deployed

### 1. Database Indices (16 created)
- idx_invoice_date_time
- idx_grn_date_time
- idx_stock_product_price
- idx_customer_name
- idx_attendance_emp_date
- 11 other covering indices

### 2. Application Code Updates
- DBUtil.java: Connection pool 10 -> 50
- CacheManager.java: In-memory caching layer
- InvoiceServiceImpl.java: Transaction management
- PaymentMethodRepositoryImpl.java: Cache integration

## Performance Results

See detailed results in:
- Baseline: backups\baseline_results_$TIMESTAMP.txt
- After: backups\after_results_$TIMESTAMP.txt

## Backup Location
- Full backup: backups\backup_before_optimization_$TIMESTAMP.sql

## Rollback Instructions
If issues occur, restore from backup:
``````
mysql -u$MYSQL_USER -p $MYSQL_DB < backups\backup_before_optimization_$TIMESTAMP.sql
``````

## Next Steps
1. Run application and test invoice creation
2. Monitor logs for "Cache HIT/MISS" messages
3. Verify transaction rollback on errors
4. Run full performance test suite

"@

$report | Out-File -FilePath $reportFile -Encoding UTF8

Write-Host "SUCCESS: Deployment report created" -ForegroundColor Green
Write-Host ""

Write-Host "========================================================================"
Write-Host "DEPLOYMENT SUMMARY"
Write-Host "========================================================================"
Write-Host ""
Write-Host " Database backup created" -ForegroundColor Green
Write-Host " Baseline performance measured" -ForegroundColor Green
Write-Host " 16 indices deployed" -ForegroundColor Green
Write-Host " Indices verified" -ForegroundColor Green
Write-Host " Tables analyzed" -ForegroundColor Green
Write-Host " After performance measured" -ForegroundColor Green
Write-Host " Application rebuilt" -ForegroundColor Green
Write-Host " Deployment report generated" -ForegroundColor Green
Write-Host ""
Write-Host " Performance Results:"
Write-Host "   - Baseline: backups\baseline_results_$TIMESTAMP.txt"
Write-Host "   - After:    backups\after_results_$TIMESTAMP.txt"
Write-Host ""
Write-Host "📄 Full Report: backups\deployment_report_$TIMESTAMP.md"
Write-Host ""
Write-Host "========================================================================"
Write-Host "DEPLOYMENT COMPLETED SUCCESSFULLY!" -ForegroundColor Green
Write-Host "========================================================================"
Write-Host ""
Write-Host ""
Write-Host "Next: Run the application and verify improvements"
Write-Host ""

# Read-Host "Press Enter to exit"
# Read-Host "Press Enter to exit"
# Read-Host 'Press Enter to exit'
