<div class="infobox">
    <div class="infobox-header">Provider Logistics Pipe</div>
    <div class="infobox-image">
        <img src="../../assets/icons/logistics__pipe___provider_logistics_pipe.png" alt="Provider Logistics Pipe" title="Provider Logistics Pipe">
    </div>
    <table class="infobox-table">
        <tr>
            <td class="infobox-label">ID</td>
            <td class="infobox-value"><code>logistics:pipe/provider_logistics_pipe</code></td>
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

# Provider Logistics Pipe

The **Provider Logistics Pipe** is a [Tier 3](../core/tier-system.md) network logistics pipe that **advertises inventory contents to the network** and fulfills item requests. When a [Requester](requester-logistics-pipe.md) or [Supplier](supplier-logistics-pipe.md) pipe requests an item, the Provider extracts it from an adjacent inventory and routes it through the network to its destination.

## Recipe

<div class="crafting-recipe">
    <div class="crafting-grid">
        <div class="crafting-slot"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__core___bronze_gear.png" class="crafting-item" alt="Bronze Gear" title="Bronze Gear"></div>
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
        <img src="../../assets/icons/logistics__pipe___provider_logistics_pipe.png" class="crafting-item" alt="Provider Logistics Pipe" title="Provider Logistics Pipe">
        <span class="crafting-count">1</span>
    </div>
</div>

**Yields:** 1× Provider Logistics Pipe

## Behavior

The Provider Logistics Pipe connects to **all adjacent inventories simultaneously** — no wrench configuration needed to set an extraction face. Every second it scans connected inventories and updates the network cache with available item counts. When the network receives a request it cannot fulfill from elsewhere, this pipe extracts items and sends them with the requester's address attached.

**Key features:**
- Automatically connects to all adjacent inventories (no wrench required to set faces)
- Scans and registers available items with the network every second
- Fulfills network requests by extracting items and routing them to the requester
- Optional item filter (9 slots, whitelist or blacklist mode)
- Five **Provider Modes** control how much of the inventory is made available

## Provider Modes

The Provider Mode determines what portion of inventory contents is offered to the network. Configure via the [Wrench](../tools/wrench.md) GUI.

| Mode | Behavior |
|---|---|
| **Supply** | Provide all items — the full inventory is available to the network (default) |
| **Reserve** | Skip the first inventory slot — that slot is never extracted from |
| **Guarded** | Skip the first **and** last inventory slots — two slots are protected |
| **Seeded** | Always leave 1 item in each slot — ensures every slot stays seeded |
| **Sample** | Always leave 1 item of each type — maintains at least 1 of every item type in the inventory |

**Reserve** and **Guarded** are useful when a chest's first (or last) slot is used as a manual input/output that shouldn't be touched by the network. **Seeded** and **Sample** are useful for storage systems where you want to retain examples of every item.

## Item Filter

Use the optional filter to restrict which items this provider makes available:

- **9 filter slots** — place items to configure the filter
- **Whitelist** (default) — only items in the filter are provided to the network
- **Blacklist** (inverted) — items in the filter are withheld; everything else is provided
- Leave all filter slots empty to provide all items (subject to Provider Mode)

Configure via the [Wrench](../tools/wrench.md) GUI.

## Tips

- No wrench setup needed — just place adjacent to a chest and connect to a logistics network
- Use **Sample** mode when building a resource bank where you always want to keep at least one of everything
- Use **Reserve** or **Guarded** mode to protect manual access slots from the network pulling from them
- The item filter is useful for dedicating a provider to only certain item types, even if a chest holds mixed items
- Multiple Provider Pipes can service the same network — the network will pull from whichever has the requested item

## See Also
- [Basic Logistics Pipe](basic-logistics-pipe.md) - Required crafting component; the network backbone pipe
- [Requester Logistics Pipe](requester-logistics-pipe.md) - Manually request items this pipe provides
- [Supplier Logistics Pipe](supplier-logistics-pipe.md) - Automatically maintain stock using this provider
- [Pipe Networks](../core/pipe-networks.md) - How the logistics network works
- [Tier System](../core/tier-system.md) - Tier 3 network logistics
- [Wrench](../tools/wrench.md) - Open provider GUI to set mode and filter
- [Bronze Gear](../materials/bronze-gear.md) - Crafting component
