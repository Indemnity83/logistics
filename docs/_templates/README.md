# Documentation Templates

This directory contains reusable templates for creating consistent wiki pages.

## Templates

### Infobox Templates
- **[infobox-item.md](infobox-item.md)** - ftbwiki.org-style infobox for items, blocks, and machines

## Icon Generation

### Recommended Mod: [InventoryProfilesNext](https://modrinth.com/mod/inventory-profiles-next)
This mod can export item/block icons at various scales.

### Alternative: Manual Screenshots
1. Use F1 to hide HUD
2. Screenshot items in inventory (2x GUI scale)
3. Crop to 64x64px for consistency

### Directory Structure
```
docs/assets/icons/
├── items/          # Flat item icons (64x64)
│   ├── copper_transport_pipe.png
│   ├── valve_copper.png
│   └── wooden_gear.png
└── blocks/         # 3D block renders (64x64)
    ├── kiln.png
    ├── laser_quarry.png
    └── redstone_engine.png
```

### Icon Guidelines
- **Size**: 64x64 pixels (2x scale)
- **Format**: PNG with transparency
- **Naming**: Match Minecraft ID (e.g., `copper_transport_pipe.png` for `logistics:pipe/copper_transport_pipe`)
- **Rendering**: Use pixelated/crisp-edges for authentic Minecraft look

## Usage

1. Copy template code from infobox-item.md
2. Replace placeholder values (ITEM_NAME, ITEM_ID, etc.)
3. Add icon to appropriate directory
4. Paste at the top of your page (before main heading)

## CSS Integration

The infobox CSS is in `docs/assets/css/infobox.css`. Make sure Zensical includes this in the build.
