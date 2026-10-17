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
| `ph.gabay` | 225.5 MB | Nationwide spatial binary A* road-following routing graph (2.4M nodes, 4.99M edges) |
| `pois_fts.sqlite` | 101.6 MB | SQLite FTS5 database with 23,558 places/POIs and 358,392 indexed road polylines |
| `philippines_vector.pmtiles` | 75.3 MB | Nationwide vector map tiles (streets, rivers, buildings, places, POIs) |
| `topography_contours.pmtiles` | 17.3 MB | Tactical dark monochrome elevation relief contours (zero red/green glare) |
| `satellite_base.pmtiles` | 1.3 MB | Locked overview satellite raster imagery |

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
     ┌─────┐      ┌─────┐    ┌─────┐    ┌─────┐    ┌─────┐
     │ [🗂]│      │ [🧭]│    │ [🎙]│    │ [📐]│    │ [🎯]│
     └─────┘      └─────┘    └─────┘    └─────┘    └─────┘
   Style Dial    Compass     Co-Pilot    2D / 3D    Recenter
                   ┌───────────────────────┐
                   │  [←]     0   KM/H [→] │ Middle-Bottom Speedometer
                   │             0 mph     │
                   └───────────────────────┘
```

### Visual Components Breakdown
- **Top Google Maps-Style Search Bar**: Floating pill with rounded corners (26dp), left Menu button, clear button, and real-time SQLite FTS5 autocomplete suggestions. Tapping outside or clearing immediately dismisses the software keyboard.
- **Middle-Bottom Speedometer**: Large clean speed numeral in `km/h` (displays `0` when stationary — zero fake telemetry). Small `mph` readout on bottom right. Directional blinking arrows (`←` and `→`) for forthcoming turns.
- **Floating Telemetry Pills**: Remaining distance (e.g. `3.0 KM REMAINING`) and dynamic ETA pace (`~12-16 MIN EST PACE`) float at the sides only during active navigation.
- **Action Buttons Dock**:
  - **Map Style Speed Dial `[🗂]`**: Staggered upward animation offering `STREET`, `TOPO`, and `SATELLITE` modes with delayed rightward text badges.
  - **Compass Needle `[🧭]`**: Automatically appears when map rotation exceeds 2°. Shows North heading indicator and smoothly eases back to 0° True North when tapped.
  - **Center Mic / Co-Pilot `[🎙]`**: Opens the two-way voice and text agent co-pilot drawer.
  - **2D / 3D Perspective Toggle `[📐]`**: Seamlessly transitions camera pitch between 0° overhead 2D and 55° tilted 3D on any active map layer.
  - **Recenter GPS `[🎯]`**: Re-centers camera onto vehicle coordinates.

---

## 3. Top Menu & Drive History

Tapping the `[≡]` Menu button on the top search bar opens the **Gabay Cockpit & History Drawer**:
- **Vehicle Mode Selector**: Switch between `[SCOOTER]` (avoids tollways/expressways), `[CAR]` (standard routing), and `[WALK]` (prioritizes paths and walkways).
- **Successful Drives History**: Displays your past successful drives with trip distance, duration, and timestamp (e.g., *"Makati CBD to BGC - 4.2 KM • 14 MIN"*).

---

## 4. Local AI Co-Pilot & Natural Voice Agent

Tapping the circular microphone button `[🎙]` opens the **Live Agent Co-Pilot Sheet**:
- **Voice & Text Input**: Hold or tap the voice mic to speak, or type into the integrated text input bar.
- **Contextual Awareness**: Real-time parked vs. moving status. When stationary, the agent correctly reports: *"Parked at current location (15.261, 120.910). Speed is 0 km/h. Map ready."*
- **Agentic Commands**:
  - *"Find nearest gas station"* -> Performs live spatial query and pins nearest fuel station.
  - *"Navigate to pin"* / *"Start route"* -> Computes road route and initiates navigation.
  - *"Switch to satellite"* / *"Switch to topo"* / *"Switch to street"* -> Toggles layer.
  - *"Toggle 3D"* -> Tilts camera to 55° perspective.
  - *"Point north"* -> Resets bearing to True North.
  - *"Cancel route"* -> Stops active navigation session.

---

## 5. Long-Press Pin Inspector & Route Creation

1. Long-press any location on the map to drop a tactical pin.
2. The **Pin Inspector Sheet** appears with exact coordinates, distance from vehicle, and a copy button.
3. Tap **"NAVIGATE TO PIN"**: The offline Rust engine computes an A* route from your position, displays the glowing cyan route polyline, fits camera bounds, and begins turn-by-turn navigation.

---

## 6. Multi-Country Extensibility

Gabay is not hardcoded to one geography. To use Gabay in another country:
1. Export your region's road network to `.gabay` and `.geojson` layers using the provided build scripts.
2. Index your region's landmarks into a `pois_fts.sqlite` database.
3. Drop the files into `/sdcard/Documents/Gabay/`. Gabay will automatically adopt your custom boundaries, road network, and search index!

