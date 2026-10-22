# Offline Vault Provisioning Guide

This guide details how to build and automatically provision the offline routing vault (`ph.gabay` and `pois_fts.sqlite`) onto Android devices without manual copying or committing massive binary files to Git.

---

## 1. Storage & Git Policy

- **Never Commit Raw Maps to Git**: `*.osm.pbf` (580MB+), `*.gabay` (270MB+), and `*.pmtiles` are strictly ignored by `.gitignore`.
- **Reproducible Pipeline**: All tools to reproduce the exact graph and POI database are committed under `tools/` and `scripts/`.
- **Target Drive**: On Windows workstations with constrained `C:` space, all intermediate build outputs and OSM files are kept on external/secondary storage (`G:\gabay_builds\`).

---

## 2. Generating the Offline Vault (Desktop)

### Step 1: Download Canonical Philippines PBF Extract
Run the failsafe download script:
```bash
python scripts/fetch_philippines_pbf.py
```
This fetches `philippines-latest.osm.pbf` directly to `G:\gabay_builds\osm\philippines.osm.pbf` with resumable HTTP range headers.

### Step 2: Compile the Binary Road Graph & POI Database
Run the release compiler:
```bash
.\target\release\map_builder.exe -i "G:\gabay_builds\osm\philippines.osm.pbf" -g "G:\gabay_builds\ph.gabay" -p "G:\gabay_builds\pois_fts.sqlite" -b "14.0,120.0,16.5,122.0"
```
*(Omit the `-b` bounding box flag to compile the entire Philippine archipelago).*

### Step 3: Run Desktop Verification Benchmark
Run the desktop test harness to verify routing accuracy against Google Maps before touching the phone:
```bash
cargo run --release --bin desktop_router_test --manifest-path rust_core/Cargo.toml
```

---

## 3. Zero-Touch Automatic Provisioning to Android Devices

To get the offline vault files onto the phone (`/sdcard/Documents/Gabay/`) without manual file explorer drag-and-drop:

### Method A: One-Click Desktop Sync Script (Recommended)
Connect your Android phone via USB cable or wireless ADB, then double-click:
```cmd
scripts\sync_vault_to_device.bat
```
**What this script does automatically:**
1. Checks for connected Android devices via ADB.
2. Creates the directory `/sdcard/Documents/Gabay/` on the device.
3. Automatically pushes `G:\gabay_builds\ph.gabay` and `G:\gabay_builds\pois_fts.sqlite`.
4. Completes in ~10 seconds.

### Method B: In-App Asset Unpacking (Standalone Production APK)
For standalone distribution without requiring ADB or desktop scripts:
1. **Regional Bundling**: A regional extract (e.g. Central Luzon / Nueva Ecija) is ~25 MB compressed with gzip (`ph.gabay.gz`).
2. Placed inside `android_app/app/src/main/assets/vault/`.
3. On first launch, `VaultProvisioner.kt` in the Android app checks if `/sdcard/Documents/Gabay/ph.gabay` exists. If missing, it extracts the asset stream to storage in the background while displaying a first-run setup indicator.

### Method C: One-Click In-App Cloud Download
For nationwide maps (270MB+):
- Hosted as a release asset on GitHub Releases or a Cloudflare R2 bucket.
- An in-app "Download Philippine Offline Map" button fetches and verifies the checksum directly to `/sdcard/Documents/Gabay/`.
