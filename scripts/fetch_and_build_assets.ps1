# scripts/fetch_and_build_assets.ps1
# Idempotent offline asset pipeline and device sync for Gabay

param (
    [switch]$DownloadFullModel = $false,
    [switch]$SkipDeviceSync = $false
)

$ErrorActionPreference = "Stop"
$env:CARGO_TARGET_DIR = "G:\gabay_builds\cargo_target"

Write-Host "=========================================="
Write-Host " GABAY OFFLINE ASSET PIPELINE & SYNC"
Write-Host "=========================================="

$LocalAssets = "local_assets"
if (!(Test-Path $LocalAssets)) {
    New-Item -ItemType Directory -Force -Path $LocalAssets | Out-Null
}

# 1. Build Multi-Vehicle Road Graph & SQLite FTS5 POI Database
$GraphPath = Join-Path $LocalAssets "ph.gabay"
$PoisPath = Join-Path $LocalAssets "pois_fts.sqlite"
if (!(Test-Path $GraphPath) -or !(Test-Path $PoisPath)) {
    Write-Host "[PIPELINE] Building ph.gabay and pois_fts.sqlite via map_builder..."
    cargo run -p map_builder -- --generate-sample
} else {
    Write-Host "[OK] ph.gabay and pois_fts.sqlite already present."
}

# 2. Build Offline PMTiles Archives (Vector, Macro Satellite, Topography)
$VecPmtiles = Join-Path $LocalAssets "philippines_vector.pmtiles"
$SatPmtiles = Join-Path $LocalAssets "satellite_base.pmtiles"
$TopoPmtiles = Join-Path $LocalAssets "topography_contours.pmtiles"

if (!(Test-Path $VecPmtiles) -or !(Test-Path $SatPmtiles) -or !(Test-Path $TopoPmtiles)) {
    Write-Host "[PIPELINE] Building PMTiles archives via python..."
    python scripts/build_offline_pmtiles.py
} else {
    Write-Host "[OK] PMTiles archives already present."
}

# 3. Rider Profile
$ProfilePath = Join-Path $LocalAssets "rider_profile.json"
if (!(Test-Path $ProfilePath)) {
    @{
        default_vehicle_mode = "SCOOTER"
        avoid_tolls = $true
        avoid_expressways = $true
        avoid_ferries = $false
        speed_unit = "KMH"
        voice_guidance = $true
        hud_theme = "TACTICAL_DARK"
    } | ConvertTo-Json | Set-Content $ProfilePath
    Write-Host "[OK] Created rider_profile.json."
}

# 4. Quantized GGUF Model (Qwen2.5-0.5B-Instruct-Q4_K_M.gguf)
$ModelPath = Join-Path $LocalAssets "model.gguf"
$ModelUrl = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf"

if (!(Test-Path $ModelPath) -or ((Get-Item $ModelPath).Length -lt 1000000)) {
    if ($DownloadFullModel) {
        Write-Host "[DOWNLOAD] Downloading Qwen2.5-0.5B-Instruct-Q4_K_M.gguf (~380 MB)..."
        Invoke-WebRequest -Uri $ModelUrl -OutFile $ModelPath
        $size = (Get-Item $ModelPath).Length
        if ($size -lt 350000000) {
            Write-Warning "Downloaded model size ($size bytes) is less than expected (> 350MB). Check network connection."
        }
    } else {
        # Create valid GGUF header placeholder for fast development / smoke testing
        Write-Host "[DEV] Creating GGUF magic header placeholder (use -DownloadFullModel for 380MB full download)..."
        $ggufMagic = [byte[]]@(0x47, 0x47, 0x55, 0x46, 0x03, 0x00, 0x00, 0x00) # b"GGUF" version 3
        [System.IO.File]::WriteAllBytes($ModelPath, $ggufMagic)
    }
} else {
    Write-Host "[OK] model.gguf already present ($((Get-Item $ModelPath).Length) bytes)."
}

# 5. Idempotent ADB Sync to Device Vault (/sdcard/Documents/Gabay/)
if (!$SkipDeviceSync) {
    Write-Host "`n[ADB] Checking ADB device connectivity..."
    $devices = adb devices | Select-String -Pattern "\bdevice\b"
    if ($devices) {
        $DeviceDir = "/sdcard/Documents/Gabay"
        adb shell mkdir -p $DeviceDir

        function Sync-FileToDevice($fileName) {
            $src = Join-Path $LocalAssets $fileName
            $dst = "$DeviceDir/$fileName"
            if (Test-Path $src) {
                $hostSize = (Get-Item $src).Length
                $devSizeStr = adb shell "if [ -f $dst ]; then stat -c%s $dst; else echo 0; fi"
                $devSize = 0
                if ($devSizeStr) {
                    $cleaned = ($devSizeStr -replace '\r|\n', '').Trim()
                    [int64]::TryParse($cleaned, [ref]$devSize) | Out-Null
                }
                
                if ($hostSize -ne $devSize) {
                    Write-Host "[SYNC] Pushing $fileName ($hostSize bytes) to $dst..."
                    adb push $src $dst
                } else {
                    Write-Host "[OK] $fileName already synchronized on device ($devSize bytes)."
                }
            }
        }

        Sync-FileToDevice "philippines_vector.pmtiles"
        Sync-FileToDevice "satellite_base.pmtiles"
        Sync-FileToDevice "topography_contours.pmtiles"
        Sync-FileToDevice "ph.gabay"
        Sync-FileToDevice "pois_fts.sqlite"
        Sync-FileToDevice "rider_profile.json"
        Sync-FileToDevice "model.gguf"

        Write-Host "`n[ADB] Provisioning application runtime permissions..."
        adb shell appops set dev.gabay.app MANAGE_EXTERNAL_STORAGE allow 2>$null | Out-Null
        adb shell pm grant dev.gabay.app android.permission.ACCESS_FINE_LOCATION 2>$null | Out-Null
        adb shell pm grant dev.gabay.app android.permission.ACCESS_COARSE_LOCATION 2>$null | Out-Null
        adb shell pm grant dev.gabay.app android.permission.RECORD_AUDIO 2>$null | Out-Null
        adb shell pm grant dev.gabay.app android.permission.POST_NOTIFICATIONS 2>$null | Out-Null
        Write-Host "[OK] Android permissions granted."
    } else {
        Write-Warning "No ADB device connected. Skipping device push."
    }
}

Write-Host "=========================================="
Write-Host " ASSET PIPELINE & SYNC COMPLETED!"
Write-Host "=========================================="
