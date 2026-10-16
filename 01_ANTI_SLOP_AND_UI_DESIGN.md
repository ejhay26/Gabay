# 01 — ANTI-SLOP CONSTITUTION & PRODUCTION UI DESIGN SYSTEM

Every line of UI code, copywriting, log output, and styling in **Gabay (`dev.gabay.app`)** must obey this specification. Any violation during the Visual QA step automatically fails the phase.

---

## 1. ABSOLUTE BANS (ZERO TOLERANCE FOR AI SLOP)

1. **NO EMOJIS ANYWHERE:**
   - Never use emojis in Kotlin UI text, buttons, logs, comments, Rust error messages, or LLM prompts (e.g., ban 📍, 🛵, 🚗, 🤖, ✨, 🚀, ⚠️).
   - Use sharp geometric vector icons (`VectorDrawable` paths with `1.75dp` stroke width) or crisp uppercase typographic badges (`[SCOOTER]`, `[CAR]`, `[SAT]`, `[3D]`).
2. **NO GRADIENTS OR NEON AI COLORS:**
   - Ban all `Brush.linearGradient`, `radialGradient`, neon purple (`#8B5CF6`), neon cyan, or translucent pastel glassmorphism.
   - Use solid, high-contrast flat surfaces engineered for direct sunlight readability on a vehicle mount.
3. **NO BUBBLY / PILL-SHAPED CARDS:**
   - Ban `RoundedCornerShape(24.dp)`, `CircleShape` floating action blobs, and default Material 3 pastel purple themes.
   - Structural panels and HUD cards must use tight, mechanical **`4.dp` or `6.dp` corner radii** with a **`1.dp` solid hairline border** (`#262E38`).
4. **NO AI SLOP PHRASING:**
   - Ban conversational filler in UI labels and LLM responses: *"Hey there!"*, *"Embark on your journey"*, *"Smart AI Co-Pilot"*, *"Seamless navigation"*, *"How can I help you today?"*.
   - Use terse, tactical rally-cockpit telemetry: `"14.2 KM REM"`, `"PACE 28-34 MIN"`, `"NO TOLL"`, `"BYPASS ACTIVE"`, `"OFF-ROUTE +38M"`.

---

## 2. COLOR PALETTE (TACTICAL HIGH-CONTRAST DARK)

Hardcode these exact hex values in `Theme.kt`:

| Token | Hex | Usage |
| :--- | :--- | :--- |
| `BgObsidian` | `#0A0D10` | Primary HUD background & bottom sheet base |
| `SurfacePanel` | `#12161C` | Elevated telemetry cards, search bar, control strips |
| `SurfaceInteractive` | `#1A2028` | Unselected buttons, mode pills, input fields |
| `BorderHairline` | `#262E38` | `1.dp` structural borders on all HUD panels and buttons |
| `TextPrimary` | `#F0F4F8` | Primary numbers (Speed, Distance, Maneuver Street Name) |
| `TextSecondary` | `#8A94A6` | Units (`KM/H`, `KM`, `MIN`), secondary coordinates, subtitles |
| `AccentEmerald` | `#10B981` | Active route line, GPS lock indicator, active mode toggle border |
| `AccentAmber` | `#F59E0B` | Upcoming turn alert (< 250m), pace drop warning, rerouting state |
| `AccentCrimson` | `#EF4444` | Off-route alert, restricted road warning, cancel/clear action |

---

## 3. TYPOGRAPHY & TABULAR NUMERALS

1. **Tabular Monospace for Live Telemetry (`fontFeatureSettings = "tnum"`):**
   - Speed (`48 KM/H`), Remaining Distance (`24.6 KM`), Turn Distance (`450 M`), and Coordinates (`15.3129, 120.9064`) update at 1Hz.
   - They MUST use `FontFamily.Monospace` or `fontFeatureSettings = "tnum"` so digits never jitter horizontally as numbers change.
2. **Type Scale:**
   - **Primary Maneuver Distance:** `28.sp`, `FontWeight.Bold`, `TextPrimary`
   - **Speedometer Readout:** `32.sp`, `FontWeight.Bold`, `FontFamily.Monospace`
   - **Street / Landmark Name:** `18.sp`, `FontWeight.SemiBold`, max 1 line with `TextOverflow.Ellipsis`
   - **HUD Section Micro-Labels:** `10.sp`, `FontWeight.Bold`, uppercase, letter-spacing `1.2.sp`, `TextSecondary` (e.g., `REMAINING`, `ROLLING PACE`, `HEADING`)

---

## 4. SPATIAL GRID, LAYOUT & TOUCH TARGETS

1. **Strict 4dp / 8dp Grid:** All padding, margins, and gaps must be multiples of `4.dp` (`4.dp`, `8.dp`, `12.dp`, `16.dp`).
2. **Canvas Dominance:** The MapLibre viewport must occupy **100% of the screen (`fillMaxSize()`)**, with HUD panels anchored cleanly to the top and bottom edges using `WindowInsets.systemBars` padding so nothing ever collides with the Android status bar or gesture navigation bar.
3. **Glove-Friendly Ergonomics:** Every interactive button (`[SCOOTER]`, `[CAR]`, `[SAT]`, `[TOPO]`, `[3D]`, `[ASK GABAY]`, `[COPY COORDS]`) must have a minimum touch target of **`48.dp` height** and clear active-state feedback (`1.dp` `AccentEmerald` border + `#162923` fill when active).
4. **Top Maneuver Banner:**
   - Anchored at top with `12.dp` horizontal margin.
   - Displays turn direction vector icon, exact distance in meters/km, target road name, and active avoidance badges (`[NO TOLL]`, `[NO EXPWY]`).
5. **Bottom Cockpit Telemetry Dock:**
   - Anchored at bottom, split into a crisp 3-column grid separated by `1.dp` vertical `BorderHairline` dividers:
     - **Col 1:** Live Speed (`KM/H`) + Heading (`N/NE/S`)
     - **Col 2:** Remaining Distance (`KM`) + Adaptive Pace Window (`~XX-YY MIN`)
     - **Col 3:** Co-Pilot Trigger Button (`[GABAY VOICE]`) + Layer Strip (`[SAT]`, `[TOPO]`, `[3D]`)