<div class="infobox">
    <div class="infobox-header">Terminus Module</div>
    <div class="infobox-image">
        <img src="../../assets/icons/logistics__pipe___terminus_module.png" alt="Terminus Module" title="Terminus Module">
    </div>
    <table class="infobox-table">
        <tr>
            <td class="infobox-label">ID</td>
            <td class="infobox-value"><code>logistics:pipe/terminus_module</code></td>
        </tr>
        <tr>
            <td class="infobox-label">Type</td>
            <td class="infobox-value">Item (Module)</td>
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

# Terminus Module

The **Terminus Module** is a [Chassis Logistics Pipe](../pipes/chassis-logistics-pipe.md) module that marks the chassis as a **network terminus** — a defined end-point in the [logistics network](../core/pipe-networks.md). Items routed to this terminus are held or redirected according to the module's configuration.

## Recipe

<div class="crafting-recipe">
    <div class="crafting-grid">
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__purple_dye.png" class="crafting-item" alt="Purple Dye" title="Purple Dye"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__ender_pearl.png" class="crafting-item" alt="Ender Pearl" title="Ender Pearl"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__purple_dye.png" class="crafting-item" alt="Purple Dye" title="Purple Dye"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone" title="Redstone"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__pipe___blank_module.png" class="crafting-item" alt="Blank Module" title="Blank Module"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone" title="Redstone"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__purple_dye.png" class="crafting-item" alt="Purple Dye" title="Purple Dye"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone" title="Redstone"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__purple_dye.png" class="crafting-item" alt="Purple Dye" title="Purple Dye"></div>
    </div>
    <div class="crafting-arrow">→</div>
    <div class="crafting-output">
        <img src="../../assets/icons/logistics__pipe___terminus_module.png" class="crafting-item" alt="Terminus Module" title="Terminus Module">
        <span class="crafting-count">1</span>
    </div>
</div>

**Yields:** 1× Terminus Module

## Behavior

When installed in a [Chassis Logistics Pipe](../pipes/chassis-logistics-pipe.md), the Terminus Module prevents items from passing further through the network at this pipe. Items that arrive here are deposited into an adjacent inventory. It effectively creates a dead-end in the network, ensuring items don't loop or get lost.

**Key features:**
- Marks the chassis as a network dead-end
- Items arriving at this point are deposited into an adjacent inventory
- Prevents routing loops in complex networks
- Can be combined with filter modules to create a selective terminus

## Tips

- Use at the physical end of a pipe run to prevent items from bouncing back into the network
- Combine with an [Item Sink Module](item-sink-module.md) to deposit specific item types at this terminus
- The ender pearl ingredient reflects its role in "closing" or "teleporting" items to a final destination

## See Also
- [Chassis Logistics Pipe](../pipes/chassis-logistics-pipe.md) - Required container
- [Blank Module](blank-module.md) - Crafting base
- [Item Sink Module](item-sink-module.md) - Accept specific items at the terminus
- [Satellite Logistics Pipe](../pipes/satellite-logistics-pipe.md) - Named remote destinations
- [Pipe Networks](../core/pipe-networks.md) - Network topology concepts
