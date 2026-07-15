#!/usr/bin/env python3
"""Generate the logistics module item textures and write them into the asset tree.

Design:
  - Tier families (Crafter, Extractor, Provider) share ONE stencil; only chip colors
    change across Mk levels.
  - Tier language is cumulative: iron = all gray chips, gold = +1 gold chip,
    diamond = +1 diamond chip. So diamond-tier modules show both a gold and a diamond.
  - Each chip's gray color picks the dark-or-light variant with the best luminance
    contrast against that module's base (colorblind-safe: luminance, not hue).
  - Gold / diamond accents are fixed bright colors so they always read as their tier.
  - Two-tone modules (terminus) use a 2px diagonal stripe base.
  - blank module is white with no chips.

Layouts are seeded by family/filename, so re-running reproduces identical textures.
Run:  python3 gen_modules.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from pngkit import encode_rgba
import gen_stencils as gs

# Write straight into the multiloader source-of-truth asset dir.
OUT = os.path.normpath(os.path.join(
    os.path.dirname(os.path.abspath(__file__)),
    "..", "..", "common", "src", "main", "resources",
    "assets", "logistics", "textures", "item", "pipe"))

BASES = {
    "purple": "#8932B8", "brown": "#835432", "gray": "#474F52", "cyan": "#169C9C",
    "green": "#5E7C16", "orange": "#F9801D", "red": "#B02E26", "light_blue": "#3AB3DA",
    "black": "#1D1D21", "white": "#F9FFFE",
}
# Bright shiny copper connector, used everywhere it reads. A slightly deeper copper is
# only substituted on bases that sit right on the copper tone (orange, light blue), so
# the strip stays distinct without the connector looking different across the set.
COPPER = (0xFC, 0x99, 0x82)
COPPER_DEEP = (0xC9, 0x6A, 0x4E)
CONN_MIN_CONTRAST = 1.4
GRAY = [(0x22, 0x22, 0x22), (0xCC, 0xCC, 0xCC)]   # [dark, light]; picked by contrast
GOLD = (0xF2, 0xC9, 0x4C)
DIAMOND = (0x6F, 0xE0, 0xD6)
RANK = {"iron": 0, "gold": 1, "diamond": 2}
STRIPE = 2

# family name -> (base dye, [(filename, gear), ...]) -- all members share one stencil
FAMILIES = {
    "crafter":   ("brown", [("crafter_module.png", "iron"),
                            ("crafter_mkii_module.png", "gold"),
                            ("crafter_mkiii_module.png", "diamond")]),
    "extractor": ("cyan",  [("extractor_module.png", "iron"),
                            ("extractor_module_mkii.png", "gold"),
                            ("extractor_module_mkiii.png", "diamond")]),
    "provider":  ("red",   [("provider_module.png", "gold"),
                            ("provider_mkii_module.png", "diamond")]),
}
# filename, [base dye(s)], gear   (two dyes -> diagonal two-tone)
STANDALONE = [
    ("active_supplier_module.png", ["purple"], "gold"),
    ("enchantment_sink_module.png", ["gray"], "gold"),
    ("item_sink_module.png", ["green"], "iron"),
    ("mod_item_sink_module.png", ["green"], "gold"),
    ("passive_supplier_module.png", ["purple"], "iron"),
    ("polymorphic_sink_module.png", ["orange"], "iron"),
    ("quicksort_module.png", ["light_blue"], "diamond"),
    ("terminus_module.png", ["black", "purple"], "iron"),
]


def rgb(h):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16))


def _lin(c):
    c /= 255
    return c / 12.92 if c <= 0.03928 else ((c + 0.055) / 1.055) ** 2.4


def lum(c):
    return 0.2126 * _lin(c[0]) + 0.7152 * _lin(c[1]) + 0.0722 * _lin(c[2])


def contrast(a, b):
    la, lb = lum(a), lum(b)
    return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)


def pick(variants, base_cols):
    """Variant with the best worst-case contrast over all base colors."""
    return max(variants, key=lambda v: min(contrast(v, b) for b in base_cols))


def setpx(buf, x, y, c):
    buf[(y * 16 + x) * 4:(y * 16 + x) * 4 + 4] = bytes(tuple(c) + (0xFF,))


def render(base_names, layout, rank):
    base_cols = [rgb(BASES[n]) for n in base_names]
    buf = bytearray(16 * 16 * 4)
    for y in gs.FILL_Y:
        for x in gs.FILL_X:
            c = base_cols[((x + y) // STRIPE) % 2] if len(base_cols) == 2 else base_cols[0]
            setpx(buf, x, y, c)
    conn = COPPER if min(contrast(COPPER, b) for b in base_cols) >= CONN_MIN_CONTRAST else COPPER_DEEP
    for y in gs.CONN_Y:
        for x in gs.CONN_X:
            setpx(buf, x, y, conn)

    gray = pick(GRAY, base_cols)
    chips = sorted(layout, key=lambda c: -c[2] * c[3])   # largest first
    gold_idx, diamond_idx = 1, 2                          # medium chips carry accents
    for i, (x0, y0, w, h) in enumerate(chips):
        col = gray
        if i == gold_idx and rank >= 1:
            col = GOLD
        elif i == diamond_idx and rank >= 2:
            col = DIAMOND
        for dx in range(w):
            for dy in range(h):
                setpx(buf, x0 + dx, y0 + dy, col)
    return buf


def main():
    n = 0
    for fam, (base, members) in FAMILIES.items():
        layout = gs.good_layout(f"family:{fam}")
        for fname, gear in members:
            encode_rgba(f"{OUT}/{fname}", 16, 16, render([base], layout, RANK[gear]))
            n += 1
    for fname, bases, gear in STANDALONE:
        layout = gs.good_layout(f"module:{fname}")
        encode_rgba(f"{OUT}/{fname}", 16, 16, render(bases, layout, RANK[gear]))
        n += 1
    encode_rgba(f"{OUT}/blank_module.png", 16, 16, render(["white"], [], 0))
    n += 1
    print(f"wrote {n} module textures to {OUT}")


if __name__ == "__main__":
    main()
