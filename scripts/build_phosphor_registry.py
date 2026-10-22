#!/usr/bin/env python3
"""
scripts/build_phosphor_registry.py
Downloads official Phosphor Bold SVGs (256x256 viewport, solid filled geometric vectors),
converts them into:
1. android_app/app/src/main/res/drawable/ic_<name>.xml (Android Vector Drawables)
2. android_app/app/src/main/java/dev/gabay/app/ui/GabayIcons.kt (Jetpack Compose ImageVectors)
"""

import os
import re
import urllib.request
import xml.etree.ElementTree as ET

DRAWABLE_DIR = r"android_app\app\src\main\res\drawable"
KOTLIN_DIR = r"android_app\app\src\main\java\dev\gabay\app\ui"
os.makedirs(DRAWABLE_DIR, exist_ok=True)
os.makedirs(KOTLIN_DIR, exist_ok=True)

PHOSPHOR_MAP = {
    'ActionCheck': 'check',
    'ActionClose': 'x',
    'ActionCopy': 'copy',
    'AudioMute': 'speaker-slash',
    'AudioTts': 'speaker-high',
    'ChevronDown': 'caret-down',
    'ChevronLeft': 'caret-left',
    'ChevronUp': 'caret-up',
    'CompassHeading': 'compass',
    'CopilotMic': 'microphone',
    'DestinationPin': 'map-pin',
    'FinishFlag': 'flag-banner',
    'Layer3d': 'cube',
    'LayerSatellite': 'globe-hemisphere-west',
    'LayerStack': 'stack',
    'LayerTopo': 'mountains',
    'LayerVector': 'map-trifold',
    'ManeuverFork': 'git-fork',
    'ManeuverLeft': 'arrow-bend-up-left',
    'ManeuverMerge': 'git-merge',
    'ManeuverRight': 'arrow-bend-up-right',
    'ManeuverSlightLeft': 'arrow-up-left',
    'ManeuverSlightRight': 'arrow-up-right',
    'ManeuverStraight': 'arrow-up',
    'ManeuverUTurn': 'arrow-u-down-left',
    'ModeCar': 'car-profile',
    'ModeScooter': 'moped',
    'ModeWalk': 'person-simple-walk',
    'NavCursor': 'navigation-arrow',
    'RecenterGps': 'crosshair',
    'RerouteActive': 'arrows-clockwise',
    'RoadProhibited': 'prohibit',
    'RouteDist': 'signpost',
    'SearchPoi': 'magnifying-glass',
    'SettingsTuning': 'faders',
    'SpeedGauge': 'speedometer',
    'TimePace': 'timer',
    'WarningHazard': 'warning',
    'Menu': 'list',
    'TurnArrowLeft': 'arrow-left',
    'TurnArrowRight': 'arrow-right',
    'Send': 'paper-plane-tilt',
    'RotateCcw': 'arrow-counter-clockwise',
    'Compass': 'compass',
    'Crosshairs': 'crosshair',
}

def camel_to_snake(name):
    s1 = re.sub('(.)([A-Z][a-z]+)', r'\1_\2', name)
    return re.sub('([a-z0-9])([A-Z])', r'\1_\2', s1).lower()

icons_data = {}

print("=== Downloading Phosphor Bold SVGs ===")
for gabay_name, p_name in sorted(PHOSPHOR_MAP.items()):
    url = f"https://raw.githubusercontent.com/phosphor-icons/core/main/assets/bold/{p_name}-bold.svg"
    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req) as resp:
            svg_text = resp.read().decode('utf-8')
            root = ET.fromstring(svg_text)
            paths = []
            for elem in root.iter():
                tag = elem.tag.split('}')[-1]
                if tag == 'path':
                    d = elem.attrib.get('d', '').strip()
                    if d:
                        paths.append(d)
                elif tag == 'circle':
                    cx = float(elem.attrib.get('cx', '128'))
                    cy = float(elem.attrib.get('cy', '128'))
                    r = float(elem.attrib.get('r', '16'))
                    d = f"M {cx-r} {cy} a {r} {r} 0 1 0 {r*2} 0 a {r} {r} 0 1 0 -{r*2} 0 Z"
                    paths.append(d)
            if paths:
                icons_data[gabay_name] = paths
                print(f"[OK] {gabay_name} -> {p_name}-bold ({len(paths)} path(s))")
            else:
                print(f"[ERR] No paths for {gabay_name}")
    except Exception as e:
        print(f"[FAIL] {gabay_name}: {e}")

print(f"\nSuccessfully parsed {len(icons_data)} icons.")

# 1. Generate XML Vector Drawables
for gabay_name, paths in icons_data.items():
    snake = camel_to_snake(gabay_name)
    xml_path = os.path.join(DRAWABLE_DIR, f"ic_{snake}.xml")
    
    xml_content = ['<vector xmlns:android="http://schemas.android.com/apk/res/android"',
                   '    android:width="24dp"',
                   '    android:height="24dp"',
                   '    android:viewportWidth="256"',
                   '    android:viewportHeight="256">']
    for d in paths:
        # Escape quotes if any
        clean_d = d.replace('"', '&quot;')
        xml_content.append(f'    <path')
        xml_content.append(f'        android:fillColor="#F0F4F8"')
        xml_content.append(f'        android:pathData="{clean_d}" />')
    xml_content.append('</vector>\n')
    
    with open(xml_path, 'w', encoding='utf-8') as f:
        f.write('\n'.join(xml_content))

print(f"Generated {len(icons_data)} Android Vector Drawables in {DRAWABLE_DIR}")

# 2. Generate GabayIcons.kt Jetpack Compose Registry
kt_path = os.path.join(KOTLIN_DIR, "GabayIcons.kt")
kt_lines = [
    "package dev.gabay.app.ui",
    "",
    "import androidx.compose.ui.graphics.Color",
    "import androidx.compose.ui.graphics.SolidColor",
    "import androidx.compose.ui.graphics.vector.ImageVector",
    "import androidx.compose.ui.graphics.vector.PathParser",
    "import androidx.compose.ui.unit.dp",
    "",
    "/**",
    " * Gabay Global Tactical Icon Registry (Phosphor Icons Bold)",
    " * Pure vector outline geometry tailored for high-contrast motorcycle/scooter cockpit HUDs.",
    " * Replaces generic icon sets with high-legibility tactical icons.",
    " */",
    "object GabayIcons {"
]

for gabay_name, paths in sorted(icons_data.items()):
    snake = camel_to_snake(gabay_name)
    kt_lines.append(f"    // ic_{snake}")
    kt_lines.append(f"    val {gabay_name}Res: Int = dev.gabay.app.R.drawable.ic_{snake}")
    kt_lines.append(f"    val {gabay_name}: ImageVector by lazy {{")
    kt_lines.append(f"        val builder = ImageVector.Builder(")
    kt_lines.append(f"            name = \"{gabay_name}\",")
    kt_lines.append(f"            defaultWidth = 24.dp,")
    kt_lines.append(f"            defaultHeight = 24.dp,")
    kt_lines.append(f"            viewportWidth = 256f,")
    kt_lines.append(f"            viewportHeight = 256f")
    kt_lines.append(f"        )")
    for d in paths:
        escaped_d = d.replace('\\', '\\\\').replace('"', '\\"')
        kt_lines.append(f"        builder.addPath(")
        kt_lines.append(f"            pathData = PathParser().parsePathString(\"{escaped_d}\").toNodes(),")
        kt_lines.append(f"            fill = SolidColor(Color(0xFFF0F4F8))")
        kt_lines.append(f"        )")
    kt_lines.append(f"        builder.build()")
    kt_lines.append(f"    }}")
    kt_lines.append("")

kt_lines.append("}")
kt_lines.append("")

with open(kt_path, 'w', encoding='utf-8') as f:
    f.write('\n'.join(kt_lines))

print(f"Generated {kt_path} successfully!")
