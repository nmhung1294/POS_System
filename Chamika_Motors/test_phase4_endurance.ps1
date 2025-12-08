# ================================================================
# Phase 4: Endurance Tests
# Tests: EnduranceTest
# Total: 4 tests (1min, 5min, 15min, Memory Leak Detection)
# ================================================================

Write-Host ""
Write-Host "========================================"
Write-Host "PHASE 4: ENDURANCE TESTS"
Write-Host "========================================"
Write-Host ""

$PROJECT_DIR = Get-Location
$BUILD_DIR = Join-Path $PROJECT_DIR "build\classes"
$LIB_DIR = Join-Path $PROJECT_DIR "lib"

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

Write-Host "Running Endurance Tests..." -ForegroundColor Cyan
Write-Host "Note: Tests run for 1min, 5min, 15min to detect memory leaks" -ForegroundColor Yellow
Write-Host "WARNING: This will take approximately 21+ minutes!" -ForegroundColor Red
Write-Host ""

$startTime = Get-Date
java -cp $classpath `
    org.junit.platform.console.ConsoleLauncher `
    --select-class test.performance.EnduranceTest `
    --details=tree

$endTime = Get-Date
$duration = $endTime - $startTime

Write-Host ""
Write-Host "========================================"
Write-Host "PHASE 4 COMPLETED"
Write-Host "Total Duration: $($duration.ToString('hh\:mm\:ss'))"
Write-Host "========================================"
Write-Host ""

Read-Host "Press Enter to exit"
