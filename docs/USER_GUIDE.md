# Gabay User Guide & Field Manual

A practical operational guide for using **Gabay (`dev.gabay.app`)** on Android devices.

---

## 1. First-Time Setup & Device Vault

Gabay operates 100% offline. All map data, routing graphs, and AI models reside in your device's external storage directory:

```text
/sdcard/Documents/Gabay/
```

| Vault Asset | Approximate Size | Function |
| :--- | :--- | :--- |
| `model.gguf` | 468 MB | Qwen2.5-0.5B-Instruct-Q4_K_M local AI co-pilot |
| `ph.gabay` | 1.17 MB | Spatial binary A* routing graph |
| `pois_fts.sqlite` | 152 KB | SQLite FTS5 landmark and street search database |
| `metro_manila_roads_major.geojson` | 4.45 MB | Expressways & primary arterial corridors |
| `metro_manila_roads_minor.geojson` | 1.99 MB | Minor streets and residential road grid |
| `metro_manila_water.geojson` | 398 KB | Waterways, rivers, and coastlines |
| `metro_manila_parks.geojson` | 309 KB | Urban parks and nature reserves |

The app requests `MANAGE_EXTERNAL_STORAGE` and `ACCESS_FINE_LOCATION` on first launch to access this vault and read GPS data.

---

## 2. Redesigned Cockpit HUD

The Gabay interface is engineered for high visibility in all lighting conditions and weathers, matching automotive dark cockpits with Apple-style fluid micro-interactions:

```text
┌────────────────────────────────────────────────────────┐
│  [≡]  Search places, roads, landmarks...          [⌕]  │ Top Floating Search Bar (FTS5)
└────────────────────────────────────────────────────────┘
     ┌──────────────┐                  ┌──────────────┐
     │ 0.5 KM REM.  │                  │ ~2-5 MIN EST │ Floating Telemetry Pills (Active Route)
     └──────────────┘                  └──────────────┘
                            ▲
                            │
                   Active Route Polyline
                 (Neon Mint #00F5D4 with
                   Dark Casing #00382C)
                            │
                            ▼
     ┌─────┐                 ┌─────┐             ┌─────┐
     │ [🚗]│                 │ [🎙]│             │ [🎯]│ Circular Action Buttons
     └─────┘                 └─────┘             └─────┘
                  ┌───────────────────────┐
                  │  [←]     0   KM/H [→] │ Middle-Bottom Speedometer
                  │             0 mph     │
                  └───────────────────────┘
```

### Visual Components Breakdown
- **Top Google Maps-Style Search Bar**: Floating pill with rounded corners (26dp), left Menu button, clear button, and real-time SQLite FTS5 autocomplete suggestions.
- **Middle-Bottom Speedometer**: Large clean speed numeral in `km/h` (displays `0` when stationary — zero fake telemetry). Small `mph` readout on bottom right. Subtle turn indicator arrows on left and right (`←` and `→`) that blink when approaching a turn.
- **Floating Telemetry Pills**: Remaining distance (e.g. `0.5 KM REMAINING`) and dynamic ETA pace (`~2-5 MIN EST PACE`) float unobtrusively at the sides only during active navigation.
- **Circular Floating Action Buttons**:
  - **Bottom Left**: Fused Map Style toggle (cycles Street -> Topo -> Satellite -> 3D perspective).
  - **Center**: Prominent Talk / Co-Pilot button (opens the live transcript drawer).
  - **Bottom Right**: Recenter GPS button (`🎯`).

---

## 3. Top Menu & Drive History

Tapping the `[≡]` Menu button on the top search bar opens the **Gabay Cockpit & History Drawer**:
- **Vehicle Mode Selector**: Switch between `[SCOOTER]` (avoids tollways/expressways), `[CAR]` (standard routing), and `[WALK]` (prioritizes paths and walkways).
- **Successful Drives History**: Displays your past successful drives with trip distance, duration, and timestamp (e.g., *"Makati CBD to BGC - 4.2 KM • 14 MIN"*).

---

## 4. Local AI Co-Pilot & Transcript Drawer

Tapping the circular microphone button `[🎙]` opens the Apple-fluid **Live Agent Co-Pilot Sheet**:
- **Interactive Transcript**: Displays chronological conversation history modeled after the Antigravity IDE.
- **Quick Action Pills**: Tap `[Nearest Gas]`, `[Avoid Tollways]`, `[ETA & Pace]`, or `[Traffic Ahead]` for instant rally-style updates.
- **CPU Protection & Deduplication**: All queries are checked against an in-memory cache to prevent repetitive prompts from re-triggering LLM inference, keeping device CPU cool and battery usage minimal.

---

## 5. Multi-Country Extensibility

Gabay is not hardcoded to one geography. To use Gabay in another country:
1. Export your region's road network to `.gabay` and `.geojson` layers using the provided build scripts.
2. Index your region's landmarks into a `pois_fts.sqlite` database.
3. Drop the files into `/sdcard/Documents/Gabay/`. Gabay will automatically adopt your custom boundaries, road network, and search index!
