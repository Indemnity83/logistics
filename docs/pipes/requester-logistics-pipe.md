<div class="infobox">
    <div class="infobox-header">Requester Logistics Pipe</div>
    <div class="infobox-image">
        <img src="../../assets/icons/logistics__pipe___requester_logistics_pipe.png" alt="Requester Logistics Pipe" title="Requester Logistics Pipe">
    </div>
    <table class="infobox-table">
        <tr>
            <td class="infobox-label">ID</td>
            <td class="infobox-value"><code>logistics:pipe/requester_logistics_pipe</code></td>
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

# Requester Logistics Pipe

The **Requester Logistics Pipe** is a [Tier 3](../core/tier-system.md) network logistics pipe that lets you **manually request items from the network**. Open the GUI to browse everything available across all [Provider Logistics Pipes](provider-logistics-pipe.md) on the network, then click to pull specific items directly to the connected inventory.

## Recipe

<div class="crafting-recipe">
    <div class="crafting-grid">
        <div class="crafting-slot"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__core___gold_gear.png" class="crafting-item" alt="Gold Gear" title="Gold Gear"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__core___gold_gear.png" class="crafting-item" alt="Gold Gear" title="Gold Gear"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__pipe___basic_logistics_pipe.png" class="crafting-item" alt="Basic Logistics Pipe" title="Basic Logistics Pipe"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__core___gold_gear.png" class="crafting-item" alt="Gold Gear" title="Gold Gear"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__core___gold_gear.png" class="crafting-item" alt="Gold Gear" title="Gold Gear"></div>
        <div class="crafting-slot"></div>
    </div>
    <div class="crafting-arrow">→</div>
    <div class="crafting-output">
        <img src="../../assets/icons/logistics__pipe___requester_logistics_pipe.png" class="crafting-item" alt="Requester Logistics Pipe" title="Requester Logistics Pipe">
        <span class="crafting-count">1</span>
    </div>
</div>

**Yields:** 1× Requester Logistics Pipe

## Behavior

The Requester Logistics Pipe queries the [logistics network](../core/pipe-networks.md) for available items and places requests on your behalf. When a request is fulfilled, a [Provider Logistics Pipe](provider-logistics-pipe.md) extracts the items and routes them through the network to this pipe, which then deposits them into the connected inventory.

**Key features:**
- Browse the full network inventory via a searchable, paginated item grid
- Manually request any available item in any quantity
- Automatically deposits delivered items into the connected inventory

## Configuration

Use a [Wrench](../tools/wrench.md) to open the requester GUI:

- The GUI shows all items currently available across all providers on the network
- Search by name using one or more space-separated keywords
- Browse pages if more than 32 items are available
- Click an item, enter the quantity you want, and confirm to place the request

## Tips

- Place next to a chest to use as an on-demand item terminal — open the GUI and pull whatever you need
- For recurring needs (e.g., always keeping a specific amount of something on hand), use a [Supplier Logistics Pipe](supplier-logistics-pipe.md) instead — it monitors inventory levels and re-orders automatically
- The network browser shows live availability; if a provider runs out, that item's count drops to zero
- Items are routed to the adjacent inventory automatically when delivered — no manual collection needed

## See Also
- [Basic Logistics Pipe](basic-logistics-pipe.md) - Required crafting component
- [Provider Logistics Pipe](provider-logistics-pipe.md) - Provides the items this pipe requests
- [Supplier Logistics Pipe](supplier-logistics-pipe.md) - Automatic counterpart; monitors and restocks inventory
- [Pipe Networks](../core/pipe-networks.md) - How the logistics network works
- [Tier System](../core/tier-system.md) - Tier 3 network logistics
- [Wrench](../tools/wrench.md) - Open requester GUI to browse and request items
- [Gold Gear](../materials/gold-gear.md) - Crafting component
