Set-Location "E:\MyCode\SB-courselist"
Write-Host "Reading context file..." -ForegroundColor Cyan
Get-Content ".\AGENT_CONTEXT.md" -TotalCount 60
Write-Host ""
if (-not (Test-Path ".\local.properties")) {
    Write-Host "local.properties not found. Configure SDK first:" -ForegroundColor Yellow
    Write-Host "powershell -ExecutionPolicy Bypass -File .\scripts\setup_android_sdk.ps1 -SdkDir <sdk-path>" -ForegroundColor Yellow
    exit 1
}

Write-Host "Running debug build with local Gradle..." -ForegroundColor Cyan
.\gradle-8.7-bin\gradle-8.7\bin\gradle.bat :app:assembleDebug --no-daemon --console=plain
