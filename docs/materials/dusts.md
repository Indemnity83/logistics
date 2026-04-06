# Dusts

**Dusts** are ground materials produced by the [Macerator](../automation/macerator.md). They are used as crafting ingredients for [modules](../modules/index.md) and [chips](chips.md), and can be smelted back into ingots for better ore yields.

## All Dust Types

| Dust | Source | Uses |
|------|--------|------|
| Iron Dust | Iron Ore, Iron Ingot, Iron Block, Raw Iron | [Provider Module](../modules/provider-module.md), smelt → Iron Ingot |
| Copper Dust | Copper Ore, Copper Ingot, Copper Block, Raw Copper | [Item Sink Module](../modules/item-sink-module.md), smelt → Copper Ingot |
| Gold Dust | Gold Ore, Gold Ingot, Gold Block, Raw Gold, Nether Gold Ore | Smelt → Gold Ingot |
| Tin Dust | Tin Ore, Tin Ingot | Smelt → Tin Ingot |
| Bronze Dust | Bronze Ingot | [Crafter Module](../modules/crafter-module.md), smelt → Bronze Ingot |
| Diamond Dust | Diamond Ore, Diamond Block | [Quicksort Module](../modules/quicksort-module.md), [Diamond Core](cores.md) |
| Emerald Dust | Emerald Ore, Emerald Block | [Supplier Module](../modules/supplier-module.md), [Emerald Core](cores.md) |
| Lapis Dust | Lapis Ore, Lapis Block | [Marker](../automation/index.md), smelt or craft |
| Coal Dust | Coal Ore, Coal, Charcoal | [Silicon Mix](chips.md#silicon-wafer), [Carbon Chip](chips.md) |
| Quartz Dust | Nether Quartz, Quartz Ore, Quartz Block | [Quartz Crystal](cores.md#quartz-crystal), [Silicon Mix](chips.md#silicon-wafer) |
| Amethyst Dust | Amethyst Shard | [Amethyst Chip](chips.md) |
| Echo Dust | Echo Shard | [Echo Chip](chips.md) |
| Ender Dust | Ender Pearl | [Ender Core](cores.md), [Chips](chips.md) (shared ingredient) |
| Tin Dust | Tin Ore, Tin Ingot | Smelt → Tin Ingot |
| Netherite Dust | — | Smelt → Netherite Ingot |
| Obsidian Dust | Obsidian | [Obsidian Core](cores.md) |
| Prismarine Dust | Prismarine Shard, Prismarine Bricks | Decorative / future use |

## Wood Pulp {#wood-pulp}

Wood Pulp is a plant-based ground material, not strictly a "dust" but produced by the Macerator:

- **Source:** Logs (8× per log), Planks (2× per plank), Sticks (1× per stick)
- **Uses:** [Wooden Core](cores.md), [Extractor Module](../modules/extractor-module.md)

## Ore Doubling

The primary use of metal dusts in automation is **ore doubling**: grinding an ore block produces 2 dust, and smelting each dust yields 1 ingot — double the output of direct smelting.

```
[Iron Ore] → Macerator → [2× Iron Dust] → Kiln/Furnace → [2× Iron Ingot]
vs.
[Iron Ore] → Furnace → [1× Iron Ingot]
```

## See Also
- [Macerator](../automation/macerator.md) - Produces all dusts
- [Kiln](../automation/kiln.md) - Smelts dusts into ingots or crystals
- [Chips](chips.md) - Advanced components made from dusts
- [Cores](cores.md) - Some cores require specific dusts
- [Modules](../modules/index.md) - Consume dusts as crafting ingredients
