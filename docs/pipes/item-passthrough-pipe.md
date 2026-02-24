# Item Passthrough Pipe

The **Item Passthrough Pipe** is a [Tier 1](../core/tier-system.md) mechanical pipe that only connects to other pipes and never to inventories. Use it to bypass nearby storage or force routing around specific blocks.

## Recipe
**Crafting:**
- 1× Sandstone
- 1× Glass
- **Yields:** 8× Item Passthrough Pipe

## Behavior

Item Passthrough Pipes connect exclusively to other pipes - they ignore adjacent inventories completely. This lets you route pipes past storage blocks without the pipes connecting to them.

**Key features:**
- Connects only to pipes
- **Never** connects to inventories
- Random routing at junctions (like [Copper Transport Pipe](copper-transport-pipe.md))
- Useful for bypassing storage
- Forces pipe-only pathways

## Use Cases

**Bypass adjacent storage:**
```
[Chest]
  ↓
[Item Passthrough Pipe] → [Continues past chest without connecting]
```

**Run pipes past inventories:**
```
[Storage] [Storage] [Storage]
    ↓         ↓         ↓
[Passthrough]-[Passthrough]-[Passthrough] → [No connections to storage above]
```

**Force routing to specific destinations:**
```
[Network] → [Passthrough] → [Passthrough] → [Copper Pipe] → [Intended Chest]
                                                ↑
                                          [Won't connect to other chests along the way]
```

## Tips

- Use when you need pipes to run past storage without connecting
- Prevents unwanted item insertion into nearby inventories
- Good for organizing dense storage areas
- Can't extract from inventories (use [Item Extractor Pipe](item-extractor-pipe.md))
- Random routing still applies at junctions
- Works with all other pipe types

## Common Patterns

**Storage bypass:**
```
[Main line with passthrough] runs past [multiple chests] without connecting
```

**Dedicated destination routing:**
```
[Source] → [Copper] → [Passthrough segment] → [Copper] → [Specific destination only]
```

## See Also
- [Connectivity](../core/connectivity.md) - Connection rules
- [Copper Transport Pipe](copper-transport-pipe.md) - Normal transport
- [Item Extractor Pipe](item-extractor-pipe.md) - Extracting from inventories
- [Marking Fluid](../tools/marking-fluid.md) - Alternative network segmentation method
- [Pipe Networks](../core/pipe-networks.md) - Building networks
