@echo off
REM MariaDB Password Reset Batch Script
REM Run as Administrator

echo ========================================
echo MARIADB PASSWORD RESET (MANUAL METHOD)
echo ========================================
echo.

set MARIADB_BIN="C:\Program Files\MariaDB 11.8\bin"
set NEW_PASSWORD=Mysql2003

echo Step 1: Stopping MariaDB service...
net stop MariaDB
timeout /t 3 /nobreak > nul

echo.
echo Step 2: Starting MariaDB in safe mode (skip-grant-tables)...
echo This will open a new command window. DO NOT CLOSE IT.
echo.
echo When you see "ready for connections", press any key here...
echo.
start "MariaDB Safe Mode" %MARIADB_BIN%\mysqld.exe --skip-grant-tables --user=mysql --console

echo Press any key when MariaDB safe mode is ready...
pause > nul

echo.
echo Step 3: Resetting password in new terminal window...
echo.
echo COPY AND PASTE these commands in the MariaDB safe mode window:
echo.
echo UPDATE mysql.user SET authentication_string = PASSWORD('%NEW_PASSWORD%') WHERE User = 'root' AND Host = 'localhost';
echo UPDATE mysql.user SET plugin = '' WHERE User = 'root' AND Host = 'localhost';
echo FLUSH PRIVILEGES;
echo EXIT;
echo.
echo After running those commands, close the safe mode window.
echo.

pause

echo.
echo Step 4: Starting MariaDB service normally...
net start MariaDB
timeout /t 3 /nobreak > nul

echo.
echo Step 5: Testing new password...
%MARIADB_BIN%\mariadb.exe -u root -p%NEW_PASSWORD% -e "SELECT VERSION() as 'MariaDB Version';"
if %ERRORLEVEL% EQU 0 (
    echo.
    echo SUCCESS: Password reset to '%NEW_PASSWORD%'!
    echo.
    echo Now you can run:
    echo powershell.exe -ExecutionPolicy Bypass -File setup_mariadb.ps1 -Password "%NEW_PASSWORD%"
) else (
    echo.
    echo ERROR: Password reset failed. Check the steps above.
)

echo.
echo ========================================
pause