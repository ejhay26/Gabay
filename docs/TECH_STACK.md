# Gabay Tech Stack & Dependencies

Specification of all tools, libraries, and compilation parameters used across the Gabay ecosystem.

---

## 1. Core Platform & Toolchains

| Component | Target / Version | Description |
| :--- | :--- | :--- |
| **Operating System** | Android 10+ (API 29 to 36) | Target OS for mobile deployment |
| **Architecture** | `arm64-v8a` | Exclusively 64-bit ARM target for performance and compact bundle size |
| **Language (Core)** | Rust 2021 Edition (1.80+) | High-performance memory-safe routing, spatial indices, and local LLM runtime |
| **Language (App)** | Kotlin 2.0+ / JDK 17 | Modern Android app shell with Jetpack Compose |
| **Android NDK** | NDK 28.2.13676358 | Cross-compilation toolchain for Rust JNI shared library (`libgabay_core.so`) |
| **Build Tools** | Android SDK Build-Tools 34.0.0 / 36.0.0 | AAPT2, D8/R8 compiler, and signing tools |

---

## 2. Rust Core Crates (`rust_core` & `map_builder`)

| Crate | Version | Purpose |
| :--- | :--- | :--- |
| `uniffi` | `0.32.2` | Proc-macro automatic FFI bridge to Kotlin |
| `rstar` | `0.12` | High-performance R-Tree spatial indexing for 1Hz road snapping |
| `geo` | `0.28` | Geometric primitives, distance calculations, and polyline operations |
| `rusqlite` | `0.31` (bundled `fts5`) | Offline SQLite database with Full-Text Search for POIs and barangays |
| `memmap2` | `0.9` | Zero-copy memory mapping for multi-hundred megabyte road graphs |
| `rkyv` / `bincode` | `0.8` / `1.3` | Zero-overhead binary serialization of routing networks |
| `osmpbf` | `0.3` | Fast parallel OSM PBF parser for host `map_builder` |
| `llama-cpp-2` / `candle-core` | `latest` | Quantized GGUF inference runtime for Qwen2.5-0.5B co-pilot |

---

## 3. Android Dependencies & Libraries

| Dependency | Version | Purpose |
| :--- | :--- | :--- |
| `org.maplibre.gl:android-sdk` | `13.6.1` | Hardware-accelerated vector, 3D, topography, and raster tile renderer |
| `androidx.compose.ui:ui` | `1.7+` | Modern declarative UI framework |
| `androidx.compose.material3:material3` | `1.3+` | Foundation components adapted to custom Anti-Slop Dark theme |
| `androidx.core:core-ktx` | `1.13+` | Kotlin extensions for Android system APIs |
| `androidx.lifecycle:lifecycle-service` | `2.8+` | Immortal ForegroundService lifecycle binding |
| `net.java.dev.jna:jna` | `5.14.0@aar` | Low-level C library loading required by UniFFI |

---

## 4. Build Configuration & Minification

### Android Release Configuration (`build.gradle.kts`)
```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
        ndk {
            abiFilters.add("arm64-v8a")
        }
    }
}
```

### Rust Size Optimization Profile (`Cargo.toml`)
```toml
[profile.release]
opt-level = "z"         # Optimize for binary size
lto = true              # Full link-time optimization
codegen-units = 1       # Single code generation unit for maximum dead-code elimination
panic = "abort"         # Abort on panic to remove stack unwinding landing pads
strip = "symbols"       # Strip symbol tables and debug info
```
