<div class="infobox">
    <div class="infobox-header">Supplier Module</div>
    <div class="infobox-image">
        <img src="../../assets/icons/logistics__pipe___passive_supplier_module.png" alt="Passive Supplier Module" title="Passive Supplier Module">
    </div>
    <table class="infobox-table">
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

# Supplier Module

The **Supplier Module** is a [Chassis Logistics Pipe](../pipes/chassis-logistics-pipe.md) module that maintains configured stock levels in an adjacent inventory by requesting items from the [logistics network](../core/pipe-networks.md). Two variants exist: **Passive** and **Active**.

## Variants

| Variant | Gear | Behavior |
|---------|------|----------|
| Passive Supplier Module | Iron Gear | Requests items only when the inventory falls below configured minimum — waits for a network trigger |
| Active Supplier Module | Bronze Gear | Proactively checks stock levels on a schedule and requests replenishment before the inventory empties |

## Recipes

### Passive Supplier Module

<div class="crafting-recipe">
    <div class="crafting-grid">
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__red_dye.png" class="crafting-item" alt="Red Dye" title="Red Dye"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__core___iron_gear.png" class="crafting-item" alt="Iron Gear" title="Iron Gear"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__black_dye.png" class="crafting-item" alt="Black Dye" title="Black Dye"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone" title="Redstone"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__pipe___blank_module.png" class="crafting-item" alt="Blank Module" title="Blank Module"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone" title="Redstone"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__black_dye.png" class="crafting-item" alt="Black Dye" title="Black Dye"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone" title="Redstone"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__red_dye.png" class="crafting-item" alt="Red Dye" title="Red Dye"></div>
    </div>
    <div class="crafting-arrow">→</div>
    <div class="crafting-output">
        <img src="../../assets/icons/logistics__pipe___passive_supplier_module.png" class="crafting-item" alt="Passive Supplier Module" title="Passive Supplier Module">
        <span class="crafting-count">1</span>
    </div>
</div>

**Yields:** 1× Passive Supplier Module

### Active Supplier Module

<div class="crafting-recipe">
    <div class="crafting-grid">
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__red_dye.png" class="crafting-item" alt="Red Dye" title="Red Dye"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__core___bronze_gear.png" class="crafting-item" alt="Bronze Gear" title="Bronze Gear"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__black_dye.png" class="crafting-item" alt="Black Dye" title="Black Dye"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone" title="Redstone"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__pipe___blank_module.png" class="crafting-item" alt="Blank Module" title="Blank Module"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone" title="Redstone"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__black_dye.png" class="crafting-item" alt="Black Dye" title="Black Dye"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone" title="Redstone"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__red_dye.png" class="crafting-item" alt="Red Dye" title="Red Dye"></div>
    </div>
    <div class="crafting-arrow">→</div>
    <div class="crafting-output">
        <img src="../../assets/icons/logistics__pipe___active_supplier_module.png" class="crafting-item" alt="Active Supplier Module" title="Active Supplier Module">
        <span class="crafting-count">1</span>
    </div>
</div>

**Yields:** 1× Active Supplier Module

## Behavior

When installed in a [Chassis Logistics Pipe](../pipes/chassis-logistics-pipe.md), this module monitors an adjacent inventory and requests items from the network to maintain configured stock levels.

**Passive:** Only requests items when triggered — for example, when the inventory reports falling below the minimum. Lower network overhead.

**Active:** Periodically polls the inventory and proactively orders replenishment before stock runs out. Better for machines that consume items continuously.

## Tips

- Use **Passive** for backup stockpiles where empty occasionally is acceptable
- Use **Active** for machine input buffers (furnaces, kilns) that must never run dry
- Configure minimum stock quantities per item type via the module GUI in the chassis
- Combine with a [Provider Module](provider-module.md) on a nearby chassis to create a closed-loop supply system

## See Also
- [Supplier Logistics Pipe](../pipes/supplier-logistics-pipe.md) - Standalone version of this behavior
- [Chassis Logistics Pipe](../pipes/chassis-logistics-pipe.md) - Required container
- [Blank Module](blank-module.md) - Crafting base
- [Provider Module](provider-module.md) - Supplies items that this module requests
- [Iron Gear](../materials/iron-gear.md) - Passive variant crafting component
- [Bronze Gear](../materials/bronze-gear.md) - Active variant crafting component
