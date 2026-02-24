# Redstone Engine

The **Redstone Engine** is a basic power generator that converts a redstone signal into a small, steady RF energy output. It's the simplest engine in Logistics and cannot overheat.

## Recipe
**Crafting:**
- 3× Planks (any type)
- 1× Glass
- 2× [Wooden Gear](../materials/wooden-gear.md)
- 1× Piston
- **Yields:** 1× Redstone Engine

## Behavior

The Redstone Engine generates RF energy when powered by a redstone signal. Energy outputs from one configurable face and transfers directly to adjacent machines.

**Key features:**
- Powered by redstone signal only (no fuel)
- Small, steady energy output
- **Cannot overheat** - safe to run indefinitely
- Output face configurable with [Wrench](../tools/wrench.md)
- No GUI - just place and power

## Operation

1. Place engine next to machine
2. Use [Wrench](../tools/wrench.md) to rotate output face toward machine
3. Apply redstone signal (lever, torch, redstone dust, etc.)
4. Engine runs automatically, generating RF

**Output face:** The face touching the machine must be the engine's output face.

## Configuration

Use a [Wrench](../tools/wrench.md) to rotate the output face:
- Right-click engine with wrench
- Output face cycles through directions
- Position output face against machine input

## Power Output

Redstone Engines produce **low RF output** suitable for:
- Light-duty machines
- Testing setups
- Early-game automation

For high-power needs like the [Laser Quarry](../automation/laser-quarry.md), use a [Stirling Engine](stirling-engine.md) instead.

## Tips

- Simplest engine - no fuel, no overheat management
- Always safe to leave running
- Low output limits usefulness for big machines
- Good for learning the power system
- Requires constant redstone signal to operate
- Upgrade to [Stirling Engine](stirling-engine.md) for more power

## See Also
- [RF Energy](rf-energy.md) - Understanding the power system
- [Stirling Engine](stirling-engine.md) - High-power alternative
- [Wrench](../tools/wrench.md) - Rotate output face
- [Wooden Gear](../materials/wooden-gear.md) - Crafting component
- [Laser Quarry](../automation/laser-quarry.md) - Example power consumer
