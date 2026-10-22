import gzip
import io
import struct

class Entry:
    def __init__(self, tile_id, offset, length, run_length):
        self.tile_id = tile_id
        self.offset = offset
        self.length = length
        self.run_length = run_length

def read_varint(stream):
    res = 0
    shift = 0
    while True:
        b = stream.read(1)
        if not b:
            break
        val = ord(b)
        res |= (val & 0x7F) << shift
        if (val & 0x80) == 0:
            break
        shift += 7
    return res

def deserialize_directory(buf):
    b_io = io.BytesIO(gzip.decompress(buf))
    entries = []
    num_entries = read_varint(b_io)
    last_id = 0
    for _ in range(num_entries):
        tmp = read_varint(b_io)
        entries.append(Entry(last_id + tmp, 0, 0, 0))
        last_id += tmp
    for i in range(num_entries):
        entries[i].run_length = read_varint(b_io)
    for i in range(num_entries):
        entries[i].length = read_varint(b_io)
    for i in range(num_entries):
        tmp = read_varint(b_io)
        if i > 0 and tmp == 0:
            entries[i].offset = entries[i - 1].offset + entries[i - 1].length
        else:
            entries[i].offset = tmp - 1
    return entries

def rotate(n, x, y, rx, ry):
    if ry == 0:
        if rx != 0:
            x = n - 1 - x
            y = n - 1 - y
        x, y = y, x
    return x, y

def zxy_to_tileid(z, x, y):
    acc = ((1 << (z * 2)) - 1) // 3
    a = z - 1
    while a >= 0:
        s = 1 << a
        rx = s & x
        ry = s & y
        acc += ((3 * rx) ^ ry) << a
        x, y = rotate(s, x, y, rx, ry)
        a -= 1
    return acc

def find_tile(entries, tile_id):
    m = 0
    n = len(entries) - 1
    while m <= n:
        k = (n + m) >> 1
        c = tile_id - entries[k].tile_id
        if c > 0:
            m = k + 1
        elif c < 0:
            n = k - 1
        else:
            return entries[k]
    if n >= 0:
        if entries[n].run_length == 0:
            return entries[n]
        if tile_id - entries[n].tile_id < entries[n].run_length:
            return entries[n]
    return None

# Test on philippines_vector.pmtiles
with open("G:/gabay_builds/philippines_vector.pmtiles", "rb") as f:
    header_bytes = f.read(127)
    magic = header_bytes[:7]
    root_offset, root_len = struct.unpack_from("<QQ", header_bytes, 8)
    leaf_offset, leaf_len = struct.unpack_from("<QQ", header_bytes, 40)
    tile_data_offset, tile_data_len = struct.unpack_from("<QQ", header_bytes, 56)
    tile_comp = header_bytes[98]
    
    f.seek(root_offset)
    root_entries = deserialize_directory(f.read(root_len))
    print(f"Loaded root directory with {len(root_entries)} entries")

    # Test tile at zoom 12 (user's location: x=3423, y=1872)
    tid = zxy_to_tileid(12, 3423, 1872)
    entry = find_tile(root_entries, tid)
    if entry and entry.run_length == 0:
        f.seek(leaf_offset + entry.offset)
        leaf_entries = deserialize_directory(f.read(entry.length))
        entry = find_tile(leaf_entries, tid)
    
    if entry:
        f.seek(tile_data_offset + entry.offset)
        tile_data = f.read(entry.length)
        print(f"SUCCESS! Found user tile: {len(tile_data)} bytes")
    else:
        print("Tile not found")
