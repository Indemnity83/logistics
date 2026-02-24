# Copper Transport Pipe

The **Copper Transport Pipe** is a [Tier 1](../core/tier-system.md) mechanical pipe that provides standard-speed item transport. It's the backbone of most pipe networks, offering reliable connectivity with random routing.

## Recipe
**Crafting:**
- 1× Copper Ingot
- 1× Glass
- **Yields:** 8× Copper Transport Pipe

## Behavior

Copper Transport Pipes move items through your network at standard speed. When an item reaches a junction with multiple possible exits, it randomly selects a direction.

**Key features:**
- Standard transport speed
- Random routing at junctions
- Reliable backbone for networks
- Connects to all other pipe types
- Connects to inventories
- Can be color-marked for network segmentation

## Network Segmentation

Mark copper pipes with [Marking Fluid](../tools/marking-fluid.md) to segment networks by color:

1. Right-click pipe with marking fluid (water bottle + dye)
2. Pipe takes on the dye color
3. Pipes with the same marking **will not connect** to each other
4. Different colors connect normally
5. Sneak + empty hand to clear marking

This lets you run multiple independent networks side-by-side without them merging.

## Tips

- Standard choice for network backbone
- Use marking fluid to prevent unwanted connections
- Random routing works fine for single-destination networks
- For controlled routing, use [Item Merger Pipes](item-merger-pipe.md) or [Item Filter Pipes](item-filter-pipe.md)
- Faster than [Stone Transport Pipes](stone-transport-pipe.md), cheaper than smart pipes

## See Also
- [Marking Fluid](../tools/marking-fluid.md) - Color-code for segmentation
- [Stone Transport Pipe](stone-transport-pipe.md) - Slower, cheaper alternative
- [Item Merger Pipe](item-merger-pipe.md) - Controlled directional routing
- [Item Filter Pipe](item-filter-pipe.md) - Item-aware routing
- [Routing](../core/routing.md) - How random routing works
- [Connectivity](../core/connectivity.md) - Connection rules
