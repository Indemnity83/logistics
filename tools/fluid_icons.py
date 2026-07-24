#!/usr/bin/env python3
"""Generate wiki fluid assets from the mod's fluid textures:

  * a static swatch  `Grid <Fluid>.png`      — the fluid's icon in recipe lists (from the still strip)
  * an animated hero `Fluid <Fluid>.gif`     — the infobox image on each fluid page
  * a bucket icon    `Bucket of <Fluid>.png` — the filled bucket item (from the bucket item texture)

Two kinds of fluid are handled:

  * "baked" fluids (BAKED) — the mod ships its own `<name>_still.png` strip with colour + alpha
    already applied (render tint is neutral), so the still frames are used directly.
  * "tinted" fluids (TINTED) — registered with `FluidDef.tinted(name, rgb)`, which draws the
    vanilla `minecraft:block/water_still` animation multiplied by a flat `0xRRGGBB` tint (opaque).
    We reproduce that: multiply the white water frames by the tint and force full alpha. These need
    a Minecraft `assets/minecraft` dir (`--vanilla`), since the water sprite is vanilla, not the mod's.

The swatch is forced opaque so it reads as a solid slot icon; the gif keeps the animation
timing/order from the source texture's .mcmeta. Buckets always come from the mod's item texture.

Usage:
  python tools/fluid_icons.py                                    # baked fluids, default 26.2 assets
  python tools/fluid_icons.py --vanilla <assets/minecraft>       # also render the tinted fluids
  python tools/fluid_icons.py --assets <assets/logistics> --vanilla <assets/minecraft> --out wiki/media

Get an `assets/minecraft` by unzipping a client jar, e.g.
  unzip ~/.gradle/caches/fabric-loom/26.2/minecraft-client.jar 'assets/minecraft/*' -d <dir>
"""
import argparse
import json
import os
import sys

from PIL import Image

# "baked" fluids: registry name -> wiki display name (own colored still strip under the mod assets)
FLUIDS = {
    "crude_oil": "Crude Oil",
    "liquid_redstone": "Liquid Redstone",
    "liquid_ender": "Liquid Ender",
    "liquid_glowstone": "Liquid Glowstone",
    "liquid_biomass": "Liquid Biomass",
}

# "tinted" fluids: registry name -> (wiki display name, 0xRRGGBB tint) — drawn from vanilla water.
# Mirror FluidDef.tinted(...) in LogisticsCore.java.
TINTED = {
    "bio_fuel": ("Bio Fuel", 0xFFFC5C),
    "fuel_oil": ("Fuel Oil", 0xFE8C01),
}


def frames(strip):
    """Split a vertical animation strip into its square frames."""
    w = strip.width
    return [strip.crop((0, i * w, w, (i + 1) * w)) for i in range(strip.height // w)]


def tint_frames(fr, rgb):
    """Multiply each (white) water frame by the tint and force full alpha, as the game renders it."""
    tr, tg, tb = (rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255
    out = []
    for f in fr:
        im = f.convert("RGBA")
        px = im.load()
        for y in range(im.height):
            for x in range(im.width):
                r, g, b, _ = px[x, y]
                px[x, y] = (r * tr // 255, g * tg // 255, b * tb // 255, 255)
        out.append(im)
    return out


def emit(disp, fr, ft, order, bucket_src, args):
    """Write the swatch + gif + bucket for one fluid; return a one-line report."""
    # static swatch: first frame, forced opaque, nearest-upscaled
    swatch = fr[0].copy()
    px = swatch.load()
    for y in range(swatch.height):
        for x in range(swatch.width):
            r, g, b, _ = px[x, y]
            px[x, y] = (r, g, b, 255)
    swatch = swatch.resize((args.size, args.size), Image.NEAREST)

    # animated hero: honour the .mcmeta frametime and (optional) explicit frame order
    gif_frames, durations = [], []
    for idx in order:
        bg = Image.new("RGBA", fr[idx].size, (0, 0, 0, 0))
        g = Image.alpha_composite(bg, fr[idx]).convert("RGB")
        gif_frames.append(g.resize((args.gif_size, args.gif_size), Image.NEAREST))
        durations.append(ft * 50)

    # bucket item icon: the filled-bucket sprite, upscaled flat
    bucket = Image.open(bucket_src).convert("RGBA").resize((args.size, args.size), Image.NEAREST)

    if not args.dry_run:
        swatch.save(os.path.join(args.out, f"Grid {disp}.png"))
        bucket.save(os.path.join(args.out, f"Bucket of {disp}.png"))
        gif_frames[0].save(os.path.join(args.out, f"Fluid {disp}.gif"), save_all=True,
                           append_images=gif_frames[1:], duration=durations, loop=0, optimize=True)
    return f"  Grid {disp}.png (swatch)  +  Fluid {disp}.gif ({len(gif_frames)}f)  +  Bucket of {disp}.png"


def load_meta(path):
    ft, order = 1, None
    if os.path.exists(path):
        anim = json.load(open(path)).get("animation", {})
        ft = anim.get("frametime", 1)
        order = anim.get("frames")
    return ft, order


def main():
    repo = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    default_assets = os.path.normpath(os.path.join(
        repo, "..", "logistics-mc-26.2", "common", "src", "main", "resources", "assets", "logistics"))
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--assets", default=default_assets, help="mod assets/logistics dir")
    ap.add_argument("--vanilla", default=None,
                    help="a Minecraft assets/minecraft dir (needed to render the tinted fluids)")
    ap.add_argument("--out", default=os.path.join(repo, "wiki", "media"))
    ap.add_argument("--size", type=int, default=256, help="swatch px (nearest upscale)")
    ap.add_argument("--gif-size", type=int, default=96, help="gif px (nearest upscale)")
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()

    fdir = os.path.join(args.assets, "textures", "block", "core", "fluid")
    idir = os.path.join(args.assets, "textures", "item", "core")
    if not os.path.isdir(fdir):
        sys.exit(f"fluid textures not found: {fdir}\nPass --assets <path to assets/logistics>.")
    os.makedirs(args.out, exist_ok=True)

    n = 0
    # baked fluids — own coloured still strip
    for name, disp in FLUIDS.items():
        fr = frames(Image.open(os.path.join(fdir, f"{name}_still.png")).convert("RGBA"))
        ft, order = load_meta(os.path.join(fdir, f"{name}_still.png.mcmeta"))
        print(emit(disp, fr, ft, order or list(range(len(fr))),
                   os.path.join(idir, f"{name}.png"), args))
        n += 1

    # tinted fluids — vanilla water multiplied by the tint
    if TINTED:
        if not args.vanilla:
            print(f"\n  skipping {len(TINTED)} tinted fluid(s) ({', '.join(d for d, _ in TINTED.values())}) "
                  f"— pass --vanilla <assets/minecraft> to render them")
        else:
            water = os.path.join(args.vanilla, "textures", "block", "water_still.png")
            if not os.path.isfile(water):
                sys.exit(f"water sprite not found: {water}\nPass --vanilla <path to assets/minecraft>.")
            wfr = frames(Image.open(water).convert("RGBA"))
            ft, order = load_meta(water + ".mcmeta")
            order = order or list(range(len(wfr)))
            for name, (disp, rgb) in TINTED.items():
                print(emit(disp, tint_frames(wfr, rgb), ft, order,
                           os.path.join(idir, f"{name}.png"), args))
                n += 1

    print(f"\n{'would write' if args.dry_run else 'wrote'} {n} fluid swatches + gifs -> {args.out}")


if __name__ == "__main__":
    main()
