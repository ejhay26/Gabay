import urllib.request
import re

url = "https://maps.protomaps.com/assets/builds-Cdq4B9B1.js"
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
content = urllib.request.urlopen(req).read().decode('utf-8')

# Search for any endpoints or bucket references
matches = re.findall(r'https?://[a-zA-Z0-9\.\-_/]+', content)
print("URLs:", matches[:10])

# Also search for "build.protomaps.com" or r2 or s3 or pmtiles
pmtiles_refs = re.findall(r'[a-zA-Z0-9\.\-_/]+\.pmtiles', content)
print("PMTiles refs:", pmtiles_refs[:10])
