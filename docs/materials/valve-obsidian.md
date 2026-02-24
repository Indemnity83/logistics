<div class="infobox">
    <div class="infobox-header">Obsidian Valve</div>
    <div class="infobox-image">
        <img src="/assets/icons/logistics__core___valve_obsidian.png" alt="Obsidian Valve">
    </div>
    <table class="infobox-table">
        <tr>
            <td class="infobox-label">ID</td>
            <td class="infobox-value"><code>logistics:core/valve_obsidian</code></td>
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
            <td class="infobox-value"><span class="infobox-tier infobox-tier-6">Tier 6</span></td>
        </tr>
        <tr>
            <td class="infobox-label">Added</td>
            <td class="infobox-value">v0.2.0</td>
        </tr>
    </table>
</div>

# Obsidian Valve

The **Obsidian Valve** is a Tier 6 valve crafted in the [Kiln](../automation/kiln.md). It has extreme energy demand that exceeds lava bucket capacity.

## Recipe

<div class="crafting-recipe">
    <div class="crafting-grid">
        <div class="crafting-slot"></div>
        <div class="crafting-slot"><img src="/assets/icons/minecraft__obsidian.png" class="crafting-item" alt="Obsidian"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"><img src="/assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone"></div>
        <div class="crafting-slot"><img src="/assets/icons/minecraft__obsidian.png" class="crafting-item" alt="Obsidian"></div>
        <div class="crafting-slot"><img src="/assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone"></div>
        <div class="crafting-slot"><img src="/assets/icons/minecraft__obsidian.png" class="crafting-item" alt="Obsidian"></div>
        <div class="crafting-slot"><img src="/assets/icons/minecraft__obsidian.png" class="crafting-item" alt="Obsidian"></div>
        <div class="crafting-slot"><img src="/assets/icons/minecraft__obsidian.png" class="crafting-item" alt="Obsidian"></div>
    </div>
    <div class="crafting-arrow">→</div>
    <div class="crafting-output">
        <img src="/assets/icons/logistics__core___valve_obsidian.png" class="crafting-item" alt="Obsidian Valve">
        <span class="crafting-count">4</span>
    </div>
</div>

**[Kiln](../automation/kiln.md) crafting:**
- 5× Obsidian + 2× Redstone + 250mb Molten Glass
- **Process time:** 180 ticks
- **Energy demand:** 360 energy/tick
- **Yields:** 4× Obsidian Valve

## Fuel Requirements

**Tier 6 - Lava struggles**

Energy demand: 360 energy/tick
- **Lava Bucket:** **Insufficient** (~315 capacity, temperature drops)

Even lava buckets cannot sustain this energy demand. Temperature will drop, crafting will pause and resume cyclically.

## Usage

Valves are crafting components for future features. Obsidian valves will complete but very slowly with current fuels.

## Tips

- Exceeds lava capacity - expect temperature drops
- Crafting pauses when temperature falls, resumes when recovered
- Will eventually complete, just slowly
- Future better fuels may smooth operation

## See Also
- [Kiln](../automation/kiln.md) - Crafting machine
- [Lapis Valve](valve-lapis.md) - Previous tier (320 energy/tick)
- [Netherite Valve](valve-netherite.md) - Next tier (400 energy/tick)
- [Blazing Valve](valve-blazing.md) - Maximum tier (440 energy/tick)
