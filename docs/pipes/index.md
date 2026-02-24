# Pipes

Pipes are the core of the Logistics mod, transporting items through your network. Pipes connect automatically to adjacent inventories and other pipes, forming networks that move items from extractors to destinations.

## Tier 1: Mechanical Pipes
Basic pipes that perform mechanical operations without item awareness. These pipes don't look at what's flowing through them - they just move, merge, accelerate, or delete items regardless of type.

- **[Stone Transport Pipe](stone-transport-pipe.md)** - Very slow transport, random routing at junctions
- **[Copper Transport Pipe](copper-transport-pipe.md)** - Standard transport backbone, random routing
- **[Item Extractor Pipe](item-extractor-pipe.md)** - Pull items from adjacent inventories
- **[Item Merger Pipe](item-merger-pipe.md)** - Converge all inputs to single output
- **[Golden Transport Pipe](golden-transport-pipe.md)** - Speed boost when powered by redstone
- **[Item Passthrough Pipe](item-passthrough-pipe.md)** - Connect only to pipes, bypass inventories
- **[Item Void Pipe](item-void-pipe.md)** - Delete unwanted items

## Tier 2: Smart Pipes
Item-aware pipes that make routing decisions based on what they're transporting. These pipes inspect items and change behavior conditionally.

- **[Item Filter Pipe](item-filter-pipe.md)** - Route specific items to specific destinations
- **[Item Insertion Pipe](item-insertion-pipe.md)** - Prefer inventories with space, otherwise route to pipes

## See Also
- [Pipe Networks](../core/pipe-networks.md) - Understanding how pipes connect
- [Tier System](../core/tier-system.md) - Mechanical vs Smart vs Network tiers
- [Routing](../core/routing.md) - How items choose paths
- [Item Transport](../core/item-transport.md) - How items move through pipes
