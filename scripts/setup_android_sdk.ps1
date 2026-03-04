param(
    [string]$SdkDir
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot

if (-not $SdkDir) {
    $candidates = @(
        $env:ANDROID_HOME,
        $env:ANDROID_SDK_ROOT,
        "C:\Users\LSG\AppData\Local\Android\Sdk",
        "E:\Android\Sdk",
        "D:\Android\Sdk"
    ) | Where-Object { $_ -and (Test-Path $_) }

    $SdkDir = $candidates | Select-Object -First 1
}

if (-not $SdkDir -or -not (Test-Path $SdkDir)) {
    Write-Error "Android SDK directory not found. Pass -SdkDir explicitly."
}

$sdkDirEscaped = $SdkDir -replace "\\", "\\\\"
$localProps = @"
sdk.dir=$sdkDirEscaped
"@

Set-Content -Path (Join-Path $projectRoot "local.properties") -Value $localProps -Encoding ascii
Write-Host "local.properties written with sdk.dir=$SdkDir"
Write-Host "Next: .\gradle-8.7-bin\gradle-8.7\bin\gradle.bat :app:assembleDebug --no-daemon --console=plain"

