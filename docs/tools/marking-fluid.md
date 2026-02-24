<div class="infobox">
    <div class="infobox-header">Marking Fluid</div>
    <div class="infobox-image">
        <img src="../assets/icons/logistics__pipe___marking_fluid_red.png" alt="Marking Fluid">
    </div>
    <table class="infobox-table">
        <tr>
            <td class="infobox-label">ID</td>
            <td class="infobox-value"><code>logistics:tool/marking_fluid</code></td>
        </tr>
        <tr>
            <td class="infobox-label">Type</td>
            <td class="infobox-value">Tool</td>
        </tr>
        <tr>
            <td class="infobox-label">Stackable</td>
            <td class="infobox-value"><span class="stackable-yes">Yes (64)</span></td>
        </tr>
        <tr>
            <td class="infobox-label">Function</td>
            <td class="infobox-value">Pipe color-coding</td>
        </tr>
        <tr>
            <td class="infobox-label">Added</td>
            <td class="infobox-value">v0.1.0</td>
        </tr>
    </table>
</div>

# Marking Fluid

**Marking Fluid** is a color-coding tool that prevents [Copper Transport Pipes](../pipes/copper-transport-pipe.md) from connecting to each other. Use it to segment networks and run multiple independent pipe systems side-by-side.

## Recipe

<div class="crafting-recipe">
    <div class="crafting-grid">
        <div class="crafting-slot"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"><img src="../assets/icons/minecraft__potion__{'minecraft__potion_contents'__{potion__'minecraft__water'}}.png" class="crafting-item" alt="Water Bottle"></div>
        <div class="crafting-slot"><img src="../assets/icons/minecraft__red_dye.png" class="crafting-item" alt="Dye"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"></div>
    </div>
    <div class="crafting-arrow">→</div>
    <div class="crafting-output">
        <img src="../assets/icons/logistics__pipe___marking_fluid_red.png" class="crafting-item" alt="Marking Fluid">
    </div>
</div>

**Shapeless crafting:** Water Bottle + Dye (any color) = Marking Fluid (colored)

## Available Colors

Marking fluid comes in all 16 Minecraft dye colors:

<div style="display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; margin: 20px 0;">
    <div style="text-align: center;"><img src="../assets/icons/logistics__pipe___marking_fluid_white.png" alt="White" style="width: 48px;"><br>White</div>
    <div style="text-align: center;"><img src="../assets/icons/logistics__pipe___marking_fluid_light_gray.png" alt="Light Gray" style="width: 48px;"><br>Light Gray</div>
    <div style="text-align: center;"><img src="../assets/icons/logistics__pipe___marking_fluid_gray.png" alt="Gray" style="width: 48px;"><br>Gray</div>
    <div style="text-align: center;"><img src="../assets/icons/logistics__pipe___marking_fluid_black.png" alt="Black" style="width: 48px;"><br>Black</div>
    <div style="text-align: center;"><img src="../assets/icons/logistics__pipe___marking_fluid_red.png" alt="Red" style="width: 48px;"><br>Red</div>
    <div style="text-align: center;"><img src="../assets/icons/logistics__pipe___marking_fluid_orange.png" alt="Orange" style="width: 48px;"><br>Orange</div>
    <div style="text-align: center;"><img src="../assets/icons/logistics__pipe___marking_fluid_yellow.png" alt="Yellow" style="width: 48px;"><br>Yellow</div>
    <div style="text-align: center;"><img src="../assets/icons/logistics__pipe___marking_fluid_lime.png" alt="Lime" style="width: 48px;"><br>Lime</div>
    <div style="text-align: center;"><img src="../assets/icons/logistics__pipe___marking_fluid_green.png" alt="Green" style="width: 48px;"><br>Green</div>
    <div style="text-align: center;"><img src="../assets/icons/logistics__pipe___marking_fluid_cyan.png" alt="Cyan" style="width: 48px;"><br>Cyan</div>
    <div style="text-align: center;"><img src="../assets/icons/logistics__pipe___marking_fluid_light_blue.png" alt="Light Blue" style="width: 48px;"><br>Light Blue</div>
    <div style="text-align: center;"><img src="../assets/icons/logistics__pipe___marking_fluid_blue.png" alt="Blue" style="width: 48px;"><br>Blue</div>
    <div style="text-align: center;"><img src="../assets/icons/logistics__pipe___marking_fluid_purple.png" alt="Purple" style="width: 48px;"><br>Purple</div>
    <div style="text-align: center;"><img src="../assets/icons/logistics__pipe___marking_fluid_magenta.png" alt="Magenta" style="width: 48px;"><br>Magenta</div>
    <div style="text-align: center;"><img src="../assets/icons/logistics__pipe___marking_fluid_pink.png" alt="Pink" style="width: 48px;"><br>Pink</div>
    <div style="text-align: center;"><img src="../assets/icons/logistics__pipe___marking_fluid_brown.png" alt="Brown" style="width: 48px;"><br>Brown</div>
</div>

## Usage

Right-click [Copper Transport Pipes](../pipes/copper-transport-pipe.md) with marking fluid to color-code them:

1. Craft marking fluid with desired dye color
2. Right-click copper pipe with marking fluid
3. Pipe takes on the dye color
4. **Pipes will only connect to other pipes of the same color**
5. Different colored pipes will not connect to each other
6. Unmarked pipes connect to all colors

## Removing Marking

To clear a pipe's color marking:
- Sneak + right-click with empty hand
- Pipe returns to unmarked state
- Can now connect to any pipe

## Connection Rules

**Same color pipes:**
- **Connect to each other**
- Form isolated networks by color

**Different color pipes:**
- **Do not connect** to each other
- Keeps colored networks separate

**Unmarked pipes:**
- **Connect to all pipes** regardless of color
- Can bridge between different colored networks

## Use Cases

**Run parallel networks:**
```
[Red pipes] ═══ [Red network A] (red only connects to red)
[Blue pipes] ══ [Blue network B] (blue only connects to blue)
    (Red and blue networks remain separate)
```

**Isolate same-colored networks:**
```
[Green network A] → [Green pipes only]

[Green network B] → [Green pipes only]
    (Two separate green networks won't merge)
```

**Organize complex builds:**
```
Different colored pipes for different purposes:
- Red = ore processing
- Blue = item sorting
- Green = overflow management
```

## Tips

- Only works on [Copper Transport Pipes](../pipes/copper-transport-pipe.md)
- Each dye creates a different color
- Sneak + empty hand to remove marking
- Visual indicator shows pipe color
- Alternative to physical separation
- Cleaner than using blocks to separate networks
- Different from filters - this prevents physical connections

## See Also
- [Copper Transport Pipe](../pipes/copper-transport-pipe.md) - Pipe type that accepts marking
- [Connectivity](../core/connectivity.md) - Connection rules
- [Pipe Networks](../core/pipe-networks.md) - Network segmentation
- [Item Passthrough Pipe](../pipes/item-passthrough-pipe.md) - Alternative segmentation method
