import pmtiles.reader
import math

f = open("G:/gabay_builds/philippines_vector.pmtiles", "rb")
r = pmtiles.reader.Reader(lambda o, l: (f.seek(o), f.read(l))[1])

lat, lon = 15.2611, 120.9105

for z in range(6, 13):
    rad = math.radians(lat)
    n = 2 ** z
    x = int((lon + 180) / 360 * n)
    y = int((1 - math.asinh(math.tan(rad)) / math.pi) / 2 * n)
    t = r.get(z, x, y)
    print(f"Zoom {z} tile ({x}, {y}): {len(t) if t else 'None'} bytes")
