"""Minimal, dependency-free RGBA PNG I/O and raster ops shared by texturekit tools.

decode_rgba fast-paths 8-bit RGBA (all Minecraft textures) and also decodes grayscale,
indexed (palette), grayscale+alpha, and 16-bit PNGs, converting each to 8-bit RGBA
(16-bit samples are downscaled to their high byte). Adam7-interlaced PNGs and unknown
color types raise a clear error. 16-bit color-keyed (tRNS) transparency is rejected
rather than mis-matched. Pure stdlib (zlib); no Pillow, no venv.
"""
import os
import sys
import zlib

PNG_SIG = b"\x89PNG\r\n\x1a\n"


def _paeth(a, b, c):
    p = a + b - c
    pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
    if pa <= pb and pa <= pc:
        return a
    return b if pb <= pc else c


_CHANNELS = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}  # grayscale, RGB, indexed, gray+alpha, RGBA


def _unfilter(raw, h, stride, bpp):
    """Reverse PNG scanline filters, returning the concatenated unfiltered scanlines."""
    out, prev = bytearray(), bytearray(stride)
    pos = 0
    for _ in range(h):
        filt = raw[pos]; pos += 1
        line = bytearray(raw[pos:pos + stride]); pos += stride
        if filt == 1:
            for x in range(bpp, stride):
                line[x] = (line[x] + line[x - bpp]) & 255
        elif filt == 2:
            for x in range(stride):
                line[x] = (line[x] + prev[x]) & 255
        elif filt == 3:
            for x in range(stride):
                a = line[x - bpp] if x >= bpp else 0
                line[x] = (line[x] + ((a + prev[x]) >> 1)) & 255
        elif filt == 4:
            for x in range(stride):
                a = line[x - bpp] if x >= bpp else 0
                c = prev[x - bpp] if x >= bpp else 0
                line[x] = (line[x] + _paeth(a, prev[x], c)) & 255
        out += line
        prev = line
    return out


def _samples(line, w, channels, bit_depth):
    """Yield channels-tuples of samples for each of w pixels in one unfiltered scanline."""
    if bit_depth == 8:
        for x in range(w):
            base = x * channels
            yield tuple(line[base:base + channels])
    elif bit_depth == 16:
        for x in range(w):
            base = x * channels * 2
            yield tuple(line[base + c * 2] for c in range(channels))  # high byte only
    else:  # 1/2/4-bit: sub-byte samples, MSB-first (grayscale or indexed => channels == 1)
        maxval = (1 << bit_depth) - 1
        per_byte = 8 // bit_depth
        for x in range(w):
            i = x // per_byte
            shift = 8 - bit_depth * (x % per_byte + 1)
            yield ((line[i] >> shift) & maxval,)


def decode_rgba(path):
    """Return (w, h, bytearray RGBA) for a non-interlaced PNG. A path of "-" reads stdin.

    Supports grayscale, RGB, indexed (incl. 1/2/4-bit), grayscale+alpha, and RGBA,
    with tRNS transparency. Adam7-interlaced PNGs are rejected with a clear error.
    """
    data = sys.stdin.buffer.read() if path == "-" else open(path, "rb").read()
    if data[:8] != PNG_SIG:
        raise ValueError(f"{path}: not a PNG")
    pos, w, h, idat = 8, None, None, bytearray()
    bit_depth = color_type = interlace = None
    plte, trns = None, None
    while pos < len(data):
        ln = int.from_bytes(data[pos:pos + 4], "big")
        typ = data[pos + 4:pos + 8]
        chunk = data[pos + 8:pos + 8 + ln]
        pos += 12 + ln
        if typ == b"IHDR":
            w = int.from_bytes(chunk[0:4], "big")
            h = int.from_bytes(chunk[4:8], "big")
            bit_depth, color_type, interlace = chunk[8], chunk[9], chunk[12]
        elif typ == b"PLTE":
            plte = chunk
        elif typ == b"tRNS":
            trns = chunk
        elif typ == b"IDAT":
            idat += chunk
        elif typ == b"IEND":
            break

    if interlace != 0:
        raise ValueError(f"{path}: Adam7-interlaced PNGs are not supported; de-interlace first.")
    if color_type not in _CHANNELS:
        raise ValueError(f"{path}: unsupported PNG color type {color_type}.")
    channels = _CHANNELS[color_type]
    stride = (w * channels * bit_depth + 7) // 8
    bpp = max(1, (channels * bit_depth + 7) // 8)
    lines = _unfilter(zlib.decompress(bytes(idat)), h, stride, bpp)

    # Fast path: 8-bit RGBA is already the target layout.
    if color_type == 6 and bit_depth == 8:
        out = bytearray()
        for y in range(h):
            out += lines[y * stride:(y + 1) * stride]
        return w, h, out

    # Build a palette (indexed) or note tRNS transparency for gray/RGB.
    palette = None
    if color_type == 3:
        if plte is None:
            raise ValueError(f"{path}: indexed PNG missing PLTE chunk.")
        palette = [(plte[i], plte[i + 1], plte[i + 2]) for i in range(0, len(plte), 3)]
        alphas = list(trns) if trns else []
    # 16-bit samples are truncated to their high byte below; a full-width tRNS key can no longer be
    # compared meaningfully, so reject it rather than silently mis-matching transparency.
    if bit_depth == 16 and trns is not None and color_type in (0, 2):
        raise ValueError(f"{path}: 16-bit color-keyed (tRNS) transparency is not supported.")
    trns_gray = int.from_bytes(trns[0:2], "big") if (color_type == 0 and trns) else None
    trns_rgb = (tuple(trns[i] for i in (1, 3, 5)) if (color_type == 2 and trns) else None)
    gmax = (1 << bit_depth) - 1

    out = bytearray(w * h * 4)
    o = 0
    for y in range(h):
        line = lines[y * stride:(y + 1) * stride]
        for s in _samples(line, w, channels, bit_depth):
            if color_type == 0:  # grayscale
                g = s[0] * 255 // gmax if bit_depth < 8 else s[0]
                a = 0 if trns_gray is not None and s[0] == trns_gray else 255
                r = gg = bb = g
                out[o:o + 4] = bytes([r, gg, bb, a]); o += 4
            elif color_type == 2:  # RGB
                a = 0 if trns_rgb is not None and s == trns_rgb else 255
                out[o:o + 4] = bytes([s[0], s[1], s[2], a]); o += 4
            elif color_type == 3:  # indexed
                idx = s[0]
                r, g, b = palette[idx]
                a = alphas[idx] if idx < len(alphas) else 255
                out[o:o + 4] = bytes([r, g, b, a]); o += 4
            elif color_type == 4:  # grayscale + alpha
                out[o:o + 4] = bytes([s[0], s[0], s[0], s[1]]); o += 4
            else:  # RGBA, non-8-bit (16)
                out[o:o + 4] = bytes([s[0], s[1], s[2], s[3]]); o += 4
    return w, h, out


def encode_rgba(path, w, h, px):
    stride = w * 4
    raw = bytearray()
    for y in range(h):
        raw.append(0)  # filter type 0 (None)
        raw += px[y * stride:(y + 1) * stride]
    comp = zlib.compress(bytes(raw), 9)

    def chunk(typ, payload):
        return (len(payload).to_bytes(4, "big") + typ + payload
                + zlib.crc32(typ + payload).to_bytes(4, "big"))

    ihdr = w.to_bytes(4, "big") + h.to_bytes(4, "big") + bytes([8, 6, 0, 0, 0])
    blob = PNG_SIG + chunk(b"IHDR", ihdr) + chunk(b"IDAT", comp) + chunk(b"IEND", b"")
    if path == "-":
        sys.stdout.buffer.write(blob)
    else:
        with open(path, "wb") as fh:
            fh.write(blob)


def first_frame(w, h, px, path):
    """Animated textures are a vertical strip; keep the top w x w frame."""
    animated = os.path.exists(path + ".mcmeta") or (h > w and h % w == 0)
    if animated and h > w:
        return w, w, bytearray(px[:w * w * 4])
    return w, h, px


def composite_over(base, top):
    """Source-over alpha composite of two equally sized RGBA buffers."""
    out = bytearray(len(base))
    for i in range(0, len(base), 4):
        ta = top[i + 3]
        if ta == 255:
            out[i:i + 4] = top[i:i + 4]
        elif ta == 0:
            out[i:i + 4] = base[i:i + 4]
        else:
            inv = 255 - ta
            for k in range(3):
                out[i + k] = (top[i + k] * ta + base[i + k] * inv) // 255
            out[i + 3] = ta + base[i + 3] * inv // 255
    return out


def nearest_resize(w, h, px, nw, nh):
    """Nearest-neighbor resample of an RGBA buffer to nw x nh (keeps pixels crisp)."""
    sx_map = [x * w // nw for x in range(nw)]
    row_cache, out = {}, bytearray()
    for ny in range(nh):
        sy = ny * h // nh
        row = row_cache.get(sy)
        if row is None:
            base = sy * w * 4
            row = b"".join(px[base + sx * 4: base + sx * 4 + 4] for sx in sx_map)
            row_cache[sy] = row
        out += row
    return out
