# ================================================================
# DEPLOYMENT SCRIPT FOR PERFORMANCE IMPROVEMENTS
# PowerShell version for Windows
# ================================================================

Write-Host ""
Write-Host "========================================"
Write-Host "POS System Performance Improvements"
Write-Host "Deployment Script v1.0"
Write-Host "========================================"
Write-Host ""

# Configuration
$DB_HOST = "localhost"
$DB_PORT = "3306"
$DB_NAME = "chamika_motors"
$DB_USER = "root"
$DB_PASS = Read-Host "Enter MySQL password" -AsSecureString
$DB_PASS_PLAIN = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($DB_PASS))

$MYSQL_BIN = "C:\Program Files\MySQL\MySQL Server 8.0\bin"
$MYSQLDUMP = "$MYSQL_BIN\mysqldump.exe"
$MYSQL = "$MYSQL_BIN\mysql.exe"

# Step 1: Backup
Write-Host ""
Write-Host "Step 1/5: Backing up current database..."
Write-Host "========================================"

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$BACKUP_FILE = "backup_before_optimization_$timestamp.sql"

& $MYSQLDUMP -h $DB_HOST -P $DB_PORT -u $DB_USER -p$DB_PASS_PLAIN $DB_NAME | Out-File $BACKUP_FILE -Encoding UTF8

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Backup failed!" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host "SUCCESS: Backup created: $BACKUP_FILE" -ForegroundColor Green
Write-Host ""

# Step 2: Apply optimizations
Write-Host "Step 2/5: Applying database optimizations..."
Write-Host "========================================"

Get-Content database_optimization.sql | & $MYSQL -h $DB_HOST -P $DB_PORT -u $DB_USER -p$DB_PASS_PLAIN $DB_NAME

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Database optimization failed!" -ForegroundColor Red
    Write-Host "Rolling back from backup..."
    Get-Content $BACKUP_FILE | & $MYSQL -h $DB_HOST -P $DB_PORT -u $DB_USER -p$DB_PASS_PLAIN $DB_NAME
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host "SUCCESS: Database optimizations applied" -ForegroundColor Green
Write-Host ""

# Step 3: Run performance tests
Write-Host "Step 3/5: Running performance tests..."
Write-Host "========================================"

Get-Content test_performance.sql | & $MYSQL -h $DB_HOST -P $DB_PORT -u $DB_USER -p$DB_PASS_PLAIN $DB_NAME | Out-File test_results.txt -Encoding UTF8

if ($LASTEXITCODE -ne 0) {
    Write-Host "WARNING: Performance tests had issues" -ForegroundColor Yellow
    Write-Host "Check test_results.txt for details"
} else {
    Write-Host "SUCCESS: Performance tests completed" -ForegroundColor Green
    Write-Host "Results saved to: test_results.txt"
}

Write-Host ""

# Step 4: Rebuild application
Write-Host "Step 4/5: Rebuilding application..."
Write-Host "========================================"

Push-Location Chamika_Motors

if (Test-Path "build.xml") {
    Write-Host "Using Ant build..."
    ant clean compile
} else {
    Write-Host "Using javac build..."
    
    if (-not (Test-Path "build\classes")) {
        New-Item -ItemType Directory -Path "build\classes" | Out-Null
    }
    
    $sourceFiles = Get-ChildItem -Path "src" -Filter "*.java" -Recurse | Select-Object -ExpandProperty FullName
    $sourceFiles | Out-File sources.txt -Encoding UTF8
    
    javac -cp "lib/*" -d build/classes "@sources.txt"
    Remove-Item sources.txt
}

Pop-Location

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Build failed!" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host "SUCCESS: Application rebuilt" -ForegroundColor Green
Write-Host ""

# Step 5: Verification
Write-Host "Step 5/5: Verification..."
Write-Host "========================================"

Write-Host "Verifying indices created..."
$query = "SELECT COUNT(*) AS total_indices FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = '$DB_NAME' AND INDEX_NAME LIKE 'idx_%';"
echo $query | & $MYSQL -h $DB_HOST -P $DB_PORT -u $DB_USER -p$DB_PASS_PLAIN $DB_NAME

Write-Host ""
Write-Host "========================================"
Write-Host "DEPLOYMENT COMPLETED SUCCESSFULLY!" -ForegroundColor Green
Write-Host "========================================"
Write-Host ""
Write-Host "Next steps:"
Write-Host "1. Review test_results.txt for performance metrics"
Write-Host "2. Start the application and monitor logs"
Write-Host "3. Test with multiple concurrent users"
Write-Host ""
Write-Host "Backup location: $BACKUP_FILE"
Write-Host ""

Read-Host "Press Enter to exit"
