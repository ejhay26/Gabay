# 02 — SYSTEMS ARCHITECTURE & TECH STACK SPECIFICATION

## 1. CORE STACK & DEPENDENCY VERSIONS

- **Package Name:** `dev.gabay.app`
- **Rust Shared Library:** `libgabay_core.so` (`arm64-v8a`)
- **Persistent Device Vault:** `/sdcard/Documents/Gabay/`
- **FFI Bridge:** `uniffi = "0.32.2"` (proc-macro `#[uniffi::export]` mode)
- **Map Renderer:** `org.maplibre.gl:android-sdk:13.6.1` (Vulkan/OpenGL hardware-accelerated vector + raster + 3D extrusion renderer)
- **Spatial & Routing Crates:** `osmpbf`, `geo`, `rstar` (R-Tree), `fast_paths` or custom A*/Dijkstra with Contraction Hierarchies, `memmap2`, `rkyv` / `bincode`, `rusqlite` (bundled with `fts5` enabled)
- **Local LLM Runtime:** `llama-cpp-2` (or `candle-core` / `candle-transformers` if NDK CMake linkage fails on host) running `Qwen2.5-0.5B-Instruct-Q4_K_M.gguf`

---

## 2. WORKSPACE FILE TREE

```text
gabay/
├── Cargo.toml                        # Workspace: ["tools/map_builder", "rust_core"]
├── scripts/
│   ├── fetch_and_build_assets.sh     # Idempotent downloader & graph builder
│   └── verify_phase.sh               # Automated build, ADB deploy, uiautomator & screencap QA
├── tools/
│   └── map_builder/                  # Host CLI: parses .osm.pbf -> outputs ph.gabay & pois_fts.sqlite
│       ├── Cargo.toml
│       └── src/main.rs
├── rust_core/                        # Mobile Brain -> compiles to libgabay_core.so
│   ├── Cargo.toml
│   └── src/
│       ├── lib.rs                    # UniFFI exports & Engine lifecycle
│       ├── types.rs                  # VehicleMode, RouteOptions, HudTelemetry, PinInfo
│       ├── router.rs                 # Multi-modal bitmask graph solver (A* / Dijkstra)
│       ├── tracker.rs                # 1Hz R-Tree road snapper, off-route & EMA pace calculator
│       ├── poi_search.rs             # Offline SQLite FTS5 + R-Tree landmark/building search
│       └── copilot.rs                # Two-Tier Gatekeeper + KV-Cache frozen LLM runner
└── android_app/
    ├── build.gradle.kts
    └── app/src/main/
        ├── AndroidManifest.xml       # ForegroundService(location), MANAGE_EXTERNAL_STORAGE
        ├── jniLibs/arm64-v8a/
        │   └── libgabay_core.so
        └── java/dev/gabay/app/
            ├── uniffi/               # Auto-generated UniFFI Kotlin bindings
            ├── MainActivity.kt       # MapLibre canvas, 3D/Satellite layers, Pin drop, HUD
            ├── NavigationService.kt  # Immortal 1Hz GPS ForegroundService
            ├── AudioController.kt    # Offline TTS + AudioFocus Ducking + Media Volume control
            └── ui/
                ├── Theme.kt          # Anti-slop tactical dark palette & typography
                ├── GabayIcons.kt     # Global Icon Registry (Geometric vector SVGs)
                ├── CockpitHud.kt     # Top maneuver banner, bottom telemetry dock, mode bar
                └── PinSheet.kt       # Long-press coordinate inspector, clipboard copy, A->B router

---

## 3. BUNDLE SIZE OPTIMIZATION & MINIFICATION (TARGET: < 20 MB APK)

1. **Android Gradle Minification (R8 & ProGuard):**
   - Enable code shrinking and resource shrinking in `build.gradle.kts`:
     ```kotlin
     buildTypes {
         release {
             isMinifyEnabled = true
             isShrinkResources = true
             proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
         }
         debug {
             ndk { abiFilters.add("arm64-v8a") }
         }
     }
     ```
   - Target only `arm64-v8a` to eliminate multi-ABI binary bloat.

2. **Rust Binary Stripping & Link-Time Optimization (`Cargo.toml`):**
   - Size-optimized release profile for `libgabay_core.so`:
     ```toml
     [profile.release]
     opt-level = "z"         # Optimize for binary size
     lto = true              # Thin/Full Link Time Optimization across crates
     codegen-units = 1       # Maximum dead-code elimination
     panic = "abort"         # Removes stack unwinding tables (~25% size reduction)
     strip = "symbols"       # Strips all debug symbols
     ```

---

## 4. MULTI-LAYER OFFLINE SPATIAL ENGINE

1. **Vector Base Layer (`philippines_vector.pmtiles`):**
   - Comprehensive street network, building footprints, barangays, water, and POIs.
2. **3D Building Extrusions:**
   - Dynamic hardware-accelerated 3D polygon extrusion using `fill-extrusion` based on `render_height` / `height` attributes.
3. **Single-Zoom Macro Satellite Peek (`satellite_base.pmtiles`):**
   - Locked strictly to Zoom level 10-11 (~150-250 MB total nationwide) with bilinear overzooming in MapLibre.
   - Provides macroscopic terrain context (forests, rivers, mountain ridges, coastlines) at zero storage bloat compared to 100+ GB full-pyramid satellite imagery.
4. **Offline Topography & Elevation Contours (`topography_contours.pmtiles`):**
   - 50m / 100m elevation contour vector lines (~60 MB nationwide).
   - Toggled via `[TOPO]` button to visualize slopes, mountain passes, and elevation changes.
5. **Dynamic Country/Region Elastic Bounds & Multi-Country Extensibility:**
   - Default flagship region: Philippines (`[116.5, 4.5]` to `[127.0, 21.5]`).
   - **Universal Country Adoption:** The spatial engine dynamically reads the metadata header of the vector PMTiles file in `/sdcard/Documents/Gabay/`. Any country's PMTiles contains its geographic bounding box (`bounds: [min_lon, min_lat, max_lon, max_lat]`).
   - If a rider replaces or supplements the files with another country (e.g., `japan_vector.pmtiles` and `japan.gabay`), Gabay dynamically binds the camera elastic bounce-back constraint to that country's exact borders without requiring code modification.

---

## 5. APP PERMISSIONS & VOICE CHAT PIPELINE

### A. Android System Permissions
The app mandates these permissions in `AndroidManifest.xml` for full offline vehicular operation:
- `android.permission.ACCESS_FINE_LOCATION` & `ACCESS_COARSE_LOCATION`: 1Hz GPS polling for road snapping and telemetry.
- `android.permission.ACCESS_BACKGROUND_LOCATION`: Uninterrupted navigation when the phone screen is locked or docked.
- `android.permission.FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_LOCATION`: Android 14+ persistent navigation notification.
- `android.permission.RECORD_AUDIO`: Voice chat interaction with the local AI co-pilot.
- `android.permission.MANAGE_EXTERNAL_STORAGE`: Direct I/O access to the offline `/sdcard/Documents/Gabay/` vault.
- `android.permission.WAKE_LOCK`: Keeps CPU active during turn-by-turn guidance.
- `android.permission.POST_NOTIFICATIONS`: Android 13+ status bar telemetry banner.

### B. Two-Way Voice Chat Architecture
- **Inbound Audio (Rider -> Co-Pilot):** Tap `[GABAY VOICE]` or voice trigger activates `AudioRecord` / local offline speech recognition to capture rider queries (`"Where is the nearest gas station?"`, `"Avoid tollways"`).
- **Outbound Audio (Co-Pilot -> Rider):** Offline Android `TextToSpeech` with `AudioFocusRequest.Builder(AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)` so vehicle intercom, podcasts, or music automatically duck to 20% volume during tactical turn announcements and co-pilot responses.