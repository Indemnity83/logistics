<div class="infobox">
    <div class="infobox-header">Iron Valve</div>
    <div class="infobox-image">
        <img src="/assets/icons/logistics__core___valve_iron.png" alt="Iron Valve">
    </div>
    <table class="infobox-table">
        <tr>
            <td class="infobox-label">ID</td>
            <td class="infobox-value"><code>logistics:core/valve_iron</code></td>
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

# Iron Valve

The **Iron Valve** is a Tier 2 valve crafted in the [Kiln](../automation/kiln.md). It pushes coal to its capacity limit at 100 energy/tick.

## Recipe

<div class="crafting-recipe">
    <div class="crafting-grid">
        <div class="crafting-slot"></div>
        <div class="crafting-slot"><img src="/assets/icons/minecraft__iron_ingot.png" class="crafting-item" alt="Iron Ingot"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"><img src="/assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone"></div>
        <div class="crafting-slot"><img src="/assets/icons/minecraft__iron_ingot.png" class="crafting-item" alt="Iron Ingot"></div>
        <div class="crafting-slot"><img src="/assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone"></div>
        <div class="crafting-slot"><img src="/assets/icons/minecraft__iron_ingot.png" class="crafting-item" alt="Iron Ingot"></div>
        <div class="crafting-slot"><img src="/assets/icons/minecraft__iron_ingot.png" class="crafting-item" alt="Iron Ingot"></div>
        <div class="crafting-slot"><img src="/assets/icons/minecraft__iron_ingot.png" class="crafting-item" alt="Iron Ingot"></div>
    </div>
    <div class="crafting-arrow">→</div>
    <div class="crafting-output">
        <img src="/assets/icons/logistics__core___valve_iron.png" class="crafting-item" alt="Iron Valve">
        <span class="crafting-count">4</span>
    </div>
</div>

**[Kiln](../automation/kiln.md) crafting:**
- 5× Iron Ingot + 2× Redstone + 250mb Molten Glass
- **Process time:** 140 ticks
- **Energy demand:** 100 energy/tick
- **Yields:** 4× Iron Valve

## Fuel Requirements

**Tier 2 - Coal at limit**

Energy demand: 100 energy/tick
- **Coal:** Works but at near capacity (~110 observed)
- **Blaze Rod:** Comfortable margin
- **Lava Bucket:** Overkill but works

Coal barely sustains this recipe. Consider upgrading fuel for smoother operation.

## Usage

Valves are crafting components for future features. Iron valves mark the transition point where coal begins to struggle.

## Tips

- Coal works but runs near capacity - may see temperature fluctuations
- Blaze rods or lava give smoother operation
- Yields 4 valves per craft
- First valve where fuel choice really matters

## See Also
- [Kiln](../automation/kiln.md) - Crafting machine
- [Tin Valve](valve-tin.md) - Lower energy alternative (60 energy/tick)
- [Bronze Valve](valve-bronze.md) - Next tier up (140 energy/tick, coal fails)
