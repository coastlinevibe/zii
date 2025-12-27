# Build APK for specific school
# Usage: .\build-for-school.ps1 <school_id>
# Example: .\build-for-school.ps1 demo-school-001

param(
    [Parameter(Mandatory=$true)]
    [string]$SchoolId,
    
    [Parameter(Mandatory=$false)]
    [string]$WebsiteUrl = "http://localhost:3000"
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Zii School APK Builder" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Download school config from website
Write-Host "[1/4] Downloading school configuration..." -ForegroundColor Yellow
$configUrl = "$WebsiteUrl/api/admin/schools/$SchoolId/export-config"
$configPath = "app\src\main\assets\school_config.json"

try {
    $response = Invoke-WebRequest -Uri $configUrl -Method GET
    $config = $response.Content | ConvertFrom-Json
    
    Write-Host "  ✓ School: $($config.school.name)" -ForegroundColor Green
    Write-Host "  ✓ Code: $($config.school.code)" -ForegroundColor Green
    Write-Host "  ✓ Grades: $($config.grades.Count)" -ForegroundColor Green
    Write-Host "  ✓ Teachers: $($config.teachers.Count)" -ForegroundColor Green
    Write-Host "  ✓ Students: $($config.students.Count)" -ForegroundColor Green
    Write-Host "  ✓ Parents: $($config.parents.Count)" -ForegroundColor Green
    
    # Save to assets folder
    $response.Content | Out-File -FilePath $configPath -Encoding UTF8
    Write-Host "  ✓ Config saved to $configPath" -ForegroundColor Green
    
} catch {
    Write-Host "  ✗ Error downloading config: $_" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Step 2: Update app version
Write-Host "[2/4] Updating app version..." -ForegroundColor Yellow
$buildGradle = "app\build.gradle.kts"
$content = Get-Content $buildGradle -Raw

# Extract current version code
if ($content -match 'versionCode\s*=\s*(\d+)') {
    $currentVersionCode = [int]$matches[1]
    $newVersionCode = $currentVersionCode + 1
    $content = $content -replace 'versionCode\s*=\s*\d+', "versionCode = $newVersionCode"
    
    Write-Host "  ✓ Version code: $currentVersionCode → $newVersionCode" -ForegroundColor Green
} else {
    Write-Host "  ! Could not find versionCode, keeping current" -ForegroundColor Yellow
}

# Update version name with school code
$versionName = "0.4.0-$($config.school.code)"
$content = $content -replace 'versionName\s*=\s*"[^"]*"', "versionName = `"$versionName`""
Write-Host "  ✓ Version name: $versionName" -ForegroundColor Green

$content | Out-File -FilePath $buildGradle -Encoding UTF8 -NoNewline

Write-Host ""

# Step 3: Clean and build APK
Write-Host "[3/4] Building APK..." -ForegroundColor Yellow
Write-Host "  This may take a few minutes..." -ForegroundColor Gray

try {
    & .\gradlew.bat clean assembleDebug
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  ✓ Build successful!" -ForegroundColor Green
    } else {
        Write-Host "  ✗ Build failed with exit code $LASTEXITCODE" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "  ✗ Build error: $_" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Step 4: Rename and copy APK
Write-Host "[4/4] Finalizing APK..." -ForegroundColor Yellow

$sourceApk = "app\build\outputs\apk\debug\zii-school-0.4.0-alpha-debug.apk"
$schoolCode = $config.school.code
$targetApk = "zii-school-$schoolCode-debug.apk"

if (Test-Path $sourceApk) {
    Copy-Item $sourceApk $targetApk -Force
    $apkSize = (Get-Item $targetApk).Length / 1MB
    Write-Host "  ✓ APK created: $targetApk" -ForegroundColor Green
    Write-Host "  ✓ Size: $([math]::Round($apkSize, 2)) MB" -ForegroundColor Green
} else {
    Write-Host "  ✗ APK not found at $sourceApk" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "✓ BUILD COMPLETE!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "School: $($config.school.name)" -ForegroundColor White
Write-Host "Code: $($config.school.code)" -ForegroundColor White
Write-Host "APK: $targetApk" -ForegroundColor White
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "1. Test the APK on a device:" -ForegroundColor Gray
Write-Host "   adb install $targetApk" -ForegroundColor Gray
Write-Host ""
Write-Host "2. Distribute to school:" -ForegroundColor Gray
Write-Host "   - Upload to school admin portal" -ForegroundColor Gray
Write-Host "   - Share download link with teachers/parents" -ForegroundColor Gray
Write-Host ""
