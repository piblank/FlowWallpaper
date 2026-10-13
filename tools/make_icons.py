import zlib, struct, os, math

BASE = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res")
DENSITIES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}


def chunk(typ, data):
    c = typ + data
    return struct.pack(">I", len(data)) + c + struct.pack(">I", zlib.crc32(c) & 0xFFFFFFFF)


def write_png(path, w, h, px):
    sig = b"\x89PNG\r\n\x1a\n"
    ihdr = struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0)  # 8-bit RGBA
    raw = bytearray()
    for y in range(h):
        raw.append(0)  # filter type 0 (none)
        raw.extend(px[y * w * 4:(y + 1) * w * 4])
    idat = zlib.compress(bytes(raw), 9)
    with open(path, "wb") as f:
        f.write(sig + chunk(b"IHDR", ihdr) + chunk(b"IDAT", idat) + chunk(b"IEND", b""))


def make_icon(size):
    cx = cy = size / 2.0
    maxr = size / 2.0
    buf = bytearray(size * size * 4)
    c_in = (255, 179, 71)    # amber center
    c_mid = (232, 74, 138)   # magenta
    c_out = (40, 30, 70)     # deep indigo
    for y in range(size):
        for x in range(size):
            dx = x - cx + 0.5
            dy = y - cy + 0.5
            d = math.sqrt(dx * dx + dy * dy) / maxr
            if d > 1.0:
                r = g = b = a = 0
            else:
                if d < 0.5:
                    t = d / 0.5
                    r = c_in[0] + (c_mid[0] - c_in[0]) * t
                    g = c_in[1] + (c_mid[1] - c_in[1]) * t
                    b = c_in[2] + (c_mid[2] - c_in[2]) * t
                else:
                    t = (d - 0.5) / 0.5
                    r = c_mid[0] + (c_out[0] - c_mid[0]) * t
                    g = c_mid[1] + (c_out[1] - c_mid[1]) * t
                    b = c_mid[2] + (c_out[2] - c_mid[2]) * t
                a = 255
            i = (y * size + x) * 4
            buf[i] = int(r)
            buf[i + 1] = int(g)
            buf[i + 2] = int(b)
            buf[i + 3] = int(a)
    return buf


for d, s in DENSITIES.items():
    out_dir = os.path.join(BASE, d)
    os.makedirs(out_dir, exist_ok=True)
    write_png(os.path.join(out_dir, "ic_launcher.png"), s, s, make_icon(s))
    print("wrote", d, s)
