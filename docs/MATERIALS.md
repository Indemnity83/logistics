# Materials Guide

This document explains the materials system in Logistics, including design decisions and technical details.

## Bronze Alloy

**Bronze is a copper-tin alloy**, one of the oldest known metal alloys in human history.

### About Bronze

In Logistics, bronze follows the traditional copper + tin composition:

- **Historically accurate**: Bronze is made from copper and tin (brass uses copper and zinc)
- **Minecraft mods**: Most tech mods use bronze for copper + tin alloys
  - **Thermal Series**: Uses bronze for copper + tin
  - **Create mod**: Uses brass for copper + zinc
  - **Industrial mods**: Generally follow this convention

Our bronze uses a warm golden-bronze color (#C8A858) that's distinct from both copper and gold.

### Technical Details

**Bronze Composition** (2:1 copper-to-tin ratio):
```
2 Copper Ingots + 1 Tin Ingot → 3 Bronze Ingots
```

This 2:1 ratio matches historical bronze composition (~67% copper, ~33% tin) and provides balanced crafting:
- Not too expensive (3 ingots output for 3 input)
- Requires tin mining (progression gate)
- Consistent with vanilla metal crafting patterns

## Material Colors

Reference palette for texture consistency:

- **Tin**: Silver #B8C0C8 (cool silver-gray, lighter than iron)
- **Bronze**: Golden-bronze #C8A858 (warm golden color, distinct from gold)
- **Copper**: Vanilla copper #C77E4E (warm orange-brown)

## Tin Ore Generation

**Copper-Balanced System**: Tin ore generation is calibrated to copper's density without increasing total ore count.

**Generation Rates**:
- **Stone tin (Y 0-112)**: 1 vein/chunk (vs copper's 16 → ~6% of copper density)
- **Deepslate tin (Y -16 to 0)**: 13 veins/chunk (vs copper's 16 → ~81% of copper density)

This makes tin:
- **Discoverable** in early caves (you'll find some while mining)
- **Abundant in deepslate** (the main source for bulk tin mining)
- **Consistent** across all biomes and seeds

**Distribution**:
- Stone tin: Trapezoid distribution Y 0-112 (overlaps upper half of copper range)
- Deepslate tin: Trapezoid distribution Y -16 to 0 (overlaps lower half)
- Vein size: 10 blocks (same as copper)

**Variants**:
- **Stone Tin Ore**: Found in stone layers (rare, discovery mining)
- **Deepslate Tin Ore**: Found in deepslate (common, bulk mining)

**Drops**:
- Normal pickaxe: 2-5 Raw Tin (affected by Fortune)
- Silk Touch: Ore block itself
- No XP drop (matches copper ore behavior)

**Design Philosophy**:
- Similar total density to copper, but shifted toward deeper mining
- Tin becomes common below Y=0 (rewarding deep exploration)
- Above Y=0, tin is rare enough to keep copper as the primary early resource
- No biome restrictions - players naturally encounter both ores

## Gear Progression

Gears are crafted using a tiered progression system:

1. **Wooden Gear** - Base gear (crafted from planks)
2. **Stone Gear** - Stone + Wooden Gear
3. **Copper Gear** - Copper Ingots + Stone Gear
4. **Tin Gear** - Tin Ingots + Stone Gear
5. **Iron Gear** - Iron Ingots + Tin Gear (or Copper Gear)
6. **Gold Gear** - Gold Ingots + Iron Gear
7. **Bronze Gear** - Bronze Ingots + Tin Gear
8. **Diamond Gear** - Diamond + Gold Gear
9. **Netherite Gear** - Netherite + Diamond Gear

**Bronze Gear** sits between Tin and Gold in the progression, offering a mid-tier option for machines.

## Storage Blocks

All metals follow vanilla storage block patterns:

**9 Items ↔ 1 Block**:
- 9 Tin Ingots ↔ Block of Tin
- 9 Raw Tin ↔ Block of Raw Tin
- 9 Bronze Ingots ↔ Block of Bronze

**9 Nuggets ↔ 1 Ingot**:
- 9 Tin Nuggets ↔ 1 Tin Ingot
- 9 Bronze Nuggets ↔ 1 Bronze Ingot

## Common Convention Tags

All materials use Fabric common convention tags for cross-mod compatibility:

**Block Tags**:
- `c:ores`, `c:tin_ores` - Ore blocks
- `c:storage_blocks`, `c:storage_blocks/tin`, `c:storage_blocks/raw_tin`, `c:storage_blocks/bronze`

**Item Tags**:
- `c:ores`, `c:tin_ores` - Ore items
- `c:raw_materials`, `c:raw_materials/tin` - Raw materials
- `c:ingots`, `c:tin_ingots`, `c:bronze_ingots` - Ingots
- `c:nuggets`, `c:tin_nuggets`, `c:bronze_nuggets` - Nuggets
- `c:storage_blocks` (item) - Storage block items

These tags enable:
- Recipe compatibility with other mods' tin/bronze
- JEI/REI integration
- Ore dictionary-style crafting

## Future Considerations

**Why shapeless crafting?**
- Early game accessible (no special machines needed)
- Vanilla precedent (netherite ingots, firework stars)
- Can be upgraded to smelting/alloying machines in future updates

**Potential future additions**:
- Alloying furnace for more complex recipes
- Alternative bronze recipes (Create mod integration?)
- Bronze-specific machine upgrades

---

**See also**:
- `docs/DESIGN.md` - Overall mod architecture and vision
- `docs/ASSETS.md` - Texture and asset guidelines
