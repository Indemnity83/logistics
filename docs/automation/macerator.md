<div class="infobox">
    <div class="infobox-header">Macerator</div>
    <div class="infobox-image">
        <img src="../../assets/icons/logistics__core___macerator.png" alt="Macerator" title="Macerator">
    </div>
    <table class="infobox-table">
        <tr>
            <td class="infobox-label">ID</td>
            <td class="infobox-value"><code>logistics:core/macerator</code></td>
        </tr>
        <tr>
            <td class="infobox-label">Type</td>
            <td class="infobox-value">Block (Machine)</td>
        </tr>
        <tr>
            <td class="infobox-label">Stackable</td>
            <td class="infobox-value"><span class="stackable-yes">Yes (64)</span></td>
        </tr>
        <tr>
            <td class="infobox-label">Function</td>
            <td class="infobox-value">Grinding / dust production</td>
        </tr>
        <tr>
            <td class="infobox-label">Power</td>
            <td class="infobox-value">RF Energy</td>
        </tr>
        <tr>
            <td class="infobox-label">Added</td>
            <td class="infobox-value">v0.5.0</td>
        </tr>
    </table>
</div>

# Macerator

The **Macerator** is an RF-powered grinding machine that converts items into [dust](../materials/dusts.md) and other ground materials. Dusts are used as crafting ingredients for [modules](../modules/index.md) and can be smelted into ingots for higher ore yields.

## Recipe

<div class="crafting-recipe">
    <div class="crafting-grid">
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__flint.png" class="crafting-item" alt="Flint" title="Flint"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__flint.png" class="crafting-item" alt="Flint" title="Flint"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__flint.png" class="crafting-item" alt="Flint" title="Flint"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__iron_ingot.png" class="crafting-item" alt="Iron Ingot" title="Iron Ingot"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__core___machine_core.png" class="crafting-item" alt="Machine Core" title="Machine Core"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__iron_ingot.png" class="crafting-item" alt="Iron Ingot" title="Iron Ingot"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__iron_ingot.png" class="crafting-item" alt="Iron Ingot" title="Iron Ingot"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__iron_ingot.png" class="crafting-item" alt="Iron Ingot" title="Iron Ingot"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__iron_ingot.png" class="crafting-item" alt="Iron Ingot" title="Iron Ingot"></div>
    </div>
    <div class="crafting-arrow">→</div>
    <div class="crafting-output">
        <img src="../../assets/icons/logistics__core___macerator.png" class="crafting-item" alt="Macerator" title="Macerator">
        <span class="crafting-count">1</span>
    </div>
</div>

**Yields:** 1× Macerator (requires [Machine Core](../materials/machine-core.md), 5× Iron Ingot, 3× Flint)

## Behavior

The Macerator grinds input items into dust over 200 ticks (10 seconds). Place an item in the input slot; the output slot receives the ground result when complete.

**Key features:**
- Input from top and sides; output from bottom
- Processes 70+ recipes covering ores, raw metals, plants, and more
- Grants XP when processing ore-based recipes
- Energy capacity: 10,000 RF; max input: 128 RF/tick; consumption: 10 RF/tick while active
- Recipe book integration — browse all valid recipes in the GUI

## Setup

1. **Place the Macerator** in your processing area
2. **Connect power** — attach a [Stirling Engine](../power/stirling-engine.md) or any RF source
3. **Feed input** — pipe items into the top or sides
4. **Collect output** — pipe from the bottom into storage or directly to a [Kiln](kiln.md)

## Recipes

### Ores → Dust

Grinding ores produces more dust than grinding the ingot directly, making the Macerator a key part of an efficient ore processing chain.

| Input | Output | Count |
|-------|--------|-------|
| Iron Ore / Deepslate Iron Ore | Iron Dust | 2 |
| Raw Iron | Iron Dust | 1 |
| Iron Block | Iron Dust | 9 |
| Copper Ore / Deepslate Copper Ore | Copper Dust | 2 |
| Raw Copper | Copper Dust | 1 |
| Copper Block | Copper Dust | 9 |
| Gold Ore / Deepslate Gold Ore / Nether Gold Ore | Gold Dust | 2 |
| Raw Gold | Gold Dust | 1 |
| Gold Block | Gold Dust | 9 |
| Tin Ore / Deepslate Tin Ore | Tin Dust | 2 |
| Tin Ingot | Tin Dust | 1 |
| Bronze Ingot | Bronze Dust | 1 |
| Diamond Ore / Deepslate Diamond Ore | Diamond Dust | 2 |
| Diamond Block | Diamond Dust | 9 |
| Emerald Ore / Deepslate Emerald Ore | Emerald Dust | 2 |
| Emerald Block | Emerald Dust | 9 |
| Lapis Ore / Deepslate Lapis Ore | Lapis Dust | 4 |
| Lapis Block | Lapis Dust | 9 |
| Coal Ore / Deepslate Coal Ore | Coal Dust | 2 |
| Coal | Coal Dust | 2 |
| Charcoal | Coal Dust | 2 |
| Redstone Ore / Deepslate Redstone Ore | Redstone | 4 |
| Nether Quartz / Quartz Ore | Quartz Dust | 2 |
| Quartz Block | Quartz Dust | 4 |
| Apatite Ore | Apatite Dust | 6 |
| Amethyst Shard | Amethyst Dust | 1 |
| Echo Shard | Echo Dust | 1 |
| Prismarine Shard | Prismarine Dust | 1 |
| Prismarine Bricks | Prismarine Dust | 4 |

### Plants & Organics

| Input | Output | Count |
|-------|--------|-------|
| Wheat | Flour | 1 |
| Log (any type) | Wood Pulp | 8 |
| Planks (any type) | Wood Pulp | 2 |
| Stick | Wood Pulp | 1 |
| Bone | Bone Meal | 6 |
| Blaze Rod | Blaze Powder | 4 |
| Glowstone | Glowstone Dust | 4 |

### Flowers → Dyes

Any flower can be ground into its corresponding dye color (Poppy → Red Dye, Dandelion → Yellow Dye, etc.). Supports all 16 vanilla flower variants including multi-block plants (Lilac, Sunflower, etc.).

## Common Uses

**Ore doubling:**
```
[Ore] → Macerator → [Dust × 2] → Kiln → [Ingot × 2]
```
Grinding an ore block yields 2 dust, each smeltable into 1 ingot — effectively doubling your ore output compared to direct smelting.

**Module crafting:**
```
[Iron Ore] → Macerator → [Iron Dust] → craft Provider Module
[Emerald] → Macerator → [Emerald Dust] → craft Supplier Module
```

**Quartz Crystal production (required for Valves):**
```
[Nether Quartz] → Macerator → [Quartz Dust] → Kiln → [Quartz Crystal]
```

## Tips

- At 10 RF/tick the Macerator uses more power than the [Kiln](kiln.md) — ensure your engine can sustain it
- Pair with the Kiln for a full ore processing line: Macerator doubles the dust, Kiln smelts it
- Quartz Dust is critical for [Valve](../materials/index.md#valves) crafting — set up a Macerator early to build a stock
- Wood Pulp from logs is used in [Wooden Cores](../materials/cores.md) and [Extractor Modules](../modules/extractor-module.md)
- Flour from wheat can be used to bake bread and other food items

## See Also
- [Machine Core](../materials/machine-core.md) - Crafting component
- [Kiln](kiln.md) - Companion smelting machine
- [Dusts](../materials/dusts.md) - All dust types and their uses
- [Stirling Engine](../power/stirling-engine.md) - Recommended power source
- [RF Energy](../power/rf-energy.md) - Power system overview
- [Valves](../materials/index.md#valves) - Require Quartz Crystal from Macerator → Kiln chain
- [Modules](../modules/index.md) - Consume dusts as crafting ingredients
