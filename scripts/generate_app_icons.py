#!/usr/bin/env python3
"""
Generate crisp PNG icons for Gabay in res/mipmap-* folders
"""
import os
import math
from PIL import Image, ImageDraw

def render_gabay_icon(size):
    # Render at 4x for smooth supersampling antialiasing
    scale = 4
    s = size * scale
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    center = s / 2.0
    r_bg = s / 2.0

    # Rounded Squircle / Circle background
    draw.ellipse([0, 0, s - 1, s - 1], fill=(10, 13, 16, 255))

    # Outer Solar Halo ring
    r_halo = s * 0.40
    w_halo = int(max(1, s * 0.015))
    draw.ellipse([center - r_halo, center - r_halo, center + r_halo, center + r_halo], 
                 outline=(255, 179, 0, 75), width=w_halo)

    # Secondary radar ring
    r_radar = s * 0.32
    w_radar = int(max(1, s * 0.008))
    draw.ellipse([center - r_radar, center - r_radar, center + r_radar, center + r_radar], 
                 outline=(255, 179, 0, 45), width=w_radar)

    # Master 'G' Track in Solar Amber (#FFB300)
    r_outer = s * 0.28
    r_inner = s * 0.18
    # Draw arc from 45 deg to 360 deg
    draw.arc([center - r_outer, center - r_outer, center + r_outer, center + r_outer],
             start=45, end=360, fill=(255, 179, 0, 255), width=int(r_outer - r_inner))

    # Center Waypoint Crossbar
    bar_y1 = center - s * 0.04
    bar_y2 = center + s * 0.04
    bar_x1 = center - s * 0.02
    bar_x2 = center + r_outer
    draw.rectangle([bar_x1, bar_y1, bar_x2, bar_y2], fill=(255, 179, 0, 255))

    # Forward Thrust Tactical Heading Chevron in White (#FFFFFF)
    # Arrow points forward-right (heading)
    arr_tip = (center + s * 0.16, center)
    arr_top = (center - s * 0.02, center - s * 0.12)
    arr_bot = (center - s * 0.02, center + s * 0.12)
    arr_in  = (center + s * 0.04, center)
    draw.polygon([arr_tip, arr_top, arr_in, arr_bot], fill=(255, 255, 255, 255))

    # Downsample with Lanczos for perfect edges
    res = img.resize((size, size), Image.Resampling.LANCZOS)
    return res

sizes = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

base_res = os.path.abspath("android_app/app/src/main/res")

for folder, sz in sizes.items():
    d = os.path.join(base_res, folder)
    os.makedirs(d, exist_ok=True)
    icon = render_gabay_icon(sz)
    icon.save(os.path.join(d, "ic_launcher.png"), "PNG")
    icon.save(os.path.join(d, "ic_launcher_round.png"), "PNG")
    print(f"Generated {folder}/ic_launcher.png ({sz}x{sz})")

# Also save 512px store / asset icon
os.makedirs("assets/logo", exist_ok=True)
icon_512 = render_gabay_icon(512)
icon_512.save("assets/logo/gabay_app_icon_512.png", "PNG")
print("Generated assets/logo/gabay_app_icon_512.png (512x512)")
