# Icon Assets

This directory contains icons for items and blocks used in infoboxes. Icons are generated using the [icon-exporter mod](https://modrinth.com/mod/icon-exporter).

## Naming Convention

Icons use the **icon-exporter format**:
```
<modid>__<itemid>.png
```

### Logistics Examples
- `logistics__copper_transport_pipe.png` - Copper Transport Pipe block
- `logistics__valve_copper.png` - Copper Valve item
- `logistics__wooden_gear.png` - Wooden Gear item
- `logistics__kiln.png` - Kiln block

### Converting Minecraft ID to Icon Filename

| Minecraft ID | Icon Filename |
|---|---|
| `logistics:pipe/copper_transport_pipe` | `logistics__copper_transport_pipe.png` |
| `logistics:core/valve_copper` | `logistics__valve_copper.png` |
| `logistics:core/kiln` | `logistics__kiln.png` |

**Pattern**: `logistics__` + item name (without namespace/path) + `.png`

## Icon Specifications

- **Size**: 256x256 pixels (exported from icon-exporter)
- **Display**: Auto-scaled to 64x64 in infoboxes via CSS
- **Format**: PNG with transparency
- **Rendering**: Pixelated/crisp-edges for authentic Minecraft look

## Generation Methods

### Method 1: Mod-Based Export (Recommended)
Use a mod that can export all item/block icons:
- **[InventoryProfilesNext](https://modrinth.com/mod/inventory-profiles-next)** - Has icon export feature
- **[ItemZoom](https://modrinth.com/mod/itemzoom)** - Can screenshot items
- Or custom tool using Minecraft's rendering engine

### Method 2: Manual Screenshots
1. Set GUI scale to 2x (Options → Video Settings)
2. Open inventory (E)
3. Use F1 to hide HUD
4. Screenshot items/blocks
5. Crop to 64x64px
6. Save with correct filename

### Method 3: Extract from Mod Assets
For items (flat icons):
```bash
# Item textures are at 16x16 in mod resources
# Scale them up 4x to 64x64 with nearest-neighbor
# From: ../logistics-mc-1.21.11/src/main/resources/assets/logistics/textures/item/
```

For blocks (3D renders):
- Requires in-game rendering or 3D tool
- Isometric view with lighting
- Can use a mod to batch-export

## Batch Icon Generation Script

For generating all icons from the main mod worktree:

```bash
#!/bin/bash
# generate-icons.sh

# Source directories
MOD_WORKTREE="../logistics-mc-1.21.11"
ITEM_TEXTURES="$MOD_WORKTREE/src/main/resources/assets/logistics/textures/item"

# Target directories
TARGET_ITEMS="./docs/assets/icons/items"
TARGET_BLOCKS="./docs/assets/icons/blocks"

# Create directories
mkdir -p "$TARGET_ITEMS" "$TARGET_BLOCKS"

# Copy and scale item textures (requires ImageMagick)
for texture in "$ITEM_TEXTURES"/*.png; do
    filename=$(basename "$texture")
    # Scale 16x16 → 64x64 with nearest-neighbor (pixelated)
    convert "$texture" -filter point -resize 64x64 "$TARGET_ITEMS/$filename"
    echo "Generated: $filename"
done

echo "Item icons generated. Block icons require in-game rendering."
```

## In-Game Batch Export

Recommended approach for both items and blocks:

1. Install a batch screenshot mod
2. Load game with Logistics mod
3. Run export command/script
4. Icons auto-saved to output directory
5. Copy to this directory

### Suggested Mod Options:
- **Custom export mod** - Write a simple Fabric mod that iterates all items/blocks and renders them
- **[EMI](https://modrinth.com/mod/emi)** or **[JEI](https://www.curseforge.com/minecraft/mc-mods/jei)** - Can export recipe images (includes item icons)

## Placeholder Icons

Until icons are generated, use placeholder or copy from similar items:
- Pipes can share base texture with color variations
- Valves might share similar templates
- Gears can use same icon with different colors

## Icon Checklist

Track icon generation progress:

### Items - Gears (9)
- [ ] wooden_gear.png
- [ ] stone_gear.png
- [ ] iron_gear.png
- [ ] copper_gear.png
- [ ] tin_gear.png
- [ ] bronze_gear.png
- [ ] gold_gear.png
- [ ] diamond_gear.png
- [ ] netherite_gear.png

### Items - Valves (13)
- [ ] valve_copper.png
- [ ] valve_tin.png
- [ ] valve_iron.png
- [ ] valve_bronze.png
- [ ] valve_gold.png
- [ ] valve_apatite.png
- [ ] valve_diamond.png
- [ ] valve_ender.png
- [ ] valve_emerald.png
- [ ] valve_lapis.png
- [ ] valve_obsidian.png
- [ ] valve_netherite.png
- [ ] valve_blazing.png

### Items - Materials
- [ ] raw_tin.png
- [ ] tin_ingot.png
- [ ] tin_nugget.png
- [ ] bronze_ingot.png
- [ ] bronze_nugget.png
- [ ] apatite.png

### Items - Tools
- [ ] wrench.png
- [ ] marking_fluid_*.png (16 colors)

### Blocks - Pipes (9)
- [ ] stone_transport_pipe.png
- [ ] copper_transport_pipe.png
- [ ] item_extractor_pipe.png
- [ ] item_merger_pipe.png
- [ ] golden_transport_pipe.png
- [ ] item_filter_pipe.png
- [ ] item_insertion_pipe.png
- [ ] item_passthrough_pipe.png
- [ ] item_void_pipe.png

### Blocks - Machines
- [ ] laser_quarry.png
- [ ] kiln.png
- [ ] redstone_engine.png
- [ ] stirling_engine.png

### Blocks - Ores
- [ ] tin_ore.png
- [ ] deepslate_tin_ore.png
- [ ] apatite_ore.png

## Contributing Icons

If generating icons manually:
1. Ensure 64x64 size
2. Use nearest-neighbor scaling (no blur/smooth)
3. Transparent background for items
4. Follow naming convention (lowercase, underscores)
5. Commit to this directory
