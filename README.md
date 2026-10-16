# Gabay (`dev.gabay.app`)

> **100% Offline Multi-Vehicle Navigation, 3D/Topography/Satellite Map, & Local AI Co-Pilot for the Philippines**

Gabay is an offline-first navigation engine engineered for the unique topography, road networks, and vehicular dynamics of the Philippines (Metro Manila to the Cordilleras). Built with a high-performance **Rust Core Engine**, a native **Kotlin Android Shell**, and hardware-accelerated **MapLibre Native**.

---

## Key Highlights

- **100% Offline Navigation**: Full multi-vehicle turn-by-turn routing (Scooter, Motorcycle, Car, Bicycle, Pedestrian) computed locally without cellular connection.
- **Strict Philippine Borders with Elastic Bounce**: The map camera is strictly bounded to the Philippine archipelago (`[116.5, 4.5]` to `[127.0, 21.5]`). Panning past the national border is prohibited and smoothly bounces back via spring elasticity.
- **Multi-Layer Spatial Engine**:
  - **Vector Streets & Buildings**: Detailed OpenStreetMap vector roads with 3D polygon extrusions for landmarks and structures.
  - **Single-Zoom Macro Satellite Peek**: Ultra-low storage satellite base layer (Zoom 10-11, ~150-250 MB nationwide) with smooth overzooming for peeking at terrain, rivers, and forests without 100+ GB bloat.
  - **Offline Topography Contours (`[TOPO]`)**: Lightweight 50m/100m elevation contours (~60 MB) for mountain passes, steep ascents, and ridge navigation.
  - **On-Demand Corridor Cache**: High-resolution Zoom 14-16 satellite imagery cached strictly along active route corridors on-demand.
- **Low-Resource Local Co-Pilot Agent**: Powered by an embedded `Qwen2.5-0.5B-Instruct-Q4_K_M` running a **Two-Tier Gatekeeper**. Monitored via a zero-cost deterministic Rust state engine; LLM inference only triggers on critical maneuvers or driver voice queries, preventing CPU overheating and battery drain.
- **Tactical High-Contrast Cockpit UI**: Sunlight-readable `#0A0D10` dark theme, monospace tabular numerals (`tnum`) for 1Hz speed and distance stability, glove-friendly &ge; 48dp touch targets, and zero AI slop / decorative emojis.
- **Ultra-Compact App Bundle**: Engineered with R8/ProGuard minification, resource shrinking, ABI filtering (`arm64-v8a`), and Rust symbol stripping to keep the APK under 20 MB.

---

## Screenshots

| 3D Vector Cockpit | Topography & Contours | Macro Satellite Peek |
| :---: | :---: | :---: |
| ![Cockpit View](docs/screenshots/cockpit_3d.png) | ![Topography View](docs/screenshots/topography_mode.png) | ![Satellite Peek](docs/screenshots/satellite_peek.png) |

*(Screenshots are updated and verified during automated Phase QA)*

---

## Systems Architecture

```text
┌────────────────────────────────────────────────────────┐
│                   Android Kotlin Shell                 │
│  - Jetpack Compose Cockpit HUD & Tactical Dark Theme   │
│  - MapLibre Native SDK (Vector + 3D + Satellite + Topo) │
│  - Persistent 1Hz GPS Navigation ForegroundService     │
│  - Offline Audio TTS Ducking Controller               │
└───────────────────────────▲────────────────────────────┘
                            │ UniFFI Bridge (proc-macro)
┌───────────────────────────▼────────────────────────────┐
│                    Rust Core Engine                    │
│  - Multi-Modal Graph Router (A* / Dijkstra + Bitmask)  │
│  - 1Hz R-Tree Road Snapper & EMA Pace Calculator       │
│  - Offline SQLite FTS5 Landmark & Barangay Search      │
│  - Two-Tier Gatekeeper Co-Pilot & Frozen KV-Cache LLM  │
└────────────────────────────────────────────────────────┘
```

For full architectural deep-dives, see:
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)
- [docs/TECH_STACK.md](docs/TECH_STACK.md)
- [docs/USER_GUIDE.md](docs/USER_GUIDE.md)

---

## Build & Installation

### Prerequisites
- Android NDK 28+ & Android SDK 34+
- Rust 1.80+ with `aarch64-linux-android` target
- Connected physical device or emulator (`arm64-v8a`) via ADB

### Build Steps
```bash
# 1. Compile Rust core engine for Android
cargo ndk -t arm64-v8a -o android_app/app/src/main/jniLibs build --release -p rust_core

# 2. Build Android Debug APK
cd android_app
./gradlew assembleDebug

# 3. Deploy to device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## License

This project is licensed under the terms of the [LICENSE](LICENSE) file.
