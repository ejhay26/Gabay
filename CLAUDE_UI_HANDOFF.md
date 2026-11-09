# Gabay: Android Kotlin UI Architecture & Handoff Guide

## 1. Project Separation & Boundaries
- **`android_app/`**: The primary Android Kotlin production application (Jetpack Compose + MapLibre Native SDK + Google Built-in TTS + Android Auto).
- **`rust_core/`**: High-performance Rust backend compiled to `libgabay_core.so` via UniFFI (A* routing, FTS5 spatial index, live GPS tracker).
- **`crates/`**: Pure-Rust algorithmic libraries and isolated desktop prototypes (set aside for desktop; completely separated from Android).

---

## 2. Important Architectural Rules for Claude
> [!IMPORTANT]
> 1. **No AI Slop / No Clutter**: Do NOT replicate or bring back the old messy Kotlin UI (no fake liquid glass compute shaders, no 3,000-line god activities, no tacky sci-fi gimmicks).
> 2. **Clean Terminology**: Do NOT use "cockpit" or "copilot". Use standard navigation terms: `NavigationHud`, `GuidanceBar`, `VoiceAssistant`, `RouteSheet`.
> 3. **Completely Fresh Modern Design**: Design a fresh, clean interface (think Apple Maps / minimal Bauhaus navigation: clean typography, calm Earthy/Sand/Sage tones, clear contrast, fluid gesture animations).
> 4. **MapLibre Native Vector Map**: The vector renderer is `org.maplibre.gl:android-sdk:13.6.1` (already in `build.gradle.kts`), reading `philippines_vector.pmtiles` from `/sdcard/Documents/Gabay` via a local tile server.

---

## 3. Kotlin Architectural Structure (`android_app`)

The old 2,900-line monolith has been replaced by a clean MVVM structure:

```
android_app/app/src/main/java/dev/gabay/app/
├── MainActivity.kt                # ~100-line clean entry point (permissions, edge-to-edge)
├── NavigationService.kt           # Foreground service for turn-by-turn navigation & notifications
├── car/                           # Android Auto CarAppService & Projected bridge
│
├── core/
│   ├── VaultManager.kt            # Resolves /sdcard/Documents/Gabay files:
│   │                              #   - ph.gabay (CSR road graph)
│   │                              #   - pois_fts.sqlite (FTS5 search index)
│   │                              #   - philippines_vector.pmtiles (MapLibre vector tiles)
│   │                              #   - *.gguf models
│   ├── PermissionManager.kt       # Real Android 11-15 storage (MANAGE_EXTERNAL_STORAGE)
│   │                              # & location permissions flow
│   ├── TtsManager.kt              # Google Built-in Android TextToSpeech (en-PH / en-US)
│   └── EngineBridge.kt            # Coroutine-safe wrapper over UniFFI GabayEngine (Dispatchers.IO)
│
├── viewmodel/
│   └── MapViewModel.kt            # Single source of truth:
│                                  #   - Location tracking
│                                  #   - FTS search queries & debouncing
│                                  #   - POI selection & Route calculation
│                                  #   - Turn-by-turn navigation & Voice TTS cues
│
└── ui/
    ├── theme/
    │   ├── Color.kt               # Earthy Minimal tokens (Cream, Terracotta, Sage, Slate)
    │   ├── Type.kt                # Typography scale
    │   └── Theme.kt               # Material3 GabayTheme (Light / Dark mode)
    ├── components/
    │   ├── SearchPill.kt          # Floating search input & hit results
    │   ├── RouteSheet.kt          # Destination preview, distance, time, start navigation
    │   ├── NavigationHud.kt       # Navigation HUD (speed, distance, next turn instruction)
    │   └── PermissionBanner.kt    # Permission request prompts triggering system intents
    └── MainScreen.kt              # Root screen orchestrating the map, search, sheets & HUD
```

---

## 4. Key Capabilities & Integrations Ready to Use

1. **MapLibre GL Native (`org.maplibre.gl:android-sdk:13.6.1`)**:
   - Integrated in `android_app/app/build.gradle.kts`. Ready to be wired into `ui/MainScreen.kt` for native 60/120fps hardware-accelerated vector map rendering.
2. **Real Storage Permissions**:
   - `PermissionManager.createManageStorageIntent(context)` prompts for All Files Access on Android 11+ so `/sdcard/Documents/Gabay` is fully readable.
3. **Google Built-in TTS**:
   - `TtsManager.speak(text)` handles audio cues without stutter.
4. **Offline Search & Routing**:
   - `EngineBridge` runs FTS queries on `pois_fts.sqlite` and A* calculations on `ph.gabay` asynchronously on `Dispatchers.IO`.

---

## 5. Claude's Next Tasks
1. Overhaul `ui/MainScreen.kt` with a fresh, clean modern layout hosting the MapLibre view.
2. Build fluid spring animations for `SearchPill.kt` (expanding smoothly from pill into search results) and gesture-dismissible sheets for `RouteSheet.kt`.
3. Style `NavigationHud.kt` with a sleek, readable turn guidance display.
