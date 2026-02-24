# Routing

**Routing** is how items choose their path when multiple directions are available. Different pipe types use different routing strategies.

## When Routing Happens

Routing decisions occur when an item reaches the **center of a pipe** and multiple exit directions are available.

If only one direction is available, the item takes that path automatically (no routing decision needed).

## Routing Strategies

### Random Routing (Tier 1)
**Used by:** [Stone Transport Pipe](../pipes/stone-transport-pipe.md), [Copper Transport Pipe](../pipes/copper-transport-pipe.md)

- Randomly selects from available exit directions
- No item awareness
- Simple and unpredictable
- Suitable for early networks with single destinations

### Directional Routing (Tier 1)
**Used by:** [Item Merger Pipe](../pipes/item-merger-pipe.md)

- All inputs route to single configured output
- Output direction set with [Wrench](../tools/wrench.md)
- Items cannot enter through output face

### Filter-Based Routing (Tier 2)
**Used by:** [Item Filter Pipe](../pipes/item-filter-pipe.md)

- Routes based on per-side item filters
- Configure which items go to which faces
- Unmatched items route to unfiltered directions
- Items drop if no valid route found

### Inventory-Preference Routing (Tier 2)
**Used by:** [Item Insertion Pipe](../pipes/item-insertion-pipe.md)

- Prefers directions with accepting inventories
- Falls back to pipe directions if no inventory
- Items drop if no valid output

### Deletion (Tier 1)
**Used by:** [Item Void Pipe](../pipes/item-void-pipe.md)

- "Routes" items to deletion
- Items disappear at pipe center
- No actual exit direction chosen

## Routing Priority

When multiple pipes could make routing decisions, the pipe's module evaluates in order:
1. First module to return a decision wins
2. If all modules pass, default random routing applies
3. Empty direction list results in item drop

## Invalid Routes

Items drop as entities when:
- No valid exit directions available
- Filter pipe has no matching filter and no unfiltered exits
- Insertion pipe finds no accepting inventory or pipe
- Routing module returns empty direction list

## Building Reliable Routes

**For Tier 1 networks:**
- Use [Item Merger Pipes](../pipes/item-merger-pipe.md) to guarantee direction
- Design single-path routes where possible
- Accept randomness for multi-destination networks

**For Tier 2 networks:**
- Use [Item Filter Pipes](../pipes/item-filter-pipe.md) for deterministic sorting
- Use [Item Insertion Pipes](../pipes/item-insertion-pipe.md) to prefer storage
- Combine both for complex routing logic

**General tips:**
- Minimize junctions when path matters
- Use [Item Void Pipes](../pipes/item-void-pipe.md) for overflow
- Test networks with small item batches first

## See Also
- [Tier System](tier-system.md) - How tiers affect routing
- [Item Transport](item-transport.md) - Item movement mechanics
- [Pipe Networks](pipe-networks.md) - Network structure
- [Item Filter Pipe](../pipes/item-filter-pipe.md) - Advanced routing
- [Item Merger Pipe](../pipes/item-merger-pipe.md) - Directional routing
