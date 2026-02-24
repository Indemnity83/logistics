<div class="infobox">
    <div class="infobox-header">Laser Quarry</div>
    <div class="infobox-image">
        <img src="../../assets/icons/logistics__automation___laser_quarry.png" alt="Laser Quarry">
    </div>
    <table class="infobox-table">
        <tr>
            <td class="infobox-label">ID</td>
            <td class="infobox-value"><code>logistics:automation/laser_quarry</code></td>
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
            <td class="infobox-value">Automated mining</td>
        </tr>
        <tr>
            <td class="infobox-label">Power</td>
            <td class="infobox-value">RF Energy</td>
        </tr>
        <tr>
            <td class="infobox-label">Mining Area</td>
            <td class="infobox-value">16×16</td>
        </tr>
        <tr>
            <td class="infobox-label">Added</td>
            <td class="infobox-value">v0.1.0</td>
        </tr>
    </table>
</div>

# Laser Quarry

The **Laser Quarry** is a powered mining machine that automatically excavates an area below it down to bedrock. It builds its own frame and mines continuously when powered with RF energy. By default it mines a 16×16 area, but you can use markers to define custom boundaries of any size.

## Recipe

<div class="crafting-recipe">
    <div class="crafting-grid">
        <div class="crafting-slot"><img src="../../assets/icons/logistics__core___iron_gear.png" class="crafting-item" alt="Iron Gear"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__core___iron_gear.png" class="crafting-item" alt="Iron Gear"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__core___gold_gear.png" class="crafting-item" alt="Gold Gear"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__core___copper_gear.png" class="crafting-item" alt="Copper Gear"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__core___gold_gear.png" class="crafting-item" alt="Gold Gear"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__core___diamond_gear.png" class="crafting-item" alt="Diamond Gear"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__diamond_pickaxe.png" class="crafting-item" alt="Diamond Pickaxe"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__core___diamond_gear.png" class="crafting-item" alt="Diamond Gear"></div>
    </div>
    <div class="crafting-arrow">→</div>
    <div class="crafting-output">
        <img src="../../assets/icons/logistics__automation___laser_quarry.png" class="crafting-item" alt="Laser Quarry">
    </div>
</div>

**Yields:** 1× Laser Quarry (requires [Iron Gear](../materials/iron-gear.md), [Copper Gear](../materials/copper-gear.md), [Gold Gear](../materials/gold-gear.md), [Diamond Gear](../materials/diamond-gear.md))

## Behavior

The Laser Quarry automatically mines an area below itself. When placed and powered, it:

1. **Builds frame** - Constructs mining frame structure around the area
2. **Mines downward** - Excavates layer by layer to bedrock
3. **Outputs items** - Mined blocks output from the top of the quarry
4. **Runs indefinitely** - Continues until it reaches bedrock

**Key features:**
- Automatic frame construction
- Default 16×16 mining area (customizable with markers)
- Requires RF power to operate
- Mining speed scales with power throughput
- Items output from top (into chest or pipe)
- No fuel needed - only RF energy

## Mining Area

**Default (no markers):**
- Mines a 16×16 area
- Centered on the quarry's placement

**Custom area (with markers):**
1. Place markers to outline desired mining area
2. Place quarry adjacent to any side of the marked boundary
3. Markers break and drop when quarry activates
4. Quarry uses marked boundary for mining
5. Can create any size area (up to marker limits)

This allows you to define exactly what area to mine, avoiding buildings, important terrain, or other quarries.

## Setup

**Basic setup (16×16 default):**
1. **Place quarry** at desired location (mining happens below)
2. **Connect power** - Attach [Stirling Engine](../power/stirling-engine.md) or other RF source
3. **Connect collection** - Place chest or pipe on top to receive items
4. **Provide power** - Ensure engine has fuel and redstone signal
5. **Quarry runs automatically** - Frame builds, then mining begins

**Custom area setup (with markers):**
1. **Place markers** - Outline the area you want to mine
2. **Place quarry** - Adjacent to any side of the marker boundary
3. **Markers break** - Quarry consumes markers and uses that boundary
4. **Connect power and collection** - Same as basic setup
5. **Quarry mines custom area** - Uses marked boundaries instead of default 16×16

## Power Requirements

The Laser Quarry consumes RF energy continuously while mining:

- **Power source:** [Stirling Engine](../power/stirling-engine.md) recommended
- **Power scaling:** More RF throughput = faster mining
- **No operation without power** - Quarry stops if power is interrupted

Connect the engine's output face directly to the quarry.

## Item Collection

Mined items are **output from the top of the quarry**:

**Collection options:**

**With chest:**
```
     [Chest]
        ↑
  [Laser Quarry]
```

**With pipes:**
```
[Copper Transport Pipe] → [Filter Pipe] → [Storage]
         ↑
  [Laser Quarry]
```

**Tips for collection:**
- Place chest on top for simple storage
- Or connect any pipe on top (no extractor needed)
- Use [Item Filter Pipes](../pipes/item-filter-pipe.md) to sort valuable ores
- Consider [Item Void Pipe](../pipes/item-void-pipe.md) for cobblestone/dirt overflow
- Items output directly into connected inventory/pipe

## Tips

- Mining speed depends on RF throughput from engine
- Well-fed quarry mines faster than power-starved one
- Frame builds automatically - no manual construction
- Default 16×16 area - use markers for custom sizes
- Use markers to avoid mining your base or other structures
- Markers are consumed when quarry activates (you get them back)
- Mines all blocks (stone, ores, dirt, gravel, etc.)
- Set up item sorting before starting large-scale mining
- Multiple quarries can run simultaneously with separate power
- Quarry stops at bedrock

## Common Setup

**Complete quarry system:**
```
[Stirling Engine] → [Laser Quarry]
                         ↑ (outputs to top)
                    [Transport Pipe]
                         ↓
                    [Filter Pipe] → [Ores] → [Storage]
                         ↓
                    [Void Pipe] ← [Cobblestone/dirt]
```

## See Also
- [Stirling Engine](../power/stirling-engine.md) - Recommended power source
- [RF Energy](../power/rf-energy.md) - Power system
- [Copper Transport Pipe](../pipes/copper-transport-pipe.md) - Collect mined items
- [Item Filter Pipe](../pipes/item-filter-pipe.md) - Sort ores from waste
- [Item Void Pipe](../pipes/item-void-pipe.md) - Delete cobblestone overflow
- [Iron Gear](../materials/iron-gear.md) - Crafting component
- [Copper Gear](../materials/copper-gear.md) - Crafting component
- [Diamond Gear](../materials/diamond-gear.md) - Crafting component
