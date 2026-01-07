#!/usr/bin/env python3
"""
Pipe Texture Generator for Logistics Mod

This script generates 16x16 pipe textures with a cross/plus pattern.
The textures have border on pixels 4-11 on both axes, with transparent corners,
creating the classic BuildCraft-style pipe appearance.

Usage:
    python3 generate_pipe_textures.py

Output:
    Generates PNG files in ../src/main/resources/assets/logistics/textures/block/
"""

from PIL import Image, ImageDraw
import os
import hashlib
from typing import Callable, Optional, Tuple

# Output directory (relative to script location)
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
OUTPUT_DIR = os.path.join(SCRIPT_DIR, '..', 'src', 'main', 'resources', 'assets', 'logistics', 'textures', 'block')

# Texture size
SIZE = 16

# Cross/plus pattern bounds (pixels 4-11 inclusive)
CROSS_MIN = 4
CROSS_MAX = 11

# Global seed for deterministic generation (bump the suffix if you want a new look)
TEXTURE_SEED = "logistics_pipes_v1"

def _clamp_u8(v: int) -> int:
    return max(0, min(255, int(v)))


def _stable_hash_u32(seed: str, x: int, y: int) -> int:
    """Stable 32-bit hash from (seed, x, y)."""
    h = hashlib.md5(f"{seed}:{x},{y}".encode("utf-8")).digest()
    return int.from_bytes(h[:4], byteorder="big", signed=False)


def stable_noise_0_1(seed: str, x: int, y: int) -> float:
    """Deterministic per-pixel noise in [0, 1)."""
    return (_stable_hash_u32(seed, x, y) % 1_000_000) / 1_000_000.0


def stable_randint(seed: str, x: int, y: int, lo: int, hi: int) -> int:
    """Deterministic randint inclusive."""
    if hi < lo:
        lo, hi = hi, lo
    span = hi - lo + 1
    return lo + (_stable_hash_u32(seed, x, y) % span)


def add_noise(color, seed: str, x: int, y: int, variance: int = 10):
    """Add deterministic *luma-only* noise to avoid introducing color shifts.

    We shift R/G/B together by the same delta so grays stay gray.
    """
    r, g, b, a = color
    d = stable_randint(seed, x, y, -variance, variance)
    return (_clamp_u8(r + d), _clamp_u8(g + d), _clamp_u8(b + d), a)


def pick_from_palette(palette, seed: str, x: int, y: int):
    """Pick a deterministic color from a palette based on per-pixel noise."""
    if not palette:
        raise ValueError("Palette must not be empty")
    n = stable_noise_0_1(seed, x, y)
    idx = int(n * len(palette))
    if idx >= len(palette):
        idx = len(palette) - 1
    return palette[idx]

def create_base_texture():
    """Create a 16x16 transparent image"""
    return Image.new('RGBA', (SIZE, SIZE), (0, 0, 0, 0))

def is_in_cross(x, y):
    """Check if pixel is in the cross/plus pattern"""
    return (CROSS_MIN <= x <= CROSS_MAX) or (CROSS_MIN <= y <= CROSS_MAX)

def is_border(x, y):
    """
    Check if pixel is on the border of the cross/plus pattern
    """
    in_cross = is_in_cross(x, y)
    if not in_cross:
        return False
    return x == CROSS_MIN or x == CROSS_MAX or y == CROSS_MIN or y == CROSS_MAX

def is_clear_channel(x, y):
    """Always-transparent viewing channel through the pipe.

    - Middle 2 pixels of each arm (a 2px "window" slit)
    - Center 2x2 pixels (covered by the slits, but documented explicitly)

    This gives a consistent view into the pipe while the remaining interior
    retains some texture via the dithered transparency mask.
    """
    mid_lo = (CROSS_MIN + CROSS_MAX) // 2  # 7 when CROSS_MIN=4 and CROSS_MAX=11
    mid_hi = mid_lo + 1                   # 8

    # 2px-wide slit through each arm (center rows/cols)
    return (x == mid_lo or x == mid_hi or y == mid_lo or y == mid_hi)

def is_center_clear(x: int, y: int) -> bool:
    """Hub area where pipes visually connect.

    Use a 6x6 square in the middle of the pipe so the connector region reads as
    a solid block in the transparent variants.
    """
    mid_lo = (CROSS_MIN + CROSS_MAX) // 2  # 7
    mid_hi = mid_lo + 1                   # 8
    center_min = mid_lo - 2               # 5
    center_max = mid_hi + 2               # 10
    return center_min <= x <= center_max and center_min <= y <= center_max


def is_arm_interior(x: int, y: int) -> bool:
    """True for non-border pixels that are part of the cross arms (not the hub)."""
    if not is_in_cross(x, y) or is_border(x, y):
        return False
    # Exclude the hub area
    if is_center_clear(x, y):
        return False

    in_hub_band_x = CROSS_MIN <= x <= CROSS_MAX
    in_hub_band_y = CROSS_MIN <= y <= CROSS_MAX

    # Arm pixels are those where exactly one coordinate is in the cross band
    # (the other is outside), i.e. the extensions from the hub.
    return (in_hub_band_x and not in_hub_band_y) or (in_hub_band_y and not in_hub_band_x)


def make_transparent_variant(
    generator_func: Callable[[], Image.Image],
    *,
    # Legacy parameters (kept for compatibility with older calls)
    interior_keep_ratio: float = 0.0,
    interior_alpha: Tuple[int, int] = (48, 96),
    seed: Optional[str] = None,
    dither_alpha: bool = True,
    # New structured mask controls
    pattern_style: str = "inset_lines",  # "inset_lines" or "diagonal"
    pattern_alpha: int = 96,              # alpha applied to arm pattern pixels
    clear_center_hub: bool = True,
):
    """Create a transparent version of a pipe texture with a structured mask.

    Goals:
    - The center hub is empty by default so connection points are obvious (can be kept solid for special variants).
    - Each arm has a deterministic pattern that lines up across materials.
    - Avoid random/noise-based removal that can look "messy" at 16x16.

    Notes:
    - Borders remain fully opaque (they come from the base generator).
    - Non-pattern interior pixels are fully transparent.
    """

    pattern_alpha = max(0, min(255, int(pattern_alpha)))

    def is_arm_pattern_pixel(x: int, y: int) -> bool:
        if not is_arm_interior(x, y):
            return False

        # Inset lines: draw 1px-inset rails parallel to the arm edges, now 2px in from the border.
        if pattern_style == "inset_lines":
            if CROSS_MIN <= y <= CROSS_MAX and not (CROSS_MIN <= x <= CROSS_MAX):
                # Horizontal arm (left/right)
                # border (CROSS_MIN/CROSS_MAX), then clear pixel (±1), then rail (±2)
                return y == (CROSS_MIN + 2) or y == (CROSS_MAX - 2)
            if CROSS_MIN <= x <= CROSS_MAX and not (CROSS_MIN <= y <= CROSS_MAX):
                # Vertical arm (up/down)
                # border (CROSS_MIN/CROSS_MAX), then clear pixel (±1), then rail (±2)
                return x == (CROSS_MIN + 2) or x == (CROSS_MAX - 2)
            return False

        # Diagonal: one 45° diagonal rail per arm, mirrored so it lines up across materials/sides.
        if pattern_style == "diagonal":
            # One 45° diagonal rail per arm, mirrored so it lines up across materials/sides.
            #
            # Use "distance from the outer edge" so left/right (and top/bottom) arms share the
            # same phase, which helps mixed pipe types still look connected.
            mid_lo = (CROSS_MIN + CROSS_MAX) // 2
            mid_hi = mid_lo + 1

            # Horizontal arms: y within the cross band, x outside.
            if CROSS_MIN <= y <= CROSS_MAX and not (CROSS_MIN <= x <= CROSS_MAX):
                x_edge = x if x < CROSS_MIN else (SIZE - 1 - x)  # 0..3 from the outer edge
                y_in = y - CROSS_MIN                             # 0..7 within the band
                return y_in == (x_edge + 3)

            # Vertical arms: x within the cross band, y outside.
            if CROSS_MIN <= x <= CROSS_MAX and not (CROSS_MIN <= y <= CROSS_MAX):
                y_edge = y if y < CROSS_MIN else (SIZE - 1 - y)  # 0..3 from the outer edge
                x_in = x - CROSS_MIN                             # 0..7 within the band
                return x_in == (y_edge + 3)

            return False

                # X: BuildCraft-ish tiling arm pattern.
        #
        # For each arm (8px tall/wide band, 4px thick), draw:
        # - full 4px bar on the first and last row of the arm band
        # - a single pixel diagonal that goes out to the edge and back
        #
        # Expected (example for a right arm, shown as 8 rows x 4 cols):
        # ####
        #  #
        #   #
        #    #
        #    #
        #   #
        #  #
        # ####
        if pattern_style == "x":
            # Horizontal arms: y within the cross band, x outside.
            if CROSS_MIN <= y <= CROSS_MAX and not (CROSS_MIN <= x <= CROSS_MAX):
                y_in = y - CROSS_MIN  # 0..7

                # Distance from the hub edge (0..3) so left/right share the same phase.
                if x < CROSS_MIN:
                    # left arm: hub-adjacent column is x == CROSS_MIN-1
                    dist = (CROSS_MIN - 1) - x
                else:
                    # right arm: hub-adjacent column is x == CROSS_MAX+1
                    dist = x - (CROSS_MAX + 1)

                # Bars at the ends
                if y_in == 0 or y_in == 7:
                    return True

                # Single diagonal pixel, out-and-back
                col = min(y_in, 7 - y_in)  # 1,2,3,3,2,1
                return dist == col

            # Vertical arms: x within the cross band, y outside.
            if CROSS_MIN <= x <= CROSS_MAX and not (CROSS_MIN <= y <= CROSS_MAX):
                x_in = x - CROSS_MIN  # 0..7

                # Distance from the hub edge (0..3) so top/bottom share the same phase.
                if y < CROSS_MIN:
                    # top arm: hub-adjacent row is y == CROSS_MIN-1
                    dist = (CROSS_MIN - 1) - y
                else:
                    # bottom arm: hub-adjacent row is y == CROSS_MAX+1
                    dist = y - (CROSS_MAX + 1)

                # Bars at the ends
                if x_in == 0 or x_in == 7:
                    return True

                # Single diagonal pixel, out-and-back
                col = min(x_in, 7 - x_in)  # 1,2,3,3,2,1
                return dist == col

            return False

        # Unknown style -> no pattern
        return False

    def transparent_generator():
        img = generator_func()
        pixels = img.load()

        for y in range(SIZE):
            for x in range(SIZE):
                if not is_in_cross(x, y) or is_border(x, y):
                    continue

                # Clear hub by default for obvious connectivity (some variants keep it solid)
                if clear_center_hub and is_center_clear(x, y):
                    pixels[x, y] = (0, 0, 0, 0)
                    continue

                # If the hub should be solid, preserve its original pixel (leave as-is)
                if (not clear_center_hub) and is_center_clear(x, y):
                    continue

                # Arm pattern pixels: keep color but apply a consistent translucent alpha
                if is_arm_pattern_pixel(x, y):
                    r, g, b, a = pixels[x, y]
                    if a != 0:
                        pixels[x, y] = (r, g, b, pattern_alpha)
                    continue

                # Everything else inside the cross (non-border, non-hub, non-pattern)
                # is fully transparent.
                pixels[x, y] = (0, 0, 0, 0)

        return img

    return transparent_generator

def generate_cobblestone_pipe():
    """Generate cobblestone pipe texture - gray with rough mottled pattern (lighter, closer to vanilla)."""
    img = create_base_texture()
    pixels = img.load()

    seed_base = f"{TEXTURE_SEED}:cobblestone:base"
    seed_border = f"{TEXTURE_SEED}:cobblestone:border"

    # Vanilla-ish cobblestone: neutral grays with a broad value range (not too dark)
    interior_palette = [
        (92, 92, 92, 255),
        (105, 105, 105, 255),
        (120, 120, 120, 255),
        (132, 132, 132, 255),
        (145, 145, 145, 255),
        (158, 158, 158, 255),
    ]
    # Slightly darker outline band to read as a pipe frame
    border_palette = [
        (78, 78, 78, 255),
        (88, 88, 88, 255),
        (98, 98, 98, 255),
    ]

    for y in range(SIZE):
        for x in range(SIZE):
            if is_in_cross(x, y):
                if x == CROSS_MIN or x == CROSS_MAX or y == CROSS_MIN or y == CROSS_MAX:
                    c = pick_from_palette(border_palette, seed_border, x, y)
                    pixels[x, y] = add_noise(c, seed_border + ":n", x, y, variance=4)
                else:
                    c = pick_from_palette(interior_palette, seed_base, x, y)
                    pixels[x, y] = add_noise(c, seed_base + ":n", x, y, variance=7)

    return img

def generate_stone_pipe():
    """Generate stone pipe texture - clean smooth gray (deterministic, no color shifts)."""
    img = create_base_texture()
    pixels = img.load()

    seed_base = f"{TEXTURE_SEED}:stone:base"
    seed_border = f"{TEXTURE_SEED}:stone:border"

    # Vanilla-ish stone: slightly warm light gray. Keep it subtle.
    interior_palette = [
        (118, 118, 118, 255),
        (124, 124, 124, 255),
        (130, 130, 130, 255),
    ]
    border_palette = [
        (136, 136, 136, 255),
        (142, 142, 142, 255),
    ]

    for y in range(SIZE):
        for x in range(SIZE):
            if is_in_cross(x, y):
                if x == CROSS_MIN or x == CROSS_MAX or y == CROSS_MIN or y == CROSS_MAX:
                    pixels[x, y] = add_noise(pick_from_palette(border_palette, seed_border, x, y), seed_border + ":n", x, y, variance=2)
                else:
                    pixels[x, y] = add_noise(pick_from_palette(interior_palette, seed_base, x, y), seed_base + ":n", x, y, variance=3)

    return img

def generate_wood_pipe():
    """Generate wood pipe texture - brown with vertical grain"""
    img = create_base_texture()
    pixels = img.load()

    # Vanilla oak-planks-ish tone, slightly darkened to read as a pipe
    base_color = (155, 122, 74, 255)
    border_color = (110, 86, 52, 255)

    for y in range(SIZE):
        for x in range(SIZE):
            if is_in_cross(x, y):
                # Dark border
                if x == CROSS_MIN or x == CROSS_MAX or y == CROSS_MIN or y == CROSS_MAX:
                    pixels[x, y] = add_noise(border_color, f"{TEXTURE_SEED}:wood:border", x, y, variance=3)
                else:
                    # Wood grain - vertical variation
                    grain_offset = int((x % 3) * 10 - 15)
                    color = (
                        base_color[0] + grain_offset,
                        base_color[1] + grain_offset,
                        base_color[2] + grain_offset,
                        255
                    )
                    pixels[x, y] = add_noise(color, f"{TEXTURE_SEED}:wood:grain", x, y, variance=4)

    return img

def generate_iron_pipe():
    """Generate iron pipe texture - metallic gray with highlights"""
    img = create_base_texture()
    pixels = img.load()

    # Brighter iron block feel: very light gray with subtle contrast
    base_color = (214, 214, 214, 255)
    border_color = (235, 235, 235, 255)
    highlight_color = (245, 245, 245, 255)

    for y in range(SIZE):
        for x in range(SIZE):
            if is_in_cross(x, y):
                # Border
                if x == CROSS_MIN or x == CROSS_MAX or y == CROSS_MIN or y == CROSS_MAX:
                    pixels[x, y] = add_noise(border_color, f"{TEXTURE_SEED}:iron:border", x, y, variance=2)
                else:
                    # Metallic highlights
                    if (x + y) % 3 == 0:
                        pixels[x, y] = add_noise(highlight_color, f"{TEXTURE_SEED}:iron:hl", x, y, variance=2)
                    else:
                        pixels[x, y] = add_noise(base_color, f"{TEXTURE_SEED}:iron:base", x, y, variance=2)

    return img

def generate_gold_pipe():
    """Generate gold pipe texture - yellow-gold with shine"""
    img = create_base_texture()
    pixels = img.load()

    # Gold block feel: pale yellow-gold (less orange than pure RGB gold)
    base_color = (248, 227, 92, 255)
    border_color = (208, 176, 54, 255)
    shine_color = (255, 245, 170, 255)

    for y in range(SIZE):
        for x in range(SIZE):
            if is_in_cross(x, y):
                # Border
                if x == CROSS_MIN or x == CROSS_MAX or y == CROSS_MIN or y == CROSS_MAX:
                    pixels[x, y] = add_noise(border_color, f"{TEXTURE_SEED}:gold:border", x, y, variance=4)
                else:
                    # Golden shine
                    if (x + y) % 4 <= 1:
                        pixels[x, y] = add_noise(shine_color, f"{TEXTURE_SEED}:gold:shine", x, y, variance=5)
                    else:
                        pixels[x, y] = add_noise(base_color, f"{TEXTURE_SEED}:gold:base", x, y, variance=5)

    return img

def generate_gold_pipe_powered():
    """Generate powered gold pipe texture - gold with red redstone energy lines"""
    img = create_base_texture()
    pixels = img.load()

    # Start with gold base
    base_color = (248, 227, 92, 255)
    border_color = (208, 176, 54, 255)
    shine_color = (255, 245, 170, 255)

    # Redstone energy colors
    redstone_glow = (255, 50, 50, 255)      # Bright red glow
    redstone_energy = (220, 30, 30, 255)    # Dark red energy

    for y in range(SIZE):
        for x in range(SIZE):
            if is_in_cross(x, y):
                # Border - add red energy tint
                if x == CROSS_MIN or x == CROSS_MAX or y == CROSS_MIN or y == CROSS_MAX:
                    # Mix gold border with red glow
                    r = min(255, border_color[0] + 30)
                    g = max(0, border_color[1] - 30)
                    b = max(0, border_color[2] - 30)
                    pixels[x, y] = add_noise((r, g, b, 255), f"{TEXTURE_SEED}:gold_powered:border", x, y, variance=4)
                else:
                    # Interior: add red energy lines/crosses
                    # Energy cross pattern in the center
                    mid = (CROSS_MIN + CROSS_MAX) // 2
                    is_energy_line = (x == mid or x == mid + 1 or y == mid or y == mid + 1)

                    # Diagonal energy lines
                    is_diagonal = ((x - CROSS_MIN) == (y - CROSS_MIN)) or ((x - CROSS_MIN) == (CROSS_MAX - y))

                    if is_energy_line or is_diagonal:
                        # Red energy lines
                        pixels[x, y] = add_noise(redstone_glow, f"{TEXTURE_SEED}:gold_powered:energy", x, y, variance=8)
                    elif (x + y) % 4 <= 1:
                        # Golden shine with slight red tint
                        r = min(255, shine_color[0] + 7)
                        g = max(0, shine_color[1] - 20)
                        b = max(0, shine_color[2] - 40)
                        pixels[x, y] = add_noise((r, g, b, 255), f"{TEXTURE_SEED}:gold_powered:shine", x, y, variance=5)
                    else:
                        # Base gold
                        pixels[x, y] = add_noise(base_color, f"{TEXTURE_SEED}:gold_powered:base", x, y, variance=5)

    return img

def generate_diamond_pipe():
    """Generate diamond pipe texture - cyan/aqua with crystalline sparkles"""
    img = create_base_texture()
    pixels = img.load()

    # Diamond block feel: soft teal
    base_color = (90, 205, 200, 255)
    border_color = (55, 165, 160, 255)
    sparkle_color = (190, 250, 245, 255)

    for y in range(SIZE):
        for x in range(SIZE):
            if is_in_cross(x, y):
                # Border
                if x == CROSS_MIN or x == CROSS_MAX or y == CROSS_MIN or y == CROSS_MAX:
                    pixels[x, y] = add_noise(border_color, f"{TEXTURE_SEED}:diamond:border", x, y, variance=4)
                else:
                    # Crystalline sparkles
                    if (x * y) % 7 == 0:
                        pixels[x, y] = add_noise(sparkle_color, f"{TEXTURE_SEED}:diamond:sparkle", x, y, variance=6)
                    else:
                        pixels[x, y] = add_noise(base_color, f"{TEXTURE_SEED}:diamond:base", x, y, variance=5)

    return img

def generate_copper_pipe():
    """Generate copper pipe texture - warm orange with subtle oxidation"""
    img = create_base_texture()
    pixels = img.load()

    seed_base = f"{TEXTURE_SEED}:copper:base"
    seed_border = f"{TEXTURE_SEED}:copper:border"

    interior_palette = [
        (176, 96, 54, 255),
        (186, 102, 58, 255),
        (196, 110, 64, 255),
        (207, 118, 70, 255),
        (216, 126, 74, 255),
        (225, 134, 80, 255),
    ]
    border_palette = [
        (150, 78, 42, 255),
        (160, 84, 46, 255),
        (170, 90, 50, 255),
    ]

    for y in range(SIZE):
        for x in range(SIZE):
            if not is_in_cross(x, y):
                continue

            if is_border(x, y):
                color = pick_from_palette(border_palette, seed_border, x, y)
                pixels[x, y] = add_noise(color, seed_border, x, y, variance=3)
            else:
                color = pick_from_palette(interior_palette, seed_base, x, y)
                pixels[x, y] = add_noise(color, seed_base, x, y, variance=4)

    return img


# --- Void (Obsidian-like) Pipe ---
def generate_void_pipe():
    """Generate void (obsidian-like) pipe texture.

    Visual goals:
    - Reads like obsidian (very dark purple with subtle speckle)
    - Border slightly brighter for definition
    """
    img = create_base_texture()
    pixels = img.load()

    seed_base = f"{TEXTURE_SEED}:void:base"
    seed_border = f"{TEXTURE_SEED}:void:border"

    # Obsidian-ish palette (dark purple/near-black). Keep luma-only noise small.
    interior_palette = [
        (14, 8, 24, 255),
        (18, 10, 30, 255),
        (22, 12, 36, 255),
        (26, 14, 42, 255),
    ]
    border_palette = [
        (30, 16, 52, 255),
        (34, 18, 58, 255),
        (38, 20, 64, 255),
    ]

    for y in range(SIZE):
        for x in range(SIZE):
            if is_in_cross(x, y):
                if x == CROSS_MIN or x == CROSS_MAX or y == CROSS_MIN or y == CROSS_MAX:
                    c = pick_from_palette(border_palette, seed_border, x, y)
                    pixels[x, y] = add_noise(c, seed_border + ":n", x, y, variance=2)
                else:
                    c = pick_from_palette(interior_palette, seed_base, x, y)
                    # Slightly more variation in the interior for obsidian speckle
                    pixels[x, y] = add_noise(c, seed_base + ":n", x, y, variance=4)

    return img

def main():
    """Generate all pipe textures (both opaque and transparent variants)"""
    # Ensure output directory exists
    os.makedirs(OUTPUT_DIR, exist_ok=True)

    # Define base pipe generators
    base_generators = {
        'cobblestone': generate_cobblestone_pipe,
        'stone': generate_stone_pipe,
        'wood': generate_wood_pipe,
        'iron': generate_iron_pipe,
        'gold': generate_gold_pipe,
        'diamond': generate_diamond_pipe,
        'void': generate_void_pipe,
        'copper': generate_copper_pipe,
    }

    # Per-material arm pattern visibility. Higher alpha => harder to see into the pipe.
    material_transparency = {
        'cobblestone': {'pattern_alpha': 140},
        'stone': {'pattern_alpha': 110},
        'wood': {'pattern_alpha': 120},
        'iron': {'pattern_alpha': 105},
        'gold': {'pattern_alpha': 105},
        'diamond': {'pattern_alpha': 90},
        'void': {'pattern_alpha': 170},
        'copper': {'pattern_alpha': 115},
    }

    # Generate transparent versions (default)
    print("Generating transparent pipe textures...")
    for name, generator in base_generators.items():
        filename = f'pipe_{name}.png'
        output_path = os.path.join(OUTPUT_DIR, filename)
        print(f"  {filename}...")

        cfg = material_transparency.get(name, {'pattern_alpha': 96})
        transparent_gen = make_transparent_variant(
            generator,
            pattern_style="x",
            pattern_alpha=cfg['pattern_alpha'],
            clear_center_hub=name != 'void',
        )
        img = transparent_gen()
        img.save(output_path, 'PNG')

    # Generate powered gold pipe variant
    print("\nGenerating powered gold pipe texture...")
    filename = 'pipe_gold_powered.png'
    output_path = os.path.join(OUTPUT_DIR, filename)
    print(f"  {filename}...")

    cfg = material_transparency.get('gold', {'pattern_alpha': 105})
    transparent_gen = make_transparent_variant(
        generate_gold_pipe_powered,
        pattern_style="x",
        pattern_alpha=cfg['pattern_alpha'],
        clear_center_hub=True,
    )
    img = transparent_gen()
    img.save(output_path, 'PNG')

    # Generate opaque versions
    print("\nGenerating opaque pipe textures...")
    for name, generator in base_generators.items():
        filename = f'pipe_{name}_opaque.png'
        output_path = os.path.join(OUTPUT_DIR, filename)
        print(f"  {filename}...")

        img = generator()
        img.save(output_path, 'PNG')

    print(f"\nAll textures generated successfully!")
    print(f"Output directory: {OUTPUT_DIR}")
    
    
if __name__ == '__main__':
    main()
