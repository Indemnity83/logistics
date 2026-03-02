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

## Tier 3: Network Logistics
System-aware pipes that participate in a logistics network. These pipes communicate with each other to advertise available items, fulfill requests, and maintain stock levels automatically.

- **[Basic Logistics Pipe](basic-logistics-pipe.md)** - Accept and deposit network items; the foundation for all Tier 3 pipes
- **[Provider Logistics Pipe](provider-logistics-pipe.md)** - Advertise adjacent inventory contents and fulfill network requests
- **[Supplier Logistics Pipe](supplier-logistics-pipe.md)** - Automatically maintain configured stock levels by requesting from the network
- **[Requester Logistics Pipe](requester-logistics-pipe.md)** - Manually request items from the network via a browsable GUI

## See Also
- [Pipe Networks](../core/pipe-networks.md) - Understanding how pipes connect
- [Tier System](../core/tier-system.md) - Mechanical vs Smart vs Network tiers
- [Routing](../core/routing.md) - How items choose paths
- [Item Transport](../core/item-transport.md) - How items move through pipes
