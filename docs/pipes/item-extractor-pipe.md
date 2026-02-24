# Item Extractor Pipe

The **Item Extractor Pipe** is a [Tier 1](../core/tier-system.md) mechanical pipe that actively pulls items from adjacent inventories into your pipe network. It's how you get items moving through your system.

## Recipe
**Crafting:**
- 1× Planks (any type)
- 1× Glass
- **Yields:** 8× Item Extractor Pipe

## Behavior

The Item Extractor Pipe pulls one item at a time from a connected inventory and inserts it into the pipe network. Extraction happens automatically at regular intervals.

**Key features:**
- Extracts from one face only (configurable)
- Pulls one item per extraction cycle
- Requires adjacent inventory to extract from
- Connects to other pipes on remaining faces
- No power required - extraction is automatic
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
- Extraction is automatic - no redstone required
- Cannot extract through [Item Passthrough Pipes](item-passthrough-pipe.md)
- One item per cycle - not a high-speed extractor

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
