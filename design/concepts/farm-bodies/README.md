# Farm body concepts

Concept art for the planned farm blocks. This is a holding area for ideas, not shipped assets.

Every farm uses the same Forestry-style glass case. The case is a 4 px base, corner posts, glass
side panes and a 2 px cap with a glass top, painted from `farm_case.png`. Only the **body** inside
the case changes from farm to farm: a 10×10 bed, 2 px high, plus something planted on it. This
sheet checks that the design reads for every farm type before the first two farms are committed.

![Farm body concepts](farm-concepts.png)

## The body envelope

- **Bed:** `[3,4,3]` to `[13,6,13]`.
- **Planting:** stays inside x/z 2–14 and y 6–15. The glass panes sit at 1 and 15, and the top
  glass at y 15.
- **Atlas:** each shipped farm paints its body from a single 32×32 atlas, `<farm>_body.png`,
  with UVs in 0–16 units (atlas px ÷ 2). The concepts here use vanilla textures instead.
- **Texel density:**
  - Blocks (beds, logs, cactus, caps) map vanilla textures 1:1.
  - Plant crosses sample at 2× density, which shows the whole sprite at half size. This is a
    placeholder shortcut; the painted atlases should go back to 1:1.

## Lineup

The two labels that appear twice are alternatives to compare.

| Farm | Body | Status |
|---|---|---|
| Crop Farm | four crop-sprout crosses on farmland | farm shipped in #1469; this case model and `crop_farm_body.png` are on `farm-case-models` |
| Arboretum | a 3-2-1 oak log pile with two iron straps, on Humus | alternative A |
| Arboretum | a mini tree on Humus (trunk, canopy, crown) | alternative B; the current model on `farm-case-models` |
| Orchard | the mini tree on grass with red apples in the foliage | alternative A |
| Orchard | a stripped-oak basket on grass, filled with `textures/apple_pile.png` | alternative B |
| Gourd Farm | a pumpkin and a melon with attached stems | planned |
| Plantation | sugar cane and bamboo either side of a water channel in sand | planned |
| Desert Farm | two cactus columns, a cactus flower and a dead bush | planned |
| Berry Farm | four sweet berry bushes on grass | planned |
| Cocoa Plantation | pods on a bare jungle log, with no leaves | planned |
| Cave Vine Farm | glow-berry vines hanging from a moss ceiling | planned; the ceiling hides the vines from above |
| Mushroom Farm | a giant red mushroom on mycelium | planned |
| Nether Wart Farm | separate wart stalks on soul sand (`textures/nether_wart_stalks.png`) | planned |
| Peat Bog | a mud bed with a water inset and stacked peat | planned; no plant |
| Aquatic Farm | kelp, seagrass and sea pickles under a full water volume | idea; needs a translucent layer |
| Chorus Farm | a branching chorus plant on end stone | idea |
| Vine Farm | twisting vines on warped nylium | idea |
| Flower Farm | six vanilla flowers on grass | idea |
| Moss & Azalea Farm | a flowering azalea and an azalea on moss | idea |
| Fungus Farm | half crimson and half warped nylium, each with its own fungus | idea |

**Decisions so far:**
- **No 2×2 Arboretum.** The plan is to add 1×1 versions of the trees that are 2×2-only in
  vanilla. The current Arboretum already plants any sapling that isn't flagged `requires_2x2`.
- **The Fungus Farm covers both Nether woods**, which is why its body is split in two.
- **The Cocoa body is pods on a log**, with no foliage.

## Regenerating

```bash
python3 -I design/concepts/farm-bodies/concepts.py
```

**Inputs:**
- **Renderer:** `../logistics-docs/tools/render_blocks.py`. Override the path with
  `LOGISTICS_DOCS_TOOLS`.
- **Vanilla textures:** extracted once into `.cache/` (git-ignored) from the 26.3 client jar in
  the Gradle cache. Override with `MC_CLIENT_JAR`. Vanilla textures are never committed.
- **PNG decoding:** optionally `~/Dev/Tools/texturekit` (`TEXTUREKIT`). Without it, the
  renderer's own decoder is used, but it drops the transparency of grayscale PNGs such as
  `attached_pumpkin_stem`.

**Outputs:**
- **`farm-concepts.png`:** the labelled contact sheet.
- **`models/<body>.json`:** each body as a plain block model with the elements only, no case.
  - Texture ids point at the vanilla or logistics textures.
  - The tints used in the concepts (grass, water, stems, sugar cane) are not saved, so those
    faces show grey.
  - The water's translucency is not saved either.
- **`textures/`:** the generated textures, `apple_pile.png` and `nether_wart_stalks.png`.
  `models/` refers to them as `logistics:block/forestry/concept/<name>`.

Each body is one function in `concepts.py`, listed in `BODIES` with its label and model file
name.
