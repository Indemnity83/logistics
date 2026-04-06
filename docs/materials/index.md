# Materials

Crafting components, ores, metals, and gems used throughout Logistics.

## Machine Components

- **[Machine Core](machine-core.md)** - Required component for all RF-powered machines (Macerator, Kiln, Laser Quarry)
- **[Cores](cores.md)** - Intermediate components used in valve crafting (Wooden Core, Iron Core, Bronze Core, etc.)
- **[Dusts](dusts.md)** - Ground materials produced by the Macerator; used in module crafting and ore doubling
- **[Chips & Silicon](chips.md)** - Electronic components for module crafting (Redstone Chip, Amethyst Chip, Echo Chip, Carbon Chip)

## Gears

**Basic Tier:**
- **[Wooden Gear](wooden-gear.md)** - Early-game gear

**Stone Tier:**
- **[Stone Gear](stone-gear.md)** - Stone-tier gear

**Mid Tier:**
- **[Iron Gear](iron-gear.md)** - Mid-tier gear
- **[Copper Gear](copper-gear.md)** - Mid-tier gear
- **[Tin Gear](tin-gear.md)** - Mid-tier gear
- **[Bronze Gear](bronze-gear.md)** - Mid-tier gear

**Advanced Tier:**
- **[Gold Gear](gold-gear.md)** - Advanced gear
- **[Diamond Gear](diamond-gear.md)** - Advanced gear

**End Tier:**
- **[Netherite Gear](netherite-gear.md)** - Highest tier gear

## Valves {#valves}

Valves are crafted at the crafting table using [Quartz Crystal](cores.md#quartz-crystal) and a matching [Core](cores.md). All valves share the same recipe pattern:

```
[Q] [Q] [Q]
[Q] [Core] [Q]
[I] [G] [I]
```

Where Q = Quartz Crystal, I = Iron Nugget, G = Gold Nugget. The Core in the center determines the valve type.

**Quartz Crystal** is obtained by smelting [Quartz Dust](dusts.md) (ground from Nether Quartz in the [Macerator](../automation/macerator.md)) in the [Kiln](../automation/kiln.md) or furnace.

### Wooden Valve
- **Core:** [Wooden Core](cores.md) (Wood Pulp + Slime Ball)
- **Used in:** Future features

### Copper Valve
- **Core:** [Copper Core](cores.md) (Copper Ingot + Redstone)
- **Used in:** Future features

### Iron Valve
- **Core:** [Iron Core](cores.md) (Iron Ingot + Redstone)
- **Used in:** [Provider Logistics Pipe](../pipes/provider-logistics-pipe.md), [Chassis MK1](../pipes/chassis-logistics-pipe.md#mk1-recipe)

### Bronze Valve
- **Core:** [Bronze Core](cores.md) (Bronze Ingot + Redstone)
- **Used in:** [Crafting Logistics Pipe](../pipes/crafting-logistics-pipe.md), [Chassis MK2](../pipes/chassis-logistics-pipe.md#mk2-recipe)

### Gold Valve
- **Core:** [Gold Core](cores.md) (Gold Ingot + Redstone)
- **Used in:** [Process Logistics Pipe](../pipes/process-logistics-pipe.md), [Chassis MK3](../pipes/chassis-logistics-pipe.md#mk3-recipe)

### Lapis Valve
- **Core:** [Lapis Core](cores.md) (Lapis Lazuli + Redstone)
- **Used in:** [Satellite Logistics Pipe](../pipes/satellite-logistics-pipe.md)

### Diamond Valve
- **Core:** [Diamond Core](cores.md) (Diamond Dust + Amethyst Shard)
- **Used in:** [Requester Logistics Pipe](../pipes/requester-logistics-pipe.md), [Chassis MK4](../pipes/chassis-logistics-pipe.md#mk4-recipe)

### Emerald Valve
- **Core:** [Emerald Core](cores.md) (Emerald Dust + Amethyst Shard)
- **Used in:** [Supplier Logistics Pipe](../pipes/supplier-logistics-pipe.md)

### Apatite Valve
- **Core:** [Apatite Core](cores.md) (Apatite + Redstone)
- **Used in:** Future features

### Obsidian Valve
- **Core:** [Obsidian Core](cores.md) (Obsidian Dust + Redstone)
- **Used in:** Future features

### Netherite Valve
- **Core:** [Netherite Core](cores.md) (Netherite Ingot + Redstone)
- **Used in:** [Chassis MK5](../pipes/chassis-logistics-pipe.md#mk5-recipe)

### Blazing Valve
- **Core:** [Blazing Core](cores.md) (Blaze Powder + Blaze Rod)
- **Used in:** Future features

### Ender Valve
- **Core:** [Ender Core](cores.md) (Ender Dust + Eye of Ender)
- **Used in:** Future features

## Ores & Metals

**Tin:**
- **[Tin Ore](tin-ore.md)** - Underground ore
- **[Tin Ingot](tin-ingot.md)** - Smelted metal

**Bronze:**
- **[Bronze Ingot](bronze-ingot.md)** - Copper + tin alloy

**Apatite:**
- **[Apatite Ore](apatite-ore.md)** - Gem ore
- **[Apatite](apatite.md)** - Gem material

## See Also
- [Macerator](../automation/macerator.md) - Produces dusts from ores and other materials
- [Kiln](../automation/kiln.md) - Smelts Quartz Dust into Quartz Crystal; processes ores
- [Machine Core](machine-core.md) - Crafts all RF machines
- [Cores](cores.md) - Valve crafting components
- [Redstone Engine](../power/redstone-engine.md) - Uses wooden gears
- [Stirling Engine](../power/stirling-engine.md) - Uses stone gears
- [Laser Quarry](../automation/laser-quarry.md) - Uses diamond gears and Machine Core
