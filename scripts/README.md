# Logistics Mod Scripts

This directory contains utility scripts for the Logistics mod development.

## Pipe Texture Generator

### Requirements

- Python 3.6+
- Pillow (PIL) library

### Setup (First Time)

A virtual environment is already set up in the `scripts/venv` directory with Pillow installed.

If you need to recreate it:
```bash
cd scripts
python3 -m venv venv
source venv/bin/activate
pip install Pillow
```

### Usage

Generate all pipe textures:
```bash
cd scripts
source venv/bin/activate
python3 generate_pipe_textures.py
```

Or as a one-liner from the project root:
```bash
cd scripts && source venv/bin/activate && python3 generate_pipe_textures.py
```

This will create/update all pipe textures in `src/main/resources/assets/logistics/textures/block/`:

**Transparent versions** (default - for visible item rendering):
- `pipe_cobblestone.png` - Border only, see-through interior
- `pipe_stone.png` - Border only, see-through interior
- `pipe_wood.png` - Border only, see-through interior
- `pipe_iron.png` - Border only, see-through interior
- `pipe_gold.png` - Border only, see-through interior
- `pipe_diamond.png` - Border only, see-through interior

**Opaque versions** (for performance mode, items not visible):
- `pipe_cobblestone_opaque.png` - Gray with rough mottled pattern
- `pipe_stone_opaque.png` - Clean smooth gray
- `pipe_wood_opaque.png` - Brown with vertical grain
- `pipe_iron_opaque.png` - Metallic gray with highlights
- `pipe_gold_opaque.png` - Yellow-gold with shine
- `pipe_diamond_opaque.png` - Cyan/aqua with crystalline sparkles

### Modifying Textures

Edit the `generate_pipe_textures.py` file to adjust:
- Colors: Change the base_color, border_color values in each generator function
- Patterns: Modify the noise variance and pattern logic
- Size: Adjust the SIZE constant (currently 16x16)
- Cross pattern: Modify CROSS_MIN and CROSS_MAX (currently pixels 4-11)

After making changes, re-run the script to regenerate textures.

## Recipe Card Generator

### Requirements

- Python 3.6+
- Pillow (PIL) library

### Usage

Generate crafting recipe cards from exported item icons:
```bash
cd scripts
source venv/bin/activate
python3 generate_recipe_cards.py
```

Defaults assume icon exports live in `run/icon-exports-x64`, result icons live in
`run/icon-exports-x88`, the template
background lives at `assets/art/crafting-grid.png`, and outputs to
`assets/recipe-cards`. The font will auto-download to `scripts/fonts/`.
Override with:
```bash
python3 generate_recipe_cards.py --icons-dir run/icon-exports-x64 --result-icons-dir run/icon-exports-x88 --out-dir assets/recipe-cards --template assets/art/crafting-grid.png --font scripts/fonts/Minecraft-Seven_v2.woff2
```

If a recipe expands into multiple variants (like plank tags), the script will also
write an animated GIF that cycles through the variants every 2.5 seconds.
