# Kiln Energy Progression System

This document explains the kiln's energy-based recipe progression and fuel requirements.

## Overview

The kiln uses an energy-based PID control system where:
- **Temperature** is derived from internal energy: `T = TMAX * (1 - exp(-E / C))`
- **Recipes** consume energy over time based on their `energyDemand` value
- **Fuels** have different burn rate caps based on their vanilla burn times
- **Progression** is gated by whether fuels can sustain recipe energy demands

## Operating Parameters

From `KilnConstants.java`:
- **TMAX** = 2000°C (maximum theoretical temperature)
- **OPERATING_SETPOINT** = 1500°C (PID target temperature)
- **CRAFTING_MIN_TEMP** = 1200°C (minimum temperature for all recipes)
- **THERMAL_MASS_C** = 74000 (energy-temperature mapping constant)
- **ENERGY_LOSS_TAU_TICKS** = 20000 (thermal decay time constant)

At 1500°C operating temperature:
- Internal energy ≈ 102,500
- Thermal decay ≈ 5.13 energy/tick

## Fuel System

### Burn Rate Caps

Each fuel has a maximum burn rate (ticks/tick) based on its vanilla burn time:

```
burnCap = BURN_CAP_REFERENCE_RATE * (burnTime / BURN_CAP_REFERENCE_ITEM_TIME)^0.5
burnCap = clamp(burnCap, BURN_CAP_MIN, BURN_CAP_MAX)
```

Where:
- `BURN_CAP_REFERENCE_RATE` = 80 (coal baseline)
- `BURN_CAP_REFERENCE_ITEM_TIME` = 1600 (coal burn time)
- `BURN_CAP_EXPONENT` = 0.5 (square root scaling)
- `BURN_CAP_MIN` = 5, `BURN_CAP_MAX` = 320

### Common Fuel Capabilities

At 1500°C with ~5 energy/tick thermal decay (BURN_CAP_REFERENCE_RATE = 20):

| Fuel | Burn Time | Burn Cap | Max Input | Net Available |
|------|-----------|----------|-----------|---------------|
| Stick | 100 | 5* | 5 | ~0 energy/tick |
| Wood Plank | 300 | 8.7 | 8.7 | **~4 energy/tick** |
| Coal | 1600 | 20 | 20 | **~15 energy/tick** |
| Blaze Rod | 2400 | 24.5 | 24.5 | ~20 energy/tick |
| Lava Bucket | 20000 | 80* | 80 | **~75 energy/tick** |

*Capped at BURN_CAP_MIN (5) or BURN_CAP_MAX (320 → scaled to 80)

**Net Available** = Max Input - Thermal Decay (available for recipe work)

### Fuel Progression Breakpoints

With reduced burn rates (BURN_CAP_REFERENCE_RATE = 20):

- **Wood fails**: All recipes (wood ~4 cap, copper needs 40)
- **Coal works**: Only with PID burst capacity (coal ~15-20 cap vs 40-60 demand)
- **Coal fails**: Iron+ definitively impossible (100+ demand vs ~15-20 cap)
- **Blaze rod struggles**: Bronze through apatite (140-200 vs ~20 cap)
- **Lava required**: All mid-tier+ recipes (lava ~75 cap)
- **Lava struggles**: High-tier recipes (240-440 demand approaching/exceeding 75 cap)

**Note**: These are theoretical caps. In-game testing with PID burst capacity and startup kick may show different effective capabilities.

## Recipe Progression Tiers

Recipe difficulty is controlled by `energyPerTick` directly in the recipe format.

Total energy cost = `energyPerTick * processTimeTicks`

### Tier 1: Coal Required (40-60 energy/tick)

| Recipe | Process Time | Energy Demand | Energy/Tick | Notes |
|--------|--------------|---------------|-------------|-------|
| Copper Valve | 100 | 4,000 | 40 | Starter - wood insufficient |
| Tin Valve | 100 | 6,000 | 60 | Coal works well |

**Fuel requirement**: Coal required (wood fails even for starter recipes)

### Tier 2: Coal At Limit (100-140 energy/tick)

| Recipe | Process Time | Energy Demand | Energy/Tick | Notes |
|--------|--------------|---------------|-------------|-------|
| Iron Valve | 140 | 14,000 | 100 | Coal at capacity (~110) |
| Bronze Valve | 120 | 16,800 | 140 | Coal insufficient |

**Fuel requirement**: Coal borderline for iron, fails at bronze

### Tier 3: Blaze Rod Minimum (180-200 energy/tick)

| Recipe | Process Time | Energy Demand | Energy/Tick | Notes |
|--------|--------------|---------------|-------------|-------|
| Gold Valve | 160 | 28,800 | 180 | Blaze rod required |
| Apatite Valve | 240 | 48,000 | 200 | Blaze rod recommended |

**Fuel requirement**: Blaze rods or lava buckets (coal insufficient)

### Tier 4: Premium Fuel (240-260 energy/tick)

| Recipe | Process Time | Energy Demand | Energy/Tick | Notes |
|--------|--------------|---------------|-------------|-------|
| Diamond Valve | 220 | 52,800 | 240 | High energy demand |
| Ender Valve | 260 | 67,600 | 260 | Lava recommended |

**Fuel requirement**: Lava buckets recommended

### Tier 5: Lava Required (280-320 energy/tick)

| Recipe | Process Time | Energy Demand | Energy/Tick | Notes |
|--------|--------------|---------------|-------------|-------|
| Emerald Valve | 220 | 61,600 | 280 | Very high demand |
| Lapis Valve | 240 | 76,800 | 320 | Lava at near capacity |

**Fuel requirement**: Lava buckets (315 cap, barely sufficient for lapis)

### Tier 6: Maximum Demand (360-440 energy/tick)

| Recipe | Process Time | Energy Demand | Energy/Tick | Notes |
|--------|--------------|---------------|-------------|-------|
| Obsidian Valve | 180 | 64,800 | 360 | Extreme energy demand |
| Netherite Valve | 280 | 112,000 | 400 | Near-maximum demand |
| Blazing Valve | 200 | 88,000 | 440 | Maximum energy demand |

**Fuel requirement**: Lava buckets or better (lava struggles with blazing at 440)

## Calculating Recipe Sustainability

To determine if a fuel can sustain a recipe:

1. **Calculate fuel net capacity**:
   ```
   netCapacity = (burnCap * throttle) - thermalDecay
   ```
   At full throttle (1.0) and 1500°C:
   ```
   netCapacity ≈ burnCap - 5
   ```

2. **Calculate recipe energy demand**:
   ```
   energyPerTick = energyDemand / processTimeTicks
   ```

3. **Check sustainability**:
   ```
   isSustainable = netCapacity >= energyPerTick
   ```

### Example: Can coal sustain bronze valve?

- Coal burn cap: 80 (theoretical)
- Observed capacity: ~110 energy/tick (with PID efficiency)
- Bronze valve: 16800 / 120 = 140 energy/tick
- **Result**: 110 < 140 → **Coal CANNOT sustain** (temperature will drop)

### Example: Can lava sustain blazing valve?

- Lava burn cap: 320
- Net capacity: 320 - 5 = 315 energy/tick
- Blazing valve: 88000 / 200 = 440 energy/tick
- **Result**: 315 < 440 → **Lava STRUGGLES** (temperature will drop slowly)

### Example: Can lava sustain netherite valve?

- Lava net capacity: ~315 energy/tick
- Netherite valve: 112000 / 280 = 400 energy/tick
- **Result**: 315 < 400 → **Lava INSUFFICIENT** (temperature will drop)

## PID Behavior Under Load

When a recipe's energy demand exceeds fuel capacity:

1. **Temperature drops** as energy drains faster than it replenishes
2. **PID increases throttle** to maximum (1.0) trying to compensate
3. **Crafting pauses** if temperature falls below CRAFTING_MIN_TEMP (1200°C)
4. **Resume when hot** - recipe continues once temperature recovers

This creates natural progression gates:
- Early recipes work with any fuel
- Mid-tier recipes need coal-level fuels
- End-game recipes require premium fuels (lava, blaze rods)

## Design Philosophy

The energy progression system creates **meaningful fuel choices**:

- **No hard gates**: Any recipe can be completed with any fuel (just slower)
- **Efficiency rewards**: Better fuels complete recipes without pauses
- **Clear breakpoints**: Players can calculate if their fuel is sufficient
- **Progression feel**: Upgrading fuels feels impactful and necessary

## Tuning Guidelines

When adding new recipes, choose `energyPerTick` based on intended tier:

- **Tier 1 (Starter)**: 40-60 energy/tick
- **Tier 2 (Mid-tier)**: 100-140 energy/tick
- **Tier 3 (Advanced)**: 180-200 energy/tick
- **Tier 4 (Premium)**: 240-260 energy/tick
- **Tier 5 (Lava-tier)**: 280-320 energy/tick
- **Tier 6 (Maximum)**: 360-440+ energy/tick

**Recipe Format**: Use `energyPerTick` directly (not total):
```json
{
  "processTimeTicks": 100,
  "energyPerTick": 40,
  "fluid": { "id": "...", "amountMb": 250 }
}
```

**Burn Rate Settings**: BURN_CAP_REFERENCE_RATE reduced from 80 to 20 to slow fuel consumption by 4x. This balances the aggressive energy demands and creates tighter fuel progression. Adjust this constant if recipes feel too easy or impossible with expected fuels.

## See Also

- `KilnConstants.java` - All tunable constants
- `KilnBlockEntity.java` - Energy-based heat implementation
- `DESIGN.md` - Overall mod architecture
