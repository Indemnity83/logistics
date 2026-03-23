<div class="infobox">
    <div class="infobox-header">Item Extractor Pipe</div>
    <div class="infobox-image">
        <img src="../../assets/icons/logistics__pipe___item_extractor_pipe.png" alt="Item Extractor Pipe" title="Item Extractor Pipe">
    </div>
    <table class="infobox-table">
        <tr>
            <td class="infobox-label">ID</td>
            <td class="infobox-value"><code>logistics:pipe/item_extractor_pipe</code></td>
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

# Item Extractor Pipe

The **Item Extractor Pipe** is a [Tier 1](../core/tier-system.md) mechanical pipe that actively pulls items from adjacent inventories into your pipe network. It's how you get items moving through your system.

## Recipe

<div class="crafting-recipe">
    <div class="crafting-grid">
        <div class="crafting-slot"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__oak_planks.png" class="crafting-item" alt="Planks" title="Planks"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__glass.png" class="crafting-item" alt="Glass" title="Glass"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__oak_planks.png" class="crafting-item" alt="Planks" title="Planks"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"></div>
    </div>
    <div class="crafting-arrow">→</div>
    <div class="crafting-output">
        <img src="../../assets/icons/logistics__pipe___item_extractor_pipe.png" class="crafting-item" alt="Item Extractor Pipe" title="Item Extractor Pipe">
        <span class="crafting-count">8</span>
    </div>
</div>

**Yields:** 8× Item Extractor Pipe

## Behavior

The Item Extractor Pipe pulls one item at a time from a connected inventory and inserts it into the pipe network. Extraction happens automatically at regular intervals.

**Key features:**
- Extracts from one face only (configurable)
- Pulls one item per extraction cycle
- Requires adjacent inventory to extract from
- Connects to other pipes on remaining faces
- Requires power from an adjacent [engine](../power/redstone-engine.md); a [Redstone Engine](../power/redstone-engine.md) extracts 1 item per cycle, while higher-powered engines can extract up to a stack per cycle
- Works with any block implementing Fabric Transfer API

## Configuration

Use a [Wrench](../tools/wrench.md) to select which face extracts from the inventory:

1. Right-click the pipe with a wrench
2. Extraction face cycles through available directions
3. Active extraction face indicated by **opaque connector**
4. Only one face can extract at a time

The opaque connector shows which face is pulling from the inventory.

## Tips

- Place extraction face directly against the inventory (chest, furnace, etc.)
- Connect other faces to [Copper Transport Pipes](copper-transport-pipe.md) for network
- Use [Item Merger Pipes](item-merger-pipe.md) to collect from multiple extractors
- Cannot extract through [Item Passthrough Pipes](item-passthrough-pipe.md)
- Use a higher-powered engine (e.g. [Stirling Engine](../power/stirling-engine.md)) to extract more items per cycle, up to a full stack

## Common Patterns

**Single chest extraction:**
```
[Chest] → [Item Extractor] → [Copper Transport Pipe] → [Network]
```

**Multi-chest collection:**
```
[Chest A] → [Extractor A] ↘
                            [Item Merger Pipe] → [Network]
[Chest B] → [Extractor B] ↗
```

## See Also
- [Wrench](../tools/wrench.md) - Configure extraction face
- [Copper Transport Pipe](copper-transport-pipe.md) - Connect to network
- [Item Merger Pipe](item-merger-pipe.md) - Collect from multiple extractors
- [Pipe Networks](../core/pipe-networks.md) - Building extraction networks
- [Connectivity](../core/connectivity.md) - How extractors connect
