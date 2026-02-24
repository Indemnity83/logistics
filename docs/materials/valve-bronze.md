<div class="infobox">
    <div class="infobox-header">Bronze Valve</div>
    <div class="infobox-image">
        <img src="/assets/icons/logistics__core___valve_bronze.png" alt="Bronze Valve">
    </div>
    <table class="infobox-table">
        <tr>
            <td class="infobox-label">ID</td>
            <td class="infobox-value"><code>logistics:core/valve_bronze</code></td>
        </tr>
        <tr>
            <td class="infobox-label">Type</td>
            <td class="infobox-value">Item</td>
        </tr>
        <tr>
            <td class="infobox-label">Stackable</td>
            <td class="infobox-value"><span class="stackable-yes">Yes (64)</span></td>
        </tr>
        <tr>
            <td class="infobox-label">Kiln Tier</td>
            <td class="infobox-value"><span class="infobox-tier infobox-tier-2">Tier 2</span></td>
        </tr>
        <tr>
            <td class="infobox-label">Added</td>
            <td class="infobox-value">v0.2.0</td>
        </tr>
    </table>
</div>

# Bronze Valve

The **Bronze Valve** is a Tier 2 valve crafted in the [Kiln](../automation/kiln.md). It requires [bronze ingots](bronze-ingot.md) and exceeds coal's capacity - upgrade fuel is needed.

## Recipe

<div class="crafting-recipe">
    <div class="crafting-grid">
        <div class="crafting-slot"></div>
        <div class="crafting-slot"><img src="/assets/icons/logistics__core___bronze_ingot.png" class="crafting-item" alt="Bronze Ingot"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"><img src="/assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone"></div>
        <div class="crafting-slot"><img src="/assets/icons/logistics__core___bronze_ingot.png" class="crafting-item" alt="Bronze Ingot"></div>
        <div class="crafting-slot"><img src="/assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone"></div>
        <div class="crafting-slot"><img src="/assets/icons/logistics__core___bronze_ingot.png" class="crafting-item" alt="Bronze Ingot"></div>
        <div class="crafting-slot"><img src="/assets/icons/logistics__core___bronze_ingot.png" class="crafting-item" alt="Bronze Ingot"></div>
        <div class="crafting-slot"><img src="/assets/icons/logistics__core___bronze_ingot.png" class="crafting-item" alt="Bronze Ingot"></div>
    </div>
    <div class="crafting-arrow">→</div>
    <div class="crafting-output">
        <img src="/assets/icons/logistics__core___valve_bronze.png" class="crafting-item" alt="Bronze Valve">
        <span class="crafting-count">4</span>
    </div>
</div>

**[Kiln](../automation/kiln.md) crafting:**
- 5× [Bronze Ingot](bronze-ingot.md) + 2× Redstone + 250mb Molten Glass
- **Process time:** 120 ticks
- **Energy demand:** 140 energy/tick
- **Yields:** 4× Bronze Valve

## Fuel Requirements

**Tier 2 - Coal insufficient**

Energy demand: 140 energy/tick
- **Coal:** **Fails** (only ~110 capacity, temperature will drop)
- **Blaze Rod:** Works (~20-25 capacity)
- **Lava Bucket:** Comfortable margin

Blaze rods or lava buckets required.

## Usage

Valves are crafting components for future features. Bronze valves require creating bronze alloy from copper and tin.

## Tips

- **Coal will not sustain** - temperature drops, crafting pauses
- Upgrade to blaze rods or lava buckets
- Requires bronze ingots (copper + tin alloy)
- Clear fuel progression gate

## See Also
- [Kiln](../automation/kiln.md) - Crafting machine
- [Bronze Ingot](bronze-ingot.md) - Required alloy material
- [Iron Valve](valve-iron.md) - Lower energy (100, coal barely works)
- [Gold Valve](valve-gold.md) - Next tier up (180 energy/tick)
