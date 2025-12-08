# MariaDB Configuration Update Script
# Updates all configuration files for MariaDB compatibility

Write-Host "========================================"
Write-Host "MARIADB CONFIGURATION UPDATE"
Write-Host "========================================"
Write-Host ""

# Update config.properties
$configPath = "src\resources\config.properties"
Write-Host "1. Updating config.properties..."

$configContent = @"
# Database Configuration - MariaDB
db.url=jdbc:mysql://localhost:3306/chamika_motors
db.user=root
db.password=Mysql2003
"@

$configContent | Out-File -FilePath $configPath -Encoding UTF8
Write-Host "   ✓ Updated config.properties" -ForegroundColor Green

# Copy to build directory
Copy-Item $configPath "build\classes\config.properties" -Force
Write-Host "   ✓ Copied to build/classes/" -ForegroundColor Green

# Update deploy_comprehensive.ps1 for MariaDB
$deployScript = "deploy_comprehensive.ps1"
Write-Host ""
Write-Host "2. Updating deploy_comprehensive.ps1 for MariaDB..."

$content = Get-Content $deployScript -Raw

# Update comments and variable names
$content = $content -replace "# MySQL credentials", "# MariaDB credentials"
$content = $content -replace "MySQL password", "MariaDB password"
$content = $content -replace "MySQL Server 8.0", "MariaDB Server"

# Update binary paths (MariaDB might be in different location)
$content = $content -replace "mysqldump", "mariadb-dump"
$content = $content -replace "mysql", "mariadb"

$content | Out-File -FilePath $deployScript -Encoding UTF8
Write-Host "   ✓ Updated deploy_comprehensive.ps1" -ForegroundColor Green

# Update test_performance.ps1
$testScript = "test_performance.ps1"
Write-Host ""
Write-Host "3. Updating test_performance.ps1..."

$content = Get-Content $testScript -Raw
$content = $content -replace "MySQL", "MariaDB"
$content | Out-File -FilePath $testScript -Encoding UTF8
Write-Host "   ✓ Updated test_performance.ps1" -ForegroundColor Green

# Update README if exists
$readmePath = "..\README.md"
if (Test-Path $readmePath) {
    Write-Host ""
    Write-Host "4. Updating README.md..."

    $content = Get-Content $readmePath -Raw
    $content = $content -replace "MySQL", "MariaDB"
    $content | Out-File -FilePath $readmePath -Encoding UTF8
    Write-Host "   ✓ Updated README.md" -ForegroundColor Green
}

Write-Host ""
Write-Host "========================================"
Write-Host "MARIADB UPDATE COMPLETE"
Write-Host "========================================"
Write-Host ""
Write-Host "Next steps:"
Write-Host "1. Import database: mariadb -u root -p chamika_motors < ..\backup.sql"
Write-Host "2. Test connection: .\test_performance.ps1"
Write-Host "3. Deploy optimizations: .\deploy_comprehensive.ps1"
Write-Host ""
Write-Host "Note: MariaDB uses same JDBC driver as MySQL, so no JAR changes needed."
Write-Host ""