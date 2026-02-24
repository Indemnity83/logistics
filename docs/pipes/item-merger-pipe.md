<div class="infobox">
    <div class="infobox-header">Item Merger Pipe</div>
    <div class="infobox-image">
        <img src="/assets/icons/logistics__pipe___item_merger_pipe.png" alt="Item Merger Pipe">
    </div>
    <table class="infobox-table">
        <tr>
            <td class="infobox-label">ID</td>
            <td class="infobox-value"><code>logistics:pipe/item_merger_pipe</code></td>
        </tr>
        <tr>
            <td class="infobox-label">Type</td>
            <td class="infobox-value">Block (Pipe)</td>
        </tr>
        <tr>
            <td class="infobox-label">Stackable</td>
            <td class="infobox-value"><span class="stackable-yes">Yes (64)</span></td>
        </tr>
        <tr>
            <td class="infobox-label">Tier</td>
            <td class="infobox-value"><span class="infobox-tier infobox-tier-1">Tier 1 - Mechanical</span></td>
        </tr>
        <tr>
            <td class="infobox-label">Added</td>
            <td class="infobox-value">v0.1.0</td>
        </tr>
    </table>
</div>

# Item Merger Pipe

The **Item Merger Pipe** is a [Tier 1](../core/tier-system.md) mechanical pipe that takes items from any input direction and routes them all to a single configured output direction. It's essential for controlled directional routing.

## Recipe

<div class="crafting-recipe">
    <div class="crafting-grid">
        <div class="crafting-slot"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"><img src="/assets/icons/minecraft__iron_ingot.png" class="crafting-item" alt="Iron Ingot"></div>
        <div class="crafting-slot"><img src="/assets/icons/minecraft__glass.png" class="crafting-item" alt="Glass"></div>
        <div class="crafting-slot"><img src="/assets/icons/minecraft__iron_ingot.png" class="crafting-item" alt="Iron Ingot"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"></div>
    </div>
    <div class="crafting-arrow">→</div>
    <div class="crafting-output">
        <img src="/assets/icons/logistics__pipe___item_merger_pipe.png" class="crafting-item" alt="Item Merger Pipe">
        <span class="crafting-count">8</span>
    </div>
</div>

**Yields:** 8× Item Merger Pipe

## Behavior

Item Merger Pipes converge all inputs to a single output. Items can enter from any direction except the output face, and all items exit in the configured output direction.

**Key features:**
- All inputs merge to single output
- Output direction configurable with [Wrench](../tools/wrench.md)
- Items **cannot** enter through output face
- Output face marked with **opaque connector**
- Guaranteed directional routing (no randomness)

## Configuration

Use a [Wrench](../tools/wrench.md) to change the output direction:

1. Right-click the pipe with a wrench
2. Output direction cycles through available faces
3. Output face indicated by **opaque connector**
4. Items will only exit through this face

## Tips

- Perfect for collecting from multiple [Item Extractor Pipes](item-extractor-pipe.md)
- Guarantees items go in one direction (unlike random routing)
- Combine with [Copper Transport Pipes](copper-transport-pipe.md) for backbone
- Cannot accept items from output direction - plan accordingly
- Use multiple mergers to build tree structures

## Common Patterns

**Multi-extractor collection:**
```
[Extractor A] ↘
[Extractor B] → [Item Merger] → [Copper Pipe] → [Destination]
[Extractor C] ↗
```

**Tree merger structure:**
```
[Sources A-C] → [Merger 1] ↘
                            [Merger 3] → [Main line]
[Sources D-F] → [Merger 2] ↗
```

**Controlled junction:**
```
[Branch A] ↘
           [Item Merger] → [Guaranteed direction]
[Branch B] ↗
```

## See Also
- [Wrench](../tools/wrench.md) - Configure output direction
- [Item Extractor Pipe](item-extractor-pipe.md) - Common input source
- [Copper Transport Pipe](copper-transport-pipe.md) - Connect to network
- [Routing](../core/routing.md) - Directional vs random routing
- [Connectivity](../core/connectivity.md) - Input/output face rules
