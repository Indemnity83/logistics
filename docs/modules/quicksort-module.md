<div class="infobox">
    <div class="infobox-header">Quicksort Module</div>
    <div class="infobox-image">
        <img src="../../assets/icons/logistics__pipe___quicksort_module.png" alt="Quicksort Module" title="Quicksort Module">
    </div>
    <table class="infobox-table">
        <tr>
            <td class="infobox-label">ID</td>
            <td class="infobox-value"><code>logistics:pipe/quicksort_module</code></td>
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

# Quicksort Module

The **Quicksort Module** is a [Chassis Logistics Pipe](../pipes/chassis-logistics-pipe.md) module that **automatically sorts items** into designated storage locations within the [logistics network](../core/pipe-networks.md). It scans available storage and routes items to inventories that already contain matching item types.

## Recipe

<div class="crafting-recipe">
    <div class="crafting-grid">
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__cyan_dye.png" class="crafting-item" alt="Cyan Dye" title="Cyan Dye"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__core___diamond_gear.png" class="crafting-item" alt="Diamond Gear" title="Diamond Gear"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__cyan_dye.png" class="crafting-item" alt="Cyan Dye" title="Cyan Dye"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone" title="Redstone"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__pipe___blank_module.png" class="crafting-item" alt="Blank Module" title="Blank Module"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone" title="Redstone"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__cyan_dye.png" class="crafting-item" alt="Cyan Dye" title="Cyan Dye"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone" title="Redstone"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__cyan_dye.png" class="crafting-item" alt="Cyan Dye" title="Cyan Dye"></div>
    </div>
    <div class="crafting-arrow">→</div>
    <div class="crafting-output">
        <img src="../../assets/icons/logistics__pipe___quicksort_module.png" class="crafting-item" alt="Quicksort Module" title="Quicksort Module">
        <span class="crafting-count">1</span>
    </div>
</div>

**Yields:** 1× Quicksort Module

## Behavior

When installed in a [Chassis Logistics Pipe](../pipes/chassis-logistics-pipe.md), the Quicksort Module intercepts items arriving at the chassis from the network and intelligently routes each item to a storage location that already holds that item type. This fills existing stacks before opening new storage slots, keeping your network organized automatically.

**Key features:**
- Routes items to existing partial stacks of the same type
- Reduces storage fragmentation by consolidating item types
- Requires [Provider Logistics Pipes](../pipes/provider-logistics-pipe.md) nearby to know where items are stored
- Items with no existing storage destination fall through to the default route

## Tips

- Place in a chassis near your main storage array — items passing through will be sorted as they arrive
- Works best when paired with a well-organized storage system where each inventory type stores one item category
- If an item has no existing home, it may land on the default route — add an [Item Sink Module](item-sink-module.md) as a fallback

## See Also
- [Chassis Logistics Pipe](../pipes/chassis-logistics-pipe.md) - Required container
- [Blank Module](blank-module.md) - Crafting base
- [Item Sink Module](item-sink-module.md) - Accept specific item types as fallback
- [Polymorphic Sink Module](polymorphic-sink-module.md) - Fills existing stacks of any type
- [Provider Logistics Pipe](../pipes/provider-logistics-pipe.md) - Provides location information for sorting
- [Diamond Gear](../materials/diamond-gear.md) - Crafting component
