#!/usr/bin/env python3
"""
Generate wiki icons for the VANILLA Minecraft ingredients our recipes reference.

Cross-wiki image embedding isn't possible on Fandom, so the vanilla item/block icons
(Glass, Redstone, Iron Ingot, Stone, ...) must be hosted on our own wiki. This renders
just the ingredients the pages actually reference (not all of Minecraft) from an extracted
vanilla `assets/minecraft` folder, reusing the existing tools: flat items go through the
nearest-neighbor upscaler path, blocks through the model renderer. Output lands in
wiki/media as `Grid <Name>.png`, matching how the pages reference them.

Source assets: an unzipped client jar's `assets/minecraft`. A complete one already exists at
  ../logistics-mc-1.21.11/scripts/1/assets/minecraft
(or unzip ~/.gradle/caches/fabric-loom/<ver>/minecraft-client.jar).

Usage:
  python tools/vanilla_icons.py [--assets <assets/minecraft>] [--size 256] [--ss 3] [--dry-run]
"""

import argparse
import importlib.util
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))


def _load(mod):
    spec = importlib.util.spec_from_file_location(mod, os.path.join(HERE, mod + ".py"))
    m = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(m)
    return m


rb = _load("render_blocks")
up = _load("upscale_icons")

# Page name -> vanilla id, where the page's wording differs from the vanilla lang display name.
ALIAS = {
    "Redstone": "redstone",            # lang: "Redstone Dust"
    "Redstone Block": "redstone_block",  # lang: "Block of Redstone"
    "Quartz": "quartz",                # lang: "Nether Quartz"
    "Planks": "oak_planks",            # generic #planks -> oak as representative
    "Slime Ball": "slime_ball",        # lang spelling varies ("Slimeball")
    "Wood Pulp": None,                 # mod item, not vanilla (skip)
}
# Names we deliberately skip (none currently).
SKIP = set()

# Vanilla potions draw a tinted liquid overlay under the glass bottle; the water bottle
# uses the water-blue potion tint.
WATER_BOTTLE_TINT = (0x38, 0x5D, 0xC6)


def reverse_lang(assets):
    lang = json.load(open(os.path.join(assets, "lang", "en_us.json"), encoding="utf-8"))
    rev = {}
    for key, val in lang.items():
        if key.startswith("item.minecraft.") or key.startswith("block.minecraft."):
            rev.setdefault(val, key.split(".", 2)[2])  # first wins (item.* iterated before block.* by name only loosely)
    return rev


def is_flat_item(vid, assets):
    """True if the item renders as a flat sprite (item/generated|handheld), not a block."""
    mp = os.path.join(assets, "models", "item", vid + ".json")
    if not os.path.isfile(mp):
        return False
    m = json.load(open(mp, encoding="utf-8"))
    parent = m.get("parent", "")
    return parent.endswith("generated") or parent.endswith("handheld") \
        or any(k.startswith("layer") for k in m.get("textures", {}))


def flat_textures(vid, assets):
    mp = os.path.join(assets, "models", "item", vid + ".json")
    if os.path.isfile(mp):
        texs = json.load(open(mp, encoding="utf-8")).get("textures", {})
        layers = [texs[k] for k in sorted(texs) if k.startswith("layer")]
        paths = [os.path.join(assets, "textures", t.split(":")[-1] + ".png") for t in layers]
        paths = [p for p in paths if os.path.isfile(p)]
        if paths:
            return paths
    p = os.path.join(assets, "textures", "item", vid + ".png")
    return [p] if os.path.isfile(p) else []


def render_water_bottle(assets, out, size):
    """Composite the tinted potion liquid (layer0) under the glass bottle (layer1)."""
    liq_p = os.path.join(assets, "textures", "item", "potion_overlay.png")
    bottle_p = os.path.join(assets, "textures", "item", "potion.png")
    w, h, liquid = rb.decode_png(liq_p); w, h, liquid = up.first_frame(w, h, liquid, liq_p)
    r, g, b = WATER_BOTTLE_TINT
    liquid = bytearray(liquid)
    for i in range(0, len(liquid), 4):
        liquid[i] = liquid[i] * r // 255; liquid[i + 1] = liquid[i + 1] * g // 255
        liquid[i + 2] = liquid[i + 2] * b // 255
    _, _, bottle = rb.decode_png(bottle_p); _, _, bottle = up.first_frame(w, h, bottle, bottle_p)
    comp = up.composite_over(liquid, bottle)  # glass bottle over the tinted liquid
    rb.encode_png(os.path.join(out, "Grid Water Bottle.png"), size, size,
                  up.nearest_resize(w, h, comp, size, size))


def referenced_missing(wiki_dir, media_dir):
    have = {os.path.basename(p)[len("Grid "):-4]
            for p in os.listdir(media_dir) if p.startswith("Grid ") and p.endswith(".png")}
    return sorted(n for n in rb.referenced_names(wiki_dir) if n not in have)


def main():
    repo = os.path.dirname(HERE)
    ap = argparse.ArgumentParser(description="Render vanilla ingredient icons the wiki references.")
    ap.add_argument("--assets", default=os.path.normpath(os.path.join(
        repo, "..", "logistics-mc-1.21.11", "scripts", "1", "assets", "minecraft")))
    ap.add_argument("--out", default=os.path.join(repo, "wiki", "media"))
    ap.add_argument("--wiki-dir", default=os.path.join(repo, "wiki"))
    ap.add_argument("--size", type=int, default=256)
    ap.add_argument("--ss", type=int, default=3)
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()
    if not os.path.isdir(args.assets):
        sys.exit(f"vanilla assets not found: {args.assets}\nUnzip a client jar's assets/minecraft and pass --assets.")

    rev = reverse_lang(args.assets)
    targets = referenced_missing(args.wiki_dir, args.out)
    blocks = flats = skipped = 0
    unresolved = []
    for name in targets:
        if name in SKIP:
            continue
        if name == "Water Bottle":
            if not args.dry_run:
                render_water_bottle(args.assets, args.out, args.size)
            flats += 1
            print("  flat   Grid Water Bottle.png   <- potion + water tint")
            continue
        vid = ALIAS[name] if name in ALIAS else rev.get(name)
        if not vid:
            unresolved.append(name); continue
        out_path = os.path.join(args.out, f"Grid {name}.png")
        ref = None if is_flat_item(vid, args.assets) else rb.resolve_model(vid, args.assets)
        try:
            if ref:  # block — render the model
                if not args.dry_run:
                    px = rb.render(rb.load_chain(ref, args.assets), args.assets, args.size, args.ss)
                    rb.encode_png(out_path, args.size, args.size, px)
                blocks += 1
                print(f"  block  Grid {name}.png   <- {ref}")
            else:     # flat — upscale the sprite
                paths = flat_textures(vid, args.assets)
                if not paths:
                    unresolved.append(name); continue
                if not args.dry_run:
                    base = None
                    for tp in paths:
                        w, h, pxs = rb.decode_png(tp)
                        w, h, pxs = up.first_frame(w, h, pxs, tp)
                        base = pxs if base is None else up.composite_over(base, pxs)
                    rb.encode_png(out_path, args.size, args.size,
                                  up.nearest_resize(w, h, base, args.size, args.size))
                flats += 1
                print(f"  flat   Grid {name}.png   <- item/{vid}")
        except Exception as e:  # noqa
            unresolved.append(f"{name} ({e})")

    print(f"\n{'(dry) ' if args.dry_run else ''}{blocks} blocks + {flats} flat = {blocks + flats} vanilla icons -> {args.out}")
    if unresolved:
        print(f"unresolved/skipped ({len(unresolved)}) — left as text-link fallbacks:")
        for u in unresolved:
            print(f"  {u}")


if __name__ == "__main__":
    main()
