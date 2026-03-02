<div class="infobox">
    <div class="infobox-header">Supplier Logistics Pipe</div>
    <div class="infobox-image">
        <img src="../../assets/icons/logistics__pipe___supplier_logistics_pipe.png" alt="Supplier Logistics Pipe" title="Supplier Logistics Pipe">
    </div>
    <table class="infobox-table">
        <tr>
            <td class="infobox-label">ID</td>
            <td class="infobox-value"><code>logistics:pipe/supplier_logistics_pipe</code></td>
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

# Supplier Logistics Pipe

The **Supplier Logistics Pipe** is a [Tier 3](../core/tier-system.md) network logistics pipe that **automatically maintains stock levels** in an adjacent inventory. You configure which items should be kept and in what quantity; the pipe monitors the inventory and requests more from the network whenever supply runs low.

## Recipe

<div class="crafting-recipe">
    <div class="crafting-grid">
        <div class="crafting-slot"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__core___gold_gear.png" class="crafting-item" alt="Gold Gear" title="Gold Gear"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone" title="Redstone"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__pipe___basic_logistics_pipe.png" class="crafting-item" alt="Basic Logistics Pipe" title="Basic Logistics Pipe"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone" title="Redstone"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"></div>
    </div>
    <div class="crafting-arrow">→</div>
    <div class="crafting-output">
        <img src="../../assets/icons/logistics__pipe___supplier_logistics_pipe.png" class="crafting-item" alt="Supplier Logistics Pipe" title="Supplier Logistics Pipe">
        <span class="crafting-count">1</span>
    </div>
</div>

**Yields:** 1× Supplier Logistics Pipe

## Behavior

The Supplier Logistics Pipe checks the adjacent inventory every second against up to 9 configured supply targets. If any item falls below its target amount, the pipe places a request into the [logistics network](../core/pipe-networks.md). A [Provider Logistics Pipe](provider-logistics-pipe.md) elsewhere on the network fulfills the request and routes items back to this pipe, which then deposits them into the connected inventory.

**Key features:**
- Monitors one adjacent inventory for stock levels every second
- Up to 9 configurable supply slots (item + target amount each)
- Tracks items in transit to avoid over-requesting
- Four **Supply Modes** control when and how much to request
- Items delivered by the network are automatically deposited into the connected inventory

## Supply Modes

Supply Modes control the requesting strategy. Configure via the [Wrench](../tools/wrench.md) GUI.

| Mode | Behavior |
|---|---|
| **Partial** | Request whatever is available, up to the needed amount (default) |
| **Stocked** | Only request when inventory drops to **50% or below** the target — then request the full shortfall |
| **Infinite** | Ignore the target amount; continuously fill all available space, one stack at a time |
| **Full** | Only request if the **complete shortfall** is available — all-or-nothing |

**Partial** is the standard mode and works well in most situations. **Stocked** is useful for bulk storage where you don't want constant small top-ups — it waits until supply is genuinely low before ordering a large batch. **Infinite** is for always-full buffers where you want to maximize inventory space regardless of a specific target. **Full** is useful when partial deliveries would cause problems (e.g., a process that needs an exact batch size).

## Configuration

Use a [Wrench](../tools/wrench.md) to open the supplier GUI:

1. Each of the 9 supply slots takes an item and a target amount
2. Set the item by placing or clicking an item into the slot
3. Set the target amount — the pipe will maintain at least this many in the inventory
4. Choose a Supply Mode (applies to all supply slots)

## Tips

- The pipe tracks items currently in transit to prevent duplicate requests; it won't re-order until deliveries are received or the request times out
- Use **Stocked** mode on large buffer chests to reduce network chatter — orders arrive in bulk rather than constantly trickling in
- Use **Infinite** mode on a crafting station's input chest to keep it topped up at all times
- Use **Full** mode when you need a guaranteed minimum batch (e.g., smelting processes that need a full stack)
- Only one inventory face is used for delivery; the pipe automatically selects the first available adjacent inventory
- Combine with a [Provider Logistics Pipe](provider-logistics-pipe.md) on the same chest to both provide to and replenish from the network

## See Also
- [Basic Logistics Pipe](basic-logistics-pipe.md) - Required crafting component
- [Provider Logistics Pipe](provider-logistics-pipe.md) - Fulfills the requests this pipe places
- [Requester Logistics Pipe](requester-logistics-pipe.md) - Manual counterpart to automatic supply
- [Pipe Networks](../core/pipe-networks.md) - How the logistics network works
- [Tier System](../core/tier-system.md) - Tier 3 network logistics
- [Wrench](../tools/wrench.md) - Open supplier GUI to configure supply slots and mode
- [Gold Gear](../materials/gold-gear.md) - Crafting component
