# Gabay Scripts & Failsafe Tooling Reference

This document provides a guide to the offline tooling and automation scripts located in `scripts/`. These scripts ensure that the entire pipeline—from raw OpenStreetMap download to binary graph compilation, visual verification against Google Maps, and zero-touch device provisioning—can be reproduced deterministically on any workstation without storing multi-gigabyte binaries in Git.

---

## 1. Directory Structure

```
Gabay/
├── docs/
│   ├── CANONICAL_OSM_ROUTING.md          # Architectural specification of canonical router & vehicle matrix
│   ├── OFFLINE_VAULT_PROVISIONING.md     # Automated installation & provisioning guide for Android
│   └── SCRIPTS_AND_TOOLING.md            # Reference manual for all scripts in scripts/
├── scripts/
│   ├── fetch_philippines_pbf.py          # Resilient downloader for canonical OSM PBF
│   ├── build_full_offline_vault.py       # Master orchestrator for offline vault compilation
│   ├── compare_with_google_maps.py       # Accuracy & variance benchmark against Google / OSRM
│   ├── render_visual_comparison.py       # Visual tile stitcher & dual-layer map renderer
│   ├── sync_vault_to_device.bat          # 1-click zero-touch ADB auto-push batch script
│   └── build_rust.bat                    # NDK cross-compilation script for aarch64 Android
└── tools/
    └── map_builder/                      # Rust 2-pass canonical PBF to CSR graph compiler
```

---

## 2. Script Reference Manual

### `scripts/fetch_philippines_pbf.py`
**Purpose**: Resilient, failsafe downloader for the canonical `philippines-latest.osm.pbf` dataset directly from Geofabrik mirrors.
- **Features**:
  - Automatically targets external Drive `G:\gabay_builds\osm\philippines.osm.pbf` to protect laptop internal drive space.
  - Supports HTTP Range header resume in case of Wi-Fi interruptions.
  - Validates file size and downloads companion MD5 checksum.
- **Usage**:
  ```bash
  python scripts/fetch_philippines_pbf.py
  # Optional custom destination:
  python scripts/fetch_philippines_pbf.py --output D:/vault/philippines.osm.pbf
  ```

---

### `tools/map_builder` (Rust Binary Compiler)
**Purpose**: High-throughput 2-pass parser that converts raw `.osm.pbf` into Gabay's binary CSR routing graph (`ph.gabay`) and SQLite FTS5 POI database (`pois_fts.sqlite`).
- **Features**:
  - Zero threshold heuristics: connectivity derived 100% from matching OpenStreetMap Node IDs.
  - Multi-vehicle road flag assignment (`FLAG_SCOOTER`, `FLAG_CAR`, `FLAG_BICYCLE`, `FLAG_WALK`, `FLAG_EXPRESSWAY`, `FLAG_TOLL`).
  - Strict Philippine traffic rules: Motorways & Tollways tagged with `FLAG_EXPRESSWAY` (prohibiting scooters `< 400cc`).
- **Usage**:
  ```bash
  cargo run --manifest-path tools/map_builder/Cargo.toml --release -- \
    --input G:/gabay_builds/osm/philippines.osm.pbf \
    --output-graph G:/gabay_builds/ph.gabay \
    --output-pois G:/gabay_builds/pois_fts.sqlite
  ```

---

### `scripts/render_visual_comparison.py`
**Purpose**: Generates high-resolution side-by-side and overlay comparison images between Gabay's offline routes and Google Maps / OSRM reference routes.
- **Features**:
  - Dynamically fetches standard OSM base tiles and stitches them into high-res satellite/street backdrops.
  - Plots Gabay canonical route polyline (solid Cyan `#00E5FF`) and Google reference route (dashed Tangerine `#FF6D00`).
  - Outputs visual comparison PNG artifacts and compiles an interactive dual-map HTML inspector at `G:/gabay_builds/route_visual_comparison.html`.
- **Usage**:
  ```bash
  python scripts/render_visual_comparison.py
  ```

---

### `scripts/compare_with_google_maps.py`
**Purpose**: Automated quantitative metric comparator.
- **Features**:
  - Calculates percentage distance variance, step counts, and duration delta.
  - Verifies that Gabay routes never violate 0-field chord constraints.
- **Usage**:
  ```bash
  python scripts/compare_with_google_maps.py
  ```

---

### `scripts/sync_vault_to_device.bat`
**Purpose**: 1-click zero-touch ADB auto-push script.
- **Features**:
  - Detects connected Android device via ADB (USB or Wi-Fi).
  - Creates `/sdcard/Documents/Gabay/` automatically.
  - Pushes `ph.gabay` (273 MB), `pois_fts.sqlite` (106 MB), and `philippines_vector.pmtiles` directly into place without user interaction.
- **Usage**:
  ```cmd
  scripts\sync_vault_to_device.bat
  ```

---

### `scripts/build_rust.bat`
**Purpose**: Automated NDK cross-compilation script.
- **Features**:
  - Compiles `rust_core` for `aarch64-linux-android` using Android NDK clang.
  - Directs build artifacts to Drive `G:\cargo_target` to preserve `C:` space.
  - Automatically copies `libgabay_core.so` directly into `android_app/app/src/main/jniLibs/arm64-v8a/`.
- **Usage**:
  ```cmd
  scripts\build_rust.bat
  ```
