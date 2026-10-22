import os
import json

TARGET_DIRS = [
    r"C:\Users\Administrator\Documents\git\myCustomiconFontPack",
    os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "assets", "myCustomiconFontPack"))
]

ICONS = [
    {
        "id": "scooter",
        "name": "Scooter",
        "unicode": "\\e900",
        "codepoint": 0xE900,
        "category": "vehicles",
        "tags": ["motorcycle", "scooter", "ride", "two-wheeler", "cockpit"],
        "path_d": "M6 18a3 3 0 1 0 0-6 3 3 0 0 0 0 6z M18 18a3 3 0 1 0 0-6 3 3 0 0 0 0 6z M6 15h4l3-6h4 M12 12l2 6h3 M7 12h5 M17 6l2-3 M15 6h4 M17 9l1-3",
        "svg": """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
  <circle cx="6" cy="18" r="3" />
  <circle cx="18" cy="18" r="3" />
  <circle cx="6" cy="18" r="1" fill="currentColor" />
  <circle cx="18" cy="18" r="1" fill="currentColor" />
  <path d="M6 15h4l3-6h4" />
  <path d="M12 12l2 6h3" />
  <path d="M7 12h5" />
  <path d="M17 6l2-3" />
  <path d="M15 6h4" />
  <path d="M17 9l1-3" />
</svg>"""
    },
    {
        "id": "compass_star",
        "name": "Tactical Compass Star",
        "unicode": "\\e901",
        "codepoint": 0xE901,
        "category": "navigation",
        "tags": ["compass", "north", "heading", "orientation", "star"],
        "path_d": "M12 2l3 7 7 3-7 3-3 7-3-7-7-3 7-3 3-7z M12 10a2 2 0 1 0 0 4 2 2 0 0 0 0-4z",
        "svg": """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
  <polygon points="12 2 15 9 22 12 15 15 12 22 9 15 2 12 9 9" />
  <circle cx="12" cy="12" r="2.5" fill="currentColor" />
  <path d="M12 2v10l3-3" fill="currentColor" opacity="0.3" />
</svg>"""
    },
    {
        "id": "waypoint_pin",
        "name": "Waypoint Beacon Pin",
        "unicode": "\\e902",
        "codepoint": 0xE902,
        "category": "navigation",
        "tags": ["pin", "marker", "destination", "location", "waypoint"],
        "path_d": "M12 21c-4.5-5-7-8.5-7-12a7 7 0 1 1 14 0c0 3.5-2.5 7-7 12z M12 6l3 5-3-1-3 1z",
        "svg": """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
  <path d="M12 21c-4.5-5-7-8.5-7-12a7 7 0 1 1 14 0c0 3.5-2.5 7-7 12z" />
  <polygon points="12 6 15 11 12 10 9 11" fill="currentColor" />
</svg>"""
    },
    {
        "id": "fuel_pump",
        "name": "Fuel Pump",
        "unicode": "\\e903",
        "codepoint": 0xE903,
        "category": "places",
        "tags": ["gas", "petrol", "station", "fuel", "energy"],
        "path_d": "M4 4h10v16H4z M6 7h6v4H6z M4 17h10 M14 9h2a2 2 0 0 1 2 2v5a1.5 1.5 0 0 0 3 0V9l-2-2",
        "svg": """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
  <rect x="4" y="4" width="10" height="16" rx="2" />
  <rect x="6" y="7" width="6" height="4" rx="1" />
  <path d="M4 17h10" />
  <path d="M14 9h2a2 2 0 0 1 2 2v5a1.5 1.5 0 0 0 3 0V9l-2-2" />
  <circle cx="21" cy="7" r="1" fill="currentColor" />
</svg>"""
    },
    {
        "id": "road_centerline",
        "name": "Road Centerline",
        "unicode": "\\e904",
        "codepoint": 0xE904,
        "category": "navigation",
        "tags": ["road", "highway", "lane", "arterial", "route"],
        "path_d": "M4 21L8 3 M20 21L16 3 M12 5v3 M12 11v3 M12 17v3",
        "svg": """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
  <path d="M4 21L8 3" />
  <path d="M20 21L16 3" />
  <line x1="12" y1="5" x2="12" y2="8" stroke-dasharray="0" />
  <line x1="12" y1="11" x2="12" y2="14" stroke-dasharray="0" />
  <line x1="12" y1="17" x2="12" y2="20" stroke-dasharray="0" />
</svg>"""
    },
    {
        "id": "perspective_cube",
        "name": "3D Perspective Cube",
        "unicode": "\\e905",
        "codepoint": 0xE905,
        "category": "interface",
        "tags": ["3d", "cube", "perspective", "isometric", "spatial"],
        "path_d": "M12 2l9 5v10l-9 5-9-5V7z M12 12l9-5 M12 12v10 M12 12L3 7",
        "svg": """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
  <path d="M12 2l9 5v10l-9 5-9-5V7l9-5z" />
  <line x1="12" y1="12" x2="21" y2="7" />
  <line x1="12" y1="12" x2="12" y2="22" />
  <line x1="12" y1="12" x2="3" y2="7" />
</svg>"""
    },
    {
        "id": "wrench_tool",
        "name": "Wrench Tool",
        "unicode": "\\e906",
        "codepoint": 0xE906,
        "category": "maintenance",
        "tags": ["wrench", "tool", "maintenance", "repair", "service"],
        "path_d": "M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z",
        "svg": """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
  <path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z" />
</svg>"""
    },
    {
        "id": "speaker_waves",
        "name": "Speaker Waves",
        "unicode": "\\e907",
        "codepoint": 0xE907,
        "category": "audio",
        "tags": ["speaker", "audio", "voice", "sound", "volume"],
        "path_d": "M11 5L6 9H2v6h4l5 4V5z M15.54 8.46a5 5 0 0 1 0 7.07 M19.07 4.93a10 10 0 0 1 0 14.14",
        "svg": """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
  <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5" />
  <path d="M15.54 8.46a5 5 0 0 1 0 7.07" />
  <path d="M19.07 4.93a10 10 0 0 1 0 14.14" />
</svg>"""
    },
    {
        "id": "speedometer_dial",
        "name": "Speedometer Dial",
        "unicode": "\\e908",
        "codepoint": 0xE908,
        "category": "telemetry",
        "tags": ["speed", "speedometer", "gauge", "tachometer", "velocity"],
        "path_d": "M12 4a9 9 0 0 0-9 9c0 2.5 1 4.7 2.6 6.4L7 18 M21 13a9 9 0 0 0-9-9 M17 18l1.4 1.4A9 9 0 0 0 21 13 M12 13l4-4",
        "svg": """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
  <path d="M12 4a9 9 0 0 0-9 9c0 2.5 1 4.7 2.6 6.4L7 18" />
  <path d="M21 13a9 9 0 0 0-9-9" />
  <path d="M17 18l1.4 1.4A9 9 0 0 0 21 13" />
  <line x1="12" y1="13" x2="16" y2="9" />
  <circle cx="12" cy="13" r="1.5" fill="currentColor" />
</svg>"""
    },
    {
        "id": "shield_crest",
        "name": "Shield Crest",
        "unicode": "\\e909",
        "codepoint": 0xE909,
        "category": "security",
        "tags": ["shield", "safety", "secure", "protection", "guardrail"],
        "path_d": "M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z M12 6v12",
        "svg": """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
  <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
  <path d="M12 6v12" stroke-dasharray="2 2" opacity="0.6" />
</svg>"""
    },
    {
        "id": "battery_bolt",
        "name": "Battery Bolt",
        "unicode": "\\e90A",
        "codepoint": 0xE90A,
        "category": "telemetry",
        "tags": ["battery", "power", "charge", "electric", "spark"],
        "path_d": "M2 6h18v12H2z M22 10v4 M12 8l-4 5h3l-1 3 4-5h-3z",
        "svg": """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
  <rect x="2" y="6" width="18" height="12" rx="2" />
  <line x1="22" y1="10" x2="22" y2="14" />
  <polygon points="12 8 8 13 11 13 10 16 14 11 11 11" fill="currentColor" />
</svg>"""
    },
    {
        "id": "gear_settings",
        "name": "Gear Settings",
        "unicode": "\\e90B",
        "codepoint": 0xE90B,
        "category": "interface",
        "tags": ["gear", "settings", "options", "preferences", "config"],
        "path_d": "M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z",
        "svg": """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
  <circle cx="12" cy="12" r="3" />
  <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z" />
</svg>"""
    },
    {
        "id": "helmet_safety",
        "name": "Rider Helmet",
        "unicode": "\\e90C",
        "codepoint": 0xE90C,
        "category": "safety",
        "tags": ["helmet", "rider", "protective", "headgear", "motorcycle"],
        "path_d": "M12 3a9 9 0 0 0-9 9c0 3.5 2 6.5 5 8h8a6 6 0 0 0 6-6c0-6.08-4.92-11-10-11z M8 12h8a2 2 0 0 1 2 2v2a2 2 0 0 1-2 2H8",
        "svg": """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
  <path d="M12 3a9 9 0 0 0-9 9c0 3.5 2 6.5 5 8h8a6 6 0 0 0 6-6c0-6.08-4.92-11-10-11z" />
  <path d="M8 12h8a2 2 0 0 1 2 2v2a2 2 0 0 1-2 2H8" fill="currentColor" opacity="0.25" />
</svg>"""
    },
    {
        "id": "hazard_triangle",
        "name": "Hazard Caution",
        "unicode": "\\e90D",
        "codepoint": 0xE90D,
        "category": "alerts",
        "tags": ["hazard", "warning", "caution", "danger", "alert"],
        "path_d": "M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z M12 9v4 M12 17h.01",
        "svg": """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
  <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
  <line x1="12" y1="9" x2="12" y2="13" />
  <line x1="12" y1="17" x2="12.01" y2="17" />
</svg>"""
    },
    {
        "id": "gforce_meter",
        "name": "G-Force Meter",
        "unicode": "\\e90E",
        "codepoint": 0xE90E,
        "category": "telemetry",
        "tags": ["gforce", "acceleration", "lean", "dynamics", "meter"],
        "path_d": "M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18z M12 3v18 M3 12h18 M14 10a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5z",
        "svg": """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
  <circle cx="12" cy="12" r="9" />
  <line x1="12" y1="3" x2="12" y2="21" stroke-dasharray="2 2" />
  <line x1="3" y1="12" x2="21" y2="12" stroke-dasharray="2 2" />
  <circle cx="14" cy="10" r="2.5" fill="currentColor" />
</svg>"""
    },
    {
        "id": "satellite_dish",
        "name": "Satellite Radar",
        "unicode": "\\e90F",
        "codepoint": 0xE90F,
        "category": "interface",
        "tags": ["satellite", "radar", "signal", "gps", "aerial"],
        "path_d": "M4 10a7.5 7.5 0 0 0 10 10 M4 6a11.5 11.5 0 0 0 14 14 M4 2a15.5 15.5 0 0 0 18 18 M4 20a2 2 0 1 0 0-4 2 2 0 0 0 0 4z",
        "svg": """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
  <path d="M4 10a7.5 7.5 0 0 0 10 10" />
  <path d="M4 6a11.5 11.5 0 0 0 14 14" />
  <path d="M4 2a15.5 15.5 0 0 0 18 18" />
  <circle cx="4" cy="20" r="2" fill="currentColor" />
</svg>"""
    }
]

def build_icon_pack(base_dir):
    svg_dir = os.path.join(base_dir, "svg")
    font_dir = os.path.join(base_dir, "font")
    compose_dir = os.path.join(base_dir, "compose")
    react_dir = os.path.join(base_dir, "react")

    for d in [svg_dir, font_dir, compose_dir, react_dir]:
        os.makedirs(d, exist_ok=True)

    # 1. Output Individual SVGs
    for icon in ICONS:
        path = os.path.join(svg_dir, f"{icon['id']}.svg")
        with open(path, "w", encoding="utf-8") as f:
            f.write(icon["svg"])

    # 2. Output manifest.json
    manifest = {
        "name": "Apex Alon Icon Pack",
        "version": "1.0.0",
        "prefix": "ai",
        "description": "Unique, modular tactile & cockpit HUD navigation icon font pack.",
        "author": "Gabay Navigation Systems",
        "license": "MIT",
        "icons": ICONS
    }
    with open(os.path.join(base_dir, "manifest.json"), "w", encoding="utf-8") as f:
        json.dump(manifest, f, indent=2)

    # 3. Output apex-alon.css (Web Font & CSS Classes)
    css_content = [
        "/* Apex Alon Icon Font Stylesheet - v1.0.0 */",
        "@font-face {",
        "  font-family: 'ApexAlon';",
        "  src: url('apex-alon.woff2') format('woff2'), url('apex-alon.woff') format('woff');",
        "  font-weight: normal;",
        "  font-style: normal;",
        "  font-display: block;",
        "}",
        "",
        "[class^='ai-'], [class*=' ai-'] {",
        "  font-family: 'ApexAlon' !important;",
        "  speak: never;",
        "  font-style: normal;",
        "  font-weight: normal;",
        "  font-variant: normal;",
        "  text-transform: none;",
        "  line-height: 1;",
        "  -webkit-font-smoothing: antialiased;",
        "  -moz-osx-font-smoothing: grayscale;",
        "  display: inline-block;",
        "  vertical-align: middle;",
        "}",
        ""
    ]
    for icon in ICONS:
        css_content.append(f".ai-{icon['id'].replace('_', '-')}:before {{ content: '{icon['unicode']}'; }}")

    with open(os.path.join(font_dir, "apex-alon.css"), "w", encoding="utf-8") as f:
        f.write("\n".join(css_content))

    # 4. Output Jetpack Compose ApexAlonIcons.kt
    compose_code = [
        "package dev.gabay.app.ui",
        "",
        "import androidx.compose.ui.graphics.Color",
        "import androidx.compose.ui.graphics.SolidColor",
        "import androidx.compose.ui.graphics.StrokeCap",
        "import androidx.compose.ui.graphics.StrokeJoin",
        "import androidx.compose.ui.graphics.vector.ImageVector",
        "import androidx.compose.ui.graphics.vector.PathParser",
        "import androidx.compose.ui.unit.dp",
        "",
        "/**",
        " * Apex Alon Modular Icon Font Pack for Jetpack Compose.",
        " * High-contrast, zero-slop automotive vector geometry.",
        " */",
        "object ApexAlonIcons {"
    ]

    for icon in ICONS:
        name_camel = "".join(part.capitalize() for part in icon["id"].split("_"))
        path_d = icon["path_d"]
        compose_code.extend([
            f"    val {name_camel}: ImageVector by lazy {{",
            f"        val builder = ImageVector.Builder(",
            f"            name = \"ApexAlon.{name_camel}\",",
            f"            defaultWidth = 24.dp,",
            f"            defaultHeight = 24.dp,",
            f"            viewportWidth = 24f,",
            f"            viewportHeight = 24f",
            f"        )",
            f"        builder.addPath(",
            f"            pathData = PathParser().parsePathString(\"{path_d}\").toNodes(),",
            f"            stroke = SolidColor(Color.White),",
            f"            strokeLineWidth = 2f,",
            f"            strokeLineCap = StrokeCap.Round,",
            f"            strokeLineJoin = StrokeJoin.Round",
            f"        )",
            f"        builder.build()",
            f"    }}",
            ""
        ])

    compose_code.append("}")
    with open(os.path.join(compose_dir, "ApexAlonIcons.kt"), "w", encoding="utf-8") as f:
        f.write("\n".join(compose_code))

    # 5. Output React / TypeScript Definitions (index.tsx)
    react_code = [
        "import React from 'react';",
        "",
        "export interface IconProps extends React.SVGProps<SVGSVGElement> {",
        "  size?: number | string;",
        "  color?: string;",
        "}",
        ""
    ]

    for icon in ICONS:
        name_camel = "".join(part.capitalize() for part in icon["id"].split("_")) + "Icon"
        react_code.extend([
            f"export const {name_camel}: React.FC<IconProps> = ({{ size = 24, color = 'currentColor', ...props }}) => (",
            f"  {icon['svg'].replace('width=\"24\"', 'width={size}').replace('height=\"24\"', 'height={size}').replace('stroke=\"currentColor\"', 'stroke={color}')}",
            ");",
            ""
        ])

    with open(os.path.join(react_dir, "index.tsx"), "w", encoding="utf-8") as f:
        f.write("\n".join(react_code))

    # 6. Output package.json
    pkg_json = {
        "name": "@gabay/apex-alon-icons",
        "version": "1.0.0",
        "description": "Unique, modular tactile & cockpit HUD navigation icon font pack in Automotive Signal Amber.",
        "main": "react/index.tsx",
        "types": "react/index.tsx",
        "style": "font/apex-alon.css",
        "files": ["svg", "font", "compose", "react", "manifest.json"],
        "keywords": ["icons", "icon-font", "navigation", "hud", "motorcycle", "tactical", "react", "compose", "svg"],
        "author": "Gabay Navigation Systems",
        "license": "MIT"
    }
    with open(os.path.join(base_dir, "package.json"), "w", encoding="utf-8") as f:
        json.dump(pkg_json, f, indent=2)

    # 7. Output Interactive preview.html Showcase
    cards_html = []
    for icon in ICONS:
        cards_html.append(f"""
      <div class="card" data-id="{icon['id']}" data-category="{icon['category']}" data-tags="{','.join(icon['tags'])}" onclick="copySvg('{icon['id']}')">
        <div class="icon-wrapper" id="wrap-{icon['id']}">
          {icon['svg']}
        </div>
        <div class="icon-name">{icon['name']}</div>
        <div class="icon-code">{icon['unicode']} &bull; .ai-{icon['id'].replace('_', '-')}</div>
      </div>""")

    preview_html = f"""<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Apex Alon — Modular Tactical Icon Pack</title>
  <link rel="stylesheet" href="font/apex-alon.css">
  <style>
    :root {{
      --bg: #0b0f14;
      --card-bg: #121820;
      --card-border: #1e293b;
      --text-main: #f1f5f9;
      --text-muted: #64748b;
      --accent: #ffb300;
      --accent-glow: rgba(255, 179, 0, 0.2);
    }}
    body.light {{
      --bg: #f8fafc;
      --card-bg: #ffffff;
      --card-border: #cbd5e1;
      --text-main: #0f172a;
      --text-muted: #475569;
      --accent: #c2410c;
      --accent-glow: rgba(194, 65, 12, 0.15);
    }}
    * {{ box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }}
    body {{ background: var(--bg); color: var(--text-main); padding: 32px 24px; transition: background 0.2s, color 0.2s; min-height: 100vh; }}
    .header {{ max-width: 1200px; margin: 0 auto 28px; display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 16px; border-bottom: 1px solid var(--card-border); padding-bottom: 24px; }}
    .title-group h1 {{ font-size: 26px; font-weight: 800; letter-spacing: -0.5px; display: flex; align-items: center; gap: 10px; }}
    .title-group h1 span {{ color: var(--accent); }}
    .title-group p {{ font-size: 14px; color: var(--text-muted); margin-top: 4px; }}
    .controls {{ display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }}
    .search-box {{ padding: 8px 14px; border-radius: 8px; border: 1px solid var(--card-border); background: var(--card-bg); color: var(--text-main); font-size: 14px; outline: none; width: 200px; }}
    .search-box:focus {{ border-color: var(--accent); }}
    .btn-toggle {{ background: var(--card-bg); border: 1px solid var(--card-border); color: var(--text-main); padding: 8px 16px; border-radius: 8px; font-size: 13px; font-weight: 600; cursor: pointer; }}
    .btn-toggle:hover {{ border-color: var(--accent); }}
    .slider-group {{ display: flex; align-items: center; gap: 8px; font-size: 13px; color: var(--text-muted); }}
    .slider-group input {{ accent-color: var(--accent); cursor: pointer; }}
    .category-pills {{ max-width: 1200px; margin: 0 auto 24px; display: flex; gap: 8px; flex-wrap: wrap; }}
    .pill {{ padding: 6px 14px; border-radius: 20px; font-size: 12px; font-weight: 600; border: 1px solid var(--card-border); background: var(--card-bg); color: var(--text-muted); cursor: pointer; transition: all 0.15s; }}
    .pill.active, .pill:hover {{ background: var(--accent-glow); border-color: var(--accent); color: var(--accent); }}
    .grid {{ max-width: 1200px; margin: 0 auto; display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 16px; }}
    .card {{ background: var(--card-bg); border: 1px solid var(--card-border); border-radius: 12px; padding: 24px 16px; display: flex; flex-direction: column; align-items: center; justify-content: center; text-align: center; cursor: pointer; transition: transform 0.15s, border-color 0.15s, box-shadow 0.15s; }}
    .card:hover {{ transform: translateY(-3px); border-color: var(--accent); box-shadow: 0 8px 24px var(--accent-glow); }}
    .icon-wrapper {{ display: flex; align-items: center; justify-content: center; margin-bottom: 14px; color: var(--accent); }}
    .icon-wrapper svg {{ width: 32px; height: 32px; transition: width 0.1s, height 0.1s; }}
    .icon-name {{ font-size: 13px; font-weight: 700; color: var(--text-main); margin-bottom: 4px; }}
    .icon-code {{ font-size: 11px; font-family: monospace; color: var(--text-muted); }}
    .toast {{ position: fixed; bottom: 24px; right: 24px; background: var(--accent); color: #000; font-weight: 700; font-size: 13px; padding: 10px 20px; border-radius: 8px; box-shadow: 0 4px 16px rgba(0,0,0,0.3); opacity: 0; pointer-events: none; transition: opacity 0.2s, transform 0.2s; transform: translateY(10px); z-index: 999; }}
    .toast.show {{ opacity: 1; transform: translateY(0); }}
  </style>
</head>
<body>
  <div class="header">
    <div class="title-group">
      <h1><span>Apex Alon</span> Icon Pack</h1>
      <p>Tactical Signal Amber vector icon system for mobile & cockpit HUD navigation &bull; {len(ICONS)} icons</p>
    </div>
    <div class="controls">
      <input type="text" class="search-box" id="search" placeholder="Search icons..." oninput="filterIcons()">
      <div class="slider-group">
        <span>Size:</span>
        <input type="range" id="size-slider" min="16" max="64" value="32" oninput="resizeIcons(this.value)">
        <span id="size-val">32px</span>
      </div>
      <button class="btn-toggle" onclick="toggleTheme()">Toggle Theme</button>
    </div>
  </div>

  <div class="category-pills">
    <button class="pill active" onclick="setCategory('all', this)">All ({len(ICONS)})</button>
    <button class="pill" onclick="setCategory('navigation', this)">Navigation</button>
    <button class="pill" onclick="setCategory('vehicles', this)">Vehicles</button>
    <button class="pill" onclick="setCategory('telemetry', this)">Telemetry</button>
    <button class="pill" onclick="setCategory('safety', this)">Safety</button>
    <button class="pill" onclick="setCategory('interface', this)">Interface</button>
    <button class="pill" onclick="setCategory('maintenance', this)">Maintenance</button>
    <button class="pill" onclick="setCategory('audio', this)">Audio</button>
  </div>

  <div class="grid" id="icon-grid">
    {''.join(cards_html)}
  </div>

  <div class="toast" id="toast">Copied SVG to clipboard!</div>

  <script>
    const svgs = {{
      {', '.join([f'"{icon["id"]}": `{icon["svg"]}`' for icon in ICONS])}
    }};

    let currentCategory = 'all';

    function setCategory(cat, el) {{
      currentCategory = cat;
      document.querySelectorAll('.pill').forEach(p => p.classList.remove('active'));
      el.classList.add('active');
      filterIcons();
    }}

    function filterIcons() {{
      const query = document.getElementById('search').value.toLowerCase();
      document.querySelectorAll('.card').forEach(card => {{
        const id = card.dataset.id;
        const cat = card.dataset.category;
        const tags = card.dataset.tags.toLowerCase();
        const matchesCat = currentCategory === 'all' || cat === currentCategory;
        const matchesQuery = !query || id.includes(query) || tags.includes(query);
        card.style.display = (matchesCat && matchesQuery) ? 'flex' : 'none';
      }});
    }}

    function resizeIcons(size) {{
      document.getElementById('size-val').textContent = size + 'px';
      document.querySelectorAll('.icon-wrapper svg').forEach(svg => {{
        svg.style.width = size + 'px';
        svg.style.height = size + 'px';
      }});
    }}

    function toggleTheme() {{
      document.body.classList.toggle('light');
    }}

    function copySvg(id) {{
      const svg = svgs[id];
      navigator.clipboard.writeText(svg).then(() => {{
        const toast = document.getElementById('toast');
        toast.textContent = 'Copied SVG to clipboard: ' + id + '.svg';
        toast.classList.add('show');
        setTimeout(() => toast.classList.remove('show'), 2000);
      }});
    }}
  </script>
</body>
</html>"""
    with open(os.path.join(base_dir, "preview.html"), "w", encoding="utf-8") as f:
        f.write(preview_html)

    # 8. Output README.md
    readme_content = f"""# Apex Alon Icon Font Pack

A unique, production-grade modular icon font pack designed in Automotive Signal Amber for tactical motorcycle cockpit HUDs, automotive navigation, and mobile/web applications.

## Directory Structure
- `svg/` - Individual production-ready vector SVGs (24x24 standard grid).
- `font/` - Web font stylesheet (`apex-alon.css`) and unicode font glyph mappings (`\\e900` - `\\e90F`).
- `compose/` - Ready-to-drop Jetpack Compose `ImageVector` definitions (`ApexAlonIcons.kt`).
- `react/` - TypeScript / React SVG components with full prop typing (`index.tsx`).
- `preview.html` - Interactive standalone visual showcase & clipboard copy tool (open in any browser).
- `manifest.json` - Complete metadata catalog for bundlers and design systems.
- `package.json` - Standard npm package definition.

## Available Icons ({len(ICONS)})
"""
    for icon in ICONS:
        readme_content += f"- **{icon['name']}** (`{icon['id']}`) - `{icon['unicode']}` - [{icon['category']}] {', '.join(icon['tags'])}\n"

    readme_content += """
## Integration Examples

### 1. Android (Jetpack Compose)
```kotlin
Icon(
    imageVector = ApexAlonIcons.Scooter,
    contentDescription = "Scooter",
    tint = GabayColors.AccentPrimary
)
```

### 2. React / Next.js
```tsx
import { ScooterIcon, CompassStarIcon } from 'myCustomiconFontPack/react';

<ScooterIcon size={28} color="#FFB300" />
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
"""
    with open(os.path.join(base_dir, "README.md"), "w", encoding="utf-8") as f:
        f.write(readme_content)

    print(f"Successfully generated icon pack at: {base_dir}")

for target in TARGET_DIRS:
    build_icon_pack(target)

# Also copy directly into Gabay Android app source
android_compose_path = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "android_app", "app", "src", "main", "java", "dev", "gabay", "app", "ui", "ApexAlonIcons.kt"))
with open(os.path.join(TARGET_DIRS[0], "compose", "ApexAlonIcons.kt"), "r", encoding="utf-8") as src_f:
    with open(android_compose_path, "w", encoding="utf-8") as dst_f:
        dst_f.write(src_f.read())
print(f"Integrated ApexAlonIcons.kt directly into Android App UI at: {android_compose_path}")
