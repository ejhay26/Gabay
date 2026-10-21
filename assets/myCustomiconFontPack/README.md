# Apex Alon Icon Font Pack

A unique, production-grade modular icon font pack designed for tactical motorcycle cockpit HUDs, automotive navigation, and mobile/web applications.

## Directory Structure
- `svg/` - Individual production-ready vector SVGs (24x24 standard grid).
- `font/` - Web font stylesheet (`apex-alon.css`) and unicode font glyph mappings (`\e900` - `\e90F`).
- `compose/` - Ready-to-drop Jetpack Compose `ImageVector` definitions (`ApexAlonIcons.kt`).
- `react/` - TypeScript / React SVG components with full prop typing (`index.tsx`).
- `preview.html` - Interactive standalone visual showcase & clipboard copy tool (open in any browser).
- `manifest.json` - Complete metadata catalog for bundlers and design systems.
- `package.json` - Standard npm package definition.

## Available Icons (16)
- **Scooter** (`scooter`) - `\e900` - [vehicles] motorcycle, scooter, ride, two-wheeler, cockpit
- **Tactical Compass Star** (`compass_star`) - `\e901` - [navigation] compass, north, heading, orientation, star
- **Waypoint Beacon Pin** (`waypoint_pin`) - `\e902` - [navigation] pin, marker, destination, location, waypoint
- **Fuel Pump** (`fuel_pump`) - `\e903` - [places] gas, petrol, station, fuel, energy
- **Road Centerline** (`road_centerline`) - `\e904` - [navigation] road, highway, lane, arterial, route
- **3D Perspective Cube** (`perspective_cube`) - `\e905` - [interface] 3d, cube, perspective, isometric, spatial
- **Wrench Tool** (`wrench_tool`) - `\e906` - [maintenance] wrench, tool, maintenance, repair, service
- **Speaker Waves** (`speaker_waves`) - `\e907` - [audio] speaker, audio, voice, sound, volume
- **Speedometer Dial** (`speedometer_dial`) - `\e908` - [telemetry] speed, speedometer, gauge, tachometer, velocity
- **Shield Crest** (`shield_crest`) - `\e909` - [security] shield, safety, secure, protection, guardrail
- **Battery Bolt** (`battery_bolt`) - `\e90A` - [telemetry] battery, power, charge, electric, spark
- **Gear Settings** (`gear_settings`) - `\e90B` - [interface] gear, settings, options, preferences, config
- **Rider Helmet** (`helmet_safety`) - `\e90C` - [safety] helmet, rider, protective, headgear, motorcycle
- **Hazard Caution** (`hazard_triangle`) - `\e90D` - [alerts] hazard, warning, caution, danger, alert
- **G-Force Meter** (`gforce_meter`) - `\e90E` - [telemetry] gforce, acceleration, lean, dynamics, meter
- **Satellite Radar** (`satellite_dish`) - `\e90F` - [interface] satellite, radar, signal, gps, aerial

## Integration Examples

### 1. Android (Jetpack Compose)
```kotlin
Icon(
    imageVector = ApexAlonIcons.Scooter,
    contentDescription = "Scooter",
    tint = GabayColors.AccentMint
)
```

### 2. React / Next.js
```tsx
import { ScooterIcon, CompassStarIcon } from 'myCustomiconFontPack/react';

<ScooterIcon size={28} color="#00F5D4" />
```

### 3. Web HTML / CSS
```html
<link rel="stylesheet" href="font/apex-alon.css">
<i class="ai-scooter"></i>
<i class="ai-compass-star"></i>
```

## Preview Catalog
Double-click `preview.html` in this folder to launch the interactive browser catalog with dynamic scaling, light/dark themes, and 1-click clipboard copy for SVGs, CSS classes, and React components.

## License
MIT License. Free for use in any personal, commercial, or open-source projects.
