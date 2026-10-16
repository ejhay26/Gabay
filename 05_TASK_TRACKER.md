# 05 — GABAY PRODUCTION TASK TRACKER

Tracking master state machine for **Gabay (`dev.gabay.app`)**.
Execution rule: Must follow the 6-Step Phase Gate Loop in `04_EXECUTION_LOOP_AND_QA_PROTOCOL.md` strictly one phase at a time. Zero stubs, zero emojis, zero gradients.
Commit rule: Every verified fix or completed task MUST be committed and pushed to `origin/main` immediately before proceeding.

---

## Workspace & Build Hardware Directives
- **Device Attached:** Tanzanite (`arm64-v8a`) on ADB.
- **Drive Constraint:** Drive `C:` low space (< 1 GB). All builds, caches, targets, and downloads reside on `G:\gabay_builds\`.
  - `target/` -> `G:\gabay_builds\cargo_target` (NTFS Junction active)
  - `local_assets/` -> `G:\gabay_builds\local_assets` (NTFS Junction active)
  - `gradle.user.home` -> `G:\gabay_builds\.gradle`
- **Bundle Size Optimization:** R8 minification (`isMinifyEnabled = true`, `isShrinkResources = true`) + Rust `[profile.release]` with `opt-level = "z"`, `lto = true`, `strip = "symbols"`. Target APK < 20 MB.
- **Geographic Gate & Multi-Country Extensibility:** Philippines flagship bounds (`[116.5, 4.5]` to `[127.0, 21.5]`) with elastic spring bounce-back. Dynamic detection from PMTiles metadata header allows international riders to drop in their own country's map tiles and automatically adapt borders.
- **Routing Engine:** Offline multi-modal A* / Dijkstra road solver computing turn-by-turn maneuvers, accurate distance, and live EMA pace.
- **Permissions & Voice Chat:** AndroidManifest with Location, Background Location, Foreground Service, Record Audio (two-way voice chat co-pilot), External Storage, and Wake Lock.
- **Map Layers:** 
  - Vector Streets & 3D Extrusions (`philippines_vector.pmtiles`)
  - Low-Storage Single-Zoom Macro Satellite Peek (`satellite_base.pmtiles`, Zoom 10-11, ~150-250MB with overzooming)
  - Offline Topography & Elevation Contours (`topography_contours.pmtiles`, ~60MB)
  - On-Demand High-Res Route Corridor Cache (`satellite_corridor.mbtiles`, ~5-10MB/trip)
- **Co-Pilot Rule:** Two-Tier Gatekeeper. Event-driven, low-token (<= 25 tokens), KV-cache frozen. Never fry CPU or loop prompts.
- **Design System:** Unified Tactical High-Contrast Dark (`#0A0D10`), Tabular monospace telemetry (`tnum`), sharp geometric icons (stroke `1.75dp`), zero emojis.
- **Documentation:** `README.md` and `docs/` (`ARCHITECTURE.md`, `TECH_STACK.md`, `USER_GUIDE.md`) updated with inspected screenshots.

---

## Phase 1: Workspace Toolchain, Build Redirection & Global Icon Registry
- [x] Root `Cargo.toml` workspace definition (`["tools/map_builder", "rust_core"]`) with size-optimized `[profile.release]`
- [x] Toolchain setup (`cargo-ndk`, `aarch64-linux-android` target verification)
- [x] SVG Icon Downloader script (`scripts/fetch_icons.sh` / `fetch_icons.ps1`) fetching clean geometric icons
- [x] Global Icon Registry file (`android_app/app/src/main/java/dev/gabay/app/ui/GabayIcons.kt` and vector drawables)
- [x] Android project scaffold with build configuration, R8 minification setup, and cache pointing to `G:\gabay_builds\`
- [x] Commit & push Phase 1 deliverables to remote

## Phase 2: Offline Asset Pipeline, Topography & Map Data Builder
- [x] `tools/map_builder` implementation in Rust (OSM PBF parser, multi-vehicle road graph `ph.gabay`, and SQLite FTS5 index)
- [x] Fast extract script and nationwide pipeline (`scripts/fetch_and_build_assets.sh`)
- [x] Bounded Philippines vector PMTiles + low-storage macroscopic satellite PMTiles (Zoom 10-11)
- [x] Offline Topography contour vector layer (`topography_contours.pmtiles`)
- [x] Quantized GGUF model downloader with size integrity validation (> 350 MB)
- [x] Idempotent ADB sync script to `/sdcard/Documents/Gabay/` with automated permission provisioning
- [x] Commit & push Phase 2 deliverables to remote

## Phase 3: Rust Core Engine (`rust_core`) & Low-Resource Co-Pilot
- [x] `rust_core/src/types.rs`: `VehicleMode`, `RouteOptions`, `HudTelemetry`, `PinInfo`
- [x] `rust_core/src/router.rs`: Multi-modal graph solver (Scooter vs Car vs Bike, toll & expressway avoidances)
- [x] `rust_core/src/tracker.rs`: 1Hz R-Tree road snapper, off-route delta detection, EMA rolling pace calculator
- [x] `rust_core/src/poi_search.rs`: Offline SQLite FTS5 landmark, barangay, and gas station search
- [x] `rust_core/src/copilot.rs`: Two-Tier Gatekeeper (deterministic event trigger, throttled LLM inference, zero CPU burn)
- [x] UniFFI interface & proc-macro exports (`rust_core/src/lib.rs`)
- [x] Android NDK cross-compilation pipeline producing stripped `libgabay_core.so` (`arm64-v8a`)
- [x] Rust unit test gate (`cargo test --workspace`) passing 100%
- [x] Commit & push Phase 3 deliverables to remote

## Phase 4: Android App Shell, MapLibre Canvas, Topography & Philippines Elastic Border
- [x] Unified Tactical Dark Theme (`Theme.kt`) with exact color tokens and typography scale
- [x] MapLibre Native SDK integration with offline PMTiles protocol reader
- [x] Philippines border gating (`116.5, 4.5` to `127.0, 21.5`) with smooth elastic bounce-back constraint
- [x] Vector street rendering, 3D building extrusions, single-zoom macro satellite toggle `[SAT]`, and topography toggle `[TOPO]`
- [x] ADB deployment to physical device (`69ydd6nfaycagajz`)
- [x] Zero crash / `UnsatisfiedLinkError` logcat verification
- [x] Check A: `uiautomator` screen hierarchy bounds check
- [x] Check B: `screencap` visual screenshot inspection against 5-Point Production Visual Rubric (Multimodal review)
- [x] Sync verified screenshots to `docs/screenshots/`
- [x] Commit & push Phase 4 deliverables to remote

## Phase 5: Cockpit HUD, Navigation Telemetry, & Mode Bar
- [x] Top Maneuver Banner: Sharp direction vectors from Global Icon Registry, distance in meters/km, road name, avoidance badges
- [x] Bottom Cockpit Dock: 3-column grid with live tabular speed (`tnum`), heading, remaining distance, rolling pace window
- [x] Glove-friendly interactive mode pills (`[SCOOTER]`, `[CAR]`, `[SAT]`, `[TOPO]`, `[3D]`, `[GABAY VOICE]`) with min 48dp touch targets
- [x] `PinSheet.kt`: Long-press coordinate inspector, clipboard copy, route creation
- [x] `NavigationService.kt`: Persistent 1Hz ForegroundService passing GPS to `rust_core` snapper
- [x] ADB deploy and automated XML bounds check + multimodal screenshot visual audit
- [x] Sync verified screenshots to `docs/screenshots/`
- [x] Commit & push Phase 5 deliverables to remote

## Phase 6: Voice Co-Pilot, TTS Audio Ducking, Full Simulated Drive & Documentation
- [x] `AudioController.kt`: Offline Android TTS with `AudioFocusRequest` audio ducking
- [x] Voice co-pilot integration: Rally-cockpit terse verbal cues (`"IN 200M RIGHT UNGKA FLYOVER"`, `"OFF-ROUTE +40M REROUTING"`)
- [x] Offline GPS track simulator for testing real Philippine route traversal
- [x] Verification script (`scripts/verify_phase.ps1`) passing all gates
- [x] Comprehensive documentation finalization: `README.md`, `docs/ARCHITECTURE.md`, `docs/TECH_STACK.md`, `docs/USER_GUIDE.md` with verified live screenshots
- [x] Commit & push Phase 6 deliverables to remote
