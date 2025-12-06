# ================================================================
# Performance Test Runner Script (PowerShell)
# Compiles and executes all performance tests
# ================================================================

Write-Host ""
Write-Host "========================================"
Write-Host "POS System Performance Tests"
Write-Host "========================================"
Write-Host ""

# Set paths
$PROJECT_DIR = Get-Location
$SRC_DIR = Join-Path $PROJECT_DIR "src"
$BUILD_DIR = Join-Path $PROJECT_DIR "build\classes"
$LIB_DIR = Join-Path $PROJECT_DIR "lib"
$TEST_DIR = Join-Path $PROJECT_DIR "src\test"

# Check if build directory exists
if (-not (Test-Path $BUILD_DIR)) {
    Write-Host "Creating build directory..."
    New-Item -ItemType Directory -Path $BUILD_DIR | Out-Null
}

Write-Host "Step 1: Compiling test classes..."
Write-Host "========================================"

# Get all test files
$testFiles = Get-ChildItem -Path (Join-Path $TEST_DIR "performance") -Filter "*.java" -Recurse

# Compile test classes
$classpath = "$BUILD_DIR;$LIB_DIR\*"
javac -cp $classpath -d $BUILD_DIR $testFiles.FullName

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Compilation failed!" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host "SUCCESS: Test classes compiled" -ForegroundColor Green
Write-Host ""

Write-Host "Step 2: Running performance tests..."
Write-Host "========================================"

# Run tests
java -cp $classpath test.performance.PerformanceTestRunner

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "WARNING: Some tests failed!" -ForegroundColor Yellow
    Write-Host "Check the output above for details."
} else {
    Write-Host ""
    Write-Host "SUCCESS: All tests passed!" -ForegroundColor Green
}

Write-Host ""
Write-Host "========================================"
Write-Host "Test execution completed"
Write-Host "========================================"
Write-Host ""

Read-Host "Press Enter to exit"
