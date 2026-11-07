#!/usr/bin/env python3
"""Adds barangay / municipality / province to pois_fts.sqlite.

usage: add_admin_columns.py pois_fts.sqlite boundaries.geojson

boundaries.geojson: OSM admin boundaries (Polygon / MultiPolygon) with
properties name and admin_level (4 province, 6 city/municipality, 10 barangay).
Safe to re-run: columns are added once and values are overwritten.
"""
import json, sqlite3, sys
from collections import defaultdict

LEVELS = {4: "province", 6: "municipality", 10: "barangay"}
CELL = 0.05

def rings_of(geom):
    if geom.get("type") == "Polygon":
        return [geom["coordinates"]]
    if geom.get("type") == "MultiPolygon":
        return geom["coordinates"]
    return []

def inside(poly, x, y):
    odd = False
    for ring in poly:
        j = len(ring) - 1
        for i in range(len(ring)):
            xi, yi = ring[i][0], ring[i][1]
            xj, yj = ring[j][0], ring[j][1]
            if (yi > y) != (yj > y) and x < (xj - xi) * (y - yi) / (yj - yi) + xi:
                odd = not odd
            j = i
    return odd

def load(path):
    areas, grid = [], defaultdict(list)
    with open(path, encoding="utf-8") as f:
        feats = json.load(f).get("features", [])
    for ft in feats:
        p = ft.get("properties") or {}
        try:
            level = LEVELS.get(int(p.get("admin_level")))
        except (TypeError, ValueError):
            continue
        name = (p.get("name") or "").strip()
        polys = rings_of(ft.get("geometry") or {})
        pts = [pt for poly in polys for ring in poly for pt in ring]
        if not level or not name or not pts:
            continue
        b = (min(q[0] for q in pts), min(q[1] for q in pts), max(q[0] for q in pts), max(q[1] for q in pts))
        idx = len(areas)
        areas.append((level, name, polys, b, (b[2] - b[0]) * (b[3] - b[1])))
        for cx in range(int(b[0] // CELL), int(b[2] // CELL) + 1):
            for cy in range(int(b[1] // CELL), int(b[3] // CELL) + 1):
                grid[(cx, cy)].append(idx)
    return areas, grid

def resolve(areas, grid, lon, lat):
    best = {}
    for idx in grid.get((int(lon // CELL), int(lat // CELL)), ()):
        level, name, polys, b, size = areas[idx]
        if not (b[0] <= lon <= b[2] and b[1] <= lat <= b[3]):
            continue
        if level in best and best[level][1] <= size:
            continue
        if any(inside(poly, lon, lat) for poly in polys):
            best[level] = (name, size)
    return [best[k][0] if k in best else None for k in ("barangay", "municipality", "province")]

def main(db, geo):
    areas, grid = load(geo)
    print(f"{len(areas)} boundary areas loaded")
    con = sqlite3.connect(db)
    have = {r[1] for r in con.execute("PRAGMA table_info(pois)")}
    for col in ("barangay", "municipality", "province"):
        if col not in have:
            con.execute(f"ALTER TABLE pois ADD COLUMN {col} TEXT")
    rows = con.execute("SELECT id, latitude, longitude FROM pois").fetchall()
    out = [(*resolve(areas, grid, lon, lat), pid) for pid, lat, lon in rows]
    con.executemany("UPDATE pois SET barangay=?, municipality=?, province=? WHERE id=?", out)
    con.commit()
    hit = sum(1 for r in out if r[1])
    print(f"{hit}/{len(rows)} places have a municipality")
    con.close()

if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    main(sys.argv[1], sys.argv[2])
