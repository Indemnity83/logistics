<div class="infobox">
    <div class="infobox-header">Enchantment Sink Module</div>
    <div class="infobox-image">
        <img src="../../assets/icons/logistics__pipe___enchantment_sink_module.png" alt="Enchantment Sink Module" title="Enchantment Sink Module">
    </div>
    <table class="infobox-table">
        <tr>
            <td class="infobox-label">ID</td>
            <td class="infobox-value"><code>logistics:pipe/enchantment_sink_module</code></td>
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

# Enchantment Sink Module

The **Enchantment Sink Module** is a [Chassis Logistics Pipe](../pipes/chassis-logistics-pipe.md) module that accepts **any enchanted item** from the [logistics network](../core/pipe-networks.md) and deposits it into an adjacent inventory. It acts as a catch-all for enchanted tools, armor, and books passing through the network.

## Recipe

<div class="crafting-recipe">
    <div class="crafting-grid">
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__black_dye.png" class="crafting-item" alt="Black Dye" title="Black Dye"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__book.png" class="crafting-item" alt="Book" title="Book"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__black_dye.png" class="crafting-item" alt="Black Dye" title="Black Dye"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone" title="Redstone"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__pipe___blank_module.png" class="crafting-item" alt="Blank Module" title="Blank Module"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone" title="Redstone"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__black_dye.png" class="crafting-item" alt="Black Dye" title="Black Dye"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone" title="Redstone"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__black_dye.png" class="crafting-item" alt="Black Dye" title="Black Dye"></div>
    </div>
    <div class="crafting-arrow">→</div>
    <div class="crafting-output">
        <img src="../../assets/icons/logistics__pipe___enchantment_sink_module.png" class="crafting-item" alt="Enchantment Sink Module" title="Enchantment Sink Module">
        <span class="crafting-count">1</span>
    </div>
</div>

**Yields:** 1× Enchantment Sink Module

## Behavior

When installed in a [Chassis Logistics Pipe](../pipes/chassis-logistics-pipe.md), this module checks every item arriving at the chassis for enchantment data. If an item carries any enchantment (including enchanted books), this module claims it and deposits it into the adjacent inventory. Non-enchanted items of the same type are ignored.

**Key features:**
- Accepts any item with enchantment NBT data
- Works for all enchanted item types: tools, armor, books, weapons
- Does not filter by enchantment type — any enchantment triggers acceptance
- Useful for automatically segregating enchanted loot from non-enchanted items

## Tips

- Place adjacent to an enchanted-item storage chest to automatically collect all enchanted drops from mob farms or quarry output
- Combine with a [Mod Item Sink Module](mod-item-sink-module.md) to separately route both enchanted items and specific mod items in the same chassis
- The book ingredient reflects the module's purpose: capturing enchanted books and items

## See Also
- [Item Sink Module](item-sink-module.md) - Accept a specific configured item type
- [Mod Item Sink Module](mod-item-sink-module.md) - Accept all items from a specific mod namespace
- [Chassis Logistics Pipe](../pipes/chassis-logistics-pipe.md) - Required container
- [Blank Module](blank-module.md) - Crafting base
