<div class="infobox">
    <div class="infobox-header">Extractor Module</div>
    <div class="infobox-image">
        <img src="../../assets/icons/logistics__pipe___extractor_module.png" alt="Extractor Module" title="Extractor Module">
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

# Extractor Module

The **Extractor Module** is a [Chassis Logistics Pipe](../pipes/chassis-logistics-pipe.md) module that actively pulls items from an adjacent inventory and injects them into the [logistics network](../core/pipe-networks.md). Three mark variants exist, with higher marks extracting at greater speed or quantity.

## Variants

| Variant | Gear | Notes |
|---------|------|-------|
| Extractor Module (MK1) | Iron Gear | Standard extraction speed |
| Extractor Module MK2 | Gold Gear | Faster extraction or larger stack sizes |
| Extractor Module MK3 | Diamond Gear | Highest extraction throughput |

## Recipes

### Extractor Module (MK1)

<div class="crafting-recipe">
    <div class="crafting-grid">
        <div class="crafting-slot"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__core___iron_gear.png" class="crafting-item" alt="Iron Gear" title="Iron Gear"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone" title="Redstone"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__pipe___blank_module.png" class="crafting-item" alt="Blank Module" title="Blank Module"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone" title="Redstone"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone" title="Redstone"></div>
        <div class="crafting-slot"></div>
    </div>
    <div class="crafting-arrow">→</div>
    <div class="crafting-output">
        <img src="../../assets/icons/logistics__pipe___extractor_module.png" class="crafting-item" alt="Extractor Module" title="Extractor Module">
        <span class="crafting-count">1</span>
    </div>
</div>

**Yields:** 1× Extractor Module (MK1)

### Extractor Module MK2

<div class="crafting-recipe">
    <div class="crafting-grid">
        <div class="crafting-slot"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__core___gold_gear.png" class="crafting-item" alt="Gold Gear" title="Gold Gear"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone" title="Redstone"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__pipe___blank_module.png" class="crafting-item" alt="Blank Module" title="Blank Module"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone" title="Redstone"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone" title="Redstone"></div>
        <div class="crafting-slot"></div>
    </div>
    <div class="crafting-arrow">→</div>
    <div class="crafting-output">
        <img src="../../assets/icons/logistics__pipe___extractor_module_mkii.png" class="crafting-item" alt="Extractor Module MK2" title="Extractor Module MK2">
        <span class="crafting-count">1</span>
    </div>
</div>

**Yields:** 1× Extractor Module MK2

### Extractor Module MK3

<div class="crafting-recipe">
    <div class="crafting-grid">
        <div class="crafting-slot"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__core___diamond_gear.png" class="crafting-item" alt="Diamond Gear" title="Diamond Gear"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone" title="Redstone"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__pipe___blank_module.png" class="crafting-item" alt="Blank Module" title="Blank Module"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone" title="Redstone"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone" title="Redstone"></div>
        <div class="crafting-slot"></div>
    </div>
    <div class="crafting-arrow">→</div>
    <div class="crafting-output">
        <img src="../../assets/icons/logistics__pipe___extractor_module_mkiii.png" class="crafting-item" alt="Extractor Module MK3" title="Extractor Module MK3">
        <span class="crafting-count">1</span>
    </div>
</div>

**Yields:** 1× Extractor Module MK3

## Behavior

When installed in a [Chassis Logistics Pipe](../pipes/chassis-logistics-pipe.md), this module pulls items from an adjacent inventory and routes them into the logistics network. It functions similarly to the standalone [Item Extractor Pipe](../pipes/item-extractor-pipe.md) but is network-aware and can be combined with other modules in the same chassis.

**Key features:**
- Extracts items from an adjacent inventory into the logistics network
- Configurable item filter — extract only specific item types
- Higher mark variants extract faster or in larger quantities per tick
- Can be configured to route extracted items to a specific network destination

## Tips

- Use in an MK2 chassis alongside a [Provider Module](provider-module.md) to both extract and advertise from the same inventory
- Configure the item filter to ensure only the right items leave a mixed-content chest
- MK3 is ideal for high-output machines (blast furnaces, high-tier kilns) that produce items faster than MK1 can handle

## See Also
- [Item Extractor Pipe](../pipes/item-extractor-pipe.md) - Standalone Tier 1 extractor (simpler, cheaper)
- [Advanced Extractor Module](advanced-extractor-module.md) - Extractor with more filter/scheduling options
- [Chassis Logistics Pipe](../pipes/chassis-logistics-pipe.md) - Required container
- [Blank Module](blank-module.md) - Crafting base
- [Iron Gear](../materials/iron-gear.md) - MK1 crafting component
- [Gold Gear](../materials/gold-gear.md) - MK2 crafting component
- [Diamond Gear](../materials/diamond-gear.md) - MK3 crafting component
