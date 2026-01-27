#!/usr/bin/env python3
"""
Minecraft-y 16x16 material textures + shape overlay + cutout mask + directional rim lighting.

What it does:
1) Procedurally generates a 16x16 base "material" texture (noise + optional grain + speckles).
2) Loads a single overlay PNG from a data URL.
   - Pixels exactly magenta (#ff00ff) are treated as "CUTOUT":
       -> those pixels become fully transparent in the final texture (mask).
   - Non-magenta pixels are treated as normal overlay art:
       -> alpha-composited on top of the material.
3) Adds directional rim lighting (Minecraft tool-style, top-left light):
   - For metals: highlight on edges facing light, shadow on edges away.
   - For wood/stone: gentler shading (stone mostly shadow rim, wood subtle).
4) Writes PNGs to out_textures/*.png

Requirements:
  pip install pillow
"""

from __future__ import annotations

import os
import math
import random
import base64
from dataclasses import dataclass
from io import BytesIO
from typing import Tuple, Dict, List

from PIL import Image, ImageFilter, ImageChops

RGB = Tuple[int, int, int]
MAGENTA = (255, 0, 255)

# Your overlay (one image that includes both: cutout magenta + non-magenta overlay lines)
OVERLAY_DATA_URL = (
    "data:image/png;base64,"
    "iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAA0klEQVR4AYxSCQ7CMAzL+l/2mPEavsJnSmzZqOuiblK9XI7lCFqP3itExDGi4qDXksS3xfYGUCjumf+hXiACOeOjgBokK+dw/mhm3gdzCiAZQMJQO537XwwokLe8ssDNGR69QztBAVl7tGmSd5qS2Z55cAW4diQfu3Tg7hRpU1YrEdJXAiTcfVYCOywCKQLLGa4P/0T/ApVNLALzJrg8kQ5050xa1t6hwJ3NQonnoU8BJANob6idln0KyA4Jyr10ipqdeBQAC0PAeUYSHceZ85zFDwAA//+zANOLAAAABklEQVQDAPUSc0Ua3GXPAAAAAElFTkSuQmCC"
)

# -----------------------------------------------------------------------------
# Utility
# -----------------------------------------------------------------------------

def clamp8(x: float) -> int:
    return max(0, min(255, int(round(x))))

def lerp(a: float, b: float, t: float) -> float:
    return a + (b - a) * t

def mix(c1: RGB, c2: RGB, t: float) -> RGB:
    return (clamp8(lerp(c1[0], c2[0], t)),
            clamp8(lerp(c1[1], c2[1], t)),
            clamp8(lerp(c1[2], c2[2], t)))

def add_rgb(c: RGB, dr: float, dg: float, db: float) -> RGB:
    return (clamp8(c[0] + dr), clamp8(c[1] + dg), clamp8(c[2] + db))

def mul_rgb(c: RGB, m: float) -> RGB:
    return (clamp8(c[0] * m), clamp8(c[1] * m), clamp8(c[2] * m))

def hash_noise(seed: int, x: int, y: int) -> float:
    """Deterministic pseudo-random in [0,1)."""
    n = (x * 374761393 + y * 668265263 + seed * 1442695040888963407) & 0xFFFFFFFFFFFFFFFF
    n ^= (n >> 13)
    n *= 1274126177
    n &= 0xFFFFFFFFFFFFFFFF
    n ^= (n >> 16)
    return (n & 0xFFFFFFFF) / 2**32

def value_noise(seed: int, x: float, y: float, grid: int = 4) -> float:
    """2D value noise (bilinear interpolation on a grid)."""
    gx0 = math.floor(x / grid) * grid
    gy0 = math.floor(y / grid) * grid
    gx1 = gx0 + grid
    gy1 = gy0 + grid

    tx = (x - gx0) / grid
    ty = (y - gy0) / grid

    v00 = hash_noise(seed, int(gx0), int(gy0))
    v10 = hash_noise(seed, int(gx1), int(gy0))
    v01 = hash_noise(seed, int(gx0), int(gy1))
    v11 = hash_noise(seed, int(gx1), int(gy1))

    a = lerp(v00, v10, tx)
    b = lerp(v01, v11, tx)
    return lerp(a, b, ty)

def fbm(seed: int, x: float, y: float, octaves: int = 4, lacunarity: float = 2.0, gain: float = 0.5) -> float:
    """Fractal Brownian Motion using value_noise. Returns ~[0,1]."""
    amp = 1.0
    freq = 1.0
    total = 0.0
    norm = 0.0
    for i in range(octaves):
        total += amp * value_noise(seed + i * 1013, x * freq, y * freq, grid=4)
        norm += amp
        amp *= gain
        freq *= lacunarity
    return total / max(1e-9, norm)

def make_speckles(seed: int, w: int, h: int, count: int, radius: int) -> List[Tuple[int, int]]:
    rng = random.Random(seed)
    pts = [(rng.randrange(0, w), rng.randrange(0, h)) for _ in range(count)]
    clusters: List[Tuple[int, int]] = []
    for (cx, cy) in pts:
        x, y = cx, cy
        steps = rng.randint(3, 8)
        for _ in range(steps):
            clusters.append((x, y))
            x = (x + rng.choice([-1, 0, 1])) % w
            y = (y + rng.choice([-1, 0, 1])) % h
        for _ in range(rng.randint(2, 6)):
            nx = (cx + rng.randint(-radius, radius)) % w
            ny = (cy + rng.randint(-radius, radius)) % h
            clusters.append((nx, ny))
    return clusters

# -----------------------------------------------------------------------------
# Material specs (tuned toward Minecraft tool-head vibes; edges will do the "metal" work)
# -----------------------------------------------------------------------------

@dataclass(frozen=True)
class MaterialSpec:
    name: str
    base: RGB
    shadow: RGB
    highlight: RGB
    noise_strength: float
    contrast: float
    grain: float
    speckle_color: RGB | None
    speckle_amount: int
    speckle_radius: int

MATERIALS: Dict[str, MaterialSpec] = {
    "wooden": MaterialSpec(
        name="wood",
        base=(115, 86, 34),
        shadow=(60, 42, 18),
        highlight=(160, 120, 55),
        noise_strength=10,
        contrast=0.30,
        grain=0.65,
        speckle_color=None,
        speckle_amount=0,
        speckle_radius=0,
    ),
    "stone": MaterialSpec(
        name="stone",
        base=(110, 110, 110),
        shadow=(55, 55, 55),
        highlight=(165, 165, 165),
        noise_strength=12,
        contrast=0.32,
        grain=0.05,
        speckle_color=(85, 85, 85),
        speckle_amount=8,
        speckle_radius=1,
    ),
    "copper": MaterialSpec(
        name="copper",
        base=(156, 88, 46),
        shadow=(85, 40, 18),
        highlight=(225, 145, 105),
        noise_strength=11,
        contrast=0.34,
        grain=0.05,
        speckle_color=(95, 150, 130),  # subtle oxidized hints
        speckle_amount=6,
        speckle_radius=1,
    ),
    "iron": MaterialSpec(
        name="iron",
        base=(170, 170, 170),
        shadow=(95, 95, 95),
        highlight=(235, 235, 235),
        noise_strength=9,
        contrast=0.30,
        grain=0.02,
        speckle_color=(130, 130, 130),
        speckle_amount=6,
        speckle_radius=1,
    ),
    "gold": MaterialSpec(
        name="gold",
        base=(255, 216, 62),
        shadow=(211, 150, 50),
        highlight=(254, 255, 189),
        noise_strength=3,      # was ~8-10; this is the “don’t look dirty” fix
        contrast=0.22,         # lower contrast; the palette will do the work
        grain=0.0,
        speckle_color=None,    # no sparkles here; vanilla gold uses palette steps instead
        speckle_amount=0,
        speckle_radius=0,
    ),
    "diamond": MaterialSpec(
        name="diamond",
        base=(65, 155, 125),
        shadow=(15, 55, 40),
        highlight=(120, 245, 215),
        noise_strength=3,
        contrast=0.34,
        grain=0.02,
        speckle_color=(200, 255, 255),
        speckle_amount=6,
        speckle_radius=1,
    ),
    "netherite": MaterialSpec(
        name="netherite",
        base=(85, 60, 75),
        shadow=(35, 20, 25),
        highlight=(135, 100, 115),
        noise_strength=10,
        contrast=0.36,
        grain=0.04,
        speckle_color=(150, 120, 140),
        speckle_amount=4,
        speckle_radius=1,
    ),
}

GOLD_BLOCK_PALETTE = [
    (204, 142, 39),
    (211, 150, 50),
    (245, 204, 39),
    (249, 189, 35),
    (254, 224, 72),
    (255, 216, 62),
    (255, 236, 79),
    (255, 253, 144),
    (254, 255, 189),
]

def quantize_to_palette(img_rgba: Image.Image, palette: list[RGB]) -> Image.Image:
    """Replace each opaque pixel with its nearest palette color (keeps alpha)."""
    img = img_rgba.convert("RGBA")
    px = img.load()
    w, h = img.size

    def nearest(c: RGB) -> RGB:
        r, g, b = c
        best = palette[0]
        best_d = 10**18
        for pr, pg, pb in palette:
            dr = r - pr
            dg = g - pg
            db = b - pb
            d = dr*dr + dg*dg + db*db
            if d < best_d:
                best_d = d
                best = (pr, pg, pb)
        return best

    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            nr, ng, nb = nearest((r, g, b))
            px[x, y] = (nr, ng, nb, a)
    return img

# -----------------------------------------------------------------------------
# Overlay -> (overlay_rgba, cutout_mask_L)
# -----------------------------------------------------------------------------

def load_overlay_and_mask(data_url: str, size=(16, 16)) -> tuple[Image.Image, Image.Image]:
    """
    Returns:
      overlay_rgba: magenta pixels removed (alpha=0), other pixels keep original alpha
      mask_L: 255 everywhere except magenta pixels => 0 (used to punch holes)
    """
    if not data_url.startswith("data:image/png;base64,"):
        raise ValueError("Expected a data:image/png;base64,... URL")

    raw = base64.b64decode(data_url.split(",", 1)[1])
    src = Image.open(BytesIO(raw)).convert("RGBA")

    if src.size != size:
        src = src.resize(size, resample=Image.NEAREST)

    overlay = src.copy()
    overlay_px = overlay.load()

    mask = Image.new("L", size, 255)
    mask_px = mask.load()

    for y in range(size[1]):
        for x in range(size[0]):
            r, g, b, a = overlay_px[x, y]
            if (r, g, b) == MAGENTA:
                overlay_px[x, y] = (r, g, b, 0)  # overlay doesn't draw here
                mask_px[x, y] = 0                # cutout hole in final texture
            else:
                mask_px[x, y] = 255

    return overlay, mask

def apply_cutout_mask(base_rgba: Image.Image, mask_L: Image.Image) -> Image.Image:
    """Multiply base alpha by mask_L."""
    if base_rgba.mode != "RGBA":
        base_rgba = base_rgba.convert("RGBA")
    r, g, b, a = base_rgba.split()
    a2 = ImageChops.multiply(a, mask_L)
    return Image.merge("RGBA", (r, g, b, a2))

# -----------------------------------------------------------------------------
# Base material generator
# -----------------------------------------------------------------------------

def generate_texture(spec: MaterialSpec, seed: int, w: int = 16, h: int = 16) -> Image.Image:
    """
    Procedural material texture (kept subtle; edge lighting does most of the "metal" read).
    """
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    px = img.load()

    speckles = set()
    if spec.speckle_color and spec.speckle_amount > 0:
        for (sx, sy) in make_speckles(seed + 999, w, h, spec.speckle_amount, spec.speckle_radius):
            speckles.add((sx, sy))

    for y in range(h):
        for x in range(w):
            n = fbm(seed, x + 0.33, y + 0.77, octaves=4)

            # Slight global lighting gradient (top-left brighter) for shape readability
            gx = (x / (w - 1)) if w > 1 else 0.0
            gy = (y / (h - 1)) if h > 1 else 0.0
            light = max(0.0, min(1.0, (1.0 - 0.55 * gx - 0.55 * gy)))

            # Grain (wood only, maybe tiny for others)
            grain_t = 0.0
            if spec.grain > 0:
                band = value_noise(seed + 4242, x * 2.0, 0.0, grid=3)
                wav = value_noise(seed + 7373, x * 1.2, y * 0.6, grid=5)
                grain_t = (band * 0.7 + wav * 0.3) * spec.grain

            # Contrast push
            t = (n - 0.5) * (1.0 + spec.contrast) + 0.5
            t = max(0.0, min(1.0, t))

            # Shadow -> Base -> Highlight ramp
            if t < 0.5:
                col = mix(spec.shadow, spec.base, t / 0.5)
            else:
                col = mix(spec.base, spec.highlight, (t - 0.5) / 0.5)

            # Grain modulates brightness a bit
            if spec.grain > 0:
                gshift = (grain_t - 0.5 * spec.grain) * 26.0
                col = add_rgb(col, gshift, gshift * 0.9, gshift * 0.7)

            # Fine jitter
            fine = (hash_noise(seed + 17, x, y) - 0.5) * 2.0  # -1..1
            jitter = fine * spec.noise_strength
            col = add_rgb(col, jitter, jitter, jitter)

            # Apply global light
            col = mul_rgb(col, 0.88 + 0.22 * light)

            # Speckles
            if (x, y) in speckles and spec.speckle_color:
                col = mix(col, spec.speckle_color, 0.55)

            px[x, y] = (*col, 255)

    return img

# -----------------------------------------------------------------------------
# Edge detection & directional rim lighting (tool-style)
# -----------------------------------------------------------------------------

def inner_edge_mask(mask_L: Image.Image, thickness: int = 1) -> Image.Image:
    """
    Returns an L image where 255 = pixels on the inside edge (within thickness).
    """
    m = mask_L
    for _ in range(thickness):
        m = m.filter(ImageFilter.MinFilter(3))  # erode by 1px
    return ImageChops.subtract(mask_L, m)      # original - eroded

def apply_directional_rim_light(
    base_rgba: Image.Image,
    mask_L: Image.Image,
    *,
    thickness: int = 1,
    light_dir: tuple[float, float] = (-1.0, -1.0),  # top-left
    highlight_tint: RGB = (255, 255, 255),
    highlight_strength: float = 0.70,
    shadow_strength: float = 0.45,
    sparkle_seed: int = 0,
    sparkle: bool = True,
) -> Image.Image:
    """
    Highlight on edge pixels facing light; shadow on edge pixels facing away.
    Uses 4-neighbor normals for that crunchy Minecraft feel.
    """
    if base_rgba.mode != "RGBA":
        base_rgba = base_rgba.convert("RGBA")

    w, h = base_rgba.size
    out = base_rgba.copy()
    px = out.load()
    mpx = mask_L.load()

    edge = inner_edge_mask(mask_L, thickness=thickness)
    epx = edge.load()

    # Normalize light dir
    lx, ly = light_dir
    l_len = math.hypot(lx, ly) or 1.0
    lx, ly = lx / l_len, ly / l_len

    nbrs = [(-1, 0), (1, 0), (0, -1), (0, 1)]

    for y in range(h):
        for x in range(w):
            if epx[x, y] == 0:
                continue

            # Outward normal from inside->outside by checking neighbors that are outside (mask==0)
            nx, ny = 0.0, 0.0
            for dx, dy in nbrs:
                xx, yy = x + dx, y + dy
                if 0 <= xx < w and 0 <= yy < h and mpx[xx, yy] == 0:
                    nx += dx
                    ny += dy

            n_len = math.hypot(nx, ny)
            if n_len < 1e-6:
                continue
            nx, ny = nx / n_len, ny / n_len

            ndotl = nx * lx + ny * ly  # -1..1

            r, g, b, a = px[x, y]
            if a == 0:
                continue

            # Irregularity so it doesn't look like an even stroke
            sp = 1.0
            if sparkle:
                j = hash_noise(sparkle_seed + 7777, x, y)  # 0..1
                sp = 1.0 if j > 0.92 else (0.65 if j > 0.80 else 0.35)

            if ndotl > 0.0:
                t = min(1.0, ndotl * highlight_strength * sp)
                rr = int(r + (highlight_tint[0] - r) * t)
                gg = int(g + (highlight_tint[1] - g) * t)
                bb = int(b + (highlight_tint[2] - b) * t)
                px[x, y] = (rr, gg, bb, a)
            else:
                t = min(1.0, (-ndotl) * shadow_strength)
                m = 1.0 - t * 0.70
                px[x, y] = (int(r * m), int(g * m), int(b * m), a)

    return out

# -----------------------------------------------------------------------------
# Material-specific edge styling knobs
# -----------------------------------------------------------------------------

def apply_material_edge_styling(img: Image.Image, spec: MaterialSpec, mask_L: Image.Image, seed: int) -> Image.Image:
    """
    Metals get stronger highlight + shadow rim lighting.
    Wood gets very subtle highlight and mild shadow.
    Stone gets mostly shadow rim (reads like carved/flat rock).
    Copper behaves metal-ish but slightly softer than iron/gold.
    """
    name = spec.name

    if name in ("iron", "gold", "diamond", "netherite"):
        # Use the material's highlight as tint so it doesn't just turn white
        tint = mix((255, 255, 255), spec.highlight, 0.55)
        return apply_directional_rim_light(
            img, mask_L,
            thickness=1,
            light_dir=(-1.0, -1.0),
            highlight_tint=tint,
            highlight_strength=0.85 if name == "gold" else 0.75,
            shadow_strength=0.55 if name in ("iron", "netherite") else 0.45,
            sparkle_seed=seed,
            sparkle=True,
        )

    if name == "copper":
        tint = mix((255, 255, 255), spec.highlight, 0.45)
        return apply_directional_rim_light(
            img, mask_L,
            thickness=1,
            light_dir=(-1.0, -1.0),
            highlight_tint=tint,
            highlight_strength=0.60,
            shadow_strength=0.45,
            sparkle_seed=seed,
            sparkle=True,
        )

    if name == "stone":
        # Mostly shadow rim; very little highlight (stone is matte)
        return apply_directional_rim_light(
            img, mask_L,
            thickness=1,
            light_dir=(-1.0, -1.0),
            highlight_tint=spec.highlight,
            highlight_strength=0.15,
            shadow_strength=0.55,
            sparkle_seed=seed,
            sparkle=False,
        )

    if name == "wood":
        # Subtle rim shaping; wood is also mostly matte
        return apply_directional_rim_light(
            img, mask_L,
            thickness=1,
            light_dir=(-1.0, -1.0),
            highlight_tint=spec.highlight,
            highlight_strength=0.25,
            shadow_strength=0.35,
            sparkle_seed=seed,
            sparkle=False,
        )

    return img

# -----------------------------------------------------------------------------
# Main
# -----------------------------------------------------------------------------

def main() -> None:
    out_dir = "resources/assets/logistics/textures/item"
    os.makedirs(out_dir, exist_ok=True)

    overlay_rgba, cutout_mask_L = load_overlay_and_mask(OVERLAY_DATA_URL, size=(16, 16))

    # Stable seeds per material (change master_seed to remix everything)
    master_seed = 1337

    for i, (name, spec) in enumerate(MATERIALS.items()):
        seed = master_seed + i * 10007

        # Base material
        base_img = generate_texture(spec, seed=seed, w=16, h=16)

        # Cutouts (magenta holes)
        base_cut = apply_cutout_mask(base_img, cutout_mask_L)

        # Edge lighting (tool-style)
        base_lit = apply_material_edge_styling(base_cut, spec, cutout_mask_L, seed)

        # Overlay lines/details (non-magenta pixels with alpha)
        out_img = Image.alpha_composite(base_lit, overlay_rgba)

        path = os.path.join(out_dir, f"{name}_gear.png")
        out_img.save(path)
        print(f"Wrote {path}")

if __name__ == "__main__":
    main()
