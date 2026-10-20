"""Concept farm bodies (bed + planting, no case) on one labelled contact sheet.

Blocks map vanilla textures 1:1; plant crosses sample at 2x density (full sprite at half size).
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

encode_rgba = rb.encode_png
try:  # render_blocks ignores tRNS on grayscale PNGs (attached_pumpkin_stem); pngkit does not
    sys.path.insert(0, os.environ.get("TEXTUREKIT", os.path.expanduser("~/Dev/Tools/texturekit")))
    from pngkit import decode_rgba  # noqa: E402
except ImportError:
    decode_rgba = rb.decode_png

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

def box(frm, to, side, top=None, bottom=None, off=(0, 0), top_off=None, density=1, faces=None, **extra):
    """Cuboid with uv sized to the element (x density) so texels match the block grid."""
    (x0, y0, z0), (x1, y1, z1) = frm, to
    top = top or side
    bottom = bottom or top
    top_off = top_off if top_off is not None else (8 - (x1 - x0) * density / 2, 8 - (z1 - z0) * density / 2)
    d = density

    def side_uv(w):
        u0 = off[0] if off[0] is not None else 8 - w * d / 2
        return [u0, off[1], u0 + w * d, off[1] + (y1 - y0) * d]

    tu = [top_off[0], top_off[1], top_off[0] + (x1 - x0) * d, top_off[1] + (z1 - z0) * d]
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


def cross(t, cx, cz, y0, w, h, v_top=None, density=2):
    """Two axis-aligned planes, bottom-aligned on the sprite unless v_top is given."""
    uw, uh = w * density, h * density
    u0 = 8 - uw / 2
    v0 = v_top if v_top is not None else 16 - uh
    uv = [u0, v0, u0 + uw, v0 + uh]
    a = {"from": [cx - w / 2, y0, cz], "to": [cx + w / 2, y0 + h, cz],
         "faces": {"north": {"texture": t, "uv": uv}, "south": {"texture": t, "uv": uv}}}
    b = {"from": [cx, y0, cz - w / 2], "to": [cx, y0 + h, cz + w / 2],
         "faces": {"east": {"texture": t, "uv": uv}, "west": {"texture": t, "uv": uv}}}
    return [a, b]


def bed(top, side, side_off=(0, 0)):
    return box((3, 4, 3), (13, 6, 13), side, top, off=(3, side_off[1]), top_off=(3, 3))


QUAD = [(5.5, 5.5), (10.5, 5.5), (5.5, 10.5), (10.5, 10.5)]


# ---------------------------------------------------------------- bodies

def real(model):
    d = json.load(open(os.path.join(MOD, "models/block/forestry", model + ".json")))
    body = modtex(d["textures"]["body"].split(":", 1)[1])
    els = [e for e in d["elements"] if all(f["texture"] == "#body" for f in e["faces"].values())]
    for e in els:
        for f in e["faces"].values():
            f["texture"] = body
    return els


def log_x(z, y, d=2.5, x0=4, x1=12):
    """A log lying along x: grain runs the length, rings on the ends."""
    side, end = tex("oak_log"), tex("oak_log_top")
    L = x1 - x0
    run = {"texture": side, "uv": [6, 4, 6 + d, 4 + L], "rotation": 90}
    return {"from": [x0, y, z], "to": [x1, y + d, z + d],
            "faces": {"north": dict(run), "south": dict(run), "up": dict(run), "down": dict(run),
                      "east": {"texture": end, "uv": [0, 0, 16, 16]},
                      "west": {"texture": end, "uv": [0, 0, 16, 16]}}}


def band(frm, to):
    iron = {"texture": tex("rail"), "uv": [2, 1, 4, 3]}
    return {"from": list(frm), "to": list(to), "faces": {d: dict(iron) for d in rb.DIR_NORMAL}}


def log_pile():
    humus = modtex("block/forestry/humus")
    e = [bed(humus, humus)]
    for z in (4.25, 6.75, 9.25):
        e.append(log_x(z, 6))
    for z in (5.5, 8):
        e.append(log_x(z, 8.5))
    e.append(log_x(6.75, 11))
    t = 0.25
    profile = [  # (z0, z1, y0, y1): a stepped strap hugging the 3-2-1 pyramid
        (4.25 - t, 4.25, 6, 8.5), (11.75, 11.75 + t, 6, 8.5),
        (4.25 - t, 5.5, 8.5, 8.5 + t), (10.5, 11.75 + t, 8.5, 8.5 + t),
        (5.5 - t, 5.5, 8.5 + t, 11), (10.5, 10.5 + t, 8.5 + t, 11),
        (5.5 - t, 6.75, 11, 11 + t), (9.25, 10.5 + t, 11, 11 + t),
        (6.75 - t, 6.75, 11 + t, 13.5), (9.25, 9.25 + t, 11 + t, 13.5),
        (6.75 - t, 9.25 + t, 13.5, 13.5 + t),
    ]
    for x in (5, 10):
        for z0, z1, y0, y1 in profile:
            e.append(band((x, y0, z0), (x + 1, y1, z1)))
    return e


def gourd():
    e = [bed(tex("farmland_moist"), tex("dirt"))]
    e.append(box((3, 6, 8), (8, 10, 13), tex("pumpkin_side"), tex("pumpkin_top"), off=(None, 5)))
    e.append(box((8, 6, 3), (13, 10, 8), tex("melon_side"), tex("melon_top"), off=(None, 5)))
    e += cross(tex("attached_pumpkin_stem", STEM), 5.5, 5.5, 6, 4, 4)
    e += cross(tex("attached_melon_stem", STEM), 10.5, 10.5, 6, 4, 4)
    return e


def plantation():
    sand = tex("sand")
    e = [box((3, 4, 3), (7, 6, 13), sand, off=(3, 0), top_off=(3, 3)),
         box((9, 4, 3), (13, 6, 13), sand, off=(9, 0), top_off=(9, 3)),
         box((7, 4, 3), (9, 5, 13), sand, off=(7, 0), top_off=(7, 3)),
         box((7, 5, 3), (9, 5.75, 13), tex("water_still", WATER), translucent=0.55,
             faces=("up", "north", "south"))]
    cane = tex("sugar_cane", GRASS)
    e += cross(cane, 5, 5.5, 6, 3, 8, v_top=0)
    e += cross(cane, 5, 10.5, 6, 3, 6, v_top=4)
    stalk = tex("bamboo_stalk")
    for (x, z, h) in ((10, 4.5, 8), (11.5, 9.5, 6)):
        e.append(box((x, 6, z), (x + 2, 6 + h, z + 2), stalk, off=(0, 0), top_off=(13, 0)))
        e += cross(tex("bamboo_small_leaves"), x + 1, z + 1, 6 + h - 3, 4, 4, v_top=0)
    return e


def desert():
    sand = tex("sand")
    e = [bed(sand, sand)]
    e.append(box((5, 6, 5), (9, 13, 9), tex("cactus_side"), tex("cactus_top"), off=(None, 0)))
    e += cross(tex("cactus_flower"), 7, 7, 13, 4, 2, v_top=10)
    e.append(box((9, 6, 9), (12, 10, 12), tex("cactus_side"), tex("cactus_top"), off=(None, 3)))
    e += cross(tex("dead_bush"), 10.5, 5.5, 6, 4, 4)
    return e


def berry():
    e = [bed(tex("grass_block_top", GRASS), tex("grass_block_side"))]
    for (x, z), h in zip(QUAD, (5, 4, 4, 5)):
        e += cross(tex("sweet_berry_bush_stage3"), x, z, 6, 5, h)
    return e


def cocoa():
    log = tex("jungle_log")
    e = [bed(modtex("block/forestry/humus"), modtex("block/forestry/humus"))]
    e.append(box((6, 6, 6), (10, 14, 10), log, tex("jungle_log_top"), off=(None, 0)))
    pod, ptop = tex("cocoa_stage2"), tex("cocoa_stage2")
    for frm, to in (((7, 8, 4), (10, 12, 6)), ((10, 7, 7), (12, 11, 10)), ((4, 9, 6.5), (6, 12, 9.5))):
        b = box(frm, to, pod, ptop, density=2, off=(8, 4), top_off=(0, 0))
        e.append(b)
    return e


def cave_vines():
    moss = tex("moss_block")
    e = [bed(moss, moss), box((3, 13, 3), (13, 14, 13), moss, off=(3, 0), top_off=(3, 3))]
    for (x, z), h in zip(QUAD, (6, 4, 5, 7)):
        e += cross(tex("cave_vines_plant_lit"), x, z, 13 - h + 2, 4, h - 2, v_top=0)
        e += cross(tex("cave_vines_lit"), x, z, 13 - h, 4, 2, v_top=10)
    return e


def mushroom():
    e = [bed(tex("mycelium_top"), tex("mycelium_side"))]
    e.append(box((7, 6, 7), (9, 11, 9), tex("mushroom_stem"), off=(None, 4)))
    cap = tex("red_mushroom_block")
    e.append(box((4, 11, 4), (12, 13, 12), cap, off=(4, 0), top_off=(4, 4)))
    e.append(box((5, 13, 5), (11, 14, 11), cap, off=(5, 2), top_off=(5, 5)))
    e += cross(tex("brown_mushroom"), 4.5, 11, 6, 3, 3)
    e += cross(tex("brown_mushroom"), 11.5, 11.5, 6, 3, 3)
    return e


def wart_stalks():
    """nether_wart_stage2 with the merged base cut back to one stalk under each bulb."""
    out = os.path.join(ASSETS, "textures/block/v/nether_wart_stalks.png")
    w, h, px = decode_rgba(os.path.join(VAN, "block/nether_wart_stage2.png"))
    px = bytearray(px)
    for y in range(10, 16):
        for x in range(16):
            if x not in (3, 4, 12, 13):
                px[(y * w + x) * 4 + 3] = 0
    os.makedirs(os.path.dirname(out), exist_ok=True)
    encode_rgba(out, w, h, px)
    return "logistics:block/v/nether_wart_stalks"


def nether_wart():
    ss = tex("soul_sand")
    e = [bed(ss, ss)]
    wart = wart_stalks()
    for (x, z), h in zip(QUAD, (5, 4.5, 4.5, 5)):
        e += cross(wart, x, z, 6, h, h, v_top=0, density=16 / h)
    return e


def peat_bog():
    mud = tex("mud")
    e = [box((3, 4, 3), (13, 6, 6), mud, off=(3, 0), top_off=(3, 3)),
         box((3, 4, 10), (13, 6, 13), mud, off=(3, 0), top_off=(3, 10)),
         box((3, 4, 6), (5, 6, 10), mud, off=(3, 0), top_off=(3, 6)),
         box((10, 4, 6), (13, 6, 10), mud, off=(10, 0), top_off=(10, 6)),
         box((5, 4, 6), (10, 5, 10), mud, off=(5, 0), top_off=(5, 6)),
         box((5, 5, 6), (10, 5.75, 10), tex("water_still", WATER), translucent=0.55, faces=("up",))]
    peat = tex("packed_mud")
    e.append(box((9, 6, 10), (12, 8, 12), peat, off=(2, 4)))
    e.append(box((10, 8, 10.5), (12, 9.5, 12), peat, off=(6, 2)))
    e.append(box((4, 6, 3.5), (6.5, 7.5, 5.5), peat, off=(9, 9)))
    return e


def aquatic():
    sand = tex("sand")
    e = [bed(sand, sand)]
    for x, z, h in ((5, 5.5, 7), (10.5, 6, 6), (8, 10.5, 7)):
        e += cross(tex("kelp_plant"), x, z, 6, 4, h, v_top=0)
    e += cross(tex("seagrass"), 5, 10.5, 6, 4, 4)
    e += cross(tex("seagrass"), 11, 10.5, 6, 3, 3)
    for x, z in ((8.5, 5), (9.5, 6.5)):
        e.append(box((x, 6, z), (x + 1.5, 8, z + 1.5), tex("sea_pickle"), off=(4, 8), top_off=(5, 2)))
    e.append(box((3, 6, 3), (13, 13.5, 13), tex("water_still", WATER), translucent=0.4,
                 faces=("up", "north", "south", "east", "west"), off=(3, 0), top_off=(3, 3)))
    return e


def chorus():
    es, plant, flower = tex("end_stone"), tex("chorus_plant"), tex("chorus_flower")
    e = [bed(es, es)]
    e.append(box((7, 6, 7), (9, 11, 9), plant, off=(None, 4)))
    e.append(box((6, 11, 6), (10, 14, 10), flower, off=(None, 6)))
    e.append(box((4, 8, 7), (7, 10, 9), plant, off=(None, 2)))
    e.append(box((4, 10, 7), (6, 12, 9), plant, off=(None, 8)))
    e.append(box((3.5, 12, 6.5), (6.5, 14, 9.5), flower, off=(None, 6)))
    e.append(box((9, 7, 7), (12, 9, 9), plant, off=(None, 6)))
    e.append(box((10, 9, 7), (12, 11, 9), plant, off=(None, 10)))
    e.append(box((10, 11, 7), (12, 12.5, 9), tex("chorus_flower_dead"), off=(None, 6)))
    return e


def vines():
    e = [bed(tex("warped_nylium"), tex("warped_nylium_side"))]
    for (x, z), h in zip(((5, 5.5), (10.5, 6), (7.5, 10.5)), (8, 6, 7)):
        e += cross(tex("twisting_vines_plant"), x, z, 6, 4, h - 2, v_top=0)
        e += cross(tex("twisting_vines"), x, z, 6 + h - 2, 4, 2, v_top=4)
    e += cross(tex("warped_roots"), 11, 11, 6, 3, 3)
    return e


def flowers():
    e = [bed(tex("grass_block_top", GRASS), tex("grass_block_side"))]
    names = ("poppy", "dandelion", "cornflower", "allium", "oxeye_daisy", "blue_orchid")
    spots = ((4.5, 5), (8, 4.5), (11.5, 5.5), (5, 9.5), (8.5, 8.5), (11, 11))
    for n, (x, z) in zip(names, spots):
        e += cross(tex(n), x, z, 6, 4, 6)
    return e


def moss_azalea():
    moss = tex("moss_block")
    e = [bed(moss, moss)]
    e += cross(tex("azalea_plant"), 6.5, 6.5, 6, 4, 3)
    e.append(box((3.5, 9, 3.5), (9.5, 13, 9.5), tex("flowering_azalea_side"), tex("flowering_azalea_top"),
                 density=2, off=(2, 0), top_off=(2, 2)))
    e += cross(tex("azalea_plant"), 10.5, 10.5, 6, 3, 2)
    e.append(box((8.5, 8, 8.5), (12.5, 11, 12.5), tex("azalea_side"), tex("azalea_top"),
                 density=2, off=(4, 0), top_off=(4, 4)))
    e += cross(tex("small_dripleaf_side"), 11, 5, 6, 3, 3, v_top=6)
    return e


def fungus():
    c, w = tex("crimson_nylium"), tex("warped_nylium")
    e = [box((3, 4, 3), (8, 6, 13), tex("crimson_nylium_side"), c, off=(3, 0), top_off=(3, 3)),
         box((8, 4, 3), (13, 6, 13), tex("warped_nylium_side"), w, off=(8, 0), top_off=(8, 3))]
    e += cross(tex("crimson_fungus"), 5.5, 8, 6, 5, 6)
    e += cross(tex("warped_fungus"), 10.5, 8, 6, 5, 6)
    return e


def apple_tex():
    out = os.path.join(ASSETS, "textures/item/v/apple.png")
    os.makedirs(os.path.dirname(out), exist_ok=True)
    shutil.copy(os.path.join(VAN, "item/apple.png"), out)
    return "logistics:item/v/apple"


APPLE_RED = [(84, 9, 14), (156, 16, 23), (180, 19, 30), (221, 23, 37), (255, 28, 43)]
APPLE_SHINE, APPLE_STEM = (255, 150, 157), [(84, 36, 9), (117, 40, 2)]


def apple_block():
    """Tileable 16x16 'pile of apples' in the vanilla apple palette, lit from the top-left."""
    out = os.path.join(ASSETS, "textures/block/v/apple_pile.png")
    rows = [(2, (2, 6, 10, 14)), (6, (0, 4, 8, 12)), (10, (2, 6, 10, 14)), (14, (0, 4, 8, 12))]
    jitter = [(0.2, -0.2), (-0.2, 0.1), (0.1, 0.2), (-0.1, -0.2)] * 4
    centers = [(x + jitter[i][0], y + jitter[i][1], i % 3 == 0)
               for i, (y, xs_) in enumerate((y, x) for y, xs in rows for x in xs)
               for x, y in [(xs_, y)]]
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
    return "logistics:block/v/apple_pile"


def orchard_basket():
    e = [bed(tex("grass_block_top", GRASS), tex("grass_block_side"))]
    wicker = tex("stripped_oak_log")
    e.append(box((4, 6, 4), (12, 7, 12), wicker, off=(4, 0), top_off=(4, 4)))
    for frm, to in (((4, 7, 4), (12, 9.5, 5)), ((4, 7, 11), (12, 9.5, 12)),
                    ((4, 7, 5), (5, 9.5, 11)), ((11, 7, 5), (12, 9.5, 11))):
        e.append(box(frm, to, wicker, off=(2, 4)))
    e.append(box((4, 9.5, 7.5), (5, 12.5, 8.5), wicker, off=(4, 2)))
    e.append(box((11, 9.5, 7.5), (12, 12.5, 8.5), wicker, off=(4, 2)))
    e.append(box((4, 12.5, 7.5), (12, 13.25, 8.5), wicker, off=(2, 6)))
    pile = apple_block()
    e.append(box((5, 7, 5), (11, 9.25, 11), pile, off=(5, 4), top_off=(5, 5)))
    e.append(box((6, 9.25, 6), (10, 10, 10), pile, off=(4, 0), top_off=(6, 6)))
    return e


def orchard():
    e = [bed(tex("grass_block_top", GRASS), tex("grass_block_side"))]
    e += [el for el in real("arboretum") if el.get("name") != "bed"]
    apple = apple_tex()
    spots = [((5.5, 10, 3.5)), (9.5, 9.5, 3.5), (6.5, 10.5, 11.5), (10, 9.5, 11.5),
             (3.5, 10, 6), (3.5, 9.5, 9.5), (11.5, 10.5, 5.5), (11.5, 9.5, 9),
             (7, 12.5, 8), (9, 12.5, 6), (5.8, 12.2, 10.6)]
    for x, y, z in spots:
        e.append(box((x, y, z), (x + 1, y + 1, z + 1), apple, density=2, off=(5, 7), top_off=(5, 7)))
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
    ("Cave Vine Farm", "cave_vine_farm", cave_vines),
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


GENERATED = ("apple_pile", "nether_wart_stalks")


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
    for i, (name, slug, build) in enumerate(BODIES):
        elements = build()
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
    os.makedirs(os.path.join(HERE, "textures"), exist_ok=True)
    for name in GENERATED:
        shutil.copy(os.path.join(ASSETS, "textures/block/v", name + ".png"), os.path.join(HERE, "textures"))
    out = os.path.join(HERE, "farm-concepts.png")
    encode_rgba(out, W, H, sheet)
    print("wrote", out)


if __name__ == "__main__":
    main()
