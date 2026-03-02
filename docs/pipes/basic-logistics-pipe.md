<div class="infobox">
    <div class="infobox-header">Basic Logistics Pipe</div>
    <div class="infobox-image">
        <img src="../../assets/icons/logistics__pipe___basic_logistics_pipe.png" alt="Basic Logistics Pipe" title="Basic Logistics Pipe">
    </div>
    <table class="infobox-table">
        <tr>
            <td class="infobox-label">ID</td>
            <td class="infobox-value"><code>logistics:pipe/basic_logistics_pipe</code></td>
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
            <td class="infobox-value"><span class="infobox-tier infobox-tier-3">Tier 3 - Network</span></td>
        </tr>
        <tr>
            <td class="infobox-label">Added</td>
            <td class="infobox-value">v0.2.0</td>
        </tr>
    </table>
</div>

# Basic Logistics Pipe

The **Basic Logistics Pipe** is a [Tier 3](../core/tier-system.md) network logistics pipe that acts as a **sink** in the [logistics network](../core/pipe-networks.md). It accepts items arriving from the network and deposits them into an adjacent inventory. It can be configured to accept specific items by filter, items addressed specifically to it, or act as the network's default fallback route.

## Recipe

<div class="crafting-recipe">
    <div class="crafting-grid">
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__glass.png" class="crafting-item" alt="Glass" title="Glass"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__redstone_torch.png" class="crafting-item" alt="Redstone Torch" title="Redstone Torch"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__glass.png" class="crafting-item" alt="Glass" title="Glass"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__pipe___copper_transport_pipe.png" class="crafting-item" alt="Copper Transport Pipe" title="Copper Transport Pipe"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__pipe___item_filter_pipe.png" class="crafting-item" alt="Item Filter Pipe" title="Item Filter Pipe"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__pipe___copper_transport_pipe.png" class="crafting-item" alt="Copper Transport Pipe" title="Copper Transport Pipe"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__core___gold_gear.png" class="crafting-item" alt="Gold Gear" title="Gold Gear"></div>
        <div class="crafting-slot"></div>
    </div>
    <div class="crafting-arrow">→</div>
    <div class="crafting-output">
        <img src="../../assets/icons/logistics__pipe___basic_logistics_pipe.png" class="crafting-item" alt="Basic Logistics Pipe" title="Basic Logistics Pipe">
        <span class="crafting-count">8</span>
    </div>
</div>

**Yields:** 8× Basic Logistics Pipe

## Behavior

The Basic Logistics Pipe is the entry point into Tier 3 network logistics. When connected to an adjacent inventory and a logistics network, it acts as a **delivery destination** — network items addressed to this pipe are routed through the network and inserted into the connected inventory.

**Key features:**
- Accepts items from the [logistics network](../core/pipe-networks.md) and deposits them into an adjacent inventory
- Configurable item filter (9 slots) — only accept specific item types
- Optional **Default Route** mode — catch-all for any network item with no explicit destination
- Uses network-aware A\* pathfinding for efficient routing
- Automatically selects the first adjacent inventory as its sink face

## Configuration

Use a [Wrench](../tools/wrench.md) to open the GUI:

**Filter slots (9 slots):** Place items in filter slots to restrict which items this pipe accepts. Only items matching the filter will be deposited. Leave all slots empty to accept any item addressed to this pipe.

**Default Route toggle:** When enabled, the pipe accepts any item that has no explicit destination and has run out of other routing options. This is the network's "catch-all" — useful for a central chest that should receive everything that doesn't go anywhere else.

### Routing Priority

When an item arrives at this pipe, it checks in this order:

1. **Filter match** — item matches a configured filter slot → deposit immediately
2. **Destination match** — item is explicitly addressed to this pipe → deposit
3. **Default route** — Default Route is enabled and the item has no other options → deposit

## Tips

- Place next to a chest to create a simple network delivery point
- Use the item filter to dedicate a pipe (and chest) to specific item types
- Only one Default Route pipe should exist per network — having multiple can cause unpredictable delivery behavior
- This pipe is the foundation for [Provider](provider-logistics-pipe.md), [Supplier](supplier-logistics-pipe.md), and [Requester](requester-logistics-pipe.md) pipes, which are all crafted from it
- Connect to a network using [Copper Transport Pipes](copper-transport-pipe.md)

## See Also
- [Provider Logistics Pipe](provider-logistics-pipe.md) - Expose inventory contents to the network
- [Supplier Logistics Pipe](supplier-logistics-pipe.md) - Maintain inventory stock levels automatically
- [Requester Logistics Pipe](requester-logistics-pipe.md) - Manually request items from the network
- [Pipe Networks](../core/pipe-networks.md) - Understanding the logistics network
- [Tier System](../core/tier-system.md) - Tier 3 network logistics
- [Wrench](../tools/wrench.md) - Configure filter and default route
- [Gold Gear](../materials/gold-gear.md) - Crafting component
- [Item Filter Pipe](item-filter-pipe.md) - Crafting component
- [Copper Transport Pipe](copper-transport-pipe.md) - Backbone network connectivity
