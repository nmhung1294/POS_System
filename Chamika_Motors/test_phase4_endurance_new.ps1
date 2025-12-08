# ================================================================
# Phase 4: Endurance Tests
# Tests: EnduranceTest (1min, 5min, 15min, Memory Leak Detection)
# Total: 4 tests
# WARNING: This will take approximately 21+ minutes!
# ================================================================

Write-Host ""
Write-Host "========================================"
Write-Host "PHASE 4: ENDURANCE TESTS"
Write-Host "========================================"
Write-Host ""

$PROJECT_DIR = Get-Location
$SRC_DIR = Join-Path $PROJECT_DIR "src"
$BUILD_DIR = Join-Path $PROJECT_DIR "build\classes"
$LIB_DIR = Join-Path $PROJECT_DIR "lib"

# Check if build directory exists
if (-not (Test-Path $BUILD_DIR)) {
    Write-Host "Creating build directory..."
    New-Item -ItemType Directory -Path $BUILD_DIR | Out-Null
}

# Build classpath
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

Write-Host "Compiling Phase 4 Runner..." -ForegroundColor Cyan
$phase4Runner = Join-Path $SRC_DIR "test\performance\Phase4Runner.java"
javac -cp $classpath -d $BUILD_DIR $phase4Runner

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Compilation failed!" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host "Running Phase 4 Tests..." -ForegroundColor Green
Write-Host "WARNING: This will take ~21 minutes (1min + 5min + 15min tests)" -ForegroundColor Yellow
Write-Host "Testing: Memory leak detection, System stability" -ForegroundColor Yellow
Write-Host ""

$startTime = Get-Date
java -cp $classpath test.performance.Phase4Runner
$endTime = Get-Date
$duration = $endTime - $startTime

Write-Host ""
Write-Host "Total Phase 4 Duration: $($duration.ToString('hh\:mm\:ss'))" -ForegroundColor Cyan
Write-Host ""
Read-Host "Press Enter to exit"
