Set-Location "E:\MyCode\SB-courselist"
Write-Host "Reading context file..." -ForegroundColor Cyan
Get-Content ".\AGENT_CONTEXT.md" -TotalCount 60
Write-Host ""
if (-not (Test-Path ".\local.properties")) {
    Write-Host "local.properties not found. Configure SDK first:" -ForegroundColor Yellow
    Write-Host "powershell -ExecutionPolicy Bypass -File .\scripts\setup_android_sdk.ps1 -SdkDir <sdk-path>" -ForegroundColor Yellow
    exit 1
}

Write-Host "Running parser unit tests..." -ForegroundColor Cyan
.\gradlew.bat :app:testDebugUnitTest --console=plain
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "Building debug APK..." -ForegroundColor Cyan
.\gradlew.bat :app:assembleDebug --console=plain
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$adb = "E:\Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $adb)) {
    Write-Host "adb not found at $adb" -ForegroundColor Yellow
    exit 1
}

Write-Host "Checking connected devices..." -ForegroundColor Cyan
& $adb devices

Write-Host "Installing latest debug APK..." -ForegroundColor Cyan
& $adb install -r "E:\MyCode\SB-courselist\app\build\outputs\apk\debug\app-debug.apk"

Write-Host "Installed app version:" -ForegroundColor Cyan
& $adb shell dumpsys package com.sb.courselist | Select-String "versionCode|versionName|lastUpdateTime"
