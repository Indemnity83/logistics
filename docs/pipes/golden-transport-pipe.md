<div class="infobox">
    <div class="infobox-header">Golden Transport Pipe</div>
    <div class="infobox-image">
        <img src="../assets/icons/logistics__pipe___gold_transport_pipe.png" alt="Golden Transport Pipe">
    </div>
    <table class="infobox-table">
        <tr>
            <td class="infobox-label">ID</td>
            <td class="infobox-value"><code>logistics:pipe/gold_transport_pipe</code></td>
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

# Golden Transport Pipe

The **Golden Transport Pipe** is a [Tier 1](../core/tier-system.md) mechanical pipe that accelerates items when powered by redstone. Use it to speed up item transport in your network.

## Recipe

<div class="crafting-recipe">
    <div class="crafting-grid">
        <div class="crafting-slot"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"><img src="../assets/icons/minecraft__gold_ingot.png" class="crafting-item" alt="Gold Ingot"></div>
        <div class="crafting-slot"><img src="../assets/icons/minecraft__glass.png" class="crafting-item" alt="Glass"></div>
        <div class="crafting-slot"><img src="../assets/icons/minecraft__gold_ingot.png" class="crafting-item" alt="Gold Ingot"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"></div>
        <div class="crafting-slot"></div>
    </div>
    <div class="crafting-arrow">→</div>
    <div class="crafting-output">
        <img src="../assets/icons/logistics__pipe___golden_transport_pipe.png" class="crafting-item" alt="Golden Transport Pipe">
        <span class="crafting-count">8</span>
    </div>
</div>

**Yields:** 8× Golden Transport Pipe

## Behavior

Golden Transport Pipes accelerate items passing through them **when powered by redstone**. Items gain speed and maintain their increased velocity as they continue through the network.

**Key features:**
- Speed boost when powered by redstone
- Items maintain velocity when leaving the pipe
- No effect when unpowered (acts as normal transport pipe)
- Random routing at junctions (like [Copper Transport Pipe](copper-transport-pipe.md))
- Connects to all other pipe types

## Acceleration Mechanics

- **Powered state:** Redstone signal directly adjacent to the pipe
- **Effect:** Items passing through gain speed boost
- **Velocity preservation:** Accelerated items keep their speed in subsequent pipes
- **Stacking:** Items can pass through multiple powered golden pipes for cumulative speed

## Tips

- Place redstone torch, lever, or redstone dust adjacent to the pipe
- Acceleration persists - items stay fast through normal pipes
- Use at the start of long transport runs for faster delivery
- Chain multiple golden pipes for higher speeds
- Unpowered golden pipes work as normal transport (no penalty)
- Random routing still applies - use with [Item Merger Pipes](item-merger-pipe.md) for control

## Common Patterns

**High-speed main line:**
```
[Source] → [Golden Pipe (powered)] → [Long copper pipe run] → [Destination]
```

**Speed boost chain:**
```
[Source] → [Golden] → [Golden] → [Golden] → [Fast transport]
              ↑          ↑          ↑
          [Redstone] [Redstone] [Redstone]
```

**Conditional acceleration:**
```
[Lever] → [Golden Pipe] → [Network]
            Toggle speed on/off
```

## See Also
- [Item Transport](../core/item-transport.md) - Speed and acceleration mechanics
- [Copper Transport Pipe](copper-transport-pipe.md) - Standard transport
- [Stone Transport Pipe](stone-transport-pipe.md) - Slow alternative
- [Routing](../core/routing.md) - Random routing behavior
