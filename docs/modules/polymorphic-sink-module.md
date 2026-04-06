<div class="infobox">
    <div class="infobox-header">Polymorphic Sink Module</div>
    <div class="infobox-image">
        <img src="../../assets/icons/logistics__pipe___polymorphic_sink_module.png" alt="Polymorphic Sink Module" title="Polymorphic Sink Module">
    </div>
    <table class="infobox-table">
        <tr>
            <td class="infobox-label">ID</td>
            <td class="infobox-value"><code>logistics:pipe/polymorphic_sink_module</code></td>
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

# Polymorphic Sink Module

The **Polymorphic Sink Module** is a [Chassis Logistics Pipe](../pipes/chassis-logistics-pipe.md) module that accepts **any item type that already exists** in an adjacent inventory. It dynamically fills existing partial stacks rather than requiring a preconfigured item type like the [Item Sink Module](item-sink-module.md).

## Recipe

<div class="crafting-recipe">
    <div class="crafting-grid">
        <div class="crafting-slot"><img src="../../assets/icons/logistics__core___copper_dust.png" class="crafting-item" alt="Copper Dust" title="Copper Dust"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__core___echo_chip.png" class="crafting-item" alt="Echo Chip" title="Echo Chip"></div>
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
        <img src="../../assets/icons/logistics__pipe___polymorphic_sink_module.png" class="crafting-item" alt="Polymorphic Sink Module" title="Polymorphic Sink Module">
        <span class="crafting-count">1</span>
    </div>
</div>

**Yields:** 1× Polymorphic Sink Module — 2× [Copper Dust](../materials/dusts.md) + [Echo Chip](../materials/chips.md) + 2× Redstone + [Blank Module](blank-module.md)

## Behavior

When installed in a [Chassis Logistics Pipe](../pipes/chassis-logistics-pipe.md), this module scans the adjacent inventory for existing item types. Any network item that matches a type already present in that inventory is accepted by this module and deposited to fill or extend those stacks.

**Key features:**
- Accepts any item type that already has at least one item in the adjacent inventory
- Dynamically updates as the adjacent inventory contents change
- Will not accept items for which the adjacent inventory has no existing stack
- Ideal for refilling partially depleted storage without configuring each item type manually

## Tips

- Seed the adjacent inventory with one of each item type you want it to accept — the module handles the rest automatically
- Use for buffer chests that get replenished regularly — the module keeps them topped off
- Combine with an [Item Sink Module](item-sink-module.md) in the same chassis for a two-tier acceptance strategy: polymorphic for the bulk, specific sinks for overflow

## See Also
- [Item Sink Module](item-sink-module.md) - Accept a specifically configured item type
- [Mod Item Sink Module](mod-item-sink-module.md) - Accept all items from a specific mod namespace
- [Quicksort Module](quicksort-module.md) - Route items by type across multiple inventories
- [Chassis Logistics Pipe](../pipes/chassis-logistics-pipe.md) - Required container
- [Blank Module](blank-module.md) - Crafting base
- [Bronze Gear](../materials/bronze-gear.md) - Crafting component
