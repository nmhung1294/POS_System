# MariaDB Password Reset Script
# Resets root password for MariaDB on Windows

param(
    [Parameter(Mandatory=$true)]
    [string]$NewPassword
)

$MariaDBPath = "C:\Program Files\MariaDB 11.8\bin"
$ServiceName = "MariaDB"

Write-Host "========================================"
Write-Host "MARIADB PASSWORD RESET"
Write-Host "========================================"
Write-Host ""

# Stop MariaDB service
Write-Host "1. Stopping MariaDB service..."
Stop-Service $ServiceName -ErrorAction SilentlyContinue
Start-Sleep -Seconds 2

# Start MariaDB with skip-grant-tables
Write-Host "2. Starting MariaDB in safe mode..."
$mysqlProcess = Start-Process -FilePath "$MariaDBPath\mysqld.exe" -ArgumentList "--skip-grant-tables", "--user=mysql" -NoNewWindow -PassThru

# Wait for it to start
Start-Sleep -Seconds 3

# Reset password
Write-Host "3. Resetting root password..."
$sqlCommands = @"
UPDATE mysql.user SET authentication_string = PASSWORD('$NewPassword') WHERE User = 'root' AND Host = 'localhost';
UPDATE mysql.user SET plugin = '' WHERE User = 'root' AND Host = 'localhost';
FLUSH PRIVILEGES;
"@

$sqlCommands | Out-File -FilePath "reset_password.sql" -Encoding UTF8

& "$MariaDBPath\mariadb.exe" -u root --execute="source reset_password.sql"

# Clean up
Remove-Item "reset_password.sql" -ErrorAction SilentlyContinue

# Stop the safe mode instance
Write-Host "4. Stopping safe mode instance..."
Stop-Process -Id $mysqlProcess.Id -ErrorAction SilentlyContinue

# Start MariaDB service normally
Write-Host "5. Starting MariaDB service normally..."
Start-Service $ServiceName
Start-Sleep -Seconds 3

# Test new password
Write-Host "6. Testing new password..."
& "$MariaDBPath\mariadb.exe" -u root -p$NewPassword -e "SELECT VERSION() as 'MariaDB Version';" 2>$null
if ($LASTEXITCODE -eq 0) {
    Write-Host "SUCCESS: Password reset successful!" -ForegroundColor Green
    Write-Host ""
    Write-Host "New root password: $NewPassword"
    Write-Host ""
    Write-Host "Now you can run:"
    Write-Host ".\setup_mariadb.ps1 -Password '$NewPassword'"
} else {
    Write-Host "ERROR: Password reset may have failed" -ForegroundColor Red
    Write-Host "Try manual reset or check MariaDB logs"
}

Write-Host ""
Write-Host "========================================"