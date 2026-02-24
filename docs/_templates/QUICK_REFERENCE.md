# Quick Reference - Adding Infoboxes

## Icon Filename Formula

For any Logistics item:
```
logistics__<item_name>.png
```

Where `<item_name>` is the item ID **without** the namespace/path.

### Examples

| Minecraft ID | Item Name | Icon Filename |
|---|---|---|
| `logistics:pipe/copper_transport_pipe` | `copper_transport_pipe` | `logistics__copper_transport_pipe.png` |
| `logistics:core/valve_diamond` | `valve_diamond` | `logistics__valve_diamond.png` |
| `logistics:core/wooden_gear` | `wooden_gear` | `logistics__wooden_gear.png` |
| `logistics:automation/laser_quarry` | `laser_quarry` | `logistics__laser_quarry.png` |

## Adding an Infobox - Step by Step

1. **Find the item ID** (from game or lang file)
   - Example: `logistics:pipe/copper_transport_pipe`

2. **Extract item name** (part after last `/` or `:`)
   - Result: `copper_transport_pipe`

3. **Build icon path**:
   - Formula: `../assets/icons/logistics__<item_name>.png`
   - Result: `../assets/icons/logistics__copper_transport_pipe.png`

4. **Copy template** from `_templates/infobox-item.md`

5. **Replace placeholders**:
   - `ITEM_NAME` → Display name (e.g., "Copper Transport Pipe")
   - `ITEM_ID` → Item name (e.g., `copper_transport_pipe`)
   - Update Type, Stackable, Tier, etc.

6. **Paste at top of page** (before main heading)

## Full Example

For the **Copper Transport Pipe** (`logistics:pipe/copper_transport_pipe`):

```html
<div class="infobox">
    <div class="infobox-header">Copper Transport Pipe</div>
    <div class="infobox-image">
        <img src="../assets/icons/logistics__copper_transport_pipe.png" alt="Copper Transport Pipe">
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

# Copper Transport Pipe

The **Copper Transport Pipe** is a Tier 1 mechanical pipe...
```

## Common Tier Classes

- `infobox-tier-1` → Brown (Tier 1 - Mechanical)
- `infobox-tier-2` → Blue (Tier 2 - Smart)
- `infobox-tier-3` → Purple (Tier 3 - Network)
- `infobox-tier-advanced` → Orange (Advanced tier materials)
- `infobox-tier-end` → Red (End-game tier)

## Common Types

- `Block` - Solid block
- `Block (Pipe)` - Pipe block
- `Block (Machine)` - Machine block
- `Item` - Regular item
- `Tool` - Wrench, etc.

## Stackable Values

- `<span class="stackable-yes">Yes (64)</span>` - Standard stackable
- `<span class="stackable-yes">Yes (16)</span>` - Limited stack
- `<span class="stackable-no">No</span>` - Non-stackable

## Finding Item IDs

Check the lang file:
```bash
# From main mod worktree
cat ../logistics-mc-1.21.11/src/main/resources/assets/logistics/lang/en_us.json
```

Look for patterns:
- `"block.logistics.pipe.copper_transport_pipe"` → ID is `copper_transport_pipe`
- `"item.logistics.core.valve_copper"` → ID is `valve_copper`
