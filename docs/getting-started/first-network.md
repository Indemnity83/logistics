# Your First Pipe Network

Learn the basics by building a simple pipe network that extracts items from a chest and delivers them to another chest.

## What You'll Build

A basic transport system:
```
[Source Chest] → [Extractor] → [Copper Pipes] → [Destination Chest]
```

This teaches the fundamentals: extraction, transport, and insertion.

## Materials Needed

Craft these items first:

**Pipes:**
- 8× [Copper Transport Pipe](../pipes/copper-transport-pipe.md) (copper ingot + glass)
- 8× [Item Extractor Pipe](../pipes/item-extractor-pipe.md) (planks + glass)

**Tool:**
- 1× [Wrench](../tools/wrench.md) (4× iron ingot)

**Test setup:**
- 2× Chest (source and destination)
- Some items to transport

## Step-by-Step Build

### 1. Place Source Chest
Place a chest and fill it with some items (cobblestone, dirt, anything).

### 2. Place Extractor Pipe
Place an [Item Extractor Pipe](../pipes/item-extractor-pipe.md) directly next to the chest. You should see it connect automatically (opaque connector appears).

### 3. Configure Extraction Face
1. Take out your [Wrench](../tools/wrench.md)
2. Right-click the extractor pipe
3. Watch the opaque connector - it shows which face extracts
4. Keep right-clicking until the connector points toward the chest
5. Now the extractor will pull from that chest

### 4. Build Transport Line
Place [Copper Transport Pipes](../pipes/copper-transport-pipe.md) extending from the extractor:
- Connect to any free face of the extractor
- Extend as far as you want (3-5 blocks for this example)
- Pipes connect automatically to each other

### 5. Place Destination Chest
Place a second chest at the end of your copper pipe line. The pipe should automatically connect to it (you'll see a connector appear).

### 6. Watch It Work
Items automatically extract from the source chest, travel through the pipes (you can see them moving!), and enter the destination chest.

**If items aren't moving:**
- Check extraction face points at source chest (use wrench)
- Verify pipes connect to both chests (connectors visible)
- Ensure source chest has items

## What You Learned

**Automatic connection:**
- Pipes connect to adjacent pipes and inventories automatically
- Visual connectors show connections

**Extraction:**
- [Item Extractor Pipes](../pipes/item-extractor-pipe.md) pull from inventories
- Configure extraction face with [Wrench](../tools/wrench.md)
- Only one face extracts at a time

**Transport:**
- [Copper Transport Pipes](../pipes/copper-transport-pipe.md) move items
- Items visible while traveling
- Random routing at junctions (we'll address this next)

**Insertion:**
- Items automatically try to enter adjacent inventories
- Works with any inventory (chests, furnaces, hoppers, etc.)

## Next Steps

### Controlled Routing
Random routing is fine for single destinations, but what about multiple chests?

Use [Item Merger Pipe](../pipes/item-merger-pipe.md) to guarantee direction:
```
[Source A] ↘
           [Item Merger] → [Guaranteed path to destination]
[Source B] ↗
```

### Sorting Items
Want specific items to go to specific chests?

Use [Item Filter Pipe](../pipes/item-filter-pipe.md):
```
[Mixed items] → [Filter] → [Ores] → [Ore chest]
                    ↓
              [Everything else] → [General chest]
```

### Speed Up Transport
Use [Golden Transport Pipe](../pipes/golden-transport-pipe.md) with redstone power to accelerate items.

### Learn More
- [Understanding Tiers](understanding-tiers.md) - The progression system
- [Pipe Networks](../core/pipe-networks.md) - How networks work
- [Routing](../core/routing.md) - How items choose paths
- [All Pipes](../pipes/index.md) - Explore all pipe types

## Common Mistakes

**Items drop instead of entering destination:**
- Destination chest might be full
- Pipe might not be connected (no connector visible)
- Use [Item Insertion Pipe](../pipes/item-insertion-pipe.md) to prefer inventories

**Extraction face pointing wrong way:**
- Right-click with wrench to cycle faces
- Opaque connector shows active face
- Should point directly at source chest

**Pipes don't connect:**
- Must be adjacent (no gaps)
- Check for [Marking Fluid](../tools/marking-fluid.md) preventing connection
- [Item Passthrough Pipes](../pipes/item-passthrough-pipe.md) don't connect to inventories

## See Also
- [Item Extractor Pipe](../pipes/item-extractor-pipe.md) - Detailed extractor info
- [Copper Transport Pipe](../pipes/copper-transport-pipe.md) - Backbone transport
- [Wrench](../tools/wrench.md) - Configuration tool
- [Pipe Networks](../core/pipe-networks.md) - Network concepts
- [Item Transport](../core/item-transport.md) - How items move
