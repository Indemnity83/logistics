<div class="infobox">
    <div class="infobox-header">Kiln</div>
    <div class="infobox-image">
        <img src="../../assets/icons/logistics__core___kiln.png" alt="Kiln" title="Kiln">
    </div>
    <table class="infobox-table">
        <tr>
            <td class="infobox-label">ID</td>
            <td class="infobox-value"><code>logistics:core/kiln</code></td>
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
            <td class="infobox-value">RF-powered smelting</td>
        </tr>
        <tr>
            <td class="infobox-label">Power</td>
            <td class="infobox-value">RF Energy</td>
        </tr>
        <tr>
            <td class="infobox-label">Added</td>
            <td class="infobox-value">v0.2.0</td>
        </tr>
    </table>
</div>

# Kiln

The **Kiln** is an RF-powered electric furnace that processes any vanilla smelting recipe. It works like a furnace but uses RF energy instead of fuel, and is faster and more efficient for automated setups.

## Recipe

<div class="crafting-recipe">
    <div class="crafting-grid">
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone" title="Redstone"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone" title="Redstone"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone" title="Redstone"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__iron_ingot.png" class="crafting-item" alt="Iron Ingot" title="Iron Ingot"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__core___machine_core.png" class="crafting-item" alt="Machine Core" title="Machine Core"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__iron_ingot.png" class="crafting-item" alt="Iron Ingot" title="Iron Ingot"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__iron_ingot.png" class="crafting-item" alt="Iron Ingot" title="Iron Ingot"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__iron_ingot.png" class="crafting-item" alt="Iron Ingot" title="Iron Ingot"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__iron_ingot.png" class="crafting-item" alt="Iron Ingot" title="Iron Ingot"></div>
    </div>
    <div class="crafting-arrow">→</div>
    <div class="crafting-output">
        <img src="../../assets/icons/logistics__core___kiln.png" class="crafting-item" alt="Kiln" title="Kiln">
        <span class="crafting-count">1</span>
    </div>
</div>

**Yields:** 1× Kiln (requires [Machine Core](../materials/machine-core.md), 5× Iron Ingot, 3× Redstone)

## Behavior

The Kiln is a direct replacement for a vanilla furnace in automated setups. Place an item in the input slot and the Kiln will smelt it using RF energy, depositing the result in the output slot.

**Key features:**
- Processes any vanilla smelting recipe (ores, raw metals, food, etc.)
- Input from top and sides; output from bottom (same as a vanilla furnace)
- Uses RF energy — no fuel slot
- Recipe book integration — browse all valid recipes in the GUI
- Energy capacity: 10,000 RF; max input: 128 RF/tick; consumption: 1 RF/tick while active

## Setup

1. **Place the Kiln** in your automation area
2. **Connect power** — attach a [Stirling Engine](../power/stirling-engine.md) or any RF source to a side or top face
3. **Feed input** — pipe items in from the top or sides (e.g., use an [Item Extractor Pipe](../pipes/item-extractor-pipe.md) on a chest)
4. **Collect output** — pipe from the bottom into storage

The Kiln will smelt continuously as long as it has power and input items.

## Uses

The Kiln is particularly useful for:
- Smelting ore → ingot in automated pipelines
- Processing raw metals extracted by the [Laser Quarry](laser-quarry.md)
- Smelting [Quartz Dust](../materials/dusts.md) into [Quartz Crystal](../materials/cores.md#quartz-crystal) (required for [Valve](../materials/index.md#valves) crafting)
- Smelting [Silicon Mix](../materials/chips.md#silicon-wafer) into [Silicon Wafer](../materials/chips.md#silicon-wafer) (required for [Chips](../materials/chips.md))

## Tips

- At only 1 RF/tick, the Kiln is very energy-efficient — a single Stirling Engine can power many kilns simultaneously
- Connect with pipes to fully automate: extract raw materials → kiln → output storage
- Use the recipe book in the GUI to verify which items the Kiln can process
- The [Macerator](macerator.md) pairs well with the Kiln: grind ores into dust in the Macerator, then smelt dust into ingots in the Kiln for higher yields

## See Also
- [Machine Core](../materials/machine-core.md) - Crafting component
- [Macerator](macerator.md) - Companion grinding machine
- [Stirling Engine](../power/stirling-engine.md) - Recommended power source
- [RF Energy](../power/rf-energy.md) - Power system overview
- [Quartz Crystal](../materials/cores.md#quartz-crystal) - Key product; smelted from Quartz Dust in the Kiln
- [Silicon Wafer](../materials/chips.md#silicon-wafer) - Key product; smelted from Silicon Mix in the Kiln
- [Valves](../materials/index.md#valves) - Require Quartz Crystal from the Kiln
