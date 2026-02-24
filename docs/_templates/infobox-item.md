# Infobox Template - Items

Use this template for item pages. Copy and paste into your markdown file.

## Template Code

```html
<div class="infobox">
    <div class="infobox-header">ITEM_NAME</div>
    <div class="infobox-image">
        <img src="/assets/icons/logistics__ITEM_ID.png" alt="ITEM_NAME">
    </div>
    <table class="infobox-table">
        <tr>
            <td class="infobox-label">ID</td>
            <td class="infobox-value"><code>logistics:ITEM_ID</code></td>
        </tr>
        <tr>
            <td class="infobox-label">Type</td>
            <td class="infobox-value">Item</td>
        </tr>
        <tr>
            <td class="infobox-label">Stackable</td>
            <td class="infobox-value"><span class="stackable-yes">Yes (64)</span></td>
        </tr>
        <tr>
            <td class="infobox-label">Tier</td>
            <td class="infobox-value"><span class="infobox-tier infobox-tier-1">Tier 1</span></td>
        </tr>
        <tr>
            <td class="infobox-label">Added</td>
            <td class="infobox-value">v0.1.0</td>
        </tr>
    </table>
</div>
```

## Icon Naming Convention

Icons use the **icon-exporter mod naming format**:
- **Pattern**: `logistics__<domain>___<item_id>.png` (THREE underscores after domain)
- **Location**: `docs/assets/icons/`
- **Size**: 256x256px (auto-scaled by CSS to 64x64)

### Converting Minecraft ID to Icon Filename

| Full Minecraft ID | Icon Filename |
|---|---|
| `logistics:pipe/copper_transport_pipe` | `logistics__pipe___copper_transport_pipe.png` |
| `logistics:core/valve_copper` | `logistics__core___valve_copper.png` |
| `logistics:core/wooden_gear` | `logistics__core___wooden_gear.png` |
| `logistics:core/kiln` | `logistics__core___kiln.png` |
| `logistics:power/redstone_engine` | `logistics__power___redstone_engine.png` |
| `logistics:automation/laser_quarry` | `logistics__automation___laser_quarry.png` |

**Rule**: Replace `:` with `__`, replace `/` with `___` (THREE underscores)

## Field Reference

### Required Fields
- **ITEM_NAME**: Display name (e.g., "Copper Transport Pipe")
- **ITEM_ID**: Minecraft ID without namespace (e.g., "copper_transport_pipe")
- **Type**: Item | Block | Tool | Machine
- **Stackable**: Yes (64) | Yes (16) | No

### Tier Classes
- `infobox-tier-1` - Tier 1 (brown/tan)
- `infobox-tier-2` - Tier 2 (blue)
- `infobox-tier-3` - Tier 3 (purple)
- `infobox-tier-advanced` - Advanced tier (orange)
- `infobox-tier-end` - End tier (red)

### Stackable Classes
- `stackable-yes` - Green (stackable)
- `stackable-no` - Red (not stackable)

## Example - Copper Transport Pipe (Block)

```html
<div class="infobox">
    <div class="infobox-header">Copper Transport Pipe</div>
    <div class="infobox-image">
        <img src="/assets/icons/logistics__copper_transport_pipe.png" alt="Copper Transport Pipe">
    </div>
    <table class="infobox-table">
        <tr>
            <td class="infobox-label">Mod</td>
            <td class="infobox-value">Logistics</td>
        </tr>
        <tr>
            <td class="infobox-label">ID</td>
            <td class="infobox-value"><code>logistics:pipe/copper_transport_pipe</code></td>
        </tr>
        <tr>
            <td class="infobox-label">Type</td>
            <td class="infobox-value">Block (Pipe)</td>
        </tr>
        <tr>
            <td class="infobox-label">Stackable</td>
            <td class="infobox-value"><span class="stackable-yes">Yes (64)</span></td>
        </tr>
        <tr>
            <td class="infobox-label">Tier</td>
            <td class="infobox-value"><span class="infobox-tier infobox-tier-1">Tier 1</span></td>
        </tr>
        <tr>
            <td class="infobox-label">Added</td>
            <td class="infobox-value">v0.1.0</td>
        </tr>
    </table>
</div>
```

## Example - Diamond Valve (Item)

```html
<div class="infobox">
    <div class="infobox-header">Diamond Valve</div>
    <div class="infobox-image">
        <img src="/assets/icons/logistics__valve_diamond.png" alt="Diamond Valve">
    </div>
    <table class="infobox-table">
        <tr>
            <td class="infobox-label">Mod</td>
            <td class="infobox-value">Logistics</td>
        </tr>
        <tr>
            <td class="infobox-label">ID</td>
            <td class="infobox-value"><code>logistics:core/valve_diamond</code></td>
        </tr>
        <tr>
            <td class="infobox-label">Type</td>
            <td class="infobox-value">Item</td>
        </tr>
        <tr>
            <td class="infobox-label">Stackable</td>
            <td class="infobox-value"><span class="stackable-yes">Yes (64)</span></td>
        </tr>
        <tr>
            <td class="infobox-label">Kiln Tier</td>
            <td class="infobox-value"><span class="infobox-tier infobox-tier-advanced">Tier 4</span></td>
        </tr>
        <tr>
            <td class="infobox-label">Energy</td>
            <td class="infobox-value">240 energy/tick</td>
        </tr>
        <tr>
            <td class="infobox-label">Added</td>
            <td class="infobox-value">v0.2.0</td>
        </tr>
    </table>
</div>
```

## Example - Kiln (Machine)

```html
<div class="infobox">
    <div class="infobox-header">Kiln</div>
    <div class="infobox-image">
        <img src="/assets/icons/logistics__kiln.png" alt="Kiln">
    </div>
    <table class="infobox-table">
        <tr>
            <td class="infobox-label">Mod</td>
            <td class="infobox-value">Logistics</td>
        </tr>
        <tr>
            <td class="infobox-label">ID</td>
            <td class="infobox-value"><code>logistics:core/kiln</code></td>
        </tr>
        <tr>
            <td class="infobox-label">Type</td>
            <td class="infobox-value">Block (Machine)</td>
        </tr>
        <tr>
            <td class="infobox-label">Stackable</td>
            <td class="infobox-value"><span class="stackable-no">No</span></td>
        </tr>
        <tr>
            <td class="infobox-label">Function</td>
            <td class="infobox-value">Valve crafting</td>
        </tr>
        <tr>
            <td class="infobox-label">Added</td>
            <td class="infobox-value">v0.2.0</td>
        </tr>
    </table>
</div>
```
