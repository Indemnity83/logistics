# Icon Path Helper

Quick reference for converting Minecraft IDs to icon file paths.

## Conversion Rules

1. Replace `:` with `__` (TWO underscores)
2. Replace `/` with `___` (THREE underscores)
3. Add `.png` extension

## Examples by Domain

### Pipes (`logistics:pipe/...`)
- `logistics:pipe/copper_transport_pipe` → `logistics__pipe___copper_transport_pipe.png`
- `logistics:pipe/stone_transport_pipe` → `logistics__pipe___stone_transport_pipe.png`
- `logistics:pipe/item_extractor_pipe` → `logistics__pipe___item_extractor_pipe.png`
- `logistics:pipe/item_filter_pipe` → `logistics__pipe___item_filter_pipe.png`

### Core Items (`logistics:core/...`)
- `logistics:core/valve_copper` → `logistics__core___valve_copper.png`
- `logistics:core/wooden_gear` → `logistics__core___wooden_gear.png`
- `logistics:core/kiln` → `logistics__core___kiln.png`
- `logistics:core/wrench` → `logistics__core___wrench.png`

### Power (`logistics:power/...`)
- `logistics:power/redstone_engine` → `logistics__power___redstone_engine.png`
- `logistics:power/stirling_engine` → `logistics__power___stirling_engine.png`

### Automation (`logistics:automation/...`)
- `logistics:automation/laser_quarry` → `logistics__automation___laser_quarry.png`

## Variants

Some items (like copper pipes) have metadata variants for different states:
- Base: `logistics__pipe___copper_transport_pipe.png`
- Exposed: `logistics__pipe___copper_transport_pipe__{'logistics__pipe___weathering_state'__{oxidation_stage__1},...}.png`

**For infoboxes**: Always use the base icon without metadata.

## Verification Script

```bash
# Check if an icon exists
ICON_PATH="logistics__pipe___copper_transport_pipe.png"
if [ -f "docs/assets/icons/$ICON_PATH" ]; then
    echo "✓ Icon exists: $ICON_PATH"
else
    echo "✗ Icon not found: $ICON_PATH"
fi
```
