# Understanding Tiers

Logistics organizes pipes and features into **three tiers** that represent progression from simple mechanical operations to advanced network logistics.

## The Three-Tier System

### Tier 1: Mechanical Pipes
**"Physical movement without thinking"**

Tier 1 pipes perform mechanical operations - they move, merge, accelerate, or delete items without looking at what's flowing through them.

**Characteristics:**
- No item awareness
- Simple recipes (material + glass)
- Early progression
- Fundamental operations

**Examples:**
- [Stone Transport Pipe](../pipes/stone-transport-pipe.md) - Very slow transport
- [Copper Transport Pipe](../pipes/copper-transport-pipe.md) - Standard transport
- [Item Extractor Pipe](../pipes/item-extractor-pipe.md) - Pull from inventories
- [Item Merger Pipe](../pipes/item-merger-pipe.md) - Converge to single direction
- [Golden Transport Pipe](../pipes/golden-transport-pipe.md) - Speed boost

**When to use:**
- Starting out
- Building simple transport networks
- Situations where random routing is acceptable
- Early automation (extractors)

### Tier 2: Smart Pipes
**"Making decisions based on what you see"**

Tier 2 pipes inspect items and make decisions - they filter, sort, and route based on item type or inventory availability.

**Characteristics:**
- Item-aware or inventory-aware
- More expensive recipes (diamonds, quartz)
- Mid-game progression
- Conditional behavior

**Examples:**
- [Item Filter Pipe](../pipes/item-filter-pipe.md) - Route by item type
- [Item Insertion Pipe](../pipes/item-insertion-pipe.md) - Prefer inventories with space

**When to use:**
- Sorting systems
- Ore processing automation
- Overflow management
- Complex routing logic
- Multi-destination networks

### Tier 3: Network Logistics
**"Abstract resources and requests"** *(Future - Not Yet Implemented)*

Tier 3 will treat inventories as abstract resources. Instead of building physical routes, you'll request items and the network figures out how to deliver them.

**Planned features:**
- Request Tables (ask for items)
- Provider modules (advertise inventory contents)
- Global pathfinding
- Autocrafting via vanilla Crafter
- Abstract logistics services

**When available:**
- End-game automation
- Request-based item retrieval
- Automated crafting systems
- Large-scale base logistics

## Progression Path

### Early Game: Tier 1 Only
Start with mechanical pipes:
```
[Chest] → [Extractor] → [Copper Pipes] → [Destination]
```

Simple, functional, gets items moving. Random routing is fine when you have single destinations.

### Mid Game: Tier 1 + Tier 2
Add smart pipes for control:
```
[Mining outputs] → [Filter Pipe] → [Ores] → [Smelting]
                        ↓
                   [Cobblestone] → [Void Pipe]
```

Now you can sort, filter, and make intelligent routing decisions.

### Late Game: All Tiers Together
Use Tier 1 for backbone, Tier 2 for control, Tier 3 for abstraction:
```
Physical pipes → Smart routing → Abstract requests
```

All tiers work together - you'll use pipes from every tier in complex systems.

## Design Philosophy

**Layers, not eras:**
- Tiers aren't replacements for each other
- Higher tiers build on lower tiers
- You'll use all tiers simultaneously in mature networks

**Progressive complexity:**
- Start simple (Tier 1)
- Add intelligence when needed (Tier 2)
- Abstract complexity when ready (Tier 3, future)

**Material-based progression:**
- Tier 1: Common materials (stone, copper, iron, wood)
- Tier 2: Valuable materials (diamond, quartz)
- Tier 3: End-game materials (planned)

## Common Questions

**Do I need to use all tiers?**
No. Use what you need. Simple networks work fine with just Tier 1.

**Can different tiers connect?**
Yes! All tiers connect to each other. Mix and match freely.

**Should I upgrade from Tier 1 to Tier 2?**
Not necessarily. Upgrade specific pipes where you need the functionality. Keep using [Copper Transport Pipes](../pipes/copper-transport-pipe.md) for backbone even when you add [Item Filter Pipes](../pipes/item-filter-pipe.md) for sorting.

**When do I need Tier 2?**
When you need:
- Item sorting
- Overflow management
- Inventory-preference routing
- Multi-output distribution

**What about Tier 3?**
Not implemented yet. Focus on mastering Tier 1 and Tier 2 for now.

## See Also
- [Tier System](../core/tier-system.md) - Technical tier architecture
- [Pipes](../pipes/index.md) - All pipes organized by tier
- [First Network](first-network.md) - Build with Tier 1 pipes
- [Item Filter Pipe](../pipes/item-filter-pipe.md) - Example Tier 2 smart pipe
- [Routing](../core/routing.md) - How different tiers route items
