#!/usr/bin/env python3
"""Generate <material>_valve.png item textures for the fabricated valves.

Each valve is a little vacuum-tube icon: a material-colored filament inside a
glass envelope. Build steps per material:
  1. pick the material color -- either a hand-set override, or a saturation- and
     brightness-tuned color extracted from the material's recipe ingredient
     texture (vanilla item/block from the client jar, or the mod's own texture);
  2. tint the grayscale `valve-element` filament with that color;
  3. for the crystalline materials, add a soft gem glint on the filament;
  4. composite the shared `valve-top` glass envelope over the result.

Source art lives in tools/valve-art/. Vanilla donor textures are read from the
Minecraft client jar at generation time and never committed. Pure stdlib via the
vendored tools/pngkit.py -- no venv, no Pillow.

Usage:
  python3 tools/generate_valve_textures.py [--out DIR] [--mc-jar JAR]
                                           [--element PNG] [--top PNG]
"""
import argparse
import colorsys
import os
import sys
import tempfile
import zipfile

# pngkit is vendored alongside this script (tools/pngkit.py) so the tool is
# self-contained; when run as a script its directory is on sys.path.
from pngkit import decode_rgba, encode_rgba, composite_over

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEX = os.path.join(REPO, "common/src/main/resources/assets/logistics/textures/item")
DEFAULT_OUT = os.path.join(TEX, "core")
ART = os.path.join(REPO, "tools/valve-art")
W = 16  # texture size

# material -> donor = the valve's recipe ingredient.
#   ("vanilla", <path within client jar>) or ("mod", <path under textures/item>)
DONORS = {
    "tin": ("mod", "core/tin_ingot.png"),
    "copper": ("vanilla", "assets/minecraft/textures/item/copper_ingot.png"),
    "rubber": ("mod", "power/rubber.png"),
    "bronze": ("mod", "core/bronze_ingot.png"),
    "iron": ("vanilla", "assets/minecraft/textures/item/iron_ingot.png"),
    "gold": ("vanilla", "assets/minecraft/textures/item/gold_ingot.png"),
    "lapis": ("vanilla", "assets/minecraft/textures/item/lapis_lazuli.png"),
    "apatite": ("mod", "core/apatite.png"),
    "obsidian": ("vanilla", "assets/minecraft/textures/block/obsidian.png"),
    "amethyst": ("vanilla", "assets/minecraft/textures/item/amethyst_shard.png"),
    "emerald": ("vanilla", "assets/minecraft/textures/item/emerald.png"),
    "blazing": ("vanilla", "assets/minecraft/textures/item/blaze_rod.png"),
    "diamond": ("vanilla", "assets/minecraft/textures/item/diamond.png"),
    "echo": ("vanilla", "assets/minecraft/textures/item/echo_shard.png"),
    "netherite": ("vanilla", "assets/minecraft/textures/item/netherite_ingot.png"),
}

# Materials with no distinctive hue (metals/near-blacks) get hand-set colors so
# they stay distinguishable at item scale; copper/gold/blazing are pinned to
# their intended bright tones (module-connector copper, vanilla gold, ember red).
OVERRIDE = {
    "tin": "#93a7bd", "iron": "#cbcbcb", "rubber": "#2a2623",
    "obsidian": "#3a2168", "netherite": "#5f4038",
    "copper": "#f5895a", "gold": "#f7cb35",
    # split the teal twins: apatite deeper green, diamond light icy blue
    "apatite": "#1a936c", "diamond": "#46c8e6",
}

# Materials tinted with a vertical (top, bottom) gradient instead of a flat color.
# Blazing grades from red tips down to a hot yellow base, like flame.
GRADIENT = {"blazing": ("#de3b12", "#f7c000")}

# Extraction tuning for the remaining (hue-bearing) materials.
SAT, VFLOOR = 1.30, 0.18

# Crystalline materials get a soft glint on the filament. Each spot is a bright
# center pixel plus a 1px fade ring, tinted toward `glint` (white by default;
# amethyst glints pink for the shard's purple/pink two-tone). Echo gets a second.
GLINT_GEMS = {"apatite", "emerald", "diamond", "echo", "amethyst"}
GLINT_TARGET = {"amethyst": (255, 150, 222)}
GLINT_SPOTS = {
    "_default": [((7, 7), [(6, 7), (7, 6), (7, 8)])],
    "echo": [((7, 7), [(6, 7), (7, 6), (7, 8)]),
             ((7, 12), [(6, 12), (7, 11), (7, 13)])],
    "amethyst": [((7, 7), [(6, 7), (7, 6)]),        # 1px-taller core
                 ((7, 8), [(6, 8), (7, 9)])],
    "diamond": [((7, 7), [(6, 7), (7, 6)]),         # bigger, whiter -> reads as reflective
                ((7, 8), [(6, 8), (7, 9)])],
}
CMIX, RMIX = 0.65, 0.45  # center brightness, ring fade
GLINT_CMIX = {"diamond": 0.85}  # whiter center for a hard reflective sparkle


def representative_color(px):
    """Saturation-weighted average of a donor's opaque pixels (favors the hue)."""
    tw = tr = tg = tb = 0.0
    for i in range(0, len(px), 4):
        if px[i + 3] == 0:
            continue
        r, g, b = px[i], px[i + 1], px[i + 2]
        mx, mn = max(r, g, b), min(r, g, b)
        w = ((mx - mn) / mx if mx else 0.0) + 0.15
        tw += w
        tr += r * w
        tg += g * w
        tb += b * w
    return (tr / tw, tg / tw, tb / tw)


def enhance(color):
    """Boost chroma and lift a brightness floor so hues read and darks stay visible."""
    r, g, b = [c / 255 for c in color]
    h, s, v = colorsys.rgb_to_hsv(r, g, b)
    s = min(1.0, s * SAT)
    v = VFLOOR + v * (1.0 - VFLOOR)
    r, g, b = colorsys.hsv_to_rgb(h, s, v)
    return (r * 255, g * 255, b * 255)


def hex_to_rgb(h):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16))


def mix(a, b, t):
    return tuple(round(a[k] * (1 - t) + b[k] * t) for k in range(3))


def tint(element, color):
    """Multiply-tint the grayscale element by `color`, preserving its shading."""
    cr, cg, cb = color
    out = bytearray(element)
    for i in range(0, len(element), 4):
        if element[i + 3] == 0:
            continue
        luma = element[i] * 0.2126 + element[i + 1] * 0.7152 + element[i + 2] * 0.0722
        f = luma / 255.0
        out[i] = min(255, round(cr * f))
        out[i + 1] = min(255, round(cg * f))
        out[i + 2] = min(255, round(cb * f))
    return out


def tint_gradient(element, top_color, bottom_color):
    """Multiply-tint with a vertical gradient over the filament's opaque rows."""
    ys = [(i // 4) // W for i in range(0, len(element), 4) if element[i + 3] > 0]
    ymin, ymax = min(ys), max(ys)
    span = (ymax - ymin) or 1
    out = bytearray(element)
    for i in range(0, len(element), 4):
        if element[i + 3] == 0:
            continue
        t = ((i // 4) // W - ymin) / span  # 0 at top, 1 at bottom
        cr = top_color[0] + (bottom_color[0] - top_color[0]) * t
        cg = top_color[1] + (bottom_color[1] - top_color[1]) * t
        cb = top_color[2] + (bottom_color[2] - top_color[2]) * t
        luma = element[i] * 0.2126 + element[i + 1] * 0.7152 + element[i + 2] * 0.0722
        f = luma / 255.0
        out[i] = min(255, round(cr * f))
        out[i + 1] = min(255, round(cg * f))
        out[i + 2] = min(255, round(cb * f))
    return out


def add_glint(buf, base_color, spots, target, cmix=CMIX):
    """Paint soft gem glints: bright gem-tinted center + 1px fade ring."""
    out = bytearray(buf)
    center = mix(base_color, target, cmix)
    for (cx, cy), ring in spots:
        i = (cy * W + cx) * 4
        if out[i + 3] == 0:
            out[i + 3] = 255
        out[i], out[i + 1], out[i + 2] = center
        for x, y in ring:
            j = (y * W + x) * 4
            if out[j + 3] == 0:
                continue
            out[j], out[j + 1], out[j + 2] = mix((out[j], out[j + 1], out[j + 2]), center, RMIX)
    return out


def read_mc_version():
    with open(os.path.join(REPO, "gradle.properties")) as f:
        for line in f:
            if line.strip().startswith("minecraft_version"):
                return line.split("=", 1)[1].strip()
    return None


def main():
    version = read_mc_version()
    default_jar = os.path.expanduser(
        f"~/.gradle/caches/fabric-loom/{version}/minecraft-client.jar")
    ap = argparse.ArgumentParser(description="Generate fabricated-valve item textures.")
    ap.add_argument("--out", default=DEFAULT_OUT, help="output directory")
    ap.add_argument("--element", default=os.path.join(ART, "valve-element.png"),
                    help="grayscale filament (tinted per material)")
    ap.add_argument("--top", default=os.path.join(ART, "valve-top.png"),
                    help="glass envelope overlay")
    ap.add_argument("--mc-jar", default=default_jar,
                    help="Minecraft client jar for vanilla donor textures")
    args = ap.parse_args()

    ew, eh, element = decode_rgba(args.element)
    tw, th, top = decode_rgba(args.top)
    if (ew, eh) != (tw, th) or (ew, eh) != (W, W):
        sys.exit(f"expected {W}x{W} art; got element {ew}x{eh}, top {tw}x{th}")
    if not os.path.exists(args.mc_jar):
        sys.exit(f"MC client jar not found (build once, or pass --mc-jar): {args.mc_jar}")

    os.makedirs(args.out, exist_ok=True)
    with zipfile.ZipFile(args.mc_jar) as jar, tempfile.TemporaryDirectory() as tmp:
        for material, (root, path) in DONORS.items():
            if material in GRADIENT:
                color = None
                note = f"gradient {GRADIENT[material][0]}->{GRADIENT[material][1]}"
            elif material in OVERRIDE:
                color = hex_to_rgb(OVERRIDE[material])
                note = f"override {OVERRIDE[material]}"
            else:
                if root == "vanilla":
                    donor = os.path.join(tmp, os.path.basename(path))
                    with open(donor, "wb") as f:
                        f.write(jar.read(path))
                else:
                    donor = os.path.join(TEX, path)
                _, _, dpx = decode_rgba(donor)
                color = enhance(representative_color(dpx))
                r, g, b = (min(255, round(c)) for c in color)
                note = f"{root} #{r:02x}{g:02x}{b:02x}"

            if material in GRADIENT:
                t, b = (hex_to_rgb(c) for c in GRADIENT[material])
                px = tint_gradient(element, t, b)
            else:
                px = tint(element, color)
            if material in GLINT_GEMS:
                spots = GLINT_SPOTS.get(material, GLINT_SPOTS["_default"])
                px = add_glint(px, color, spots, GLINT_TARGET.get(material, (255, 255, 255)),
                               GLINT_CMIX.get(material, CMIX))
            final = composite_over(px, top)
            encode_rgba(os.path.join(args.out, f"{material}_valve.png"), ew, eh, final)
            glint = " +glint" if material in GLINT_GEMS else ""
            print(f"wrote {material}_valve.png ({note}{glint})", file=sys.stderr)


if __name__ == "__main__":
    main()
