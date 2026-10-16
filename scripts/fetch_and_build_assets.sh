#!/usr/bin/env bash
# scripts/fetch_and_build_assets.sh
# Idempotent offline asset pipeline and device sync for Gabay

set -euo pipefail

LOCAL_ASSETS="local_assets"
mkdir -p "$LOCAL_ASSETS"

echo "=========================================="
echo " GABAY OFFLINE ASSET PIPELINE & SYNC (BASH)"
echo "=========================================="

# 1. Road Graph & POI Database
if [ ! -f "$LOCAL_ASSETS/ph.gabay" ] || [ ! -f "$LOCAL_ASSETS/pois_fts.sqlite" ]; then
    echo "[PIPELINE] Building ph.gabay and pois_fts.sqlite..."
    cargo run -p map_builder -- --generate-sample
fi

# 2. Offline PMTiles Archives
if [ ! -f "$LOCAL_ASSETS/philippines_vector.pmtiles" ] || [ ! -f "$LOCAL_ASSETS/satellite_base.pmtiles" ] || [ ! -f "$LOCAL_ASSETS/topography_contours.pmtiles" ]; then
    echo "[PIPELINE] Generating PMTiles archives..."
    python scripts/build_offline_pmtiles.py
fi

# 3. Rider Profile
if [ ! -f "$LOCAL_ASSETS/rider_profile.json" ]; then
    cat << 'EOF' > "$LOCAL_ASSETS/rider_profile.json"
{
  "default_vehicle_mode": "SCOOTER",
  "avoid_tolls": true,
  "avoid_expressways": true,
  "avoid_ferries": false,
  "speed_unit": "KMH",
  "voice_guidance": true,
  "hud_theme": "TACTICAL_DARK"
}
EOF
fi

# 4. GGUF Model Placeholder / Download
if [ ! -f "$LOCAL_ASSETS/model.gguf" ]; then
    echo "[DEV] Creating GGUF magic header placeholder..."
    printf '\x47\x47\x55\x46\x03\x00\x00\x00' > "$LOCAL_ASSETS/model.gguf"
fi

# 5. ADB Device Sync
if adb devices | grep -w "device" > /dev/null; then
    DEVICE_DIR="/sdcard/Documents/Gabay"
    adb shell mkdir -p "$DEVICE_DIR"

    sync_file() {
        local file="$1"
        local src="$LOCAL_ASSETS/$file"
        local dst="$DEVICE_DIR/$file"
        if [ -f "$src" ]; then
            local host_size
            host_size=$(stat -c%s "$src" 2>/dev/null || stat -f%z "$src")
            local dev_size
            dev_size=$(adb shell stat -c%s "$dst" 2>/dev/null | tr -d '\r' || echo "0")
            if [ "$host_size" != "$dev_size" ]; then
                echo "[SYNC] Pushing $file ($host_size bytes) to $dst..."
                adb push "$src" "$dst"
            else
                echo "[OK] $file already present on device."
            fi
        fi
    }

    sync_file "philippines_vector.pmtiles"
    sync_file "satellite_base.pmtiles"
    sync_file "topography_contours.pmtiles"
    sync_file "ph.gabay"
    sync_file "pois_fts.sqlite"
    sync_file "rider_profile.json"
    sync_file "model.gguf"

    echo "[ADB] Provisioning permissions..."
    adb shell appops set --uid dev.gabay.app MANAGE_EXTERNAL_STORAGE allow || true
    adb shell pm grant dev.gabay.app android.permission.ACCESS_FINE_LOCATION || true
    adb shell pm grant dev.gabay.app android.permission.ACCESS_COARSE_LOCATION || true
    adb shell pm grant dev.gabay.app android.permission.RECORD_AUDIO || true
    adb shell pm grant dev.gabay.app android.permission.POST_NOTIFICATIONS || true
    echo "[OK] Device sync complete."
fi

echo "=========================================="
echo " ASSET PIPELINE FINISHED"
echo "=========================================="
