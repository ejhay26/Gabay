@echo off
REM scripts/sync_vault_to_device.bat
REM One-click zero-touch synchronization of Gabay routing graph & POI database to connected Android device.

echo ============================================================
echo GABAY OFFLINE VAULT: ZERO-TOUCH DEVICE SYNCHRONIZER
echo ============================================================

REM 1. Verify ADB is in path or in standard Android SDK
set ADB=adb
where adb >nul 2>nul
if %ERRORLEVEL% neq 0 (
    if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" (
        set ADB="%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
    ) else (
        echo [ERROR] ADB not found in PATH or Android SDK.
        pause
        exit /b 1
    )
)

echo [1/3] Checking connected Android devices...
%ADB% devices
for /f "skip=1 tokens=1" %%i in ('%ADB% devices') do (
    set DEVICE_FOUND=%%i
    goto :device_found
)

echo [ERROR] No Android device connected. Please connect via USB or Wi-Fi adb.
pause
exit /b 1

:device_found
echo Found device: %DEVICE_FOUND%

echo [2/3] Ensuring target directory exists on device: /sdcard/Documents/Gabay/
%ADB% shell mkdir -p /sdcard/Documents/Gabay/

echo [3/3] Pushing ph.gabay and pois_fts.sqlite to device...
if exist "G:\gabay_builds\ph.gabay" (
    echo Pushing G:\gabay_builds\ph.gabay to /sdcard/Documents/Gabay/ph.gabay ...
    %ADB% push "G:\gabay_builds\ph.gabay" /sdcard/Documents/Gabay/ph.gabay
) else (
    echo [WARN] G:\gabay_builds\ph.gabay not found. Run map_builder first.
)

if exist "G:\gabay_builds\pois_fts.sqlite" (
    echo Pushing G:\gabay_builds\pois_fts.sqlite to /sdcard/Documents/Gabay/pois_fts.sqlite ...
    %ADB% push "G:\gabay_builds\pois_fts.sqlite" /sdcard/Documents/Gabay/pois_fts.sqlite
) else (
    echo [WARN] G:\gabay_builds\pois_fts.sqlite not found. Run map_builder first.
)

echo.
echo ============================================================
echo [SUCCESS] Vault files synced to /sdcard/Documents/Gabay/!
echo Open Gabay on your phone to navigate offline immediately.
echo ============================================================
pause
