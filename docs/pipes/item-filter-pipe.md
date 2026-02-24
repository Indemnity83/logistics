<div class="infobox">
    <div class="infobox-header">Item Filter Pipe</div>
    <div class="infobox-image">
        <img src="/assets/icons/logistics__pipe___item_filter_pipe.png" alt="Item Filter Pipe">
    </div>
    <table class="infobox-table">
        <tr>
            <td class="infobox-label">ID</td>
            <td class="infobox-value"><code>logistics:pipe/item_filter_pipe</code></td>
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

# Item Filter Pipe

The **Item Filter Pipe** is a [Tier 2](../core/tier-system.md) smart pipe that routes items based on configurable per-side filters. It's the key to building item-aware sorting systems.

## Recipe

<div class="crafting-recipe">
    <div class="crafting-grid">
        <div class="crafting-slot"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"><img src="/assets/icons/minecraft__diamond.png" class="crafting-item" alt="Diamond"></div>
        <div class="crafting-slot"><img src="/assets/icons/minecraft__glass.png" class="crafting-item" alt="Glass"></div>
        <div class="crafting-slot"><img src="/assets/icons/minecraft__diamond.png" class="crafting-item" alt="Diamond"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"></div>
    </div>
    <div class="crafting-arrow">→</div>
    <div class="crafting-output">
        <img src="/assets/icons/logistics__pipe___item_filter_pipe.png" class="crafting-item" alt="Item Filter Pipe">
        <span class="crafting-count">8</span>
    </div>
</div>

**Yields:** 8× Item Filter Pipe

## Behavior

Item Filter Pipes inspect items and route them to specific directions based on configured filters. Each connected face can have its own filter, allowing complex item sorting in a single pipe.

**Key features:**
- Item-aware routing (inspects what's flowing through)
- Configurable per-side filters
- Multiple items per filter
- Unmatched items route to unfiltered directions
- Items drop if no valid route exists

## Configuration

Right-click the pipe to open the filter GUI:

1. GUI shows all connected faces
2. Click a face's slot to set which items route to that direction
3. Multiple items can be assigned to each face
4. Empty filter slots mean "no filter" (accepts unmatched items)
5. Changes apply immediately

## Routing Logic

When an item reaches the pipe center:

1. Check all filtered faces for item match
2. If match found, route to that direction
3. If no match, route to **unfiltered** directions (empty filter slots)
4. If no match and no unfiltered directions, **item drops**

**Important:** Unfiltered faces act as "catch-all" for items that don't match any filter.

## Tips

- Open filter GUI with empty hand (not wrench)
- Use unfiltered outputs as "everything else" destinations
- Items drop if they match nothing and all faces are filtered
- Combine with [Item Void Pipe](item-void-pipe.md) for filtered deletion
- Place [Item Insertion Pipes](item-insertion-pipe.md) after filters for storage
- Diamond recipe reflects advanced functionality

## Common Patterns

**Simple sorting:**
```
[Mixed items] → [Item Filter Pipe] → [Iron/Gold] → [Chest A]
                        ↓
                [Everything else] → [Chest B]
```

**Multi-way sorting:**
```
                    → [Ores] → [Storage A]
[Source] → [Filter] → [Food] → [Storage B]
                    → [Tools] → [Storage C]
                    → [Other] → [Catch-all]
```

**Filtered voiding:**
```
[Source] → [Filter] → [Wanted items] → [Storage]
                ↓
          [Cobblestone/dirt] → [Item Void Pipe]
```

## See Also
- [Tier System](../core/tier-system.md) - Tier 2 smart pipes
- [Routing](../core/routing.md) - Filter-based routing logic
- [Item Insertion Pipe](item-insertion-pipe.md) - Pair with filters for storage
- [Item Void Pipe](item-void-pipe.md) - Delete filtered items
- [Copper Transport Pipe](copper-transport-pipe.md) - Connect to network
