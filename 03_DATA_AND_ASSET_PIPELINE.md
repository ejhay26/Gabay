# 03 — OFFLINE DATA & ASSET PIPELINE

All heavy binary files must reside permanently on the Android device at `/sdcard/Documents/Gabay/`.
They must NEVER be bundled inside the `.apk` (keeping the APK size under 25 MB and ADB install time under 4 seconds).

---

## 1. DEVICE VAULT DIRECTORY LAYOUT

~~~text
/sdcard/Documents/Gabay/
├── philippines_vector.pmtiles   # Vector streets, 3D building footprints, malls, houses, POIs
├── satellite_base.pmtiles       # Single-zoom macro satellite overview (Zoom 10-11, ~150-250MB)
├── topography_contours.pmtiles  # Offline 50m/100m elevation contour vector lines (~60MB)
├── satellite_corridor.mbtiles   # High-res Zoom 14-17 satellite cache along active/regional corridors
├── ph.gabay                     # Compiled multi-vehicle road graph + R-Tree spatial index
├── pois_fts.sqlite              # Offline SQLite FTS5 landmark, barangay, mall, & gas station index
├── model.gguf                   # Qwen2.5-0.5B-Instruct-Q4_K_M.gguf (~380 MB)
└── rider_profile.json           # Persistent user settings (default vehicle mode, avoidances)
~~~

---

## 2. AUTOMATED HOST FETCH SCRIPT (`scripts/fetch_and_build_assets.sh`)

Write an idempotent bash script (`scripts/fetch_and_build_assets.sh`) that stores downloaded source files in `./local_assets/` (which is junctioned to `G:\gabay_builds\local_assets` to protect Drive C:) and only downloads or builds files if they are missing or incomplete:

### A. OSM PBF Source (`local_assets/philippines-latest.osm.pbf`)
- **Source URL:** `https://download.geofabrik.de/asia/philippines-latest.osm.pbf`
- **Usage:** Parsed by `tools/map_builder` to output `local_assets/ph.gabay` (multi-vehicle routing graph) and `local_assets/pois_fts.sqlite` (offline landmark/building search database).
- **Fast Dev Regional Extract Option:** If full nationwide graph compilation takes longer than 3 minutes during initial iteration, extract a Luzon corridor bounding box (`13.8,119.8,16.2,121.6`) first to validate the pipeline, then build the full nationwide graph.

### B. Vector Map (`local_assets/philippines_vector.pmtiles`)
- Use the `pmtiles` CLI binary (from `https://github.com/protomaps/go-pmtiles/releases`) to extract the Philippines bounding box (`116.8,4.5,126.7,21.2`) from the Protomaps planet build, OR convert `philippines-latest.osm.pbf` using `tilemaker` into `philippines_vector.pmtiles`.
- **Required Vector Layers:**
  1. `roads` / `transportation` (highways, trunks, primaries, secondaries, residential, alleys, one-way arrows)
  2. `buildings` (2D footprints + `height` / `render_height` attributes for 3D extrusion of malls, houses, and landmarks)
  3. `pois` / `places` (towns, barangays, malls, hospitals, gas stations, convenience stores, repair shops)
  4. `water` and `landuse` (rivers, coastlines, parks, commercial zones)

### C. Low-Storage Macroscopic Satellite Layer (`satellite_base.pmtiles`)
- **Storage Reality:** High-resolution nationwide satellite imagery (Zoom 0-17) requires 80 to 150 GB, which is prohibitive for offline mobile use.
- **Single-Zoom Macro Peek Architecture:** By locking the nationwide satellite layer strictly to **Zoom 10-11**, the entire Philippine archipelago consumes only **~150 to 250 MB total**.
- **MapLibre Overzooming:** With MapLibre raster `maxzoom: 11` and bilinear filtering, users can zoom in to street level (Zoom 14-18) and clearly peek at the macroscopic landscape (determining if an area is dense forest, open plain, river basin, or mountain slope) beneath the vector roads and 3D buildings.
- **On-Demand Corridor Cache (`satellite_corridor.mbtiles`):** For riders wanting high-res Zoom 14-16 imagery along a specific planned ride, an on-demand button (`[CACHE CORRIDOR SAT]`) downloads tiles strictly within a `1.5 km` corridor buffer of the active route polyline (~5-10 MB per trip).

### D. Offline Topography Contours (`topography_contours.pmtiles`)
- **Elevation Vector Contours:** A lightweight vector tile layer containing 50m and 100m elevation contour lines and mountain peak elevations covering the Philippines (~60 MB).
- **HUD Toggle:** Toggled via `[TOPO]` button to give riders instant insight into slope steepness, mountain passes, and elevation changes without needing heavy DEM grids.

### E. Quantized Local LLM (`local_assets/model.gguf`)
- **Source URL:** `https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf`
- **Minimum Size Check:** Verify downloaded file size is `> 350000000` bytes before pushing to device so corrupted/redirected HTTP downloads are caught immediately.

---

## 3. IDEMPOTENT ADB SYNC & PERMISSION PROVISIONING

In `scripts/fetch_and_build_assets.sh`, sync `./local_assets/` to `/sdcard/Documents/Gabay/` only when a file is missing on the device or its byte size does not match:

~~~bash
#!/usr/bin/env bash
set -euo pipefail

DEVICE_DIR="/sdcard/Documents/Gabay"
adb shell mkdir -p "$DEVICE_DIR"

sync_file() {
    local src="local_assets/$1"
    local dst="$DEVICE_DIR/$1"
    local host_size
    host_size=$(stat -c%s "$src")
    local dev_size
    dev_size=$(adb shell stat -c%s "$dst" 2>/dev/null | tr -d '\r' || echo "0")

    if [ "$host_size" != "$dev_size" ]; then
        echo "[SYNC] Pushing $1 ($host_size bytes) to $dst..."
        adb push "$src" "$dst"
    else
        echo "[OK] $1 already present on device ($dev_size bytes)."
    fi
}

sync_file "philippines_vector.pmtiles"
sync_file "satellite_base.pmtiles"
sync_file "topography_contours.pmtiles"
sync_file "ph.gabay"
sync_file "pois_fts.sqlite"
sync_file "model.gguf"
~~~

After installing the APK, automatically grant storage and location permissions via ADB so the app never blocks on manual system dialogs during testing:

~~~bash
adb shell appops set --uid dev.gabay.app MANAGE_EXTERNAL_STORAGE allow || true
adb shell pm grant dev.gabay.app android.permission.ACCESS_FINE_LOCATION || true
adb shell pm grant dev.gabay.app android.permission.ACCESS_COARSE_LOCATION || true
adb shell pm grant dev.gabay.app android.permission.RECORD_AUDIO || true
adb shell pm grant dev.gabay.app android.permission.POST_NOTIFICATIONS || true
~~~