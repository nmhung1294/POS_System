# ================================================================
# Performance Test Runner Script (PowerShell)
# Compiles and executes all performance tests
# ================================================================

Write-Host ""
Write-Host "========================================"
Write-Host "POS System Performance Tests - MariaDB"
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

# Build classpath with all required JARs
$junitJars = @(
    "junit-jupiter-api-5.10.1.jar",
    "junit-jupiter-engine-5.10.1.jar",
    "junit-platform-commons-1.10.1.jar",
    "junit-platform-engine-1.10.1.jar",
    "junit-platform-launcher-1.10.1.jar",
    "opentest4j-1.3.0.jar",
    "mysql-connector-java-8.0.24.jar",
    "HikariCP-5.1.0.jar",
    "slf4j-api-2.0.9.jar",
    "slf4j-simple-2.0.9.jar"
)

$classpathParts = @($BUILD_DIR)
foreach ($jar in $junitJars) {
    $classpathParts += Join-Path $LIB_DIR $jar
}
$classpath = $classpathParts -join ";"

# Compile test classes
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
