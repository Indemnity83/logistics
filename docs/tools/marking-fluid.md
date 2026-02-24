# Marking Fluid

**Marking Fluid** is a color-coding tool that prevents [Copper Transport Pipes](../pipes/copper-transport-pipe.md) from connecting to each other. Use it to segment networks and run multiple independent pipe systems side-by-side.

## Recipe
**Crafting:**
- 1× Water Bottle
- 1× Dye (any color)
- **Yields:** 1× Marking Fluid (colored)

## Usage

Right-click [Copper Transport Pipes](../pipes/copper-transport-pipe.md) with marking fluid to color-code them:

1. Craft marking fluid with desired dye color
2. Right-click copper pipe with marking fluid
3. Pipe takes on the dye color
4. **Pipes with the same color will not connect to each other**
5. Different colors connect normally
6. Unmarked pipes connect to all colors

## Removing Marking

To clear a pipe's color marking:
- Sneak + right-click with empty hand
- Pipe returns to unmarked state
- Can now connect to any pipe

## Connection Rules

**Same color pipes:**
- **Do not connect** to each other
- Prevents merging of separate networks

**Different color pipes:**
- **Connect normally**
- Mix colors as needed

**Unmarked pipes:**
- **Connect to all pipes** regardless of color
- Default behavior

## Use Cases

**Run parallel networks:**
```
[Red pipes] ═══ [Red network A]
[Blue pipes] ══ [Blue network B]
    (Red and blue won't connect to each other)
```

**Prevent unwanted connections:**
```
[Main network] → [Green pipes]
                      ↑
            [Green pipes] (won't merge into one network)
```

**Organize complex builds:**
```
Different colored pipes for different purposes:
- Red = ore processing
- Blue = item sorting
- Green = overflow management
```

## Tips

- Only works on [Copper Transport Pipes](../pipes/copper-transport-pipe.md)
- Each dye creates a different color
- Sneak + empty hand to remove marking
- Visual indicator shows pipe color
- Alternative to physical separation
- Cleaner than using blocks to separate networks
- Different from filters - this prevents physical connections

## See Also
- [Copper Transport Pipe](../pipes/copper-transport-pipe.md) - Pipe type that accepts marking
- [Connectivity](../core/connectivity.md) - Connection rules
- [Pipe Networks](../core/pipe-networks.md) - Network segmentation
- [Item Passthrough Pipe](../pipes/item-passthrough-pipe.md) - Alternative segmentation method
