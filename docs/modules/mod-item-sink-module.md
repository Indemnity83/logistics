<div class="infobox">
    <div class="infobox-header">Mod Item Sink Module</div>
    <div class="infobox-image">
        <img src="../../assets/icons/logistics__pipe___mod_item_sink_module.png" alt="Mod Item Sink Module" title="Mod Item Sink Module">
    </div>
    <table class="infobox-table">
        <tr>
            <td class="infobox-label">ID</td>
            <td class="infobox-value"><code>logistics:pipe/mod_item_sink_module</code></td>
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

# Mod Item Sink Module

The **Mod Item Sink Module** is a [Chassis Logistics Pipe](../pipes/chassis-logistics-pipe.md) module that accepts **all items from a configured mod namespace** from the [logistics network](../core/pipe-networks.md). It routes items from a specific mod into a designated adjacent inventory, regardless of item type.

## Recipe

<div class="crafting-recipe">
    <div class="crafting-grid">
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__green_dye.png" class="crafting-item" alt="Green Dye" title="Green Dye"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__core___bronze_gear.png" class="crafting-item" alt="Bronze Gear" title="Bronze Gear"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__green_dye.png" class="crafting-item" alt="Green Dye" title="Green Dye"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone" title="Redstone"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__pipe___blank_module.png" class="crafting-item" alt="Blank Module" title="Blank Module"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone" title="Redstone"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__green_dye.png" class="crafting-item" alt="Green Dye" title="Green Dye"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone" title="Redstone"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__green_dye.png" class="crafting-item" alt="Green Dye" title="Green Dye"></div>
    </div>
    <div class="crafting-arrow">→</div>
    <div class="crafting-output">
        <img src="../../assets/icons/logistics__pipe___mod_item_sink_module.png" class="crafting-item" alt="Mod Item Sink Module" title="Mod Item Sink Module">
        <span class="crafting-count">1</span>
    </div>
</div>

**Yields:** 1× Mod Item Sink Module

## Behavior

When installed in a [Chassis Logistics Pipe](../pipes/chassis-logistics-pipe.md), this module checks the namespace (mod ID) of every item arriving at the chassis. If an item's namespace matches the configured mod ID, the module claims it and deposits it into the adjacent inventory. Items from other mods or vanilla Minecraft pass through.

**Key features:**
- Accepts all items from a single configured mod namespace (e.g., `create`, `thermal`, `ae2`)
- Namespace-based matching — any item whose item ID begins with `modid:` is accepted
- Useful for modpacks where you want to store all items from a specific mod together
- Multiple Mod Item Sink Modules can be combined in one chassis to cover multiple mods

## Configuration

Use a [Wrench](../tools/wrench.md) to open the chassis GUI, then configure the module:

**Mod namespace field:** Enter the mod ID of the mod whose items this module should accept (e.g., `logistics`, `minecraft`, `create`).

## Tips

- Particularly useful in large modpacks for keeping mod-specific resources organized in dedicated storage areas
- Combine multiple Mod Item Sink Modules in an MK3+ chassis to route items from several mods simultaneously
- Use alongside an [Item Sink Module](item-sink-module.md) to handle specific items and a Mod Item Sink for broader catch-all routing

## See Also
- [Item Sink Module](item-sink-module.md) - Accept a specific configured item type
- [Polymorphic Sink Module](polymorphic-sink-module.md) - Accept items that already exist in the adjacent inventory
- [Enchantment Sink Module](enchantment-sink-module.md) - Accept any enchanted item
- [Chassis Logistics Pipe](../pipes/chassis-logistics-pipe.md) - Required container
- [Blank Module](blank-module.md) - Crafting base
- [Bronze Gear](../materials/bronze-gear.md) - Crafting component
