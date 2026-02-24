# Pipe Networks

A **pipe network** is a connected system of pipes that transport items from sources (like [extractors](../pipes/item-extractor-pipe.md)) to destinations (like chests or other inventories).

## How Networks Form

Pipes automatically connect to:
- **Adjacent pipes** - Pipes connect to neighboring pipes on any of their six faces
- **Adjacent inventories** - Pipes connect to chests, furnaces, hoppers, and other storage blocks
- **Other mods** - Any block implementing Fabric's Transfer API (ItemStorage)

When pipes touch, they form a continuous network. Items can flow through any connected path in the network.

## Network Segmentation

You can prevent pipes from connecting using:
- **[Marking Fluid](../tools/marking-fluid.md)** - Color-code [Copper Transport Pipes](../pipes/copper-transport-pipe.md) so same colors won't connect
- **[Item Passthrough Pipe](../pipes/item-passthrough-pipe.md)** - Only connects to pipes, never inventories

This lets you run multiple independent networks side-by-side without them merging.

## Item Flow

Items enter networks through:
- **[Item Extractor Pipe](../pipes/item-extractor-pipe.md)** - Pulls from adjacent inventories
- **External insertion** - Hoppers or other mods pushing into pipes
- **Manual insertion** - Players inserting items directly

Items travel through the network until they:
- **Enter an inventory** - Successfully inserted at a destination
- **Drop** - No valid path found, item drops as entity
- **Get deleted** - Enters an [Item Void Pipe](../pipes/item-void-pipe.md)

## See Also
- [Connectivity](connectivity.md) - How pipes connect to each other
- [Routing](routing.md) - How items choose paths
- [Item Transport](item-transport.md) - How items move through pipes
- [Tier System](tier-system.md) - Different pipe capabilities
