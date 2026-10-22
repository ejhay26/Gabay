#!/usr/bin/env python3
"""
scripts/fetch_philippines_pbf.py
Downloads the official OpenStreetMap canonical extract for the Philippines from Geofabrik.
Saves directly to G:\gabay_builds\osm\philippines.osm.pbf (or specified directory) to preserve C: drive space.
"""

import os
import sys
import time
import urllib.request

PBF_URL = "https://download.geofabrik.de/asia/philippines-latest.osm.pbf"
DEFAULT_TARGET = r"G:\gabay_builds\osm\philippines.osm.pbf"

def download_pbf(target_path=DEFAULT_TARGET):
    out_dir = os.path.dirname(target_path)
    os.makedirs(out_dir, exist_ok=True)

    print("=" * 65)
    print("GABAY OFFLINE VAULT: PHILIPPINES OSM EXTRACT DOWNLOADER")
    print(f"Source: {PBF_URL}")
    print(f"Target: {target_path}")
    print("=" * 65)

    existing_bytes = 0
    if os.path.exists(target_path):
        existing_bytes = os.path.getsize(target_path)
        print(f"Found existing file with {existing_bytes / 1024 / 1024:.1f} MB.")

    req = urllib.request.Request(PBF_URL, headers={"User-Agent": "GabayPbfDownloader/1.0"})
    if existing_bytes > 0:
        req.add_header("Range", f"bytes={existing_bytes}-")

    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            total_size = resp.headers.get("Content-Length")
            if total_size:
                total_size = int(total_size) + existing_bytes
            else:
                total_size = 610000000 # ~582 MB fallback

            mode = "ab" if existing_bytes > 0 and resp.status == 206 else "wb"
            if mode == "wb":
                existing_bytes = 0

            downloaded = existing_bytes
            chunk_size = 1024 * 512 # 512 KB
            t0 = time.time()
            last_print = t0

            with open(target_path, mode) as f:
                while True:
                    chunk = resp.read(chunk_size)
                    if not chunk:
                        break
                    f.write(chunk)
                    downloaded += len(chunk)
                    now = time.time()
                    if now - last_print > 1.5:
                        speed_mb = (downloaded - existing_bytes) / (now - t0) / 1024 / 1024
                        pct = (downloaded / total_size) * 100 if total_size else 0
                        print(f"Progress: {downloaded / 1024 / 1024:.1f} / {total_size / 1024 / 1024:.1f} MB ({pct:.1f}%) @ {speed_mb:.2f} MB/s", end="\r")
                        last_print = now

            print(f"\n[SUCCESS] Download completed! Saved {downloaded / 1024 / 1024:.1f} MB to {target_path}")
            return True
    except Exception as e:
        print(f"\n[ERROR] Download failed: {e}")
        return False

if __name__ == "__main__":
    target = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_TARGET
    download_pbf(target)
