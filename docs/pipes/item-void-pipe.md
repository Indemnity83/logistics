# Item Void Pipe

The **Item Void Pipe** is a [Tier 1](../core/tier-system.md) mechanical pipe that destroys any items that enter it. Use it for overflow management or disposing of unwanted items.

## Recipe
**Crafting:**
- 1× Obsidian
- 1× Glass
- 1× Ender Pearl
- **Yields:** 8× Item Void Pipe

## Behavior

Item Void Pipes permanently delete items. When an item reaches the center of the pipe, it's destroyed - no drops, no recovery.

**Key features:**
- Deletes items at pipe center
- Items visible briefly before deletion (time to react if needed)
- No item recovery once deleted
- Connects to all pipe types
- Prevents item drops from overflowing systems

## Deletion Timing

Items are deleted when they reach the **center** of the void pipe. You can see them travel partway into the pipe before disappearing, giving you a brief window to react if items are being voided unintentionally.

## Tips

- Use for overflow management in storage systems
- Delete unwanted byproducts from automation
- Prevent item drops from full inventories
- Place at end of overflow branches
- Combine with [Item Filter Pipes](item-filter-pipe.md) to void specific items
- No recovery possible - use carefully
- More expensive than other basic pipes (requires ender pearl)

## Common Patterns

**Overflow protection:**
```
[Source] → [Item Insertion Pipe] → [Storage]
                    ↓
              [Item Void Pipe] (catches overflow when storage full)
```

**Filtered voiding:**
```
[Source] → [Item Filter Pipe] → [Cobblestone] → [Item Void Pipe]
                    ↓
              [Wanted items] → [Storage]
```

**Guaranteed item disposal:**
```
[Unwanted items] → [Item Void Pipe] (no drops, clean deletion)
```

## See Also
- [Item Filter Pipe](item-filter-pipe.md) - Route specific items to void
- [Item Insertion Pipe](item-insertion-pipe.md) - Route overflow to void
- [Item Transport](../core/item-transport.md) - Item lifecycle
- [Routing](../core/routing.md) - Directing items to void
