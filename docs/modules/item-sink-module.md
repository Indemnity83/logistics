<div class="infobox">
    <div class="infobox-header">Item Sink Module</div>
    <div class="infobox-image">
        <img src="../../assets/icons/logistics__pipe___item_sink_module.png" alt="Item Sink Module" title="Item Sink Module">
    </div>
    <table class="infobox-table">
        <tr>
            <td class="infobox-label">ID</td>
            <td class="infobox-value"><code>logistics:pipe/item_sink_module</code></td>
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

# Item Sink Module

The **Item Sink Module** is a [Chassis Logistics Pipe](../pipes/chassis-logistics-pipe.md) module that accepts a **specific configured item type** from the [logistics network](../core/pipe-networks.md) and deposits it into an adjacent inventory. It is the module-form equivalent of a [Basic Logistics Pipe](../pipes/basic-logistics-pipe.md) with a strict item filter.

## Recipe

<div class="crafting-recipe">
    <div class="crafting-grid">
        <div class="crafting-slot"><img src="../../assets/icons/logistics__core___copper_dust.png" class="crafting-item" alt="Copper Dust" title="Copper Dust"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__core___carbon_chip.png" class="crafting-item" alt="Carbon Chip" title="Carbon Chip"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__core___copper_dust.png" class="crafting-item" alt="Copper Dust" title="Copper Dust"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone" title="Redstone"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__pipe___blank_module.png" class="crafting-item" alt="Blank Module" title="Blank Module"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone" title="Redstone"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"></div>
    </div>
    <div class="crafting-arrow">→</div>
    <div class="crafting-output">
        <img src="../../assets/icons/logistics__pipe___item_sink_module.png" class="crafting-item" alt="Item Sink Module" title="Item Sink Module">
        <span class="crafting-count">1</span>
    </div>
</div>

**Yields:** 1× Item Sink Module — 2× [Copper Dust](../materials/dusts.md) + [Carbon Chip](../materials/chips.md) + 2× Redstone + [Blank Module](blank-module.md)

## Behavior

When installed in a [Chassis Logistics Pipe](../pipes/chassis-logistics-pipe.md), this module registers itself as a delivery destination for a configured item type. Network items of the matching type are routed to this chassis and deposited into an adjacent inventory.

**Key features:**
- Accepts one configured item type from the network
- Deposits received items into an adjacent inventory
- Multiple Item Sink Modules can be installed in a single chassis (using multiple module slots) to accept multiple item types
- Does not accept unconfigured or non-matching items

## Configuration

Use a [Wrench](../tools/wrench.md) to open the chassis GUI, then configure the module:

**Target item slot:** Place the item type this module should accept. Only items matching this type will be routed here.

## Tips

- Install multiple Item Sink Modules in an MK3+ chassis to create a multi-item sorting station in one pipe
- Use alongside a [Quicksort Module](quicksort-module.md) for a tiered approach: quicksort handles known items, Item Sink catches the rest
- Leave adjacent storage slots pre-stocked with the target item to help the module recognize the correct inventory slot

## See Also
- [Polymorphic Sink Module](polymorphic-sink-module.md) - Accepts any item type that already exists in the adjacent inventory
- [Mod Item Sink Module](mod-item-sink-module.md) - Accepts all items from a specific mod namespace
- [Chassis Logistics Pipe](../pipes/chassis-logistics-pipe.md) - Required container
- [Blank Module](blank-module.md) - Crafting base
- [Iron Gear](../materials/iron-gear.md) - Crafting component
