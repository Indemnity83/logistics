<div class="infobox">
    <div class="infobox-header">Crafting Logistics Pipe</div>
    <table class="infobox-table">
        <tr>
            <td class="infobox-label">ID</td>
            <td class="infobox-value"><code>logistics:pipe/crafting_logistics_pipe</code></td>
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

# Crafting Logistics Pipe

The **Crafting Logistics Pipe** is a [Tier 3](../core/tier-system.md) network logistics pipe that enables **automated crafting** within the [logistics network](../core/pipe-networks.md). It connects to an adjacent crafting inventory and fulfills network requests for items that can be crafted from available materials.

## Recipe

<div class="crafting-recipe">
    <div class="crafting-grid">
        <div class="crafting-slot"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone" title="Redstone"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone" title="Redstone"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__pipe___basic_logistics_pipe.png" class="crafting-item" alt="Basic Logistics Pipe" title="Basic Logistics Pipe"></div>
        <div class="crafting-slot"><img src="../../assets/icons/minecraft__redstone.png" class="crafting-item" alt="Redstone" title="Redstone"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"><img src="../../assets/icons/logistics__core___stone_gear.png" class="crafting-item" alt="Stone Gear" title="Stone Gear"></div>
        <div class="crafting-slot"></div>
    </div>
    <div class="crafting-arrow">→</div>
    <div class="crafting-output">
        <span class="crafting-count">1</span>
    </div>
</div>

**Yields:** 1× Crafting Logistics Pipe

## Behavior

The Crafting Logistics Pipe bridges the logistics network and a connected crafting interface. When the network receives a request for an item that isn't available in any [Provider Logistics Pipe](provider-logistics-pipe.md), it checks whether any Crafting Logistics Pipe can produce it. If a matching recipe is configured and all ingredients are available in the network, this pipe requests the ingredients from providers and processes the crafting operation.

**Key features:**
- Fulfills network requests by crafting items on demand
- Requests ingredients automatically from the logistics network
- Configurable crafting recipe via GUI
- Connects to all adjacent inventories for ingredient input and output

## Configuration

Use a [Wrench](../tools/wrench.md) to open the GUI:

**Recipe grid (3×3):** Define the crafting recipe this pipe will use. Place ingredient items in the slots to specify the pattern. The pipe will request these ingredients from the network as needed.

**Output slot:** Shows the item this pipe will produce. The output is delivered into an adjacent inventory or routed back through the network to the requester.

## Tips

- Place adjacent to a chest that serves as a crafting buffer — ingredients are deposited here before crafting begins
- Ensure all ingredients are available via [Provider Logistics Pipes](provider-logistics-pipe.md) on the same network
- Chain multiple Crafting Logistics Pipes for multi-step crafting chains — a pipe can use items produced by another crafting pipe
- Requires a [Basic Logistics Pipe](basic-logistics-pipe.md) as its crafting component — make sure your network has an established backbone first

## See Also
- [Basic Logistics Pipe](basic-logistics-pipe.md) - Required crafting component; the network backbone pipe
- [Provider Logistics Pipe](provider-logistics-pipe.md) - Supplies ingredients this pipe requests
- [Requester Logistics Pipe](requester-logistics-pipe.md) - Trigger crafting by requesting the output item
- [Chassis Logistics Pipe](chassis-logistics-pipe.md) - Modular pipe for advanced crafting with modules
- [Pipe Networks](../core/pipe-networks.md) - How the logistics network works
- [Tier System](../core/tier-system.md) - Tier 3 network logistics
- [Stone Gear](../materials/stone-gear.md) - Crafting component
