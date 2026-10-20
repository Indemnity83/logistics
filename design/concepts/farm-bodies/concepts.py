"""Concept farm bodies (bed + planting, no case) on one labelled contact sheet.

Every face samples its texture 1:1 (check_texels enforces it). Blocks and plant strands crop
vanilla textures; small plants are redrawn in their vanilla palettes (SPRITES).
Rendering reuses logistics-docs/tools/render_blocks.py, plus a blended pass for water.

Writes farm-concepts.png, models/<body>.json and the generated textures/ next to this file.
Vanilla textures are read from the Minecraft client jar into .cache/ and never committed.
"""
import glob
import json
import math
import os
import shutil
import sys
import zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.normpath(os.path.join(HERE, "..", "..", ".."))
DOCS_TOOLS = os.environ.get("LOGISTICS_DOCS_TOOLS",
                            os.path.normpath(os.path.join(REPO, "..", "logistics-docs", "tools")))
sys.path.insert(0, DOCS_TOOLS)
import render_blocks as rb  # noqa: E402

decode_rgba, encode_rgba = rb.decode_png, rb.encode_png

MOD = os.path.join(REPO, "common/src/main/resources/assets/logistics")
CACHE = os.path.join(HERE, ".cache")
ASSETS = os.path.join(CACHE, "assets")
VAN = os.path.join(CACHE, "vanilla/assets/minecraft/textures")
CLIENT_JAR = os.environ.get("MC_CLIENT_JAR", os.path.expanduser(
    "~/.gradle/caches/neoformruntime/artifacts/minecraft_26.3_client.jar"))


def ensure_vanilla():
    if os.path.isdir(VAN):
        return
    jars = glob.glob(CLIENT_JAR)
    if not jars:
        sys.exit(f"no Minecraft client jar at {CLIENT_JAR}; set MC_CLIENT_JAR")
    with zipfile.ZipFile(jars[0]) as z:
        for n in z.namelist():
            if n.startswith(("assets/minecraft/textures/block/", "assets/minecraft/textures/font/ascii",
                             "assets/minecraft/textures/item/apple")):
                z.extract(n, os.path.join(CACHE, "vanilla"))

GRASS, WATER, STEM = 0x91BD59, 0x3F76E4, 0xE0C71C


# ---------------------------------------------------------------- textures

def tex(name, tint=None):
    """Copy a vanilla block texture (first frame, optional multiply tint); return its id."""
    key = name + (f"_{tint:06x}" if tint else "")
    out = os.path.join(ASSETS, "textures/block/v", key + ".png")
    if not os.path.exists(out):
        w, h, px = decode_rgba(os.path.join(VAN, "block", name + ".png"))
        if h > w:
            px, h = px[:w * w * 4], w
        if tint:
            tr, tg, tb = (tint >> 16) & 255, (tint >> 8) & 255, tint & 255
            px = bytearray(px)
            for i in range(0, len(px), 4):
                px[i], px[i + 1], px[i + 2] = px[i] * tr // 255, px[i + 1] * tg // 255, px[i + 2] * tb // 255
        os.makedirs(os.path.dirname(out), exist_ok=True)
        encode_rgba(out, w, h, px)
    return "logistics:block/v/" + key


def modtex(path):
    dst = os.path.join(ASSETS, "textures", path + ".png")
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    shutil.copy(os.path.join(MOD, "textures", path + ".png"), dst)
    return "logistics:" + path


# ---------------------------------------------------------------- elements
#
# Every face samples its texture 1:1: a face's uv span, in texels, equals its size in model
# pixels. check_texels() enforces that before anything is rendered.

def box(frm, to, side, top=None, bottom=None, off=(0, 0), top_off=None, faces=None, **extra):
    """Cuboid cropping its textures 1:1; off/top_off pick where in the texture the crop sits."""
    (x0, y0, z0), (x1, y1, z1) = frm, to
    top = top or side
    bottom = bottom or top
    top_off = top_off if top_off is not None else (8 - (x1 - x0) / 2, 8 - (z1 - z0) / 2)

    def side_uv(w):
        u0 = off[0] if off[0] is not None else 8 - w / 2
        return [u0, off[1], u0 + w, off[1] + (y1 - y0)]

    tu = [top_off[0], top_off[1], top_off[0] + (x1 - x0), top_off[1] + (z1 - z0)]
    f = {
        "north": {"texture": side, "uv": side_uv(x1 - x0)},
        "south": {"texture": side, "uv": side_uv(x1 - x0)},
        "east": {"texture": side, "uv": side_uv(z1 - z0)},
        "west": {"texture": side, "uv": side_uv(z1 - z0)},
        "up": {"texture": top, "uv": tu},
        "down": {"texture": bottom, "uv": tu},
    }
    if faces:
        f = {k: v for k, v in f.items() if k in faces}
    return dict({"from": list(frm), "to": list(to), "faces": f}, **extra)


def cross(t, cx, cz, y0, w, h, u0=0, v0=0):
    """Two axis-aligned planes showing the w x h texels at (u0, v0) of the texture."""
    uv = [u0, v0, u0 + w, v0 + h]
    a = {"from": [cx - w / 2, y0, cz], "to": [cx + w / 2, y0 + h, cz],
         "faces": {"north": {"texture": t, "uv": uv}, "south": {"texture": t, "uv": uv}}}
    b = {"from": [cx, y0, cz - w / 2], "to": [cx, y0 + h, cz + w / 2],
         "faces": {"east": {"texture": t, "uv": uv}, "west": {"texture": t, "uv": uv}}}
    return [a, b]


def plane(t, cx, cz, y0, w, h, u0=0, v0=0, axis="x"):
    """One flat plane along x or z; its back face shows the texture mirrored."""
    uv = [u0, v0, u0 + w, v0 + h]
    if axis == "x":
        return {"from": [cx - w / 2, y0, cz], "to": [cx + w / 2, y0 + h, cz],
                "faces": {"north": {"texture": t, "uv": uv}, "south": {"texture": t, "uv": uv}}}
    return {"from": [cx, y0, cz - w / 2], "to": [cx, y0 + h, cz + w / 2],
            "faces": {"east": {"texture": t, "uv": uv}, "west": {"texture": t, "uv": uv}}}


def bed(top, side, side_off=(0, 0)):
    return box((3, 4, 3), (13, 6, 13), side, top, off=(3, side_off[1]), top_off=(3, 3))


QUAD = [(5.5, 5.5), (10.5, 5.5), (5.5, 10.5), (10.5, 10.5)]


def check_texels(name, elements):
    """Return the faces whose texels are not exactly one model pixel."""
    bad = []
    for i, el in enumerate(elements):
        (x0, y0, z0), (x1, y1, z1) = el["from"], el["to"]
        size = {"north": (x1 - x0, y1 - y0), "south": (x1 - x0, y1 - y0),
                "east": (z1 - z0, y1 - y0), "west": (z1 - z0, y1 - y0),
                "up": (x1 - x0, z1 - z0), "down": (x1 - x0, z1 - z0)}
        for d, face in el["faces"].items():
            tw, th, _ = decode_rgba(os.path.join(ASSETS, "textures", face["texture"].split(":", 1)[1] + ".png"))
            u0, v0, u1, v1 = face["uv"]
            du, dv = abs(u1 - u0) * tw / 16, abs(v1 - v0) * th / 16
            if face.get("rotation", 0) % 180:
                du, dv = dv, du
            gu, gv = size[d]
            if abs(du - gu) > 1e-6 or abs(dv - gv) > 1e-6:
                bad.append(f"{name}: element {i} {d} shows {du}x{dv} texels on {gu}x{gv} px")
    return bad


# ---------------------------------------------------------------- sprites
#
# Small plants drawn at the bed's own scale in their vanilla palettes, since the vanilla sprites
# are too big to crop 1:1 into a 10x10 bed.

GENERATED = set()


def sprite(name, rows, palette):
    """A hand-placed sprite at the top-left of a 16x16 texture; '.' is transparent."""
    out = os.path.join(ASSETS, "textures/block/v", name + ".png")
    px = bytearray(16 * 16 * 4)
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                c = palette[ch]
                px[(y * 16 + x) * 4:(y * 16 + x) * 4 + 4] = bytes((c >> 16, (c >> 8) & 255, c & 255, 255))
    os.makedirs(os.path.dirname(out), exist_ok=True)
    encode_rgba(out, 16, 16, px)
    GENERATED.add(name)
    return "logistics:block/v/" + name


def tinted(c, tint):
    return (((c >> 16) * (tint >> 16) // 255) << 16) | ((((c >> 8) & 255) * ((tint >> 8) & 255) // 255) << 8) \
        | ((c & 255) * (tint & 255) // 255)


SPRITES = {
    "berry_bush": ([".bab.",
                    "baeab",
                    "ebcag",
                    "daceb",
                    ".d.d."],
                   {"a": 0x3c6e42, "b": 0x487f4f, "c": 0x286240, "d": 0x295230, "e": 0xa50700, "g": 0xdf467e}),
    "nether_wart": (["de...",
                     "ec.de",
                     ".b.ec",
                     ".b..b",
                     ".f..f"],
                    {"b": 0x480000, "c": 0x831c20, "d": 0xc53439, "e": 0xa42429, "f": 0x730408}),
    "brown_mushroom": ([".ab.",
                        "abbc",
                        "..g."],
                       {"a": 0xcc9978, "b": 0x916d55, "c": 0x725643, "g": 0xb5947d}),
    "crimson_fungus": ([".acb.",
                        "adabe",
                        "efgfi",
                        "..n..",
                        "..m.."],
                       {"a": 0xc53439, "b": 0xa42429, "c": 0xffa468, "d": 0xff6500, "e": 0x941818,
                        "f": 0x730408, "g": 0x521810, "i": 0x480000, "m": 0x723232, "n": 0x854242}),
    "warped_fungus": (["acbca",
                       "fegef",
                       "..i..",
                       "..j.."],
                      {"a": 0x119b85, "b": 0xff6500, "c": 0x14b485, "e": 0x167e86, "f": 0x16615b,
                       "g": 0xe93e0c, "i": 0x442131, "j": 0x4b2737}),
    "poppy": (["aca",
               "cdb",
               ".g.",
               "fg.",
               ".g."],
              {"a": 0xed302c, "b": 0x9b221a, "c": 0xbf2529, "d": 0x742303, "f": 0x265a25, "g": 0x4a8f28}),
    "dandelion": ([".a.",
                   "aca",
                   ".e.",
                   "fg.",
                   ".e."],
                  {"a": 0xffec4f, "c": 0xf19d25, "e": 0x177c04, "f": 0x4a8f28, "g": 0x55ab2d}),
    "cornflower": (["aba",
                    "bdc",
                    ".f.",
                    ".fh",
                    ".i."],
                   {"a": 0x728ff1, "b": 0x466aeb, "c": 0x6281e8, "d": 0x2a4cc7, "f": 0x2e6924,
                    "h": 0x578c4d, "i": 0x5b9c51}),
    "allium": (["dab",
                "bce",
                ".f.",
                ".g.",
                ".f."],
               {"a": 0xd2a6f6, "b": 0xb878ed, "c": 0xa65ee1, "d": 0xe8cffe, "e": 0x7b4ea0,
                "f": 0x4a8f28, "g": 0x529a2e}),
    "oxeye_daisy": (["cbc",
                     "bea",
                     ".h.",
                     "ij.",
                     ".i."],
                    {"a": 0x9bbdbd, "b": 0xf7f7f7, "c": 0xd6e8e8, "e": 0xfed639, "h": 0x529a2e,
                     "i": 0x177c04, "j": 0x55ab2d}),
    "blue_orchid": (["bc.",
                     "cdb",
                     ".ea",
                     ".f.",
                     ".g."],
                    {"a": 0x2fcefd, "b": 0x2abffd, "c": 0x27a9f4, "d": 0x1c92d6, "e": 0x4a8f28,
                     "f": 0x55ab2d, "g": 0x177c04}),
    "dead_bush": (["b..a",
                   "cb.c",
                   ".cb.",
                   ".ca."],
                  {"a": 0x513d24, "b": 0x946428, "c": 0x67502c}),
    "bamboo_leaves": ([".ab",
                       "aea",
                       "b.f"],
                      {"a": 0x67a124, "b": 0x528919, "e": 0x457616, "f": 0x375a15}),
    "gourd_stem": (["...a",
                    "..ab",
                    ".ab.",
                    "ab.."],
                   {"a": tinted(0xb5b5b5, STEM), "b": tinted(0x858585, STEM)}),
    "cocoa_pod": (["cfc.bcb",  # side 3x4 at (0,0), top 3x3 at (4,0)
                   "geg.cfc",
                   "gdg.bcb",
                   "bgb"],
                  {"b": 0x652f0b, "c": 0x823c0e, "d": 0xe2b17c, "e": 0xcb8e53, "f": 0x96571a, "g": 0xb97335}),
    "log_end": (["bab",
                 "ada",
                 "bab"],
                {"a": 0x5f4a2b, "b": 0x4c3d26, "d": 0xb8945f}),
    "apple": (["ab"],
              {"a": 0xdd1725, "b": 0xff1c2b}),
}


def spr(name):
    rows, palette = SPRITES[name]
    return sprite(name, rows, palette), len(rows[0]), len(rows)


def strapped_log():
    """Oak bark turned to run along x (3 rows by 8), with iron straps at columns 1 and 6."""
    out = os.path.join(ASSETS, "textures/block/v/strapped_log.png")
    w, h, bark = decode_rgba(os.path.join(VAN, "block/oak_log.png"))
    px = bytearray(16 * 16 * 4)
    for r in range(3):
        for c in range(8):
            o = (r * 16 + c) * 4
            if c in (1, 6):
                px[o:o + 4] = bytes((156, 156, 156, 255) if r < 2 else (104, 104, 104, 255))
            else:
                s = (c * w + r) * 4
                px[o:o + 4] = bark[s:s + 4]
    os.makedirs(os.path.dirname(out), exist_ok=True)
    encode_rgba(out, 16, 16, px)
    GENERATED.add("strapped_log")
    return "logistics:block/v/strapped_log"


# ---------------------------------------------------------------- bodies

def real(model):
    d = json.load(open(os.path.join(MOD, "models/block/forestry", model + ".json")))
    body = modtex(d["textures"]["body"].split(":", 1)[1])
    els = [e for e in d["elements"] if all(f["texture"] == "#body" for f in e["faces"].values())]
    for e in els:
        for f in e["faces"].values():
            f["texture"] = body
    return els


def log_pile():
    humus = modtex("block/forestry/humus")
    side = strapped_log()
    end, _, _ = spr("log_end")
    e = [bed(humus, humus)]
    run = {"texture": side, "uv": [0, 0, 8, 3]}
    cap = {"texture": end, "uv": [0, 0, 3, 3]}
    for y, zs in ((6, (3.5, 6.5, 9.5)), (9, (5, 8))):
        for z in zs:
            e.append({"from": [4, y, z], "to": [12, y + 3, z + 3],
                      "faces": {"north": dict(run), "south": dict(run), "up": dict(run), "down": dict(run),
                                "east": dict(cap), "west": dict(cap)}})
    return e


def gourd():
    e = [bed(tex("farmland_moist"), tex("dirt"))]
    e.append(box((3, 6, 8), (8, 10, 13), tex("pumpkin_side"), tex("pumpkin_top"), off=(None, 5)))
    e.append(box((8, 6, 3), (13, 10, 8), tex("melon_side"), tex("melon_top"), off=(None, 5)))
    stem, w, h = spr("gourd_stem")
    e += cross(stem, 5.5, 5.5, 6, w, h)
    e += cross(stem, 10.5, 10.5, 6, w, h)
    return e


def plantation():
    sand = tex("sand")
    e = [box((3, 4, 3), (7, 6, 13), sand, off=(3, 0), top_off=(3, 3)),
         box((9, 4, 3), (13, 6, 13), sand, off=(9, 0), top_off=(9, 3)),
         box((7, 4, 3), (9, 5, 13), tex("water_still", WATER), translucent=0.55,
             faces=("up", "north", "south"), off=(7, 0), top_off=(7, 3))]
    cane = tex("sugar_cane", GRASS)
    e += cross(cane, 5, 5.5, 6, 3, 8, u0=4, v0=0)
    e += cross(cane, 5, 10.5, 6, 3, 6, u0=4, v0=4)
    stalk = tex("bamboo_stalk")
    leaves, lw, lh = spr("bamboo_leaves")
    for (x, z, h) in ((10, 4, 8), (11, 9, 6)):
        e.append(box((x, 6, z), (x + 2, 6 + h, z + 2), stalk, off=(0, 0), top_off=(13, 0)))
        e += cross(leaves, x + 1, z + 1, 6 + h - 2, lw, lh)
    return e


def desert():
    sand = tex("sand")
    e = [bed(sand, sand)]
    e.append(box((5, 6, 5), (9, 13, 9), tex("cactus_side"), tex("cactus_top"), off=(None, 0)))
    e += cross(tex("cactus_flower"), 7, 7, 13, 4, 2, u0=6, v0=7)
    e.append(box((9, 6, 9), (12, 10, 12), tex("cactus_side"), tex("cactus_top"), off=(None, 3)))
    bush, w, h = spr("dead_bush")
    e += cross(bush, 10.5, 5.5, 6, w, h)
    return e


def berry():
    e = [bed(tex("grass_block_top", GRASS), tex("grass_block_side"))]
    bush, w, h = spr("berry_bush")
    for x, z in QUAD:
        e += cross(bush, x, z, 6, w, h)
    return e


def cocoa():
    log = tex("jungle_log")
    e = [bed(modtex("block/forestry/humus"), modtex("block/forestry/humus"))]
    e.append(box((6, 6, 6), (10, 14, 10), log, tex("jungle_log_top"), off=(None, 0)))
    pod, _, _ = spr("cocoa_pod")
    for frm, to in (((7, 8, 3), (10, 12, 6)), ((10, 7, 7), (13, 11, 10)), ((3, 9, 6), (6, 13, 9))):
        e.append(box(frm, to, pod, off=(0, 0), top_off=(4, 0)))
    return e


def glow_berries():
    moss = tex("moss_block")
    e = [bed(moss, moss),
         box((3, 13, 3), (13, 14, 13), tex("spruce_trapdoor"), off=(3, 0), top_off=(3, 3)),
         box((7, 6, 7), (8, 13, 8), tex("spruce_log"), tex("spruce_log_top"), off=(7, 0), top_off=(7, 7))]
    vines = (((4.5, 8), 6, "z"), ((8, 4.5), 4, "x"), ((11.5, 8), 5, "z"), ((8, 11.5), 7, "x"))
    for (x, z), h, axis in vines:
        e.append(plane(tex("cave_vines_plant_lit"), x, z, 13 - h + 2, 4, h - 2, u0=1, v0=4, axis=axis))
        e.append(plane(tex("cave_vines_lit"), x, z, 13 - h, 4, 2, u0=2, v0=2, axis=axis))
    return e


def mushroom():
    e = [bed(tex("mycelium_top"), tex("mycelium_side"))]
    e.append(box((7, 6, 7), (9, 11, 9), tex("mushroom_stem"), off=(None, 4)))
    cap = tex("red_mushroom_block")
    e.append(box((4, 11, 4), (12, 13, 12), cap, off=(4, 0), top_off=(4, 4)))
    e.append(box((5, 13, 5), (11, 14, 11), cap, off=(5, 2), top_off=(5, 5)))
    shroom, w, h = spr("brown_mushroom")
    e += cross(shroom, 4.5, 11, 6, w, h)
    e += cross(shroom, 11.5, 11.5, 6, w, h)
    return e


def nether_wart():
    ss = tex("soul_sand")
    e = [bed(ss, ss)]
    wart, w, h = spr("nether_wart")
    for x, z in QUAD:
        e += cross(wart, x, z, 6, w, h)
    return e


def peat_bog():
    mud = tex("mud")
    e = [box((3, 4, 3), (13, 6, 6), mud, off=(3, 0), top_off=(3, 3)),
         box((3, 4, 10), (13, 6, 13), mud, off=(3, 0), top_off=(3, 10)),
         box((3, 4, 6), (5, 6, 10), mud, off=(3, 0), top_off=(3, 6)),
         box((10, 4, 6), (13, 6, 10), mud, off=(10, 0), top_off=(10, 6)),
         box((5, 4, 6), (10, 5, 10), tex("water_still", WATER), translucent=0.55, faces=("up",),
             top_off=(5, 6))]
    peat = tex("packed_mud")
    e.append(box((9, 6, 10), (12, 8, 12), peat, off=(2, 4)))
    e.append(box((10, 8, 10), (12, 9, 12), peat, off=(6, 2)))
    e.append(box((4, 6, 4), (6, 7, 6), peat, off=(9, 9)))
    return e


def aquatic():
    sand = tex("sand")
    e = [bed(sand, sand)]
    for x, z, h in ((5, 5.5, 7), (10.5, 6, 6), (8, 10.5, 7)):
        e += cross(tex("kelp_plant"), x, z, 6, 4, h, u0=5, v0=16 - h)
    e += cross(tex("seagrass"), 5, 10.5, 6, 4, 4, u0=1, v0=12)
    e += cross(tex("seagrass"), 11, 10.5, 6, 3, 3, u0=10, v0=12)
    for x, z in ((8, 5), (9, 6)):
        e.append(box((x, 6, z), (x + 1, 8, z + 1), tex("sea_pickle"), off=(6, 5), top_off=(6, 2)))
    e.append(box((3, 6, 3), (13, 13, 13), tex("water_still", WATER), translucent=0.4,
                 faces=("up", "north", "south", "east", "west"), off=(3, 0), top_off=(3, 3)))
    return e


def chorus():
    es, plant, flower = tex("end_stone"), tex("chorus_plant"), tex("chorus_flower")
    e = [bed(es, es)]
    e.append(box((7, 6, 7), (9, 11, 9), plant, off=(None, 4)))
    e.append(box((6, 11, 6), (10, 14, 10), flower, off=(None, 6)))
    e.append(box((4, 8, 7), (7, 10, 9), plant, off=(None, 2)))
    e.append(box((4, 10, 7), (6, 12, 9), plant, off=(None, 8)))
    e.append(box((3, 12, 7), (6, 14, 10), flower, off=(None, 6)))
    e.append(box((9, 7, 7), (12, 9, 9), plant, off=(None, 6)))
    e.append(box((10, 9, 7), (12, 11, 9), plant, off=(None, 10)))
    e.append(box((10, 11, 7), (12, 13, 9), tex("chorus_flower_dead"), off=(None, 6)))
    return e


def vines():
    e = [bed(tex("warped_nylium"), tex("warped_nylium_side"))]
    for (x, z), h in zip(((5, 5.5), (10.5, 6), (7.5, 10.5)), (8, 6, 7)):
        e += cross(tex("twisting_vines_plant"), x, z, 6, 4, h - 2, u0=5, v0=0)
        e += cross(tex("twisting_vines"), x, z, 6 + h - 2, 4, 2, u0=7, v0=2)
    return e


def flowers():
    e = [bed(tex("grass_block_top", GRASS), tex("grass_block_side"))]
    names = ("poppy", "dandelion", "cornflower", "allium", "oxeye_daisy", "blue_orchid")
    spots = ((4.5, 5), (8, 4.5), (11.5, 5.5), (5, 9.5), (8.5, 8.5), (11, 11))
    for n, (x, z) in zip(names, spots):
        t, w, h = spr(n)
        e += cross(t, x, z, 6, w, h)
    return e


def moss_azalea():
    moss = tex("moss_block")
    e = [bed(moss, moss)]
    e += cross(tex("azalea_plant"), 6.5, 6.5, 6, 4, 3, u0=6, v0=11)
    e.append(box((4, 9, 4), (10, 13, 10), tex("flowering_azalea_side"), tex("flowering_azalea_top"),
                 off=(0, 0), top_off=(5, 5)))
    e += cross(tex("azalea_plant"), 10.5, 10.5, 6, 3, 2, u0=6, v0=12)
    e.append(box((9, 8, 9), (13, 11, 13), tex("azalea_side"), tex("azalea_top"), off=(0, 0), top_off=(6, 6)))
    return e


def fungus():
    c, w = tex("crimson_nylium"), tex("warped_nylium")
    e = [box((3, 4, 3), (8, 6, 13), tex("crimson_nylium_side"), c, off=(3, 0), top_off=(3, 3)),
         box((8, 4, 3), (13, 6, 13), tex("warped_nylium_side"), w, off=(8, 0), top_off=(8, 3))]
    crimson, cw, ch = spr("crimson_fungus")
    warped, ww, wh = spr("warped_fungus")
    for z in (5.5, 10.5):
        e += cross(crimson, 5.5, z, 6, cw, ch)
        e += cross(warped, 10.5, z, 6, ww, wh)
    return e


APPLE_RED = [(84, 9, 14), (156, 16, 23), (180, 19, 30), (221, 23, 37), (255, 28, 43)]
APPLE_SHINE, APPLE_STEM = (255, 150, 157), [(84, 36, 9), (117, 40, 2)]


def apple_block():
    """Tileable 16x16 'pile of apples' in the vanilla apple palette, lit from the top-left."""
    out = os.path.join(ASSETS, "textures/block/v/apple_pile.png")
    rows = [(2, (2, 6, 10, 14)), (6, (0, 4, 8, 12)), (10, (2, 6, 10, 14)), (14, (0, 4, 8, 12))]
    jitter = [(0.2, -0.2), (-0.2, 0.1), (0.1, 0.2), (-0.1, -0.2)] * 4
    centers = [(x + jitter[i][0], y + jitter[i][1], i % 3 == 0)
               for i, (y, x) in enumerate((y, x) for y, xs in rows for x in xs)]
    r = 2.15
    px = bytearray(16 * 16 * 4)
    for y in range(16):
        for x in range(16):
            best = None
            for cx, cy, stem in centers:
                dx = (x + 0.5 - cx + 8) % 16 - 8
                dy = (y + 0.5 - cy + 8) % 16 - 8
                d = math.hypot(dx, dy)
                if d < r and (best is None or d < best[0]):
                    best = (d, dx / r, dy / r, stem)
            if best is None:
                c = tuple(v * 3 // 5 for v in APPLE_RED[0])
            else:
                d, nx, ny, stem = best
                light = -(nx + ny) * 0.9 + (1 - d / r) * 1.4
                c = APPLE_RED[max(0, min(4, int(round(1.2 + light * 1.6))))]
                if d / r > 0.82:
                    c = APPLE_RED[max(0, APPLE_RED.index(c) - 1)]
                if -0.75 < nx < -0.25 and -0.75 < ny < -0.25:
                    c = APPLE_SHINE
                if stem and -0.05 < nx < 0.45 and -0.05 < ny < 0.45:
                    c = APPLE_STEM[1]
            o = (y * 16 + x) * 4
            px[o:o + 4] = bytes(c + (255,))
    os.makedirs(os.path.dirname(out), exist_ok=True)
    encode_rgba(out, 16, 16, px)
    GENERATED.add("apple_pile")
    return "logistics:block/v/apple_pile"


def orchard_basket():
    e = [bed(tex("grass_block_top", GRASS), tex("grass_block_side"))]
    wicker = tex("stripped_oak_log")
    e.append(box((4, 6, 4), (12, 7, 12), wicker, off=(4, 0), top_off=(4, 4)))
    for frm, to in (((4, 7, 4), (12, 10, 5)), ((4, 7, 11), (12, 10, 12)),
                    ((4, 7, 5), (5, 10, 11)), ((11, 7, 5), (12, 10, 11))):
        e.append(box(frm, to, wicker, off=(2, 4)))
    e.append(box((4, 10, 7), (5, 13, 9), wicker, off=(4, 2)))
    e.append(box((11, 10, 7), (12, 13, 9), wicker, off=(4, 2)))
    e.append(box((4, 13, 7), (12, 14, 9), wicker, off=(2, 6)))
    pile = apple_block()
    e.append(box((5, 7, 5), (11, 9, 11), pile, off=(5, 4), top_off=(5, 5)))
    e.append(box((6, 9, 6), (10, 10, 10), pile, off=(4, 0), top_off=(6, 6)))
    return e


def orchard():
    e = [bed(tex("grass_block_top", GRASS), tex("grass_block_side"))]
    e += [el for el in real("arboretum") if el.get("name") != "bed"]
    apple, _, _ = spr("apple")
    spots = [(5.5, 10, 3.5), (9.5, 9.5, 3.5), (6.5, 10.5, 11.5), (10, 9.5, 11.5),
             (3.5, 10, 6), (3.5, 9.5, 9.5), (11.5, 10.5, 5.5), (11.5, 9.5, 9),
             (7, 12.5, 8), (9, 12.5, 6), (5.8, 12.2, 10.6)]
    for x, y, z in spots:
        b = box((x, y, z), (x + 1, y + 1, z + 1), apple, off=(0, 0), top_off=(1, 0))
        e.append(b)
    return e


BODIES = [  # (label, model file, builder)
    ("Crop Farm", "crop_farm", lambda: real("crop_farm")),
    ("Arboretum", "arboretum_logs", log_pile),
    ("Arboretum", "arboretum_tree", lambda: real("arboretum")),
    ("Orchard", "orchard_tree", orchard),
    ("Orchard", "orchard_basket", orchard_basket),
    ("Gourd Farm", "gourd_farm", gourd),
    ("Plantation", "plantation", plantation),
    ("Desert Farm", "desert_farm", desert),
    ("Berry Farm", "berry_farm", berry),
    ("Cocoa Plantation", "cocoa_plantation", cocoa),
    ("Glow Berry Farm", "glow_berry_farm", glow_berries),
    ("Mushroom Farm", "mushroom_farm", mushroom),
    ("Nether Wart Farm", "nether_wart_farm", nether_wart),
    ("Peat Bog", "peat_bog", peat_bog),
    ("Aquatic Farm", "aquatic_farm", aquatic),
    ("Chorus Farm", "chorus_farm", chorus),
    ("Vine Farm", "vine_farm", vines),
    ("Flower Farm", "flower_farm", flowers),
    ("Moss & Azalea Farm", "moss_azalea_farm", moss_azalea),
    ("Fungus Farm", "fungus_farm", fungus),
]


# ---------------------------------------------------------------- render

def render(elements, size, ss):
    rx, ry, rz = math.radians(30), -math.radians(225), 0.0
    cache = {}

    def get(tid):
        if tid not in cache:
            cache[tid] = rb.decode_png(os.path.join(ASSETS, "textures", tid.split(":", 1)[1] + ".png"))
        return cache[tid]

    def xf(p):
        return rb.rot_xyz((p[0] - 8, p[1] - 8, p[2] - 8), rx, ry, rz)

    opaque, clear = [], []
    for el in elements:
        for d, face in el["faces"].items():
            if rb.rot_xyz(rb.DIR_NORMAL[d], rx, ry, rz)[2] <= 1e-6:
                continue  # back face; the game culls these, and a plane's two faces would z-fight
            quad = rb.face_quad(el, d, face.get("uv") or rb.default_uv(el, d))
            n = face.get("rotation", 0) // 90
            corners, uvs = [c for c, _ in quad], [u for _, u in quad]
            if n:
                uvs = uvs[-n:] + uvs[:-n]
            v3 = [xf(c) + (uv[0] / 16.0, uv[1] / 16.0) for c, uv in zip(corners, uvs)]
            (clear if el.get("translucent") else opaque).append(
                (v3, get(face["texture"]), rb.SHADE[d], el.get("translucent")))

    # Fixed framing: every body is fitted to the same interior envelope, so scales compare.
    env = [xf((x, y, z)) for x in (2, 14) for y in (4, 15) for z in (2, 14)]
    xs, ys = [v[0] for v in env], [v[1] for v in env]
    S = size * ss
    pad = 0.05 * S
    scale = (S - 2 * pad) / max(max(xs) - min(xs), max(ys) - min(ys))
    cx, cy = (max(xs) + min(xs)) / 2, (max(ys) + min(ys)) / 2

    def proj(v):
        return (S / 2 + (v[0] - cx) * scale, S / 2 - (v[1] - cy) * scale, v[2])

    color = bytearray(S * S * 4)
    zbuf = [-1e9] * (S * S)
    for v3, t, sh, _ in opaque:
        p = [proj(v) for v in v3]
        for a, b, c in ((0, 1, 2), (0, 2, 3)):
            rb._raster(color, zbuf, S, p[a], p[b], p[c], v3[a][3:5], v3[b][3:5], v3[c][3:5], t, sh, 64)
    clear.sort(key=lambda f: sum(v[2] for v in f[0]))
    for v3, t, sh, alpha in clear:
        p = [proj(v) for v in v3]
        for a, b, c in ((0, 1, 2), (0, 2, 3)):
            blend(color, zbuf, S, p[a], p[b], p[c], v3[a][3:5], v3[b][3:5], v3[c][3:5], t, sh, alpha)
    return rb._downsample(color, S, size, ss)


def blend(color, zbuf, S, A, B, C, uvA, uvB, uvC, tex, sh, alpha):
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
            idx = y * S + x
            if w0 * A[2] + w1 * B[2] + w2 * C[2] <= zbuf[idx]:
                continue
            u = w0 * uvA[0] + w1 * uvB[0] + w2 * uvC[0]
            v = w0 * uvA[1] + w1 * uvB[1] + w2 * uvC[1]
            t = ((int(v * th) % th) * tw + int(u * tw) % tw) * 4
            o = idx * 4
            for k in range(3):
                color[o + k] = int(color[o + k] * (1 - alpha) + tpx[t + k] * sh * alpha)
            color[o + 3] = max(color[o + 3], int(255 * alpha) + 60)


# ---------------------------------------------------------------- labels

class Font:
    def __init__(self):
        self.w, self.h, self.px = decode_rgba(os.path.join(VAN, "font/ascii.png"))
        self.adv = {}
        for c in range(32, 127):
            gx, gy = (c % 16) * 8, (c // 16) * 8
            cols = [x for x in range(8) for y in range(8) if self.px[((gy + y) * self.w + gx + x) * 4 + 3]]
            self.adv[c] = 4 if c == 32 else (max(cols) + 2 if cols else 4)

    def width(self, s):
        return sum(self.adv[ord(ch)] for ch in s) - 1

    def draw(self, buf, W, s, ox, oy, k, rgb):
        x = ox
        for ch in s:
            c = ord(ch)
            gx, gy = (c % 16) * 8, (c // 16) * 8
            for yy in range(8):
                for xx in range(8):
                    if self.px[((gy + yy) * self.w + gx + xx) * 4 + 3]:
                        for dy in range(k):
                            for dx in range(k):
                                o = ((oy + yy * k + dy) * W + x + xx * k + dx) * 4
                                buf[o:o + 4] = bytes(rgb + (255,))
            x += self.adv[c] * k


def export(slug, elements):
    """Write the body as a plain block model; vanilla ids lose their concept-only tints."""
    names, textures = {}, {}

    def ref(tid):
        path = tid.split(":", 1)[1]
        if path.startswith("block/v/"):
            name = path[len("block/v/"):]
            stripped = name.rsplit("_", 1)[0] if len(name.rsplit("_", 1)[-1]) == 6 and \
                all(c in "0123456789abcdef" for c in name.rsplit("_", 1)[-1]) else name
            mid = f"logistics:block/forestry/concept/{name}" if name in GENERATED else f"minecraft:block/{stripped}"
        elif path.startswith("item/v/"):
            mid = "minecraft:item/" + path[len("item/v/"):]
        else:
            mid = tid
        if mid not in names:
            names[mid] = f"t{len(names)}"
            textures[names[mid]] = mid
        return "#" + names[mid]

    els = []
    for el in elements:
        e = {k: v for k, v in el.items() if k in ("name", "from", "to", "rotation")}
        e["faces"] = {d: dict(f, texture=ref(f["texture"])) for d, f in el["faces"].items()}
        els.append(e)
    os.makedirs(os.path.join(HERE, "models"), exist_ok=True)
    with open(os.path.join(HERE, "models", slug + ".json"), "w") as f:
        rows = ",\n".join("    " + json.dumps(e, separators=(", ", ": ")) for e in els)
        f.write('{\n  "credit": "concept body, generated by concepts.py",\n'
                f'  "textures": {json.dumps(textures)},\n  "elements": [\n{rows}\n  ]\n}}\n')


def main():
    size, ss, cols = int(os.environ.get("SIZE", 224)), int(os.environ.get("SS", 3)), 5
    ensure_vanilla()
    shutil.rmtree(ASSETS, ignore_errors=True)
    font, k = Font(), 2
    label_h = 8 * k + 14
    rows = math.ceil(len(BODIES) / cols)
    W, H = cols * size, rows * (size + label_h)
    sheet = bytearray(bytes((43, 43, 46, 255)) * (W * H))
    shutil.rmtree(os.path.join(HERE, "models"), ignore_errors=True)
    built = [(name, slug, build()) for name, slug, build in BODIES]
    bad = [msg for name, _, elements in built for msg in check_texels(name, elements)]
    if bad:
        sys.exit("faces that scale their texture:\n  " + "\n  ".join(bad))
    for i, (name, slug, elements) in enumerate(built):
        export(slug, elements)
        px = render(elements, size, ss)
        ox, oy = (i % cols) * size, (i // cols) * (size + label_h)
        for y in range(size):
            for x in range(size):
                s = (y * size + x) * 4
                a = px[s + 3] / 255
                if a:
                    o = ((oy + y) * W + ox + x) * 4
                    for c in range(3):
                        sheet[o + c] = int(sheet[o + c] * (1 - a) + px[s + c] * a)
        tx = ox + (size - font.width(name) * k) // 2
        ty = oy + size + 2
        font.draw(sheet, W, name, tx + k, ty + k, k, (62, 62, 62))
        font.draw(sheet, W, name, tx, ty, k, (255, 255, 255))
        print("rendered", name, file=sys.stderr)
    shutil.rmtree(os.path.join(HERE, "textures"), ignore_errors=True)
    os.makedirs(os.path.join(HERE, "textures"), exist_ok=True)
    for name in sorted(GENERATED):
        shutil.copy(os.path.join(ASSETS, "textures/block/v", name + ".png"), os.path.join(HERE, "textures"))
    out = os.path.join(HERE, "farm-concepts.png")
    encode_rgba(out, W, H, sheet)
    print("wrote", out)


if __name__ == "__main__":
    main()
