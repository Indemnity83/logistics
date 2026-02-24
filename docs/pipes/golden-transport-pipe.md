# Golden Transport Pipe

The **Golden Transport Pipe** is a [Tier 1](../core/tier-system.md) mechanical pipe that accelerates items when powered by redstone. Use it to speed up item transport in your network.

## Recipe
**Crafting:**
- 1× Gold Ingot
- 1× Glass
- **Yields:** 8× Golden Transport Pipe

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
