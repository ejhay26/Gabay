#!/usr/bin/env python3
"""Merges downloaded Philippine GeoJSON boundaries into a single standard boundaries.geojson
compatible with tools/add_admin_columns.py.
"""
import glob
import json
import os
import sys

def main():
    base_dir = r"G:\gabay_builds\boundaries\geojson"
    out_file = r"G:\gabay_builds\boundaries\boundaries.geojson"

    features = []

    # 1. Provinces (admin_level 4)
    print("Loading provinces...")
    prov_files = glob.glob(os.path.join(base_dir, "regions", "medres", "provdists-region-*.json"))
    for f in prov_files:
        with open(f, encoding="utf-8") as fp:
            data = json.load(fp)
            for feat in data.get("features", []):
                p = feat.get("properties", {})
                name = p.get("adm2_en", "")
                if "not a province" in name.lower() or name.startswith("NCR"):
                    name = "Metro Manila"
                features.append({
                    "type": "Feature",
                    "geometry": feat.get("geometry"),
                    "properties": {
                        "admin_level": 4,
                        "name": name,
                        "psgc": p.get("adm2_psgc"),
                    }
                })
    print(f"Provinces loaded: {len(features)}")

    # 2. Municipalities & Cities (admin_level 6)
    print("Loading municities...")
    mun_files = glob.glob(os.path.join(base_dir, "provdists", "medres", "municities-provdist-*.json"))
    mun_count = 0
    for f in mun_files:
        with open(f, encoding="utf-8") as fp:
            data = json.load(fp)
            for feat in data.get("features", []):
                p = feat.get("properties", {})
                name = p.get("adm3_en", "")
                features.append({
                    "type": "Feature",
                    "geometry": feat.get("geometry"),
                    "properties": {
                        "admin_level": 6,
                        "name": name,
                        "psgc": p.get("adm3_psgc"),
                        "prov_psgc": p.get("adm2_psgc"),
                    }
                })
                mun_count += 1
    print(f"Municities loaded: {mun_count}")

    # 3. Barangays (admin_level 10)
    print("Loading barangays...")
    bgy_files = glob.glob(os.path.join(base_dir, "municities", "medres", "bgysubmuns-municity-*.json"))
    bgy_count = 0
    for f in bgy_files:
        with open(f, encoding="utf-8") as fp:
            data = json.load(fp)
            for feat in data.get("features", []):
                p = feat.get("properties", {})
                name = p.get("adm4_en", "")
                features.append({
                    "type": "Feature",
                    "geometry": feat.get("geometry"),
                    "properties": {
                        "admin_level": 10,
                        "name": name,
                        "psgc": p.get("adm4_psgc"),
                        "mun_psgc": p.get("adm3_psgc"),
                    }
                })
                bgy_count += 1
    print(f"Barangays loaded: {bgy_count}")

    total = len(features)
    print(f"Total features: {total}. Writing to {out_file}...")
    os.makedirs(os.path.dirname(out_file), exist_ok=True)
    with open(out_file, "w", encoding="utf-8") as fp:
        json.dump({"type": "FeatureCollection", "features": features}, fp)

    size_mb = os.path.getsize(out_file) / (1024 * 1024)
    print(f"[SUCCESS] Wrote {out_file} ({size_mb:.2f} MB)")

if __name__ == "__main__":
    main()
