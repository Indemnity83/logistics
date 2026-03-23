<div class="infobox">
    <div class="infobox-header">Provider Module</div>
    <div class="infobox-image">
        <img src="../../assets/icons/logistics__pipe___provider_module.png" alt="Provider Module" title="Provider Module">
    </div>
    <table class="infobox-table">
        <tr>
            <td class="infobox-label">ID</td>
            <td class="infobox-value"><code>logistics:pipe/provider_module</code></td>
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

# Provider Module

The **Provider Module** is a [Chassis Logistics Pipe](../pipes/chassis-logistics-pipe.md) module that advertises adjacent inventory contents to the [logistics network](../core/pipe-networks.md) and fulfills item requests — the same function as a standalone [Provider Logistics Pipe](../pipes/provider-logistics-pipe.md), in module form.

Two mark variants exist: MK1 (Iron Gear) and MK2 (Gold Gear).

## Variants

| Variant | Gear | Notes |
|---------|------|-------|
| Provider Module (MK1) | Iron Gear | Standard provider behavior |
| Provider Module MK2 | Gold Gear | Enhanced — provides larger quantities per request cycle |

## Recipes

### Provider Module (MK1)

<div class="crafting-recipe">
    <div class="crafting-grid">
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__red_dye.png" class="crafting-item" alt="Red Dye" title="Red Dye"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__core___iron_gear.png" class="crafting-item" alt="Iron Gear" title="Iron Gear"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__red_dye.png" class="crafting-item" alt="Red Dye" title="Red Dye"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone" title="Redstone"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__pipe___blank_module.png" class="crafting-item" alt="Blank Module" title="Blank Module"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone" title="Redstone"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__red_dye.png" class="crafting-item" alt="Red Dye" title="Red Dye"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone" title="Redstone"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__red_dye.png" class="crafting-item" alt="Red Dye" title="Red Dye"></div>
    </div>
    <div class="crafting-arrow">→</div>
    <div class="crafting-output">
        <img src="../../assets/icons/logistics__pipe___provider_module.png" class="crafting-item" alt="Provider Module" title="Provider Module">
        <span class="crafting-count">1</span>
    </div>
</div>

**Yields:** 1× Provider Module (MK1)

### Provider Module MK2

<div class="crafting-recipe">
    <div class="crafting-grid">
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__red_dye.png" class="crafting-item" alt="Red Dye" title="Red Dye"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__core___gold_gear.png" class="crafting-item" alt="Gold Gear" title="Gold Gear"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__red_dye.png" class="crafting-item" alt="Red Dye" title="Red Dye"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone" title="Redstone"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__pipe___blank_module.png" class="crafting-item" alt="Blank Module" title="Blank Module"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone" title="Redstone"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__red_dye.png" class="crafting-item" alt="Red Dye" title="Red Dye"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone" title="Redstone"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__red_dye.png" class="crafting-item" alt="Red Dye" title="Red Dye"></div>
    </div>
    <div class="crafting-arrow">→</div>
    <div class="crafting-output">
        <img src="../../assets/icons/logistics__pipe___provider_mkii_module.png" class="crafting-item" alt="Provider Module MK2" title="Provider Module MK2">
        <span class="crafting-count">1</span>
    </div>
</div>

**Yields:** 1× Provider Module MK2

## Behavior

When installed in a [Chassis Logistics Pipe](../pipes/chassis-logistics-pipe.md), this module scans adjacent inventories and registers available items with the network. When a [Requester](../pipes/requester-logistics-pipe.md) or [Supplier](../pipes/supplier-logistics-pipe.md) requests an item, this module extracts it and routes it through the network.

Use this module when you want provider behavior combined with other module functions in the same chassis — for example, extracting from the same inventory with an [Extractor Module](extractor-module.md).

## Tips

- MK2 is worth the gold gear investment when servicing high-throughput requests
- Combine with an [Extractor Module](extractor-module.md) in an MK2 chassis to both extract and provide from the same inventory
- Configure an optional item filter to restrict which items this module makes available

## See Also
- [Provider Logistics Pipe](../pipes/provider-logistics-pipe.md) - Standalone version of this behavior
- [Chassis Logistics Pipe](../pipes/chassis-logistics-pipe.md) - Required container
- [Blank Module](blank-module.md) - Crafting base
- [Extractor Module](extractor-module.md) - Pairs well in an MK2+ chassis
- [Iron Gear](../materials/iron-gear.md) - MK1 crafting component
- [Gold Gear](../materials/gold-gear.md) - MK2 crafting component
