#!/usr/bin/env python3
"""
Render Minecraft block/item MODELS to crisp isometric icons — pure stdlib (zlib only).

Unlike a simple "draw an iso cube" tool, this reads the actual model JSON geometry so it
handles non-cube blocks (pipe cores, the glass tank frame, cables, machines): it resolves
the parent chain (plus a small set of hardcoded vanilla templates), merges `#var` textures,
walks `elements` (from/to, per-element rotation, per-face uv/texture/rotation), applies the
model's `display.gui` transform, and rasterizes with a z-buffer, Minecraft directional face
shading, nearest texture sampling, and alpha-test (so the cutout glass tank reads see-through).
Renders supersampled and box-downsamples for clean edges. Transparent PNG out.

Usage:
  python tools/render_blocks.py --model block/tank/glass_tank --out /tmp/tank.png
  python tools/render_blocks.py --model block/pipe/copper_transport_pipe_core --size 256 --ss 3
  (model is a path under models/, with or without the logistics: namespace and .json)
"""

import argparse
import glob
import json
import math
import os
import re
import struct
import sys
import zlib

# Block items whose wiki page title differs from the mod's lang string (item id -> wiki name).
BLOCK_NAME_OVERRIDES = {
    "fluid/fluid_pump": "Pump",  # page is titled "Pump"
    # chassis pages + valve anchor links use "MK1".."MK5", not lang's "MkI".."MkV"
    "pipe/chassis_logistics_pipe_mk1": "Chassis Logistics Pipe MK1",
    "pipe/chassis_logistics_pipe_mk2": "Chassis Logistics Pipe MK2",
    "pipe/chassis_logistics_pipe_mk3": "Chassis Logistics Pipe MK3",
    "pipe/chassis_logistics_pipe_mk4": "Chassis Logistics Pipe MK4",
    "pipe/chassis_logistics_pipe_mk5": "Chassis Logistics Pipe MK5",
    # storage blocks: pages reference "X Block", not lang's "Block of X"
    "core/apatite_block": "Apatite Block",
    "core/bronze_block": "Bronze Block",
    "core/tin_block": "Tin Block",
    "core/raw_tin_block": "Raw Tin Block",
}

PNG_SIG = b"\x89PNG\r\n\x1a\n"
SHADE = {"up": 1.0, "down": 0.5, "north": 0.8, "south": 0.8, "east": 0.6, "west": 0.6}
DIR_NORMAL = {"up": (0, 1, 0), "down": (0, -1, 0), "north": (0, 0, -1),
              "south": (0, 0, 1), "west": (-1, 0, 0), "east": (1, 0, 0)}


# ----------------------------------------------------------------- PNG decode

def decode_png(path):
    """Return (w, h, bytearray RGBA). Handles gray/RGB/palette/RGBA, 8-bit and sub-byte
    (1/2/4-bit) palette/grayscale, first frame of animated strips only."""
    data = open(path, "rb").read()
    if data[:8] != PNG_SIG:
        raise ValueError("not a PNG")
    pos, w, h = 8, 0, 0
    bit = ct = 0
    idat, plte, trns = bytearray(), b"", b""
    while pos < len(data):
        ln = int.from_bytes(data[pos:pos + 4], "big")
        typ = data[pos + 4:pos + 8]
        chunk = data[pos + 8:pos + 8 + ln]
        pos += 12 + ln
        if typ == b"IHDR":
            w, h, bit, ct = (int.from_bytes(chunk[0:4], "big"),
                             int.from_bytes(chunk[4:8], "big"), chunk[8], chunk[9])
        elif typ == b"PLTE":
            plte = chunk
        elif typ == b"tRNS":
            trns = chunk
        elif typ == b"IDAT":
            idat += chunk
        elif typ == b"IEND":
            break
    if ct in (2, 4, 6) and bit != 8:
        raise ValueError(f"unsupported colortype {ct} @ {bit}-bit")
    channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[ct]
    stride = (w * channels * bit + 7) // 8
    bpp = max(1, (channels * bit) // 8)
    raw = zlib.decompress(bytes(idat))
    rows, prev, p = bytearray(), bytearray(stride), 0
    for _ in range(h):
        f = raw[p]; p += 1
        line = bytearray(raw[p:p + stride]); p += stride
        if f == 1:
            for x in range(bpp, stride): line[x] = (line[x] + line[x - bpp]) & 255
        elif f == 2:
            for x in range(stride): line[x] = (line[x] + prev[x]) & 255
        elif f == 3:
            for x in range(stride):
                a = line[x - bpp] if x >= bpp else 0
                line[x] = (line[x] + ((a + prev[x]) >> 1)) & 255
        elif f == 4:
            for x in range(stride):
                a = line[x - bpp] if x >= bpp else 0
                c = prev[x - bpp] if x >= bpp else 0
                line[x] = (line[x] + _paeth(a, prev[x], c)) & 255
        rows += line; prev = line
    out = bytearray(w * h * 4)
    maxv = (1 << bit) - 1
    for y in range(h):
        ro = y * stride
        for x in range(w):
            o = (y * w + x) * 4
            if bit == 8:
                s = ro + x * channels
                if ct == 6:
                    out[o:o + 4] = rows[s:s + 4]
                elif ct == 2:
                    out[o:o + 3] = rows[s:s + 3]; out[o + 3] = 255
                elif ct == 0:
                    g = rows[s]; out[o:o + 4] = bytes((g, g, g, 255))
                elif ct == 4:
                    g = rows[s]; out[o:o + 4] = bytes((g, g, g, rows[s + 1]))
                elif ct == 3:
                    idx = rows[s]; out[o:o + 3] = plte[idx * 3:idx * 3 + 3]
                    out[o + 3] = trns[idx] if idx < len(trns) else 255
            else:  # sub-byte palette or grayscale (channels == 1)
                bp = x * bit
                val = (rows[ro + bp // 8] >> (8 - bit - (bp % 8))) & maxv
                if ct == 3:
                    out[o:o + 3] = plte[val * 3:val * 3 + 3]
                    out[o + 3] = trns[val] if val < len(trns) else 255
                else:
                    g = val * 255 // maxv; out[o:o + 4] = bytes((g, g, g, 255))
    if h > w and h % w == 0:  # animated strip -> first frame
        out = out[:w * w * 4]; h = w
    return w, h, out


def _paeth(a, b, c):
    p = a + b - c; pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
    return a if pa <= pb and pa <= pc else (b if pb <= pc else c)


def encode_png(path, w, h, px):
    raw = bytearray()
    for y in range(h):
        raw.append(0); raw += px[y * w * 4:(y + 1) * w * 4]
    comp = zlib.compress(bytes(raw), 9)

    def chunk(t, d):
        return len(d).to_bytes(4, "big") + t + d + zlib.crc32(t + d).to_bytes(4, "big")
    ihdr = struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0)
    open(path, "wb").write(PNG_SIG + chunk(b"IHDR", ihdr) + chunk(b"IDAT", comp) + chunk(b"IEND", b""))


# ------------------------------------------------------ vanilla parent templates

def _cube_faces():
    return {d: {"uv": [0, 0, 16, 16], "texture": "#" + d} for d in DIR_NORMAL}


VANILLA = {
    "block/block": {"display": {"gui": {"rotation": [30, 225, 0], "scale": [0.625, 0.625, 0.625]}}},
    "block/cube": {"parent": "block/block",
                   "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": _cube_faces()}]},
    "block/cube_all": {"parent": "block/cube",
                       "textures": {d: "#all" for d in list(DIR_NORMAL) + ["particle"]}},
    "block/cube_column": {"parent": "block/cube",
                          "textures": {"up": "#end", "down": "#end", "north": "#side", "south": "#side",
                                       "west": "#side", "east": "#side", "particle": "#side"}},
    "block/cube_bottom_top": {"parent": "block/cube",
                              "textures": {"up": "#top", "down": "#bottom", "north": "#side", "south": "#side",
                                           "west": "#side", "east": "#side", "particle": "#side"}},
    "block/cube_top": {"parent": "block/cube",
                       "textures": {"up": "#top", "down": "#bottom", "north": "#side", "south": "#side",
                                    "west": "#side", "east": "#side", "particle": "#side"}},
    "block/orientable": {"parent": "block/cube",
                         "textures": {"up": "#top", "down": "#top", "north": "#front", "south": "#side",
                                      "west": "#side", "east": "#side", "particle": "#front"}},
    "block/orientable_with_bottom": {"parent": "block/cube",
                                     "textures": {"up": "#top", "down": "#bottom", "north": "#front",
                                                  "south": "#side", "west": "#side", "east": "#side",
                                                  "particle": "#front"}},
    "block/template_torch": {  # simplified: just the post (good enough for an icon)
        "textures": {"particle": "#torch"},
        "elements": [{"from": [7, 0, 7], "to": [9, 10, 9],
                      "faces": {d: {"uv": [7, 6, 9, 16], "texture": "#torch"} for d in DIR_NORMAL}}]},
}


# ------------------------------------------------------------- model resolution

def _strip(ref):
    return ref.split(":", 1)[-1]


def load_chain(ref, assets, _seen=None):
    """Return merged model dict (textures, elements, display, texture_size)."""
    name = _strip(ref)
    if name in VANILLA:
        node = dict(VANILLA[name])
    else:
        path = os.path.join(assets, "models", name + ".json")
        if not os.path.isfile(path):
            return {}  # unknown vanilla/other parent — stop
        node = json.load(open(path, encoding="utf-8"))
    parent = load_chain(node["parent"], assets) if "parent" in node else {}
    merged = {
        "textures": {**parent.get("textures", {}), **node.get("textures", {})},
        "elements": node.get("elements", parent.get("elements", [])),
        "display": {**parent.get("display", {}), **node.get("display", {})},
        "texture_size": node.get("texture_size", parent.get("texture_size", [16, 16])),
    }
    return merged


def resolve_texture(var, textures):
    seen = 0
    while isinstance(var, str) and var.startswith("#") and seen < 10:
        var = textures.get(var[1:]); seen += 1
    return var  # a "ns:path" texture id, or None


PIPE_ARM_YAW = 90  # rotate pipes so the single connection arm points out the back, not the left


def is_pipe_core(ref):
    return ref.endswith("_core") and ("/pipe/" in ref or "/fluid/" in ref)


def build_pipe_model(core_ref, assets):
    """Pipe inventory icon: the 8^3 core plus one connection arm out the back, combined.

    The core/arm block models declare texture_size [8,8] but author their UVs in 16-space
    (so the core maps its texture once and the arm uses bottom-half strips). We resolve each
    model's #texture to an absolute id, merge the elements, and force texture_size [16,16] so
    the UVs map as intended instead of tiling."""
    core = load_chain(core_ref, assets)
    arm = load_chain(core_ref[:-len("_core")] + "_arm", assets)
    elements = []
    for m in (core, arm):
        tex = m.get("textures", {})
        for el in m.get("elements", []):
            faces = {}
            for d, f in el.get("faces", {}).items():
                nf = dict(f)
                nf["texture"] = resolve_texture(f.get("texture", "#" + d), tex) or f.get("texture")
                faces[d] = nf
            ne = dict(el); ne["faces"] = faces
            elements.append(ne)
    display = {k: dict(v) for k, v in core.get("display", {}).items()}
    gui = dict(display.get("gui", {}))
    rot = list(gui.get("rotation", [30, -50, 0]))
    rot[1] += PIPE_ARM_YAW
    gui["rotation"] = rot
    display["gui"] = gui
    return {"textures": {}, "elements": elements,
            "display": display, "texture_size": [16, 16]}


def load_item_tints(item_id, assets):
    """Read constant tints from a 26.x item definition (assets/items/<id>.json), keyed by
    tintindex. Engines tint their core face with a cool [0.2,0.4,0.8] blue this way."""
    path = os.path.join(assets, "items", item_id + ".json")
    if not os.path.isfile(path):
        return {}
    try:
        data = json.load(open(path, encoding="utf-8"))
    except (ValueError, OSError):
        return {}
    out = {}
    for i, t in enumerate(data.get("model", {}).get("tints", [])):
        if str(t.get("type", "")).endswith("constant"):
            v = t.get("value", [1, 1, 1])
            out[i] = tuple(max(0, min(255, int(c * 255))) for c in v)
    return out


def resolve_render_model(ref, assets):
    """The model to actually render for a resolved ref (pipes get the core+arm special case)."""
    return build_pipe_model(ref, assets) if is_pipe_core(ref) else load_chain(ref, assets)


# ------------------------------------------------------------------ geometry

def face_quad(el, d, uv):
    x0, y0, z0 = el["from"]; x1, y1, z1 = el["to"]
    u0, v0, u1, v1 = uv
    P = {
        "down":  [((x0, y0, z1), (u0, v1)), ((x1, y0, z1), (u1, v1)), ((x1, y0, z0), (u1, v0)), ((x0, y0, z0), (u0, v0))],
        "up":    [((x0, y1, z0), (u0, v0)), ((x1, y1, z0), (u1, v0)), ((x1, y1, z1), (u1, v1)), ((x0, y1, z1), (u0, v1))],
        "north": [((x1, y1, z0), (u0, v0)), ((x0, y1, z0), (u1, v0)), ((x0, y0, z0), (u1, v1)), ((x1, y0, z0), (u0, v1))],
        "south": [((x0, y1, z1), (u0, v0)), ((x1, y1, z1), (u1, v0)), ((x1, y0, z1), (u1, v1)), ((x0, y0, z1), (u0, v1))],
        "west":  [((x0, y1, z1), (u0, v0)), ((x0, y1, z0), (u1, v0)), ((x0, y0, z0), (u1, v1)), ((x0, y0, z1), (u0, v1))],
        "east":  [((x1, y1, z0), (u0, v0)), ((x1, y1, z1), (u1, v0)), ((x1, y0, z1), (u1, v1)), ((x1, y0, z0), (u0, v1))],
    }[d]
    return P


def default_uv(el, d):
    x0, y0, z0 = el["from"]; x1, y1, z1 = el["to"]
    return {"down": [x0, z0, x1, z1], "up": [x0, z0, x1, z1],
            "north": [16 - x1, 16 - y1, 16 - x0, 16 - y0], "south": [x0, 16 - y1, x1, 16 - y0],
            "west": [z0, 16 - y1, z1, 16 - y0], "east": [16 - z1, 16 - y1, 16 - z0, 16 - y0]}[d]


def rotate_axis(p, axis, ang, origin):
    ox, oy, oz = origin; x, y, z = p[0] - ox, p[1] - oy, p[2] - oz
    c, s = math.cos(ang), math.sin(ang)
    if axis == "x": y, z = y * c - z * s, y * s + z * c
    elif axis == "y": x, z = x * c + z * s, -x * s + z * c
    else: x, y = x * c - y * s, x * s + y * c
    return (x + ox, y + oy, z + oz)


def rot_xyz(p, rx, ry, rz):
    # Match Minecraft's gui rotationXYZ (= Rx*Ry*Rz): apply Z, then Y, then X to the vertex.
    x, y, z = p
    c, s = math.cos(rz), math.sin(rz); x, y = x * c - y * s, x * s + y * c
    c, s = math.cos(ry), math.sin(ry); x, z = x * c + z * s, -x * s + z * c
    c, s = math.cos(rx), math.sin(rx); y, z = y * c - z * s, y * s + z * c
    return (x, y, z)


# -------------------------------------------------------------------- render

def render(model, assets, size=256, ss=3, alpha_cut=64, tints=None):
    tints = tints or {}
    textures = model["textures"]
    tex_cache = {}

    def get_tex(var):
        tid = resolve_texture(var, textures)
        if not tid:
            return None
        if tid not in tex_cache:
            path = os.path.join(assets, "textures", _strip(tid) + ".png")
            tex_cache[tid] = decode_png(path) if os.path.isfile(path) else None
        return tex_cache[tid]

    # UVs are always authored in 16-space (0-16 = full texture). texture_size is only a
    # source-resolution hint (BlockBench) and is NOT a UV divisor, despite the models that
    # declare [8,8] (pipes) or [64,64] (engine atlases).
    gui = model.get("display", {}).get("gui", {})
    rx, ry, rz = [math.radians(a) for a in gui.get("rotation", [30, 225, 0])]
    ry = -ry  # Minecraft's GUI camera negates the Y rotation vs our math (front lands on the left)

    # collect faces as (verts[(x,y,z,u,v)x4], texture, shade)
    faces = []
    for el in model.get("elements", []):
        erot = el.get("rotation")
        for d, face in el.get("faces", {}).items():
            tex = get_tex(face.get("texture", "#" + d))
            if tex is None:
                continue
            uv = face.get("uv", default_uv(el, d))
            quad = face_quad(el, d, uv)
            n = face.get("rotation", 0) // 90
            corners = [c for c, _ in quad]
            uvs = [c for _, c in quad]
            if n:
                uvs = uvs[-n:] + uvs[:-n]
            v3 = []
            for (px, py, pz), (uu, vv) in zip(corners, uvs):
                p = (px, py, pz)
                if erot:
                    p = rotate_axis(p, erot["axis"], math.radians(erot["angle"]), erot["origin"])
                p = rot_xyz((p[0] - 8, p[1] - 8, p[2] - 8), rx, ry, rz)
                v3.append((p[0], p[1], p[2], uu / 16.0, vv / 16.0))
            faces.append((v3, tex, SHADE.get(d, 1.0), tints.get(face.get("tintindex"))))

    if not faces:
        raise ValueError("model produced no drawable faces")

    # auto-fit projection
    xs = [v[0] for f in faces for v in f[0]]
    ys = [v[1] for f in faces for v in f[0]]
    S = size * ss
    pad = 0.06 * S
    span = max(max(xs) - min(xs), max(ys) - min(ys)) or 1
    scale = (S - 2 * pad) / span
    cx = (max(xs) + min(xs)) / 2; cy = (max(ys) + min(ys)) / 2

    def project(v):
        return (S / 2 + (v[0] - cx) * scale, S / 2 - (v[1] - cy) * scale, v[2])

    color = bytearray(S * S * 4)
    zbuf = [-1e9] * (S * S)
    for v3, tex, sh, tint in faces:
        scr = [project(v) for v in v3]
        tri = [(0, 1, 2), (0, 2, 3)]
        for a, b, c in tri:
            _raster(color, zbuf, S, scr[a], scr[b], scr[c],
                    v3[a][3:5], v3[b][3:5], v3[c][3:5], tex, sh, alpha_cut, tint)

    return _downsample(color, S, size, ss)


def _raster(color, zbuf, S, A, B, C, uvA, uvB, uvC, tex, sh, acut, tint=None):
    tw, th, tpx = tex
    minx = max(0, int(min(A[0], B[0], C[0]))); maxx = min(S - 1, int(math.ceil(max(A[0], B[0], C[0]))))
    miny = max(0, int(min(A[1], B[1], C[1]))); maxy = min(S - 1, int(math.ceil(max(A[1], B[1], C[1]))))
    area = (B[0] - A[0]) * (C[1] - A[1]) - (B[1] - A[1]) * (C[0] - A[0])
    if abs(area) < 1e-9:
        return
    inv = 1.0 / area
    for y in range(miny, maxy + 1):
        for x in range(minx, maxx + 1):
            px, py = x + 0.5, y + 0.5
            w0 = ((B[0] - px) * (C[1] - py) - (B[1] - py) * (C[0] - px)) * inv
            w1 = ((C[0] - px) * (A[1] - py) - (C[1] - py) * (A[0] - px)) * inv
            w2 = 1 - w0 - w1
            if w0 < 0 or w1 < 0 or w2 < 0:
                continue
            depth = w0 * A[2] + w1 * B[2] + w2 * C[2]
            idx = y * S + x
            if depth <= zbuf[idx]:
                continue
            u = w0 * uvA[0] + w1 * uvB[0] + w2 * uvC[0]
            v = w0 * uvA[1] + w1 * uvB[1] + w2 * uvC[1]
            tx = int(u * tw) % tw; ty = int(v * th) % th
            t = (ty * tw + tx) * 4
            a = tpx[t + 3]
            if a < acut:
                continue
            zbuf[idx] = depth
            o = idx * 4
            if tint:
                color[o] = int(tpx[t] * sh * tint[0] / 255)
                color[o + 1] = int(tpx[t + 1] * sh * tint[1] / 255)
                color[o + 2] = int(tpx[t + 2] * sh * tint[2] / 255)
            else:
                color[o] = int(tpx[t] * sh); color[o + 1] = int(tpx[t + 1] * sh)
                color[o + 2] = int(tpx[t + 2] * sh)
            color[o + 3] = a


def _downsample(color, S, size, ss):
    out = bytearray(size * size * 4)
    n = ss * ss
    for y in range(size):
        for x in range(size):
            r = g = b = a = 0
            for dy in range(ss):
                for dx in range(ss):
                    o = ((y * ss + dy) * S + (x * ss + dx)) * 4
                    av = color[o + 3]
                    r += color[o] * av; g += color[o + 1] * av; b += color[o + 2] * av; a += av
            o = (y * size + x) * 4
            if a:
                out[o] = r // a; out[o + 1] = g // a; out[o + 2] = b // a; out[o + 3] = a // n
    return out


# ---------------------------------------------------------------- batch mode

def resolve_model(item_id, assets):
    """Find the model that represents this block item (item model, block model, *_core, basename)."""
    name = item_id.split("/")[-1]
    for cand in (f"item/{item_id}", f"block/{item_id}", f"block/{item_id}_core"):
        if load_chain(cand, assets).get("elements"):
            return cand
    for base in ("item", "block"):
        for suffix in (".json", "_core.json"):
            for f in glob.glob(f"{assets}/models/{base}/**/{name.replace('.json','')}{suffix}", recursive=True):
                cand = os.path.relpath(f, os.path.join(assets, "models"))[:-5]
                if load_chain(cand, assets).get("elements"):
                    return cand
    return None


_REFPAT = (re.compile(r"Grid ([^\n|}\]=]+?)\.png"), re.compile(r"\{\{Grid\|([^}|]+)"),
           re.compile(r"\{\{Grid Crafting Table(.*?)\}\}", re.S), re.compile(r"\|\s*(?:[ABC][123]|Output)\s*=\s*([^|}\n]+)"))


def referenced_names(wiki_dir):
    lit, tmpl, table, param = _REFPAT
    names = set()
    for fn in os.listdir(wiki_dir):
        if not fn.endswith(".txt") or fn.startswith("Template_"):
            continue
        t = open(os.path.join(wiki_dir, fn), encoding="utf-8").read()
        names |= {n.strip() for n in lit.findall(t)} | {n.strip() for n in tmpl.findall(t)}
        for blk in table.findall(t):
            names |= {v.strip() for v in param.findall(blk)}
    return names


def batch(assets, out_dir, wiki_dir, size, ss, dry):
    lang = json.load(open(os.path.join(assets, "lang", "en_us.json"), encoding="utf-8"))
    items = {k[len("block.logistics."):].replace(".", "/"): v
             for k, v in lang.items()
             if k.startswith("block.logistics.") and k[len("block.logistics."):].count(".") == 1}
    os.makedirs(out_dir, exist_ok=True)
    done, failed = [], []
    for item_id, lang_name in sorted(items.items()):
        wiki_name = BLOCK_NAME_OVERRIDES.get(item_id, lang_name)
        ref = resolve_model(item_id, assets)
        if not ref:
            failed.append((item_id, "no model")); continue
        try:
            if not dry:
                px = render(resolve_render_model(ref, assets), assets, size, ss,
                            tints=load_item_tints(item_id, assets))
                encode_png(os.path.join(out_dir, f"Grid {wiki_name}.png"), size, size, px)
            done.append(wiki_name)
            print(f"  {'(dry) ' if dry else ''}Grid {wiki_name}.png   <- {ref}")
        except Exception as e:  # noqa
            failed.append((item_id, str(e)))
    print(f"\n{'would render' if dry else 'rendered'} {len(done)} block icons -> {out_dir}  ({len(failed)} failed)")
    for iid, why in failed:
        print(f"  FAIL {iid}: {why}")
    refs = referenced_names(wiki_dir)
    orphans = sorted(n for n in done if n not in refs)
    if orphans:
        print(f"\nDrift: {len(orphans)} rendered name(s) no page references:")
        for o in orphans:
            print(f"  Grid {o}.png")


def main():
    repo = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    default_assets = os.path.normpath(os.path.join(
        repo, "..", "logistics-mc-26.1", "common", "src", "main", "resources", "assets", "logistics"))
    ap = argparse.ArgumentParser(description="Render a Minecraft block/item model to an isometric icon.")
    ap.add_argument("--assets", default=default_assets)
    ap.add_argument("--model", help="model path under models/, e.g. block/tank/glass_tank")
    ap.add_argument("--out", help="output PNG (single-model mode)")
    ap.add_argument("--batch", action="store_true", help="render all block items into --out-dir")
    ap.add_argument("--out-dir", default=os.path.join(repo, "wiki", "media"))
    ap.add_argument("--wiki-dir", default=os.path.join(repo, "wiki"))
    ap.add_argument("--dry-run", action="store_true")
    ap.add_argument("--size", type=int, default=256)
    ap.add_argument("--ss", type=int, default=3, help="supersampling factor")
    ap.add_argument("--rot", help="override gui rotation 'rx,ry,rz' (testing)")
    args = ap.parse_args()

    if args.batch:
        batch(args.assets, args.out_dir, args.wiki_dir, args.size, args.ss, args.dry_run)
        return
    if not args.model or not args.out:
        ap.error("single-model mode needs --model and --out (or use --batch)")
    model = resolve_render_model(args.model, args.assets)
    if not model.get("elements"):
        sys.exit(f"no geometry resolved for {args.model}")
    if args.rot:
        model.setdefault("display", {}).setdefault("gui", {})["rotation"] = [float(a) for a in args.rot.split(",")]
    item_id = args.model[len("item/"):] if args.model.startswith("item/") else args.model
    px = render(model, args.assets, args.size, args.ss, tints=load_item_tints(item_id, args.assets))
    encode_png(args.out, args.size, args.size, px)
    print(f"wrote {args.out} ({args.size}px, ss={args.ss}, {len(model['elements'])} elements)")


if __name__ == "__main__":
    main()
