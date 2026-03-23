<div class="infobox">
    <div class="infobox-header">Process Logistics Pipe</div>
    <table class="infobox-table">
        <tr>
            <td class="infobox-label">ID</td>
            <td class="infobox-value"><code>logistics:pipe/process_logistics_pipe</code></td>
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
            <td class="infobox-value"><span class="infobox-tier infobox-tier-3">Tier 3 - Network</span></td>
        </tr>
        <tr>
            <td class="infobox-label">Added</td>
            <td class="infobox-value">v0.2.0</td>
        </tr>
    </table>
</div>

# Process Logistics Pipe

The **Process Logistics Pipe** is a [Tier 3](../core/tier-system.md) network logistics pipe that manages **processing machines** — furnaces, kilns, and other machines that transform input items into output items. It automates the input/output cycle for machines connected to the [logistics network](../core/pipe-networks.md).

## Recipe

<div class="crafting-recipe">
    <div class="crafting-grid">
        <div class="crafting-slot"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__iron_ingot.png" class="crafting-item" alt="Iron Ingot" title="Iron Ingot"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__pipe___basic_logistics_pipe.png" class="crafting-item" alt="Basic Logistics Pipe" title="Basic Logistics Pipe"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__iron_ingot.png" class="crafting-item" alt="Iron Ingot" title="Iron Ingot"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"></div>
    </div>
    <div class="crafting-arrow">→</div>
    <div class="crafting-output">
        <span class="crafting-count">1</span>
    </div>
</div>

**Yields:** 1× Process Logistics Pipe

## Behavior

The Process Logistics Pipe connects to a processing machine (furnace, smoker, blast furnace, kiln, etc.) and manages its inventory automatically. It feeds raw materials from the logistics network into the machine's input slots and extracts processed results back into the network for delivery to a destination.

**Key features:**
- Feeds input items into the connected machine from the logistics network
- Extracts output items and routes them back into the network
- Configurable input/output slot mapping via GUI
- Monitors machine state to avoid overfilling input slots

## Configuration

Use a [Wrench](../tools/wrench.md) to open the GUI:

**Input configuration:** Specify which items to feed into the machine and from which network providers to source them.

**Output configuration:** Specify where to route processed output items once extracted from the machine.

**Slot mapping:** Define which face of the machine is the input slot and which is the output slot, if the machine has multiple inventory faces.

## Tips

- Place adjacent to a furnace to automate smelting — the pipe feeds ore in and extracts ingots automatically
- Use with a [Kiln](../automation/kiln.md) to automate valve crafting in a fully networked production line
- Combine with [Provider Logistics Pipes](provider-logistics-pipe.md) to keep raw materials flowing without manual restocking
- Use with [Requester Logistics Pipes](requester-logistics-pipe.md) to pull processed output on demand

## See Also
- [Basic Logistics Pipe](basic-logistics-pipe.md) - Required crafting component; the network backbone pipe
- [Provider Logistics Pipe](provider-logistics-pipe.md) - Source of raw materials for processing
- [Satellite Logistics Pipe](satellite-logistics-pipe.md) - Route processed items to specific destinations
- [Kiln](../automation/kiln.md) - Machine commonly paired with this pipe
- [Pipe Networks](../core/pipe-networks.md) - How the logistics network works
- [Tier System](../core/tier-system.md) - Tier 3 network logistics
- [Wrench](../tools/wrench.md) - Configure slot mapping and routing
