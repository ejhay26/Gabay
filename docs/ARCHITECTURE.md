# Gabay Systems Architecture

This document details the core architectural specifications of **Gabay (`dev.gabay.app`)**.

---

## 1. High-Level System Design

Gabay is structured as a decoupled two-layer system:
1. **The Native Rust Core (`rust_core`)**: The computational engine handling spatial indexing, multi-vehicle routing, off-route detection, Kalman-smoothed GPS road snapping, and offline co-pilot decision gating.
2. **The Kotlin Android Shell (`android_app`)**: The UI and hardware integration layer handling MapLibre OpenGL rendering, Jetpack Compose cockpit HUD, audio focus ducking, and Android ForegroundService lifecycle management.

The boundary between Rust and Kotlin is bridged by **UniFFI (`0.32.2`)** using proc-macro annotations, generating type-safe Kotlin bindings with zero manual JNI boilerplate.

```text
android_app (Kotlin)
  │
  ├── UI & Rendering
  │     ├── MapLibre GL Native (Vector PMTiles + 3D Extrusions + Topo + Raster Sat)
  │     ├── Cockpit HUD (Top maneuver banner, bottom 3-column telemetry dock)
  │     └── Global Icon Registry (GabayIcons.kt)
  │
  ├── Services & Audio
  │     ├── NavigationService (Persistent 1Hz ForegroundService)
  │     └── AudioController (TTS + AudioFocusRequest ducking)
  │
  └── UniFFI Bridge (Generated Kotlin bindings)
        │
        ▼
rust_core (Rust / libgabay_core.so)
  │
  ├── router.rs       (Multi-modal bitmask graph solver A* / Dijkstra)
  ├── tracker.rs      (1Hz R-Tree road snapping & EMA rolling pace calculator)
  ├── poi_search.rs   (Offline SQLite FTS5 landmark & barangay search)
  └── copilot.rs      (Two-Tier Gatekeeper + frozen KV-cache Qwen2.5 runner)
```

---

## 2. Low-Resource Two-Tier Co-Pilot Architecture

A critical requirement of Gabay is maintaining an active, always-on AI co-pilot without draining the battery or overheating mobile CPUs through repetitive LLM prompt evaluation.

### Tier 1: Deterministic Rust Event Gatekeeper (Zero CPU Cost)
- Runs at 1Hz inside `tracker.rs` on incoming GPS ticks.
- Evaluates spatial rules in pure Rust:
  - Is the vehicle within 200m of an upcoming maneuver?
  - Has the vehicle deviated > 35m from the planned polyline (off-route)?
  - Has average rolling speed dropped by > 60% over a 15-second window (congestion / stall)?
- If no spatial threshold is crossed, the LLM is **NEVER** invoked.

### Tier 2: Throttled Local LLM Inference (Qwen2.5-0.5B-Instruct-Q4_K_M)
- Invoked only when Tier 1 detects a critical event or when the rider taps `[GABAY VOICE]`.
- Pre-warmed frozen KV-cache with a terse rally-pace system prompt.
- Output generation is strictly capped at &le; 25 tokens.
- Strict 15-second cooldown ensures the model is never invoked in back-to-back rapid succession.

---

## 3. Offline Multi-Layer Spatial Pipeline

All heavy spatial datasets reside on the device at `/sdcard/Documents/Gabay/`:
- `philippines_vector.pmtiles`: OpenStreetMap vector tiles covering nationwide roads, residential alleys, water, and building footprints.
- `satellite_base.pmtiles`: Low-storage macroscopic satellite overview locked to **Zoom 10-11** (~150-250 MB). MapLibre smoothly overzooms this raster base beneath sharp vector roads, allowing riders to peek at terrain (forest vs mountain vs river) without requiring 100+ GB of tile storage.
- `topography_contours.pmtiles`: Vector elevation contours (50m / 100m) enabling topographical slope and ridge visualization (`[TOPO]`).
- `satellite_corridor.mbtiles`: On-demand high-resolution Zoom 14-16 satellite imagery cached strictly within a 1.5 km buffer of active routes.

---

## 4. Geographic Border Bounding & Elasticity

The camera viewport is bounded strictly to the Philippine national territory:
- **Bounding Box**: `[116.5, 4.5]` to `[127.0, 21.5]`.
- **Elastic Bounce-Back**: Camera gesture listeners monitor screen pan events. If the camera center or focal point extends past national borders, a spring animation interpolates the viewport back within national borders, physically prohibiting navigation outside the Philippines.

---

## 5. Binary Size & Minification Architecture

To guarantee fast installation and an ultra-lean footprint (< 20 MB APK):
1. **R8 Code & Resource Shrinking**:
   - `isMinifyEnabled = true` and `isShrinkResources = true` in Android release builds.
   - Comprehensive ProGuard keep rules for UniFFI FFI structs and MapLibre JNI entry points.
2. **Rust Symbol Stripping & LTO**:
   - Release profile uses `opt-level = "z"`, `lto = true`, `codegen-units = 1`, `panic = "abort"`, and `strip = "symbols"`.
   - Strips debug symbols and unwinding tables, reducing `libgabay_core.so` size by ~70%.
3. **Single ABI Target**:
   - Targeted exclusively at modern `arm64-v8a` devices.
