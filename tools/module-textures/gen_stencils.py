#!/usr/bin/env python3
"""Module chip-stencil generator + layout rules.

A "stencil" is the arrangement of chip rectangles on a logistics module. Shapes and
rules are distilled from the original Logistics Pipes module textures:

  - Pool of 5 chip shapes; each module uses a random 4 of them.
  - 4x1 is horizontal only (a tall 1-wide sliver looks wrong on the tall module).
  - Every window edge must have >= EDGE_MIN chip pixels touching it (balanced coverage).
  - Chips must spread across >= MIN_QUADS quadrants.
  - Even orientation mix: |vertical - horizontal| <= 1 among non-square chips.
  - No pareidolia: reject centered-mouth + centered-nose + symmetric-eyes arrangements.
  - 1px gaps between chips.

Run directly to dump a sheet of candidate stencils for review:
    python3 gen_stencils.py
Used as a library by gen_modules.py (try_layout / is_good / geometry).
"""
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from pngkit import encode_rgba

BASE = (0x9D, 0x9D, 0x97, 0xFF)   # preview-only neutral module fill
CONN = (0xC0, 0xC4, 0xC8, 0xFF)   # silver connector
DARK = (0x24, 0x24, 0x24, 0xFF)   # preview-only chip color

FILL_X, FILL_Y = range(3, 13), range(2, 14)      # opaque module body
CONN_X, CONN_Y = range(4, 12), range(12, 14)      # connector strip
COMP_X, COMP_Y = range(4, 12), range(3, 11)       # chip landing window, x4..11 y3..10
COMP = {(x, y) for x in COMP_X for y in COMP_Y}
MIDX, MIDY = 7.5, 6.5

# Pool of 5 shapes; each module uses a random 4 of them.
POOL = [
    [(2, 5), (5, 2)],   # 5x2 (either orientation; 2-wide, not a sliver)
    [(3, 2), (2, 3)],   # 3x2
    [(4, 1)],           # 4x1 horizontal only
    [(2, 2)],           # 2x2
    [(4, 2), (2, 4)],   # 4x2
]
PICK = 4          # chips placed per module
MIN_QUADS = 3     # chips must occupy at least this many window quadrants
EDGE_MIN = 3      # each window edge must have at least this many chip pixels touching it


def setpx(buf, x, y, c):
    buf[(y * 16 + x) * 4:(y * 16 + x) * 4 + 4] = bytes(c)


def try_layout(rng):
    """Place a random 4 of the 5 shapes with 1px gaps; return [(x,y,w,h)] or None."""
    occupied, placed = set(), []
    chosen = rng.sample(POOL, PICK)
    chosen.sort(key=lambda o: -o[0][0] * o[0][1])         # largest first packs better
    for orients in chosen:
        done = False
        for _ in range(200):
            w, h = rng.choice(orients)
            x0 = rng.randint(min(COMP_X), max(COMP_X) - w + 1)
            y0 = rng.randint(min(COMP_Y), max(COMP_Y) - h + 1)
            cells = {(x0 + dx, y0 + dy) for dx in range(w) for dy in range(h)}
            if any(c not in COMP for c in cells):
                continue
            halo = {(x + ox, y + oy) for (x, y) in cells for ox in (-1, 0, 1) for oy in (-1, 0, 1)}
            if halo & occupied:
                continue
            occupied |= cells
            placed.append((x0, y0, w, h))
            done = True
            break
        if not done:
            return None
    return placed


def looks_like_face(placed):
    """Face = centered low horizontal bar (mouth) + a centered chip above it (nose)
    + a mirror-symmetric pair of chips above (eyes). All three needed, else it's fine."""
    for mouth in placed:
        mx, my, mw, mh = mouth
        if not (mw >= 3 and mw > mh):
            continue
        mcx, mcy = mx + mw / 2, my + mh / 2
        if abs(mcx - MIDX) > 1.0 or mcy <= MIDY:
            continue
        others = [(cx + cw / 2, cy + ch / 2) for (cx, cy, cw, ch) in placed
                  if (cx, cy, cw, ch) != mouth]
        if not any(abs(cx - MIDX) <= 1.0 and cy < mcy for cx, cy in others):
            continue
        for (lx, ly) in others:
            for (rx, ry) in others:
                if (lx < mcx < rx and ly < mcy and ry < mcy
                        and abs((mcx - lx) - (rx - mcx)) <= 1.5 and abs(ly - ry) <= 2.5):
                    return True
    return False


def edge_contacts(placed):
    cells = {(x0 + dx, y0 + dy) for x0, y0, w, h in placed
             for dx in range(w) for dy in range(h)}
    top = sum((x, 3) in cells for x in COMP_X)
    bot = sum((x, 10) in cells for x in COMP_X)
    left = sum((4, y) in cells for y in COMP_Y)
    right = sum((11, y) in cells for y in COMP_Y)
    return top, bot, left, right


def is_good(placed):
    quads = {((x + w / 2) > MIDX, (y + h / 2) > MIDY) for x, y, w, h in placed}
    vert = sum(1 for x, y, w, h in placed if h > w)
    horiz = sum(1 for x, y, w, h in placed if w > h)
    return (min(edge_contacts(placed)) >= EDGE_MIN and len(quads) >= MIN_QUADS
            and abs(vert - horiz) <= 1 and not looks_like_face(placed))


def good_layout(seedkey):
    """Deterministic good layout for a stable string key (family name or filename)."""
    import zlib
    rng = random.Random(zlib.crc32(seedkey.encode()) & 0xFFFFFFFF)
    while True:
        layout = try_layout(rng)
        if layout and is_good(layout):
            return layout


def _render(placed):
    buf = bytearray(16 * 16 * 4)
    for y in FILL_Y:
        for x in FILL_X:
            setpx(buf, x, y, BASE)
    for y in CONN_Y:
        for x in CONN_X:
            setpx(buf, x, y, CONN)
    for (x0, y0, w, h) in placed:
        for dx in range(w):
            for dy in range(h):
                setpx(buf, x0 + dx, y0 + dy, DARK)
    return buf


if __name__ == "__main__":
    out = os.path.join(os.path.dirname(os.path.abspath(__file__)), "stencil_preview")
    os.makedirs(out, exist_ok=True)
    rng = random.Random(0)
    made = 0
    while made < 12:
        layout = try_layout(rng)
        if layout is None or not is_good(layout):
            continue
        made += 1
        encode_rgba(f"{out}/stencil_{made:02}.png", 16, 16, _render(layout))
    print(f"wrote 12 candidate stencils to {out}")
