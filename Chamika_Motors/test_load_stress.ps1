# ================================================================
# Test Lần 3: Load and Stress Testing
# Tests 50 concurrent users with realistic workloads
# ================================================================

Write-Host ""
Write-Host "========================================"
Write-Host "TEST LẦN 3: LOAD & STRESS TESTING"
Write-Host "50 Concurrent Users Simulation"
Write-Host "========================================"
Write-Host ""

# Set paths
$PROJECT_DIR = "C:\Users\Admin\Java_POS_System\Chamika_Motors"
Set-Location $PROJECT_DIR

$BUILD_DIR = "build\classes"
$LIB_DIR = "lib"

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

Write-Host "Step 1: Compiling LoadStressTest..."
Write-Host "========================================"

javac -cp $classpath -d $BUILD_DIR src\test\performance\LoadStressTest.java 2>&1 | Out-Null

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Compilation failed!" -ForegroundColor Red
    exit 1
}

Write-Host "SUCCESS: LoadStressTest compiled" -ForegroundColor Green
Write-Host ""

Write-Host "Step 2: Running Load & Stress Tests..."
Write-Host "========================================"
Write-Host "NOTE: This may take 30-60 seconds due to sustained load test"
Write-Host ""

# Run LoadStressTest specifically
$env:JAVA_OPTS = "-Xmx1024m"
java -cp $classpath org.junit.platform.console.ConsoleLauncher `
    --scan-classpath `
    --include-classname ".*LoadStressTest" `
    --details=verbose `
    2>&1

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "Tests completed with some failures" -ForegroundColor Yellow
} else {
    Write-Host ""
    Write-Host "SUCCESS: All load/stress tests passed!" -ForegroundColor Green
}

Write-Host ""
Write-Host "========================================"
Write-Host "Test Lần 3 completed"
Write-Host "========================================"
Write-Host ""
