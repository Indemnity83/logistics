# Tier System

Logistics organizes pipes and features into **three tiers** that represent progression from simple mechanical operations to advanced network logistics.

## Tier 1: Mechanical Pipes

**No item awareness** - These pipes perform mechanical operations without looking at what's flowing through them.

**Characteristics:**
- Operate on all items equally
- No conditional behavior
- Simple recipes (material + glass)
- Available early in progression

**Examples:**
- [Stone Transport Pipe](../pipes/stone-transport-pipe.md) - Move items slowly
- [Copper Transport Pipe](../pipes/copper-transport-pipe.md) - Standard transport
- [Item Extractor Pipe](../pipes/item-extractor-pipe.md) - Pull from inventories
- [Item Merger Pipe](../pipes/item-merger-pipe.md) - Converge to single output
- [Golden Transport Pipe](../pipes/golden-transport-pipe.md) - Speed boost

## Tier 2: Smart Pipes

**Item-aware routing** - These pipes inspect items and make decisions based on what they see.

**Characteristics:**
- Conditional behavior based on item type
- Can filter and sort
- More complex recipes (advanced materials)
- Mid-game progression

**Examples:**
- [Item Filter Pipe](../pipes/item-filter-pipe.md) - Route by item type
- [Item Insertion Pipe](../pipes/item-insertion-pipe.md) - Prefer inventories with space

## Tier 3: Network Logistics

**System-aware automation** - Future tier that treats inventories as abstract resources with global routing.

**Characteristics:**
- Request/provider model
- Global pathfinding
- Autocrafting integration
- End-game progression

**Status:** Planned, not yet implemented

## Design Philosophy

Each tier builds on the previous:
- **Tier 1** provides physical connectivity
- **Tier 2** adds intelligent routing
- **Tier 3** will add abstract logistics

All tiers work together - you'll use pipes from all tiers in complex networks.

## See Also
- [Pipe Networks](pipe-networks.md) - How tiers connect together
- [Pipes](../pipes/index.md) - All pipe types organized by tier
- [Routing](routing.md) - How different tiers route items
