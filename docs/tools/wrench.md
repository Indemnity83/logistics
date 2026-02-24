<div class="infobox">
    <div class="infobox-header">Wrench</div>
    <div class="infobox-image">
        <img src="/assets/icons/logistics__core___wrench.png" alt="Wrench">
    </div>
    <table class="infobox-table">
        <tr>
            <td class="infobox-label">ID</td>
            <td class="infobox-value"><code>logistics:tool/wrench</code></td>
        </tr>
        <tr>
            <td class="infobox-label">Type</td>
            <td class="infobox-value">Tool</td>
        </tr>
        <tr>
            <td class="infobox-label">Stackable</td>
            <td class="infobox-value"><span class="stackable-no">No</span></td>
        </tr>
        <tr>
            <td class="infobox-label">Function</td>
            <td class="infobox-value">Configuration</td>
        </tr>
        <tr>
            <td class="infobox-label">Added</td>
            <td class="infobox-value">v0.1.0</td>
        </tr>
    </table>
</div>

# Wrench

The **Wrench** is a configuration tool for pipes, engines, and machines. Use it to rotate output faces, select extraction directions, open filter GUIs, and reset overheated engines.

## Recipe

<div class="crafting-recipe">
    <div class="crafting-grid">
        <div class="crafting-slot"><img src="/assets/icons/minecraft__iron_ingot.png" class="crafting-item" alt="Iron Ingot"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"><img src="/assets/icons/minecraft__iron_ingot.png" class="crafting-item" alt="Iron Ingot"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"><img src="/assets/icons/minecraft__iron_ingot.png" class="crafting-item" alt="Iron Ingot"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"><img src="/assets/icons/minecraft__iron_ingot.png" class="crafting-item" alt="Iron Ingot"></div>
        <div class="crafting-slot"></div>
    </div>
    <div class="crafting-arrow">→</div>
    <div class="crafting-output">
        <img src="/assets/icons/logistics__core___wrench.png" class="crafting-item" alt="Wrench">
    </div>
</div>

**Yields:** 1× Wrench

## Usage

Right-click blocks with the wrench to configure them. Different blocks respond differently:

### Pipe Configuration

**[Item Merger Pipe](../pipes/item-merger-pipe.md):**
- Cycles output direction
- Output face marked with opaque connector
- Items exit only through configured direction

**[Item Extractor Pipe](../pipes/item-extractor-pipe.md):**
- Cycles extraction face
- Active face marked with opaque connector
- Only one face extracts at a time

**[Item Filter Pipe](../pipes/item-filter-pipe.md):**
- Opens filter configuration GUI
- Set which items route to which faces
- Configure per-side filters

### Engine Configuration

**[Redstone Engine](../power/redstone-engine.md) / [Stirling Engine](../power/stirling-engine.md):**
- Rotates RF output face
- Output face must touch machine input
- Position engine correctly for power transfer

**[Stirling Engine](../power/stirling-engine.md) overheat reset:**
- Right-click overheated engine to reset
- Allows engine to cool and restart
- Required after overheat shutdown

### Other Blocks

The wrench works with any configurable Logistics block. Future machines and components will use the wrench for configuration.

## Tips

- Keep wrench in hotbar for quick configuration
- Output faces shown with opaque connectors
- Right-click to cycle through options
- Filter pipes open GUI instead of cycling
- Essential tool for building complex networks
- Non-destructive - doesn't break blocks (sneak to break normally)

## Common Tasks

**Configure merger output:**
```
Right-click [Item Merger Pipe] → output direction cycles
```

**Select extraction face:**
```
Right-click [Item Extractor Pipe] → extraction face cycles
```

**Open filter GUI:**
```
Right-click [Item Filter Pipe] → GUI opens for configuration
```

**Rotate engine:**
```
Right-click [Engine] → output face rotates
```

**Reset overheated engine:**
```
Right-click [Stirling Engine] (overheated) → resets temperature
```

## See Also
- [Item Merger Pipe](../pipes/item-merger-pipe.md) - Configure output direction
- [Item Extractor Pipe](../pipes/item-extractor-pipe.md) - Select extraction face
- [Item Filter Pipe](../pipes/item-filter-pipe.md) - Open filter GUI
- [Redstone Engine](../power/redstone-engine.md) - Rotate output face
- [Stirling Engine](../power/stirling-engine.md) - Rotate output, reset overheat
- [Connectivity](../core/connectivity.md) - Understanding pipe faces
