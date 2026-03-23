<div class="infobox">
    <div class="infobox-header">Satellite Logistics Pipe</div>
    <table class="infobox-table">
        <tr>
            <td class="infobox-label">ID</td>
            <td class="infobox-value"><code>logistics:pipe/satellite_logistics_pipe</code></td>
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

# Satellite Logistics Pipe

The **Satellite Logistics Pipe** is a [Tier 3](../core/tier-system.md) network logistics pipe that acts as a **named remote destination**. It gives a pipe location a network-addressable name so that other pipes — particularly [Chassis Logistics Pipes](chassis-logistics-pipe.md) with routing modules — can send items to it by name rather than by physical proximity.

## Recipe

<div class="crafting-recipe">
    <div class="crafting-grid">
        <div class="crafting-slot"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__glowstone_dust.png" class="crafting-item" alt="Glowstone Dust" title="Glowstone Dust"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__pipe___basic_logistics_pipe.png" class="crafting-item" alt="Basic Logistics Pipe" title="Basic Logistics Pipe"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__glowstone_dust.png" class="crafting-item" alt="Glowstone Dust" title="Glowstone Dust"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"></div>
    </div>
    <div class="crafting-arrow">→</div>
    <div class="crafting-output">
        <span class="crafting-count">1</span>
    </div>
</div>

**Yields:** 1× Satellite Logistics Pipe

## Behavior

The Satellite Logistics Pipe registers itself with the [logistics network](../core/pipe-networks.md) under a configurable name. Other network pipes can route items to this pipe by referencing its satellite name. This decouples routing logic from physical location — you can move storage and the sending pipes simply continue routing to the same named destination.

**Key features:**
- Registers a named destination on the logistics network
- Accepts items routed to it by name from other network pipes
- Deposits received items into an adjacent inventory
- Configurable name via GUI
- Multiple satellite pipes can share a name to distribute delivery across multiple inventories

## Configuration

Use a [Wrench](../tools/wrench.md) to open the GUI:

**Satellite name:** Enter a text name for this destination. Any pipe on the network that references this name will route items here.

**Filter slots:** Optionally restrict which items this satellite accepts to prevent misrouted items from landing in the wrong inventory.

## Tips

- Use descriptive names ("Iron Storage", "Fuel Chest", "Overflow") to keep large networks readable
- Multiple Satellite Pipes with the same name act as load-balanced destinations — the network distributes items across all matching satellites
- Rename a satellite when reorganizing storage without needing to reconfigure every sending pipe
- Combine with [Process Logistics Pipes](process-logistics-pipe.md) to deliver machine outputs to named satellite destinations

## See Also
- [Basic Logistics Pipe](basic-logistics-pipe.md) - Required crafting component; the network backbone pipe
- [Chassis Logistics Pipe](chassis-logistics-pipe.md) - Uses satellite names to route items to named destinations
- [Process Logistics Pipe](process-logistics-pipe.md) - Route machine outputs to satellite destinations
- [Pipe Networks](../core/pipe-networks.md) - How the logistics network works
- [Tier System](../core/tier-system.md) - Tier 3 network logistics
- [Wrench](../tools/wrench.md) - Configure satellite name and filter
