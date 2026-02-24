# Pipe Connectivity

Pipes connect automatically to adjacent blocks based on simple rules. Understanding connectivity helps you build clean, organized networks.

## Connection Rules

Pipes connect to:
- **Other pipes** - Any pipe connects to any other pipe type
- **Inventories** - Chests, furnaces, hoppers, barrels, etc.
- **Fabric Transfer API blocks** - Any mod block that implements ItemStorage

Pipes do NOT connect to:
- Solid blocks (stone, dirt, etc.)
- Non-inventory blocks
- Air

## Visual Indicators

Connected faces show a **connector** (extended connection point) in the direction of the connection. No connector means no connection on that face.

**Connector types:**
- **Transparent** - Connects to another pipe
- **Opaque** - Connects to inventory or shows active face (merger/extractor pipes)

## Special Connection Behaviors

**[Item Passthrough Pipe](../pipes/item-passthrough-pipe.md):**
- Only connects to other pipes
- Never connects to inventories
- Use to bypass nearby chests

**[Item Merger Pipe](../pipes/item-merger-pipe.md):**
- Output face marked with opaque connector
- Items cannot enter through output face
- Configure output direction with [Wrench](../tools/wrench.md)

**[Item Extractor Pipe](../pipes/item-extractor-pipe.md):**
- Extraction face marked with opaque connector
- Only one face extracts at a time
- Configure extraction face with [Wrench](../tools/wrench.md)

## Network Segmentation

Prevent connections between pipes using:

**[Marking Fluid](../tools/marking-fluid.md):**
- Color-code [Copper Transport Pipes](../pipes/copper-transport-pipe.md)
- Pipes with same color won't connect to each other
- Different colors connect normally
- Sneak + empty hand removes marking

**Physical separation:**
- Place non-pipe blocks between networks
- Use vertical spacing
- Build separate systems

## All Tiers Connect

All pipe tiers connect to each other:
- [Tier 1](tier-system.md#tier-1-mechanical-pipes) mechanical pipes connect to [Tier 2](tier-system.md#tier-2-smart-pipes) smart pipes
- Mix and match pipe types as needed
- Build complex networks combining different behaviors

## See Also
- [Pipe Networks](pipe-networks.md) - How connections form networks
- [Marking Fluid](../tools/marking-fluid.md) - Prevent connections with color coding
- [Item Passthrough Pipe](../pipes/item-passthrough-pipe.md) - Bypass inventories
- [Wrench](../tools/wrench.md) - Configure pipe faces
