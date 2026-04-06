# Chips & Silicon

**Chips** are advanced electronic components used to craft [modules](../modules/index.md). The chip tier determines the module's mark (MK1, MK2, or MK3). All chips are built on a Silicon Wafer base.

## Silicon Wafer {#silicon-wafer}

The Silicon Wafer is the substrate for all chips.

**Step 1 — Silicon Mix (shapeless crafting):**

3× [Quartz Dust](dusts.md#quartz-dust) + 1× [Coal Dust](dusts.md) → 1× Silicon Mix

**Step 2 — Silicon Wafer (smelting):**

Smelt Silicon Mix in a [Kiln](../automation/kiln.md) or furnace → 1× Silicon Wafer

## Chip Recipes

All chips use the same pattern:

```
[Ender Dust] [Resin Clump] [Ender Dust]
[Ender Dust] [Core Dust  ] [Ender Dust]
[Ender Dust] [Silicon Wfr] [Ender Dust]
```

Where "Core Dust" is the substance that defines the chip type. All chips yield **4× per craft**.

### Redstone Chip

<div class="crafting-recipe">
    <div class="crafting-grid">
        <div class="crafting-slot"><img src="../../assets/icons/logistics__core___ender_dust.png" class="crafting-item" alt="Ender Dust" title="Ender Dust"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__resin_clump.png" class="crafting-item" alt="Resin Clump" title="Resin Clump"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__core___ender_dust.png" class="crafting-item" alt="Ender Dust" title="Ender Dust"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__core___ender_dust.png" class="crafting-item" alt="Ender Dust" title="Ender Dust"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone" title="Redstone"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__core___ender_dust.png" class="crafting-item" alt="Ender Dust" title="Ender Dust"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__core___ender_dust.png" class="crafting-item" alt="Ender Dust" title="Ender Dust"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__core___silicon_wafer.png" class="crafting-item" alt="Silicon Wafer" title="Silicon Wafer"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__core___ender_dust.png" class="crafting-item" alt="Ender Dust" title="Ender Dust"></div>
    </div>
    <div class="crafting-arrow">→</div>
    <div class="crafting-output">
        <img src="../../assets/icons/logistics__core___redstone_chip.png" class="crafting-item" alt="Redstone Chip" title="Redstone Chip">
        <span class="crafting-count">4</span>
    </div>
</div>

**Yields:** 4× Redstone Chip — [Ender Dust](dusts.md) (×6) + Resin Clump + Redstone + [Silicon Wafer](#silicon-wafer)

Used in: **MK1** tier modules (Extractor MK1, Provider MK1, Passive Supplier, Crafter MK1, Item/Mod Item Sink, Terminus)

---

### Amethyst Chip

Same pattern as Redstone Chip but with [Amethyst Dust](dusts.md) in the center slot.

**Yields:** 4× Amethyst Chip

Used in: **MK2** tier modules (Extractor MK2, Provider MK2, Active Supplier, Crafter MK2, Enchantment Sink)

---

### Echo Chip

Same pattern but with [Echo Dust](dusts.md) in the center slot.

**Yields:** 4× Echo Chip

Used in: **MK3** tier modules (Extractor MK3, Crafter MK3, Polymorphic Sink, Quicksort)

---

### Carbon Chip

Same pattern but with [Coal Dust](dusts.md) in the center slot.

**Yields:** 4× Carbon Chip

Used in: [Item Sink Module](../modules/item-sink-module.md) only

---

## Chip Tier Summary

| Chip | Tier | Module Grade |
|------|------|-------------|
| Redstone Chip | 1 | MK1 / base modules |
| Amethyst Chip | 2 | MK2 / enhanced modules |
| Echo Chip | 3 | MK3 / highest tier modules |
| Carbon Chip | Special | Item Sink only |

## Progression Note

Chips are mid-to-late game components. To craft them you need:
1. [Macerator](../automation/macerator.md) — to produce Ender Dust, Quartz Dust, Coal Dust, and other dusts
2. [Kiln](../automation/kiln.md) — to smelt Silicon Mix into Silicon Wafer
3. Source of Resin Clumps (vanilla item, found in mangrove swamps)

## See Also
- [Dusts](dusts.md) - Source of all dust ingredients
- [Silicon Wafer](#silicon-wafer) - Required base for all chips
- [Modules](../modules/index.md) - Chips are used in all module recipes
- [Macerator](../automation/macerator.md) - Produces the required dusts
- [Kiln](../automation/kiln.md) - Smelts Silicon Mix into Silicon Wafer
