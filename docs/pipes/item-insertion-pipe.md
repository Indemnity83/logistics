<div class="infobox">
    <div class="infobox-header">Item Insertion Pipe</div>
    <div class="infobox-image">
        <img src="/assets/icons/logistics__pipe___item_insertion_pipe.png" alt="Item Insertion Pipe">
    </div>
    <table class="infobox-table">
        <tr>
            <td class="infobox-label">ID</td>
            <td class="infobox-value"><code>logistics:pipe/item_insertion_pipe</code></td>
        </tr>
        <tr>
            <td class="infobox-label">Type</td>
            <td class="infobox-value">Block (Pipe)</td>
        </tr>
        <tr>
            <td class="infobox-label">Stackable</td>
            <td class="infobox-value"><span class="stackable-yes">Yes (64)</span></td>
        </tr>
        <tr>
            <td class="infobox-label">Tier</td>
            <td class="infobox-value"><span class="infobox-tier infobox-tier-2">Tier 2 - Smart</span></td>
        </tr>
        <tr>
            <td class="infobox-label">Added</td>
            <td class="infobox-value">v0.1.0</td>
        </tr>
    </table>
</div>

# Item Insertion Pipe

The **Item Insertion Pipe** is a [Tier 2](../core/tier-system.md) smart pipe that routes items into connected inventories with space. If no inventory accepts the item, it continues along the pipe network instead of dropping.

## Recipe

<div class="crafting-recipe">
    <div class="crafting-grid">
        <div class="crafting-slot"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"><img src="/assets/icons/minecraft__quartz.png" class="crafting-item" alt="Quartz"></div>
        <div class="crafting-slot"><img src="/assets/icons/minecraft__glass.png" class="crafting-item" alt="Glass"></div>
        <div class="crafting-slot"><img src="/assets/icons/minecraft__quartz.png" class="crafting-item" alt="Quartz"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"></div>
    </div>
    <div class="crafting-arrow">→</div>
    <div class="crafting-output">
        <img src="/assets/icons/logistics__pipe___item_insertion_pipe.png" class="crafting-item" alt="Item Insertion Pipe">
        <span class="crafting-count">8</span>
    </div>
</div>

**Yields:** 8× Item Insertion Pipe

## Behavior

Item Insertion Pipes actively try to insert items into adjacent inventories. They check connected inventories for available space and prefer those directions over pipe-only exits.

**Key features:**
- Prefers inventory directions over pipe directions
- Checks if inventory will accept the item
- Falls back to pipe routing if no inventory available
- Items drop only if no pipes or inventories can accept
- Inventory-aware but not item-type aware

## Routing Priority

When an item reaches the pipe center:

1. **Try inventories first** - Check connected inventories for space
2. **Fall back to pipes** - If no inventory accepts, route to pipe directions
3. **Drop if no route** - Only drops if no valid pipes or inventories

This makes insertion pipes "smart" about finding storage without needing manual filter configuration.

## Tips

- Place near storage systems to automatically route items to available space
- Doesn't filter by item type - will insert any item the inventory accepts
- Use after [Item Filter Pipes](item-filter-pipe.md) for sorted storage
- Good for general-purpose storage distribution
- Prevents item drops when storage is available
- Falls back gracefully to pipe network if all inventories full

## Common Patterns

**Auto-storage:**
```
[Source] → [Item Insertion Pipe] → [Chest A]
                                 → [Chest B]
                                 → [Chest C]
                (Routes to first available storage)
```

**Sorted storage:**
```
[Source] → [Filter] → [Ores] → [Insertion Pipe] → [Ore storage]
                ↓
          [Filter] → [Food] → [Insertion Pipe] → [Food storage]
```

**Overflow to void:**
```
[Source] → [Insertion Pipe] → [Storage]
                    ↓
          [Item Void Pipe] (catches overflow when storage full)
```

**Smart distribution:**
```
                       → [Chest A] (fills first)
[Items] → [Insertion] → [Chest B] (fills when A full)
                       → [Chest C] (fills when B full)
```

## See Also
- [Tier System](../core/tier-system.md) - Tier 2 smart pipes
- [Routing](../core/routing.md) - Inventory-preference routing
- [Item Filter Pipe](item-filter-pipe.md) - Item-type filtering
- [Item Void Pipe](item-void-pipe.md) - Overflow management
- [Connectivity](../core/connectivity.md) - Inventory connections
