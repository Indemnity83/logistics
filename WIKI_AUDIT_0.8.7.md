# Wiki audit — mc/26.2 @ `mc26.2-v0.8.7`

Audit of `wiki/` against the mod data/lang/Java at tag `mc26.2-v0.8.7`.

**Baseline:** the wiki was last comprehensively synced at **v0.8.6** (commit `f3ca3853`, 2026-08-07).
Four spot fixes for v0.8.7 landed 2026-08-25/26 (Transposer RF + recipe system, Kiln claims, Pump
rate, Sawmill pulping / Fabricator queueing). Everything else in v0.8.7 is undocumented.

**Branch note:** `mc26.1-v0.8.7` and `mc26.2-v0.8.7` differ only by 11 MC-26.2-only macerator
recipes (sulfur / cinnabar), which the wiki already marks *"Since Minecraft 26.2"*. Nothing here is
branch-specific.

**Not audited on purpose:** the 25 commits on `mc/26.2` after the tag are test/CI/build work plus
two asset fixes (Seed Oil Bucket model, engine/battery particle texture) with no player-facing
documentation impact.

---

## A. Factually wrong — recipe numbers that disagree with the data — ✅ DONE

Fixed in the working tree; **not yet published to Fandom**. Two further errors of the same class
turned up while verifying and were fixed too: **Nether Gold Ore** yields ×1 (the combined row
claimed ×2), and **Coal** / **Blaze Rod** have undisclosed Sulfur Dust byproducts. Also corrected
three stale prose claims on `Macerator` (dusts craft modules; Sawdust is a "versatile crafting
component"; Flour "can be baked into bread") — all false since v0.8.3.

These were live errors on current pages, independent of the version gap.

### A1. `Macerator` — six wrong rows

| Wiki row | Wiki says | Data says |
|---|---|---|
| Raw Iron | Iron Dust ×1 | **Iron Dust ×2**, + 10% Tin Dust byproduct |
| Raw Copper | Copper Dust ×1 | **Copper Dust ×2**, + 10% Gold Dust |
| Raw Gold | Gold Dust ×1 | **Gold Dust ×2**, + 10% Quicksilver |
| Lapis Lazuli Ore / Deepslate | Lapis Dust ×4 | **Lapis Dust ×8**, + 20% Sulfur Dust |
| Redstone Ore / Deepslate | Redstone ×4 | **Redstone ×2**, + 25% Quicksilver |
| Prismarine Bricks | Prismarine Dust ×4 | **Prismarine Dust ×9** |

### A2. `Macerator` — rows for recipes that do not exist

* `Block of Diamond → Diamond Dust ×9` — **no such recipe**
* `Block of Emerald → Emerald Dust ×9` — **no such recipe**
* `Block of Lapis Lazuli → Lapis Dust ×9` — **no such recipe**
* `Block of Quartz → Quartz Dust ×4` — the recipe exists but yields **Nether Quartz ×4**, not dust

(`Block of Iron/Copper/Gold → ×9` are correct.)

### A3. `Macerator` — combined row hides a different yield

`Nether Quartz;Nether Quartz Ore → Quartz Dust,2` is one row for two recipes with different yields:
Nether Quartz Ore → Quartz Dust ×2 (+15% Sulfur Dust), but the **Nether Quartz item → Quartz Dust ×1**.

### A4. Stale crafting grids

* **`Blank Module`** — grid shows a **Gold Nugget**; the recipe uses `#c:nuggets/copper`
  (**Copper Nugget**, added v0.8.3 #722).
* **`Stone Transport Pipe`** — grid shows **Cobblestone ×2**; the recipe uses **Stone ×2**.
* **`Gold Gear`** — its Basic Logistics Pipe grid is a stale variant (Copper Transport Pipe ×2 +
  1 Gold Gear). Real recipe: 4 glass + **2** Gold Gear + Item Filter Pipe + 2 Redstone Torch.
  The `Basic Logistics Pipe` page itself is correct — only the `Gold Gear` copy is wrong.
* **`Sequential Fabricator`** — its `{{Crafting}}` block has **no `|Output=`**, so the grid renders
  with an empty result slot. Only such case in the wiki.

### A5. Dust pages still show the pre-v0.8.3 module recipes

**Scope was wider than first written.** The v0.8.3 rework orphaned grids on **14** current pages, not
six: also `Amethyst Dust`, `Coal Dust`, `Diamond Dust`, `Echo Dust`, `Lapis Dust`, `Obsidian Dust`,
`Bronze Ingot`, and `Apatite (gem)`. All fixed. Two more numeric errors surfaced there:
**Marker yields 1, not 2**, and `Coal Dust` listed a removed "Rubber Mix" recipe.
`Sturdy Casing` / `Tin Gear` were also moved to `Category:Removed` (was section E) because they were
being picked up as current pages.

`Bronze Dust`, `Copper Dust`, `Emerald Dust`, `Iron Dust`, `Sawdust`, `Ender Dust` all carry
"Usage" grids building modules from **dusts + Redstone/Amethyst/Echo Chips** — chips that were
**removed in v0.8.3**. The real recipes are *dye + gear* (or *dye + chipset*), e.g.:

| Module | Wiki grid (stale) | Actual |
|---|---|---|
| Provider Module | Iron Dust ×2 + Redstone Chip + Redstone ×2 + Blank Module | Red Dye ×4 + Gold Gear + Redstone ×3 + Blank Module |
| Extractor Module | Sawdust ×2 + Redstone Chip + … | Cyan Dye ×4 + Iron Gear + Redstone ×3 + Blank Module |
| Crafter Module | Bronze Dust ×2 + Redstone Chip + … | Brown Dye ×4 + Iron Gear + Redstone ×3 + Blank Module |
| Active Supplier | Emerald Dust ×2 + Amethyst Chip + … | Purple Dye ×4 + Gold Gear + Redstone ×3 + Blank Module |
| Passive Supplier | Emerald Dust ×2 + Redstone Chip + … | Purple Dye ×4 + Iron Gear + Redstone ×3 + Blank Module |
| Terminus Module | Ender Dust ×2 + Redstone Chip + … | Black Dye ×2 + Purple Dye ×2 + Iron Gear + Redstone ×3 + Blank Module |
| Crafter/Extractor/Provider MkII–III | dust + chip grids | upgrade recipes: base module + Gold/Diamond Gear (or chipset) |

Each also has a `_from_chipset` variant the pages don't mention.

### A6. `Ender Dust` — Ender Cable grid says **Rubber Chunk**; the item is **Rubber**

---

## B. v0.8.7 content that is missing entirely — ✅ DONE

All eight items addressed. B4 required building a new `{{Transposer}}` template family
(`Template:Transposer` + `Module:Transposer` + `{{Grid Transposer Table}}` + styles); its 12 rows
cover all 48 shipped recipes and were verified against the data. Three tool fixes fell out of it:
`fluid_icons.py` gained `seed_oil`, and `render_blocks.py`'s reference scanner now recognises the
Transposer/Refinery/Sequential Fabricator templates **and** tolerates one level of nested `{{…}}`
(a non-greedy `.*?` was stopping at the `}}` of an inner `{{Mcw|…}}` and silently missing every
param after it, so referenced icons went ungenerated).

One thing deliberately *not* done: the fix for "pipe transfer rates set above the default" (#885)
restored intended behaviour that the `Fluid Pipes` page already describes correctly, so no wiki
change was warranted.

### B1. Seed Oil has no page (new fluid)

`logistics:core/seed_oil` + `Seed Oil Bucket` are the only registered names with **no wiki page and
no mention anywhere** except two passing phrases on `Transposer`. Needs a page, plus the three
generated assets (`Grid Seed Oil.png`, `Fluid Seed Oil.gif`, `Bucket of Seed Oil.png`) via
`fluid_icons.py`, and a `gen_item_links.py` rerun.

Data: Transposer presses **wheat / beetroot / melon / pumpkin seeds → 50 mB** each @ 1,600 RF;
fill/empty bucket @ 800 RF.

### B2. `Crude Oil` — swimming in it now has effects (#848)

While the player's **eyes are submerged**: **Nausea**, **Poison**, and **Slowness III**
(re-applied every 3 s, 5 s duration, fading after surfacing), plus obscured vision/fog. Nothing on
the page. Also needs a v0.8.7 History entry, and the Transposer as a fill/empty route.

### B3. Cauldrons trade fluid with pipes (#885)

A powered **Fluid Extractor Pipe drains a cauldron**, and an **Insertion Fluid Pipe fills one**.
Cauldrons are all-or-nothing: lava = 1,000 mB, water = 333⅓ mB per level. The word "cauldron"
appears nowhere in the wiki except an Alloy Smelter recycling row. Needs coverage on
`Fluid Extractor Pipe`, `Insertion Fluid Pipe`, and probably `Fluid Pipes`.

### B4. `Transposer` — 48 data-driven recipes, none tabulated

The page defers to JEI ("check JEI for the full list"), which breaks the house rule that every
recipe uses a template. There is no `{{Transposer}}` recipe-template family yet — this needs a new
wrapper + `Module` + `{{Grid Transposer Table}}` widget + styles. The recipe set:

* fill/empty buckets — water, lava, and all 8 mod fluids (800 RF, 1,000 mB)
* concrete powder → concrete, 16 colours (400 RF, 1,000 mB water)
* mossy cobblestone / mossy stone bricks (4,000 RF, 250 mB water)
* sponge ⇄ wet sponge (1,600 RF, 1,000 mB water)
* cactus → 500 mB water (2,400 RF)
* seeds → 50 mB seed oil, 4 seed types (1,600 RF)
* **sand / red sand / gravel + 1,000 mB Crude Oil → Oil Sand / Oil Red Sand / Oil Shale** (4,000 RF)

### B5. Petroleum blocks are now craftable (#851)

B4's last row closes the oil loop — `Oil Sand`, `Oil Red Sand`, `Oil Shale` gained an Obtaining
route. Those three pages currently list worldgen only.

### B6. `Quicksilver` / `Alloy Smelter` — raw ore now accepted (#840)

Three new amalgamation recipes, missing from both pages:

| Input | Flux | Output | Byproduct |
|---|---|---|---|
| `#c:raw_materials/copper` | Quicksilver | Copper Ingot ×3 | Gold Ingot 100% |
| `#c:raw_materials/gold` | Quicksilver | Gold Ingot ×3 | Rich Slag 75% |
| `#c:raw_materials/tin` | Quicksilver | Tin Ingot ×3 | Iron Ingot 100% |

### B7. `Battery` — now requires a pickaxe (#839)

Page says *"can be broken with any tool"* with `tool=none |tier=none`. The block gained
`requiresCorrectToolForDrops()` and joined `mineable/pickaxe` (no `needs_*_tool` tag → **any**
pickaxe tier). Needs the prose, the `{{Breaking}}` tier, and a History entry.

Cables were also retuned in the same PR: hardness **1.5 → 0.3**. The cable pages carry no infobox
hardness at all, so nothing is *wrong*, but it is worth recording.

### B8. Smaller v0.8.7 items with no coverage

* Engine fuels now show in the recipe browser; Catalyst Engine and (on Fabric) Reaction Engine
  gained JEI categories (#874, #886) — `Engines` / engine pages.
* Fluid pipes honour transfer rates configured above the default (#885).
* Crucible progress gauge restyled as a droplet (#834) — History line only.
* Shared `#c:gears/*` recipe tags (#871) — mod-interop; a line on `Gears` at most.

---

## C. Missing recipe families (predate v0.8.7 but never documented)

### C1. `Macerator` — whole categories absent

* **Concrete → concrete powder**, all 16 colours (4,000 RF)
* **Wool → String ×4** (3,000 RF)
* **Obsidian / Crying Obsidian → Obsidian Dust**; **Netherite Ingot → Netherite Dust**;
  **Ender Pearl → Ender Dust** — three mod dusts with no row on the machine page
* **Clay → Clay Ball ×4**, **Terracotta → Clay Ball ×4**, **Bricks → Brick ×4**,
  **Flower Pot → Brick**, **Nether Bricks → Nether Brick ×4**
* **Note Block → Redstone** (+ Sawdust), **Jukebox → Diamond** (+ Sawdust)
* **Raw Tin ×2 / Raw Tin Block ×9 / Tin Block ×9 / Bronze Block ×9**
* **Prismarine → Prismarine Dust ×4** (only the shard and bricks rows exist)
* **Diamond / Emerald / Lapis Lazuli → 1 dust each**

The page's "Flowers → dyes" prose covers the 17 dye recipes adequately; the rest need rows.

### C2. `Alloy Smelter` — missing rows

* **Rubber**: Natural Polymer + Sulfur Dust → Rubber ×2, and Synthetic Polymer + Sulfur Dust →
  Rubber ×2 (1,000 RF) — absent from the page entirely
* **Bronze Dust + sand → Bronze Ingot** (+25% Slag)
* Rich Slag rows for **copper / gold / tin** (only the iron row is shown; prose covers the rule)

### C3. `Crucible` — intro is wrong *(correctness, not just a gap — do with the quick wins)*

*"turns ore blocks … into molten metals"* — there are no molten-metal recipes. Outputs are Liquid
Redstone / Glowstone / Ender / Biomass, Crude Oil, lava, and water. All 16 recipe rows are correct.

### C4. The `classic_crafting` built-in resource pack is undocumented

16 alternate pipe/module recipes ship in `resourcepacks/classic_crafting/`. No page mentions it.

---

## D. Naming mismatches — grid icons and links silently fall back to text

`{{Grid}}` / `Module:ItemLink` are keyed on the **in-game display name**. These wiki spellings are
not in `Module:ItemLink`, so icons and links quietly degrade to plain text:

| Wiki uses | In-game name | Where |
|---|---|---|
| `Chassis Logistics Pipe MK1`…`MK5` | `Chassis Logistics Pipe MkI`…`MkV` | Chassis page, 5 chipset pages, `Logistics Pipes`, `main`, Navbox |
| `Pump` | `Fluid Pump` | `Pump` page (title + grids) |
| `Rubber Chunk` | `Rubber` | `Ender Dust`, `Rubber` |
| `Item Sink Module` | `ItemSink Module` | `Item Sink Module` |
| `Mod Item Sink Module` | `Mod-Based ItemSink Module` | `Mod Item Sink Module` |
| `Polymorphic Sink Module` | `Polymorphic ItemSink Module` | `Polymorphic Sink Module` |
| `Quicksort Module` | `QuickSort Module` | `Quicksort Module` |
| `Apatite Block` / `Bronze Block` / `Tin Block` / `Raw Tin Block` | `Block of Apatite` / `Block of Bronze` / `Block of Tin` / `Block of Raw Tin` | grid `Output=` on those pages |

Decide per row whether to rename the page/grid or add an alias to `Module:ItemLink` — the module is
generated by `gen_item_links.py`, so aliases need to survive a regen.

---

## E. Housekeeping

* ~~`Sturdy Casing` and `Tin Gear` miscategorised as `Materials`~~ — ✅ done in `de694210`.
* `Deepslate Tin Ore` and `Creative Sink` have no page (Deepslate Tin Ore is covered on `Tin`;
  `Creative Sink` is referenced only from `Module:ItemLink`).
* `Fluid Pump` has no `{{Breaking}}` table despite needing a stone pickaxe.
* Creative-menu domain tabs (v0.8.3 #738) — four tabs (`Logistics Resources / Pipes / Power /
  Automation`) are not described anywhere.

---

## F. Hand-built recipe wikitables (new finding — needs a decision)

House rule: *"All recipes use the recipe templates, never wikitables."* Two content pages still
hand-build one:

* **`Reaction Engine`** — a `{| class="wikitable"` of Reactant / Reagent / Energy / Time. The values
  are correct against the data, but it is a recipe table and now appears in JEI, so it should be a
  `{{Reaction}}` family (wrapper + `Module` + grid widget + styles), like the Transposer one.
* **`Fuel Engine`** — Fuel / Energy per bucket / Heat. More a properties table than a recipe grid,
  so this one is arguably fine as-is; worth a decision either way.

Not started — building another template family is its own task, not part of section B.

---

## Verified clean

Checks that came back with no findings, so they don't need re-doing:

* **Hardness / blast resistance** — every infobox value matches the Java registration.
* **Harvest tiers** — all `{{Breaking|tier=}}` values match the `needs_*_tool` tags except Battery (B7).
* **`{{ID}}` translation keys** — all resolve in `en_us.json` except 21 keys on removed-item pages
  (expected) and the two Category:Materials strays in E.
* **Fuel Engine** fuel table (40k / 80k / 150k RF per bucket) matches `FuelEngineFuels`.
* **Refinery**, **Sequential Fabricator** (all 7 chipsets + RF costs), and **Crucible** recipe
  tables match the data exactly.
* Machine crafting grids match, apart from the four in A4.
* Item coverage: every registered block/item/fluid has a page or is covered on a parent page,
  except Seed Oil (B1).
