# Laser Quarry

The **Laser Quarry** is a powered mining machine that automatically excavates a 16×16 area below it down to bedrock. It builds its own frame and mines continuously when powered with RF energy.

## Recipe
**Crafting:**
- [Iron Gear](../materials/iron-gear.md)
- [Copper Gear](../materials/copper-gear.md)
- [Diamond Gear](../materials/diamond-gear.md)
- 1× Diamond Pickaxe
- 1× Redstone
- **Yields:** 1× Laser Quarry

## Behavior

The Laser Quarry automatically mines a 16×16 area centered below itself. When placed and powered, it:

1. **Builds frame** - Constructs mining frame structure around the area
2. **Mines downward** - Excavates layer by layer to bedrock
3. **Drops items** - Mined blocks drop above the quarry
4. **Runs indefinitely** - Continues until it reaches bedrock

**Key features:**
- Automatic frame construction
- 16×16 mining area
- Requires RF power to operate
- Mining speed scales with power throughput
- Mined items drop above the quarry (collect with pipes)
- No fuel needed - only RF energy

## Setup

1. **Place quarry** at desired location (mining happens below)
2. **Connect power** - Attach [Stirling Engine](../power/stirling-engine.md) or other RF source
3. **Connect collection** - Place [Item Extractor Pipe](../pipes/item-extractor-pipe.md) above to collect drops
4. **Provide power** - Ensure engine has fuel and redstone signal
5. **Quarry runs automatically** - Frame builds, then mining begins

## Power Requirements

The Laser Quarry consumes RF energy continuously while mining:

- **Power source:** [Stirling Engine](../power/stirling-engine.md) recommended
- **Power scaling:** More RF throughput = faster mining
- **No operation without power** - Quarry stops if power is interrupted

Connect the engine's output face directly to the quarry.

## Item Collection

Mined items drop as entities **above the quarry**:

**Collection pattern:**
```
[Item Extractor Pipe] → [Copper Transport Pipe] → [Storage system]
         ↑
  [Laser Quarry] (items drop here)
```

**Tips for collection:**
- Place [Item Extractor Pipe](../pipes/item-extractor-pipe.md) on top of quarry
- Connect to [Copper Transport Pipes](../pipes/copper-transport-pipe.md)
- Use [Item Filter Pipes](../pipes/item-filter-pipe.md) to sort valuable ores
- Consider [Item Void Pipe](../pipes/item-void-pipe.md) for cobblestone/dirt overflow

## Tips

- Mining speed depends on RF throughput from engine
- Well-fed quarry mines faster than power-starved one
- Frame builds automatically - no manual construction
- 16×16 area is fixed size
- Mines all blocks (stone, ores, dirt, gravel, etc.)
- Set up item sorting before starting large-scale mining
- Multiple quarries can run simultaneously with separate power
- Quarry stops at bedrock

## Common Setup

**Complete quarry system:**
```
[Stirling Engine] → [Laser Quarry]
                         ↓ (drops)
                    [Extractor Pipe]
                         ↓
                    [Filter Pipe] → [Ores] → [Storage]
                         ↓
                    [Void Pipe] ← [Cobblestone/dirt]
```

## See Also
- [Stirling Engine](../power/stirling-engine.md) - Recommended power source
- [RF Energy](../power/rf-energy.md) - Power system
- [Item Extractor Pipe](../pipes/item-extractor-pipe.md) - Collect mined items
- [Item Filter Pipe](../pipes/item-filter-pipe.md) - Sort ores from waste
- [Item Void Pipe](../pipes/item-void-pipe.md) - Delete cobblestone overflow
- [Iron Gear](../materials/iron-gear.md) - Crafting component
- [Copper Gear](../materials/copper-gear.md) - Crafting component
- [Diamond Gear](../materials/diamond-gear.md) - Crafting component
