<div class="infobox">
    <div class="infobox-header">Copper Transport Pipe</div>
    <div class="infobox-image">
        <img src="../../assets/icons/logistics__pipe___copper_transport_pipe.png" alt="Copper Transport Pipe">
    </div>
    <table class="infobox-table">
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
            <td class="infobox-value"><span class="infobox-tier infobox-tier-1">Tier 1 - Mechanical</span></td>
        </tr>
        <tr>
            <td class="infobox-label">Added</td>
            <td class="infobox-value">v0.1.0</td>
        </tr>
    </table>
</div>

# Copper Transport Pipe

The **Copper Transport Pipe** is a [Tier 1](../core/tier-system.md) mechanical pipe that provides standard-speed item transport. It's the backbone of most pipe networks, offering reliable connectivity with random routing.

## Recipe

<div class="crafting-recipe">
    <div class="crafting-grid">
        <div class="crafting-slot"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__copper_ingot.png" class="crafting-item" alt="Copper Ingot"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__glass.png" class="crafting-item" alt="Glass"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__copper_ingot.png" class="crafting-item" alt="Copper Ingot"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"></div>
    </div>
    <div class="crafting-arrow">→</div>
    <div class="crafting-output">
        <img src="../../assets/icons/logistics__pipe___copper_transport_pipe.png" class="crafting-item" alt="Copper Transport Pipe">
        <span class="crafting-count">8</span>
    </div>
</div>

**Yields:** 8× Copper Transport Pipe

## Behavior

Copper Transport Pipes move items through your network at standard speed. When an item reaches a junction with multiple possible exits, it randomly selects a direction.

**Key features:**
- Standard transport speed
- Random routing at junctions
- Reliable backbone for networks
- Connects to all other pipe types
- Connects to inventories
- Can be color-marked for network segmentation

## Network Segmentation

Mark copper pipes with [Marking Fluid](../tools/marking-fluid.md) to segment networks by color:

1. Right-click pipe with marking fluid (water bottle + dye)
2. Pipe takes on the dye color
3. Pipes with the same marking **will not connect** to each other
4. Different colors connect normally
5. Sneak + empty hand to clear marking

This lets you run multiple independent networks side-by-side without them merging.

## Oxidization

Copper pipes oxidize naturally like vanilla copper blocks, going through all the standard oxidization stages over time:

<div style="display: flex; gap: 20px; align-items: center; margin: 20px 0;">
    <div style="text-align: center;">
        <img src="../../assets/icons/logistics__pipe___copper_transport_pipe.png" alt="Copper" style="width: 64px; height: 64px; image-rendering: pixelated;">
        <div><strong>Copper</strong></div>
        <div style="font-size: 0.9em;">Fresh, orange</div>
    </div>
    <div style="font-size: 1.5em;">→</div>
    <div style="text-align: center;">
        <img src="../../assets/icons/logistics__pipe___copper_transport_pipe__{'logistics__pipe___weathering_state'__{oxidation_stage__1},'minecraft__custom_model_data'__{strings__['exposed']}}.png" alt="Exposed" style="width: 64px; height: 64px; image-rendering: pixelated;">
        <div><strong>Exposed</strong></div>
        <div style="font-size: 0.9em;">Slightly weathered</div>
    </div>
    <div style="font-size: 1.5em;">→</div>
    <div style="text-align: center;">
        <img src="../../assets/icons/logistics__pipe___copper_transport_pipe__{'logistics__pipe___weathering_state'__{oxidation_stage__2},'minecraft__custom_model_data'__{strings__['weathered']}}.png" alt="Weathered" style="width: 64px; height: 64px; image-rendering: pixelated;">
        <div><strong>Weathered</strong></div>
        <div style="font-size: 0.9em;">Green-tinted</div>
    </div>
    <div style="font-size: 1.5em;">→</div>
    <div style="text-align: center;">
        <img src="../../assets/icons/logistics__pipe___copper_transport_pipe__{'logistics__pipe___weathering_state'__{oxidation_stage__3},'minecraft__custom_model_data'__{strings__['oxidized']}}.png" alt="Oxidized" style="width: 64px; height: 64px; image-rendering: pixelated;">
        <div><strong>Oxidized</strong></div>
        <div style="font-size: 0.9em;">Fully green</div>
    </div>
</div>

**Managing oxidization:**
- Use **honeycomb** to wax pipes and prevent further oxidization
- Use **axe** to scrape off oxidization layers
- Works exactly like vanilla copper mechanics
- **Purely cosmetic** - doesn't affect item transport speed or functionality
- Mix oxidization stages for aesthetic builds

## Tips

- Standard choice for network backbone
- Use marking fluid to prevent unwanted connections
- Random routing works fine for single-destination networks
- For controlled routing, use [Item Merger Pipes](item-merger-pipe.md) or [Item Filter Pipes](item-filter-pipe.md)
- Faster than [Stone Transport Pipes](stone-transport-pipe.md), cheaper than smart pipes

## See Also
- [Marking Fluid](../tools/marking-fluid.md) - Color-code for segmentation
- [Stone Transport Pipe](stone-transport-pipe.md) - Slower, cheaper alternative
- [Item Merger Pipe](item-merger-pipe.md) - Controlled directional routing
- [Item Filter Pipe](item-filter-pipe.md) - Item-aware routing
- [Routing](../core/routing.md) - How random routing works
- [Connectivity](../core/connectivity.md) - Connection rules
