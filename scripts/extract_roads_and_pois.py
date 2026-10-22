#!/usr/bin/env python3
import math
import gzip
import sqlite3
import json
import time
from pmtiles.reader import Reader, MmapSource, deserialize_directory
from pmtiles.tile import tileid_to_zxy
import mapbox_vector_tile

def tile_px_to_latlon(z, x, y, px, py, extent=4096):
    lon = (x + px / extent) / (2**z) * 360.0 - 180.0
    lat_rad = math.atan(math.sinh(math.pi * (1.0 - 2.0 * (y + py / extent) / (2**z))))
    return (math.degrees(lat_rad), lon)

def haversine(lat1, lon1, lat2, lon2):
    R = 6371000.0
    dlat = math.radians(lat2 - lat1)
    dlon = math.radians(lon2 - lon1)
    a = math.sin(dlat/2)**2 + math.cos(math.radians(lat1)) * math.cos(math.radians(lat2)) * math.sin(dlon/2)**2
    return 2 * R * math.asin(math.sqrt(a))

print("Opening local_assets/philippines_vector.pmtiles...")
t0 = time.time()
with open("local_assets/philippines_vector.pmtiles", "rb") as f:
    r = Reader(MmapSource(f))
    h = r.header()
    root_entries = deserialize_directory(r.get_bytes(h["root_offset"], h["root_length"]))
    all_tile_entries = []
    for re in root_entries:
        if re.run_length == 0:
            leaf_bytes = r.get_bytes(h["leaf_directory_offset"] + re.offset, re.length)
            all_tile_entries.extend(deserialize_directory(leaf_bytes))
        else:
            all_tile_entries.append(re)

    print(f"Total tiles in pmtiles: {len(all_tile_entries)}")

    # We will sample tiles from zoom 10, 11, and 12
    # In Central Luzon & Metro Manila: lon [120.2, 121.5], lat [14.3, 16.0]
    # Plus zoom 10 nationwide for major arteries and places
    tiles_to_scan = []
    for e in all_tile_entries:
        z, x, y = tileid_to_zxy(e.tile_id)
        if z == 10:
            tiles_to_scan.append(e)
        elif z in (11, 12):
            # Check if within Central Luzon / Metro Manila / Southern Tagalog
            # Calculate approx center of tile
            lat_center = math.degrees(math.atan(math.sinh(math.pi * (1.0 - 2.0 * (y + 0.5) / (2**z)))))
            lon_center = (x + 0.5) / (2**z) * 360.0 - 180.0
            if 14.0 <= lat_center <= 16.2 and 120.0 <= lon_center <= 121.8:
                tiles_to_scan.append(e)

    print(f"Tiles selected for extraction: {len(tiles_to_scan)}")

    places = {} # name -> (cat, lat, lon)
    pois = {}   # name -> (cat, lat, lon)
    roads_list = [] # (name, kind, coords)

    for i, e in enumerate(tiles_to_scan):
        z, x, y = tileid_to_zxy(e.tile_id)
        raw = r.get_bytes(h["tile_data_offset"] + e.offset, e.length)
        try:
            tile = mapbox_vector_tile.decode(gzip.decompress(raw))
            if "places" in tile:
                for feat in tile["places"]["features"]:
                    props = feat["properties"]
                    name = props.get("name")
                    if name and name not in places:
                        geom = feat["geometry"]
                        if geom["type"] == "Point":
                            pt = geom["coordinates"]
                            lat, lon = tile_px_to_latlon(z, x, y, pt[0], pt[1])
                            places[name] = (props.get("kind", "place").upper(), lat, lon)
            if "pois" in tile:
                for feat in tile["pois"]["features"]:
                    props = feat["properties"]
                    name = props.get("name")
                    if name and name not in pois:
                        geom = feat["geometry"]
                        if geom["type"] == "Point":
                            pt = geom["coordinates"]
                            lat, lon = tile_px_to_latlon(z, x, y, pt[0], pt[1])
                            pois[name] = (props.get("kind", "poi").upper(), lat, lon)
            if "roads" in tile:
                for feat in tile["roads"]["features"]:
                    props = feat["properties"]
                    name = props.get("name", "Road")
                    kind = props.get("kind", "road")
                    geom = feat["geometry"]
                    if geom["type"] == "LineString":
                        pts = [tile_px_to_latlon(z, x, y, p[0], p[1]) for p in geom["coordinates"]]
                        if len(pts) >= 2:
                            roads_list.append((name, kind, pts))
                    elif geom["type"] == "MultiLineString":
                        for line in geom["coordinates"]:
                            pts = [tile_px_to_latlon(z, x, y, p[0], p[1]) for p in line]
                            if len(pts) >= 2:
                                roads_list.append((name, kind, pts))
        except Exception:
            pass

print(f"Extraction finished in {time.time()-t0:.2f}s!")
print(f"Places: {len(places)}, POIs: {len(pois)}, Road polylines: {len(roads_list)}")
