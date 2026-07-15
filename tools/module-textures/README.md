# Module texture generator

Procedurally generates the logistics **module item textures** (the little "computer card"
items) and writes them into
`common/src/main/resources/assets/logistics/textures/item/pipe/`.

Pure Python 3 standard library — no venv, no Pillow. `pngkit.py` (vendored from the
`texturekit` toolset) does the PNG I/O.

## Usage

```bash
cd tools/module-textures
python3 gen_modules.py          # regenerate all module textures in place
python3 gen_stencils.py         # dump 12 candidate stencils to ./stencil_preview/ for review
```

Output is deterministic: layouts are seeded by family/filename, so re-running reproduces
byte-identical textures.

## How a module is built

Each 16×16 module is: a colored **base** (x3–12, y2–13), a copper **connector** strip
(x4–11, y12–13), and up to four dark-gray **chip** rectangles in the component window
(x4–11, y3–10).

- `gen_stencils.py` — chip-layout rules and generator (`try_layout` / `is_good` /
  `good_layout`). Shapes and rules were distilled from the original Logistics Pipes
  module art: a 5-shape pool (`5x2, 3x2, 4x1, 2x2, 4x2`), pick 4; `4x1` horizontal only;
  ≥3px chip contact on every window edge; chips spread across ≥3 quadrants; even
  vertical/horizontal mix; an anti-"face" rule; 1px gaps.
- `gen_modules.py` — maps the mod's modules to base colors + gear tiers and renders them.

## Tiers and color

Tier families (Crafter, Extractor, Provider) **share one stencil**; only chip colors
change up the Mk ladder. The tier language is cumulative:

- **iron** → all chips dark gray
- **gold** → one chip becomes gold
- **diamond** → one chip gold **and** one chip diamond

Chip gray picks a dark or light variant by **luminance contrast** against the base
(colorblind-safe). Gold (`#F2C94C`) and diamond (`#6FE0D6`) accents are fixed bright
colors. Two-tone modules (terminus) use a 2px diagonal stripe base; `blank_module` is
white with no chips.

To change a module's color or tier, edit `FAMILIES` / `STANDALONE` in `gen_modules.py`.
To change the chip-layout rules, edit the constants/predicates in `gen_stencils.py`.
