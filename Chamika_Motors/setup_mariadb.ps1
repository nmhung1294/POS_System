# MariaDB Database Setup Script
# Sets up database and imports schema/data

param(
    [string]$Password = "Mysql2003",
    [string]$Database = "chamika_motors"
)

$MariaDBPath = "C:\Program Files\MariaDB 11.8\bin"
$MariaDBClient = Join-Path $MariaDBPath "mariadb.exe"

Write-Host "========================================"
Write-Host "MARIADB DATABASE SETUP"
Write-Host "========================================"
Write-Host ""

# Test connection
Write-Host "1. Testing MariaDB connection..."
Write-Host "Testing with password: $Password" -ForegroundColor Gray
echo $Password | & $MariaDBClient -u root -p -e "SELECT VERSION() as 'MariaDB Version';" 2>$null
if ($LASTEXITCODE -eq 0) {
    Write-Host "   SUCCESS: Connection successful" -ForegroundColor Green
} else {
    Write-Host "   ERROR: Connection failed" -ForegroundColor Red
    Write-Host ""
    Write-Host "Troubleshooting:"
    Write-Host "- Check if MariaDB service is running: Get-Service *mariadb*"
    Write-Host "- Reset password if needed: mariadb-admin -u root password 'newpassword'"
    Write-Host "- Or use different password"
    exit 1
}

# Create database if not exists
Write-Host ""
Write-Host "2. Creating database..."
echo $Password | & $MariaDBClient -u root -p -e "CREATE DATABASE IF NOT EXISTS $Database CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>$null
if ($LASTEXITCODE -eq 0) {
    Write-Host "   SUCCESS: Database '$Database' ready" -ForegroundColor Green
} else {
    Write-Host "   ERROR: Failed to create database" -ForegroundColor Red
    exit 1
}

# Import backup
Write-Host ""
Write-Host "3. Importing database schema and data..."
Write-Host "   This may take a few minutes..."

$backupPath = Join-Path (Get-Location) "..\backup.sql"
if (Test-Path $backupPath) {
    Write-Host "   Importing from: $backupPath"
    $process = Start-Process -FilePath $MariaDBClient -ArgumentList "-u root -p$Password $Database" -RedirectStandardInput $backupPath -NoNewWindow -Wait -PassThru
    if ($process.ExitCode -eq 0) {
        Write-Host "   SUCCESS: Database import successful" -ForegroundColor Green
    } else {
        Write-Host "   ERROR: Database import failed" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "   ERROR: backup.sql not found at $backupPath" -ForegroundColor Red
    exit 1
}

# Verify import
Write-Host ""
Write-Host "4. Verifying import..."
$tableCount = echo $Password | & $MariaDBClient -u root -p -D $Database -e "SHOW TABLES;" 2>$null | Measure-Object -Line | Select-Object -ExpandProperty Lines
$tableCount = $tableCount - 1  # Subtract header line

if ($tableCount -gt 0) {
    Write-Host "   SUCCESS: Found $tableCount tables in database" -ForegroundColor Green

    # Show some tables
    Write-Host "   Tables: " -NoNewline
    echo $Password | & $MariaDBClient -u root -p -D $Database -e "SHOW TABLES;" 2>$null | Select-Object -Skip 1 | Select-Object -First 5 | ForEach-Object { Write-Host $_ -NoNewline; Write-Host ", " -NoNewline }
    Write-Host "..."
} else {
    Write-Host "   ERROR: No tables found - import may have failed" -ForegroundColor Red
}

Write-Host ""
Write-Host "========================================"
Write-Host "SETUP COMPLETE"
Write-Host "========================================"
Write-Host ""
Write-Host "Next steps:"
Write-Host "1. Run performance tests: .\test_performance.ps1"
Write-Host "2. Deploy optimizations: .\deploy_comprehensive.ps1"
Write-Host ""
Write-Host "Database: $Database"
Write-Host "User: root"
Write-Host "Status: Ready for testing!"
Write-Host ""