# RF Energy

**RF (Redstone Flux)** is the energy system used by Logistics to power automation machines like the [Laser Quarry](../automation/laser-quarry.md). Engines generate RF, and machines consume it to operate.

## How It Works

1. **[Engines](index.md)** generate RF energy when running
2. **Energy transfers** from engine output face to adjacent machines
3. **Machines** consume RF to perform work
4. **No cables needed** - direct face-to-face connection

## Energy Transfer

Engines output RF from **one face only** (configurable with [Wrench](../tools/wrench.md)):

```
[Engine output face] → [Machine input face]
    Direct connection required
```

- Output face must touch machine input face
- No pipes or cables for RF
- Rotate engine output with wrench
- Transfer is automatic when connected

## Engine Types

Different engines generate different amounts of RF:

**[Redstone Engine](redstone-engine.md):**
- Small, steady output
- Powered by redstone signal only
- Cannot overheat
- Good for low-power machines

**[Stirling Engine](stirling-engine.md):**
- Substantial output
- Powered by fuel (coal, charcoal, lava, etc.)
- Can overheat if run too long
- Good for high-power machines like quarries

## Power Requirements

Machines have different power needs:

**[Laser Quarry](../automation/laser-quarry.md):**
- Consumes RF continuously while mining
- Mining speed scales with RF throughput
- More power = faster mining
- Recommended: [Stirling Engine](stirling-engine.md)

## Tips

- Connect engine output face directly to machine
- Use [Wrench](../tools/wrench.md) to rotate output face
- More powerful engines allow faster operation
- Engines require redstone signal to run
- No energy storage - engines run on-demand

## See Also
- [Redstone Engine](redstone-engine.md) - Basic power generation
- [Stirling Engine](stirling-engine.md) - High power generation
- [Laser Quarry](../automation/laser-quarry.md) - Major power consumer
- [Wrench](../tools/wrench.md) - Rotate engine faces
