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

Two mark variants exist: MK1 (Redstone Chip) and MK2 (Amethyst Chip).

## Variants

| Variant | Chip | Notes |
|---------|------|-------|
| Provider Module (MK1) | Redstone Chip | Standard provider behavior |
| Provider Module MK2 | Amethyst Chip | Enhanced — provides larger quantities per request cycle |

## Recipes

All module recipes use the `DCD / RBR` pattern: Dust (corners), Chip (top center), Redstone (sides + bottom center), Blank Module (center).

### Provider Module (MK1)

<div class="crafting-recipe">
    <div class="crafting-grid">
        <div class="crafting-slot"><img src="../../assets/icons/logistics__core___iron_dust.png" class="crafting-item" alt="Iron Dust" title="Iron Dust"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__core___redstone_chip.png" class="crafting-item" alt="Redstone Chip" title="Redstone Chip"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__core___iron_dust.png" class="crafting-item" alt="Iron Dust" title="Iron Dust"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone" title="Redstone"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__pipe___blank_module.png" class="crafting-item" alt="Blank Module" title="Blank Module"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone" title="Redstone"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"></div>
    </div>
    <div class="crafting-arrow">→</div>
    <div class="crafting-output">
        <img src="../../assets/icons/logistics__pipe___provider_module.png" class="crafting-item" alt="Provider Module" title="Provider Module">
        <span class="crafting-count">1</span>
    </div>
</div>

**Yields:** 1× Provider Module (MK1) — 2× [Iron Dust](../materials/dusts.md) + [Redstone Chip](../materials/chips.md) + 2× Redstone + [Blank Module](blank-module.md)

### Provider Module MK2

<div class="crafting-recipe">
    <div class="crafting-grid">
        <div class="crafting-slot"><img src="../../assets/icons/logistics__core___iron_dust.png" class="crafting-item" alt="Iron Dust" title="Iron Dust"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__core___amethyst_chip.png" class="crafting-item" alt="Amethyst Chip" title="Amethyst Chip"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__core___iron_dust.png" class="crafting-item" alt="Iron Dust" title="Iron Dust"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone" title="Redstone"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__pipe___blank_module.png" class="crafting-item" alt="Blank Module" title="Blank Module"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone" title="Redstone"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"></div>
    </div>
    <div class="crafting-arrow">→</div>
    <div class="crafting-output">
        <img src="../../assets/icons/logistics__pipe___provider_mkii_module.png" class="crafting-item" alt="Provider Module MK2" title="Provider Module MK2">
        <span class="crafting-count">1</span>
    </div>
</div>

**Yields:** 1× Provider Module MK2 — 2× [Iron Dust](../materials/dusts.md) + [Amethyst Chip](../materials/chips.md) + 2× Redstone + [Blank Module](blank-module.md)

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
- [Iron Dust](../materials/dusts.md) - Crafting component
- [Redstone Chip](../materials/chips.md) - MK1 chip
- [Amethyst Chip](../materials/chips.md) - MK2 chip
