#!/usr/bin/env python3
"""
scripts/generate_icon_registry.py
Converts Lucide SVG icons in assets/icons/svg/ into:
1. Android Vector Drawables (android_app/app/src/main/res/drawable/ic_<name>.xml)
2. Kotlin Jetpack Compose Global Registry (android_app/app/src/main/java/dev/gabay/app/ui/GabayIcons.kt)
"""

import os
import glob
import xml.etree.ElementTree as ET

SVG_DIR = "assets/icons/svg"
DRAWABLE_DIR = "android_app/app/src/main/res/drawable"
KOTLIN_DIR = "android_app/app/src/main/java/dev/gabay/app/ui"

os.makedirs(DRAWABLE_DIR, exist_ok=True)
os.makedirs(KOTLIN_DIR, exist_ok=True)

def parse_svg_to_paths(svg_path):
    tree = ET.parse(svg_path)
    root = tree.getroot()
    paths = []
    
    # Strip namespace if present
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
            if len(points) >= 2:
                # Group into pairs
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
            # Approximate circle as two arcs
            d = f"M {cx} {cy - r} a {r} {r} 0 1 0 0.001 0 a {r} {r} 0 1 0 -0.001 0 Z"
            paths.append(d)
        elif tag == 'rect':
            x = float(elem.attrib.get('x', '0'))
            y = float(elem.attrib.get('y', '0'))
            w = float(elem.attrib.get('width', '0'))
            h = float(elem.attrib.get('height', '0'))
            rx = float(elem.attrib.get('rx', '0'))
            if rx == 0:
                d = f"M {x} {y} h {w} v {h} h {-w} Z"
            else:
                d = f"M {x + rx} {y} h {w - 2*rx} a {rx} {rx} 0 0 1 {rx} {rx} v {h - 2*rx} a {rx} {rx} 0 0 1 {-rx} {rx} h {-w + 2*rx} a {rx} {rx} 0 0 1 {-rx} {-rx} v {-h + 2*rx} a {rx} {rx} 0 0 1 {rx} {-rx} Z"
            paths.append(d)
            
    return paths

icon_files = sorted(glob.glob(os.path.join(SVG_DIR, "*.svg")))
registry_items = []

for svg_file in icon_files:
    base_name = os.path.splitext(os.path.basename(svg_file))[0]
    paths = parse_svg_to_paths(svg_file)
    if not paths:
        continue
    
    # Write Android Vector Drawable XML
    drawable_xml_path = os.path.join(DRAWABLE_DIR, f"ic_{base_name}.xml")
    combined_path_data = " ".join(paths)
    
    xml_content = f"""<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:strokeColor="#FFF0F4F8"
        android:strokeWidth="1.75"
        android:strokeLineCap="round"
        android:strokeLineJoin="round"
        android:pathData="{combined_path_data}" />
</vector>
"""
    with open(drawable_xml_path, "w", encoding="utf-8") as f:
        f.write(xml_content)
        
    # Convert snake_case to PascalCase
    pascal_name = "".join(part.capitalize() for part in base_name.split("_"))
    registry_items.append((pascal_name, base_name, combined_path_data))

print(f"Generated {len(registry_items)} Vector Drawables in {DRAWABLE_DIR}")

# Now generate GabayIcons.kt
kt_lines = [
    "package dev.gabay.app.ui",
    "",
    "import androidx.compose.ui.graphics.Color",
    "import androidx.compose.ui.graphics.SolidColor",
    "import androidx.compose.ui.graphics.StrokeCap",
    "import androidx.compose.ui.graphics.StrokeJoin",
    "import androidx.compose.ui.graphics.vector.ImageVector",
    "import androidx.compose.ui.graphics.vector.PathParser",
    "import androidx.compose.ui.graphics.vector.path",
    "import androidx.compose.ui.unit.dp",
    "",
    "/**",
    " * Gabay Global Icon Registry",
    " * Sharp geometric vector line icons with 1.75dp stroke width.",
    " * Adheres strictly to 01_ANTI_SLOP_AND_UI_DESIGN.md (Zero emojis, zero bloated gradients).",
    " */",
    "object GabayIcons {",
    "    private val DefaultStroke = 1.75f",
    ""
]

for pascal_name, base_name, path_data in registry_items:
    kt_lines.append(f"    // ic_{base_name}")
    kt_lines.append(f"    val {pascal_name}Res: Int = dev.gabay.app.R.drawable.ic_{base_name}")
    kt_lines.append(f"    val {pascal_name}: ImageVector by lazy {{")
    kt_lines.append(f"        ImageVector.Builder(")
    kt_lines.append(f'            name = "{pascal_name}",')
    kt_lines.append(f"            defaultWidth = 24.dp,")
    kt_lines.append(f"            defaultHeight = 24.dp,")
    kt_lines.append(f"            viewportWidth = 24f,")
    kt_lines.append(f"            viewportHeight = 24f")
    kt_lines.append(f"        ).addPath(")
    escaped_path = path_data.replace('"', '\\"')
    kt_lines.append(f'            pathData = PathParser().parsePathString("{escaped_path}").toNodes(),')
    kt_lines.append(f"            stroke = SolidColor(Color(0xFFF0F4F8)),")
    kt_lines.append(f"            strokeLineWidth = DefaultStroke,")
    kt_lines.append(f"            strokeLineCap = StrokeCap.Round,")
    kt_lines.append(f"            strokeLineJoin = StrokeJoin.Round")
    kt_lines.append(f"        ).build()")
    kt_lines.append(f"    }}")
    kt_lines.append("")

kt_lines.append("}")
kt_lines.append("")

kt_file_path = os.path.join(KOTLIN_DIR, "GabayIcons.kt")
with open(kt_file_path, "w", encoding="utf-8") as f:
    f.write("\n".join(kt_lines))

print(f"Generated GabayIcons.kt with {len(registry_items)} icons at {kt_file_path}")
