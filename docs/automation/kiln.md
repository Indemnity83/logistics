# Kiln

The **Kiln** is a temperature-controlled crafting machine that produces [valves](../materials/index.md#valves) using molten glass and metal. It uses an energy-based heating system with fuel progression - more advanced valves require hotter temperatures and better fuels.

## Recipe
**Crafting:**
- 8× Bricks (hollow cube pattern)
- **Yields:** 1× Kiln

## How It Works

The Kiln operates on a **temperature and energy system**:

1. **Add fuel** - Coal, lava buckets, blaze rods, etc.
2. **Temperature rises** - Fuel burns and heats the kiln
3. **Craft valves** - Place materials and molten glass in crafting pattern
4. **Energy consumption** - Recipes consume energy over time
5. **Maintain temperature** - Better fuels sustain higher energy recipes

**Key mechanics:**
- Temperature derived from internal energy
- Recipes require minimum temperature (1200°C)
- Different fuels burn at different rates
- Advanced recipes need premium fuels to maintain temperature

## Using the Kiln

### Basic Operation

1. **Place kiln** in your workshop
2. **Right-click** to open GUI
3. **Add fuel** to fuel slot (bottom left)
4. **Wait for heating** - temperature rises to operating point (1500°C)
5. **Place pattern** - Arrange metal + redstone in crafting grid
6. **Add molten glass** - 250mb required per recipe
7. **Crafting begins** - Progress bar shows completion
8. **Collect output** - Valves appear in output slot

### Fuel Requirements

Different valve tiers require different fuel quality:

**Tier 1 (Coal sufficient):**
- [Copper Valve](../materials/valve-copper.md) - 40 energy/tick
- [Tin Valve](../materials/valve-tin.md) - 60 energy/tick

**Tier 2 (Coal at limit):**
- [Iron Valve](../materials/valve-iron.md) - 100 energy/tick
- [Bronze Valve](../materials/valve-bronze.md) - 140 energy/tick

**Tier 3 (Blaze rod/lava needed):**
- [Gold Valve](../materials/valve-gold.md) - 180 energy/tick
- [Apatite Valve](../materials/valve-apatite.md) - 200 energy/tick

**Tier 4+ (Lava recommended):**
- [Diamond Valve](../materials/valve-diamond.md) - 240 energy/tick
- [Emerald Valve](../materials/valve-emerald.md) - 280 energy/tick
- [Netherite Valve](../materials/valve-netherite.md) - 400 energy/tick
- [Blazing Valve](../materials/valve-blazing.md) - 440 energy/tick (maximum)

### Fuel Types

**Wood/Planks:**
- Burns fast, low energy output
- **Cannot sustain** valve recipes (too weak)

**Coal:**
- Standard fuel for early valves
- Works for copper, tin, iron
- **Struggles** with bronze (140 energy/tick)
- **Fails** at higher tiers

**Blaze Rods:**
- Mid-tier fuel
- Sustains gold, apatite valves
- Still insufficient for highest tiers

**Lava Buckets:**
- Premium fuel source
- Required for diamond, emerald, netherite valves
- Maximum burn rate ~75 energy/tick net capacity
- **Barely sustains** blazing valve (440 demand vs ~315 capacity)

## Temperature Behavior

**Operating setpoint:** 1500°C
**Crafting minimum:** 1200°C
**Maximum theoretical:** 2000°C

**What happens:**
- Fuel burns → energy increases → temperature rises
- Recipe active → energy drains → temperature may drop
- If temperature falls below 1200°C → crafting pauses
- Add better fuel → temperature recovers → crafting resumes

**Natural progression:**
- Low-tier recipes work with any fuel (just slower)
- High-tier recipes **require** premium fuels or they stall

## Molten Glass

All valve recipes require **250mb of molten glass** (liquid glass fluid).

**Obtaining molten glass:**
- Created by smelting sand/glass (details TBD)
- Stored in tanks or buckets
- Transferred into kiln via fluid pipes or buckets

## Tips

- Start with coal for copper/tin valves
- Upgrade to lava buckets for advanced valves
- Watch temperature gauge - if it drops, crafting pauses
- Better fuels = faster, uninterrupted crafting
- Keep spare fuel on hand for continuous operation
- Molten glass requirement means you need fluid infrastructure

## Common Patterns

**Early kiln setup:**
```
[Kiln] + Coal → [Copper/Tin Valves]
```

**Advanced setup:**
```
[Kiln] + Lava Bucket → [Diamond/Emerald/Netherite Valves]
```

**With fluid system (future):**
```
[Glass smelting] → [Fluid pipes] → [Kiln] → [Valves]
```

## Troubleshooting

**Crafting paused/temperature dropping:**
- Fuel is insufficient for recipe energy demand
- Upgrade to better fuel (coal → blaze rod → lava)
- Low-tier recipes will complete eventually, just slowly

**No molten glass:**
- Need 250mb molten glass to start crafting
- Create molten glass from smelted sand/glass
- Transfer via buckets or fluid pipes

**Pattern not accepted:**
- Check recipe pattern (materials + redstone configuration)
- Ensure molten glass is available
- Verify minimum temperature reached (1200°C)

## See Also
- [Valves](../materials/index.md#valves) - All 13 valve types
- [Copper Valve](../materials/valve-copper.md) - Easiest starter valve
- [Blazing Valve](../materials/valve-blazing.md) - Highest energy demand
- [Materials](../materials/index.md) - Crafting components
