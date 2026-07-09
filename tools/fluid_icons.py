#!/usr/bin/env python3
"""Generate wiki fluid assets from the mod's fluid textures:

  * a static swatch  `Grid <Fluid>.png`      — the fluid's icon in recipe lists (from the still strip)
  * an animated hero `Fluid <Fluid>.gif`     — the infobox image on each fluid page
  * a bucket icon    `Bucket of <Fluid>.png` — the filled bucket item (from the bucket item texture)

The mod's custom fluids bake their colour + alpha into the texture (render tint is neutral),
so the still frames are already the right colour. The swatch is forced opaque so it reads as a
solid slot icon; the gif keeps the animation timing/order from the texture's .mcmeta.

Usage:
  python tools/fluid_icons.py                     # default 26.2 assets -> wiki/media
  python tools/fluid_icons.py --assets <assets/logistics> --out wiki/media
"""
import argparse
import json
import os
import sys

from PIL import Image

# fluid registry name -> wiki display name (page title)
FLUIDS = {
    "crude_oil": "Crude Oil",
    "liquid_redstone": "Liquid Redstone",
    "liquid_ender": "Liquid Ender",
    "liquid_glowstone": "Liquid Glowstone",
    "liquid_biomass": "Liquid Biomass",
}


def frames(strip):
    """Split a vertical animation strip into its square frames."""
    w = strip.width
    return [strip.crop((0, i * w, w, (i + 1) * w)) for i in range(strip.height // w)]


def main():
    repo = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    default_assets = os.path.normpath(os.path.join(
        repo, "..", "logistics-mc-26.2", "common", "src", "main", "resources", "assets", "logistics"))
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--assets", default=default_assets, help="mod assets/logistics dir")
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

    for name, disp in FLUIDS.items():
        strip = Image.open(os.path.join(fdir, f"{name}_still.png")).convert("RGBA")
        fr = frames(strip)

        # static swatch: first frame, forced opaque, nearest-upscaled
        swatch = fr[0].copy()
        px = swatch.load()
        for y in range(swatch.height):
            for x in range(swatch.width):
                r, g, b, _ = px[x, y]
                px[x, y] = (r, g, b, 255)
        swatch = swatch.resize((args.size, args.size), Image.NEAREST)
        swatch_path = os.path.join(args.out, f"Grid {disp}.png")

        # animated hero: honour the .mcmeta frametime and (optional) explicit frame order
        meta = json.load(open(os.path.join(fdir, f"{name}_still.png.mcmeta")))["animation"]
        ft = meta.get("frametime", 1)
        order = meta.get("frames", list(range(len(fr))))
        gif_frames, durations = [], []
        for idx in order:
            bg = Image.new("RGBA", fr[idx].size, (0, 0, 0, 0))
            g = Image.alpha_composite(bg, fr[idx]).convert("RGB")
            gif_frames.append(g.resize((args.gif_size, args.gif_size), Image.NEAREST))
            durations.append(ft * 50)
        gif_path = os.path.join(args.out, f"Fluid {disp}.gif")

        # bucket item icon: the filled-bucket sprite, upscaled flat
        bucket_src = os.path.join(idir, f"{name}.png")
        bucket = Image.open(bucket_src).convert("RGBA").resize((args.size, args.size), Image.NEAREST)
        bucket_path = os.path.join(args.out, f"Bucket of {disp}.png")

        if not args.dry_run:
            swatch.save(swatch_path)
            bucket.save(bucket_path)
            gif_frames[0].save(gif_path, save_all=True, append_images=gif_frames[1:],
                               duration=durations, loop=0, optimize=True)
        print(f"  Grid {disp}.png (swatch)  +  Fluid {disp}.gif ({len(gif_frames)}f)  +  Bucket of {disp}.png")

    print(f"\n{'would write' if args.dry_run else 'wrote'} {len(FLUIDS)} fluid swatches + gifs -> {args.out}")


if __name__ == "__main__":
    main()
