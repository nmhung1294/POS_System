@echo off
REM ================================================================
REM Performance Test Runner Script
REM Compiles and executes all performance tests
REM ================================================================

echo.
echo ========================================
echo POS System Performance Tests
echo ========================================
echo.

REM Set paths
set PROJECT_DIR=%cd%
set SRC_DIR=%PROJECT_DIR%\src
set BUILD_DIR=%PROJECT_DIR%\build\classes
set LIB_DIR=%PROJECT_DIR%\lib
set TEST_DIR=%PROJECT_DIR%\src\test

REM Check if build directory exists
if not exist "%BUILD_DIR%" (
    echo Creating build directory...
    mkdir "%BUILD_DIR%"
)

echo Step 1: Compiling test classes...
echo ========================================

REM Compile test classes
javac -cp "%BUILD_DIR%;%LIB_DIR%\*" ^
    -d "%BUILD_DIR%" ^
    "%TEST_DIR%\performance\*.java"

if %errorlevel% neq 0 (
    echo ERROR: Compilation failed!
    pause
    exit /b 1
)

echo SUCCESS: Test classes compiled
echo.

echo Step 2: Running performance tests...
echo ========================================

REM Run tests
java -cp "%BUILD_DIR%;%LIB_DIR%\*" ^
    test.performance.PerformanceTestRunner

if %errorlevel% neq 0 (
    echo.
    echo WARNING: Some tests failed!
    echo Check the output above for details.
) else (
    echo.
    echo SUCCESS: All tests passed!
)

echo.
echo ========================================
echo Test execution completed
echo ========================================
echo.

pause
