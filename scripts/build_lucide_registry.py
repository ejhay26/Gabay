#!/usr/bin/env python3
"""
scripts/build_lucide_registry.py
Downloads official Lucide SVGs, converts all SVG elements (path, circle, line, rect, polyline)
into exact SVG path strings, and generates:
1. android_app/app/src/main/res/drawable/ic_<name>.xml (Android Vector Drawables)
2. android_app/app/src/main/java/dev/gabay/app/ui/GabayIcons.kt (Jetpack Compose ImageVectors)
"""

import os
import urllib.request
import xml.etree.ElementTree as ET

DRAWABLE_DIR = r"android_app\app\src\main\res\drawable"
KOTLIN_DIR = r"android_app\app\src\main\java\dev\gabay\app\ui"
os.makedirs(DRAWABLE_DIR, exist_ok=True)
os.makedirs(KOTLIN_DIR, exist_ok=True)

# Mapping of Gabay icon name -> Lucide icon name
ICON_MAP = {
    'ActionCheck': 'check',
    'ActionClose': 'x',
    'ActionCopy': 'copy',
    'AudioMute': 'volume-x',
    'AudioTts': 'volume-2',
    'ChevronDown': 'chevron-down',
    'ChevronLeft': 'chevron-left',
    'ChevronUp': 'chevron-up',
    'CompassHeading': 'compass',
    'CopilotMic': 'mic',
    'DestinationPin': 'map-pin',
    'FinishFlag': 'flag',
    'Layer3d': 'box',
    'LayerSatellite': 'globe',
    'LayerStack': 'layers',
    'LayerTopo': 'mountain',
    'LayerVector': 'map',
    'ManeuverFork': 'git-branch',
    'ManeuverLeft': 'corner-up-left',
    'ManeuverMerge': 'git-merge',
    'ManeuverRight': 'corner-up-right',
    'ManeuverSlightLeft': 'arrow-up-left',
    'ManeuverSlightRight': 'arrow-up-right',
    'ManeuverStraight': 'arrow-up',
    'ManeuverUTurn': 'undo-2',
    'ModeCar': 'car',
    'ModeScooter': 'bike',
    'ModeWalk': 'footprints',
    'NavCursor': 'navigation',
    'RecenterGps': 'crosshair',
    'RerouteActive': 'route',
    'RoadProhibited': 'ban',
    'RouteDist': 'milestone',
    'SearchPoi': 'search',
    'SettingsTuning': 'sliders-horizontal',
    'SpeedGauge': 'gauge',
    'TimePace': 'timer',
    'WarningHazard': 'triangle-alert',
    'Menu': 'menu',
    'TurnArrowLeft': 'arrow-left',
    'TurnArrowRight': 'arrow-right',
    'Send': 'send',
    'RotateCcw': 'rotate-ccw',
    'Compass': 'compass',
    'Crosshairs': 'crosshair',
}

def camel_to_snake(name):
    import re
    s1 = re.sub('(.)([A-Z][a-z]+)', r'\1_\2', name)
    return re.sub('([a-z0-9])([A-Z])', r'\1_\2', s1).lower()

def svg_to_paths(svg_text):
    root = ET.fromstring(svg_text)
    paths = []
    
    def clean_tag(tag):
        return tag.split('}')[-1] if '}' in tag else tag

    for elem in root.iter():
        tag = clean_tag(elem.tag)
        if tag == 'path':
            d = elem.attrib.get('d', '').strip()
            if d:
                paths.append(d)
        elif tag == 'line':
            x1 = elem.attrib.get('x1', '0')
            y1 = elem.attrib.get('y1', '0')
            x2 = elem.attrib.get('x2', '0')
            y2 = elem.attrib.get('y2', '0')
            paths.append(f"M {x1} {y1} L {x2} {y2}")
        elif tag == 'polyline':
            points = elem.attrib.get('points', '').strip().split()
            coords = []
            for pt in points:
                if ',' in pt:
                    coords.extend(pt.split(','))
                else:
                    coords.append(pt)
            if len(coords) >= 2:
                d = f"M {coords[0]} {coords[1]}"
                for i in range(2, len(coords), 2):
                    if i + 1 < len(coords):
                        d += f" L {coords[i]} {coords[i+1]}"
                paths.append(d)
        elif tag == 'polygon':
            points = elem.attrib.get('points', '').strip().split()
            coords = []
            for pt in points:
                if ',' in pt:
                    coords.extend(pt.split(','))
                else:
                    coords.append(pt)
            if len(coords) >= 2:
                d = f"M {coords[0]} {coords[1]}"
                for i in range(2, len(coords), 2):
                    if i + 1 < len(coords):
                        d += f" L {coords[i]} {coords[i+1]}"
                d += " Z"
                paths.append(d)
        elif tag == 'circle':
            cx = float(elem.attrib.get('cx', '0'))
            cy = float(elem.attrib.get('cy', '0'))
            r = float(elem.attrib.get('r', '0'))
            # Exact two semicircles
            d = f"M {cx - r:.3f} {cy:.3f} a {r:.3f} {r:.3f} 0 1 0 {2 * r:.3f} 0 a {r:.3f} {r:.3f} 0 1 0 -{2 * r:.3f} 0 Z"
            paths.append(d)
        elif tag == 'rect':
            x = float(elem.attrib.get('x', '0'))
            y = float(elem.attrib.get('y', '0'))
            w = float(elem.attrib.get('width', '0'))
            h = float(elem.attrib.get('height', '0'))
            rx = float(elem.attrib.get('rx', '0'))
            if rx > 0:
                d = f"M {x + rx} {y} h {w - 2 * rx} a {rx} {rx} 0 0 1 {rx} {rx} v {h - 2 * rx} a {rx} {rx} 0 0 1 -{rx} {rx} h -{w - 2 * rx} a {rx} {rx} 0 0 1 -{rx} -{rx} v -{h - 2 * rx} a {rx} {rx} 0 0 1 {rx} -{rx} Z"
            else:
                d = f"M {x} {y} h {w} v {h} h -{w} Z"
            paths.append(d)

    return paths

def main():
    base_url = "https://raw.githubusercontent.com/lucide-icons/lucide/main/icons/{}.svg"
    icon_data = {}

    print(f"Downloading {len(ICON_MAP)} Lucide SVGs...")
    for gabay_name, lucide_name in ICON_MAP.items():
        url = base_url.format(lucide_name)
        try:
            svg = urllib.request.urlopen(url).read().decode('utf-8')
            paths = svg_to_paths(svg)
            icon_data[gabay_name] = paths
        except Exception as e:
            print(f"Error fetching {gabay_name} ({lucide_name}): {e}")

    # Generate Android Vector Drawables
    for gabay_name, paths in icon_data.items():
        drawable_name = "ic_" + camel_to_snake(gabay_name)
        drawable_file = os.path.join(DRAWABLE_DIR, f"{drawable_name}.xml")
        
        xml_content = ['<vector xmlns:android="http://schemas.android.com/apk/res/android"',
                       '    android:width="24dp"',
                       '    android:height="24dp"',
                       '    android:viewportWidth="24"',
                       '    android:viewportHeight="24">']
        for p in paths:
            xml_content.append(f'    <path')
            xml_content.append(f'        android:pathData="{p}"')
            xml_content.append(f'        android:strokeColor="#F0F4F8"')
            xml_content.append(f'        android:strokeWidth="1.75"')
            xml_content.append(f'        android:strokeLineCap="round"')
            xml_content.append(f'        android:strokeLineJoin="round"/>')
        xml_content.append('</vector>\n')
        
        with open(drawable_file, 'w', encoding='utf-8') as f:
            f.write('\n'.join(xml_content))

    # Generate GabayIcons.kt
    kt_lines = [
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
        " * Gabay Global Icon Registry",
        " * Pixel-aligned Lucide vector icons with 1.75dp stroke width.",
        " * Adheres strictly to 01_ANTI_SLOP_AND_UI_DESIGN.md (Zero emojis, zero bloated gradients).",
        " */",
        "object GabayIcons {",
        "    private const val DefaultStroke = 1.75f",
        ""
    ]

    for gabay_name, paths in sorted(icon_data.items()):
        drawable_res = "ic_" + camel_to_snake(gabay_name)
        combined_path = " ".join(paths)
        
        kt_lines.append(f"    // {drawable_res}")
        kt_lines.append(f"    val {gabay_name}Res: Int = dev.gabay.app.R.drawable.{drawable_res}")
        kt_lines.append(f"    val {gabay_name}: ImageVector by lazy {{")
        kt_lines.append(f"        val builder = ImageVector.Builder(")
        kt_lines.append(f"            name = \"{gabay_name}\",")
        kt_lines.append(f"            defaultWidth = 24.dp,")
        kt_lines.append(f"            defaultHeight = 24.dp,")
        kt_lines.append(f"            viewportWidth = 24f,")
        kt_lines.append(f"            viewportHeight = 24f")
        kt_lines.append(f"        )")
        for p in paths:
            escaped_p = p.replace('\\', '\\\\').replace('"', '\\"')
            kt_lines.append(f"        builder.addPath(")
            kt_lines.append(f"            pathData = PathParser().parsePathString(\"{escaped_p}\").toNodes(),")
            kt_lines.append(f"            stroke = SolidColor(Color(0xFFF0F4F8)),")
            kt_lines.append(f"            strokeLineWidth = DefaultStroke,")
            kt_lines.append(f"            strokeLineCap = StrokeCap.Round,")
            kt_lines.append(f"            strokeLineJoin = StrokeJoin.Round")
            kt_lines.append(f"        )")
        kt_lines.append(f"        builder.build()")
        kt_lines.append(f"    }}")
        kt_lines.append("")

    kt_lines.append("}")
    kt_lines.append("")

    kt_file = os.path.join(KOTLIN_DIR, "GabayIcons.kt")
    with open(kt_file, 'w', encoding='utf-8') as f:
        f.write('\n'.join(kt_lines))

    print(f"Successfully generated {len(icon_data)} icons in {kt_file} and {DRAWABLE_DIR}!")

if __name__ == '__main__':
    main()
