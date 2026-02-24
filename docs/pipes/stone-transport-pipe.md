<div class="infobox">
    <div class="infobox-header">Stone Transport Pipe</div>
    <div class="infobox-image">
        <img src="/assets/icons/logistics__pipe___stone_transport_pipe.png" alt="Stone Transport Pipe">
    </div>
    <table class="infobox-table">
        <tr>
            <td class="infobox-label">ID</td>
            <td class="infobox-value"><code>logistics:pipe/stone_transport_pipe</code></td>
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

# Stone Transport Pipe

The **Stone Transport Pipe** is a [Tier 1](../core/tier-system.md) mechanical pipe that provides very slow item transport for early-game networks. It uses random routing at junctions.

## Recipe

<div class="crafting-recipe">
    <div class="crafting-grid">
        <div class="crafting-slot"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"><img src="/assets/icons/minecraft__cobblestone.png" class="crafting-item" alt="Cobblestone"></div>
        <div class="crafting-slot"><img src="/assets/icons/minecraft__glass.png" class="crafting-item" alt="Glass"></div>
        <div class="crafting-slot"><img src="/assets/icons/minecraft__cobblestone.png" class="crafting-item" alt="Cobblestone"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"></div>
    </div>
    <div class="crafting-arrow">→</div>
    <div class="crafting-output">
        <img src="/assets/icons/logistics__pipe___stone_transport_pipe.png" class="crafting-item" alt="Stone Transport Pipe">
        <span class="crafting-count">8</span>
    </div>
</div>

**Yields:** 8× Stone Transport Pipe

## Behavior

Stone Transport Pipes move items through your network at a very slow speed - significantly slower than [Copper Transport Pipes](copper-transport-pipe.md). When an item reaches a junction with multiple possible exits, it randomly selects a direction.

**Key features:**
- Very slow transport speed
- Random routing at junctions
- Small internal capacity (about 5 items)
- Connects to all other pipe types
- Connects to inventories

## Capacity Warning

Stone pipes have limited internal capacity. Because they move items slowly, busy networks can overload the pipes and cause items to drop if too many items enter at once.

**For higher throughput:**
- Upgrade to [Copper Transport Pipes](copper-transport-pipe.md) for faster movement
- Use multiple parallel stone pipes
- Limit extraction rate at sources

## Tips

- Cheapest pipe type - good for early exploration
- Too slow for production networks
- Upgrade to copper when possible
- Fine for low-volume manual item insertion
- Random routing means items may take unpredictable paths

## See Also
- [Copper Transport Pipe](copper-transport-pipe.md) - Faster alternative
- [Item Transport](../core/item-transport.md) - Understanding item speed
- [Routing](../core/routing.md) - How random routing works
- [Pipe Networks](../core/pipe-networks.md) - Building networks
- [Tier System](../core/tier-system.md) - Tier 1 mechanical pipes
