# Logistics (Fabric 1.21) - Technical Design

## Vision
Build a modern, Fabric-based logistics/pipe mod inspired by BuildCraft and Logistics Pipes, with authentic in-pipe item motion, mod interoperability, and a request/autocrafting system.

## Guiding Principles
- **Authentic visuals**: Items travel continuously through thin pipes with visible speed
- **Interop first**: Integrate with other mods via Fabric Transfer API (ItemStorage/FluidStorage)
- **Modular systems**: Item transport first, fluids next, power/cost later
- **Classic ergonomics**: Simple placement, visible connections, easy debugging

## MVP Scope (Phase 1-2)
### Phase 1: Pipes + Items ✅ (Complete in v0.1.0)
- Thin pipe block with 6-way connections (BuildCraft-like geometry).
- Server-side traveling item simulation with continuous progress.
- Client-side interpolation for smooth visuals.
- Extraction from adjacent inventories (wooden pipes).
- Insertion into adjacent inventories at pipe exits.
- Local routing with multiple behaviors:
  - Random (cobblestone/stone pipes).
  - Directional (iron pipes).
  - Round-robin distribution (copper pipes).
  - Filter-based (diamond pipes).
  - Speed boost when powered (gold pipes).
  - Redstone output (quartz pipes).
  - Item deletion (void pipes).
- Wrench tool for pipe configuration.

### Phase 2: Logistics Basics
- Request Table block + GUI.
- Provider pipes expose inventory contents.
- Sink pipes provide destinations for items in the network.
- Priority system: Request Table > Sink pipes.
  - Requests are fulfilled first.
  - Overflow/unmatched items route to sink pipes.
- Request flow: request → routing → provider → dispatch traveling items.
- Autocraft support via vanilla Crafter (no custom crafting table).

## Post-MVP Scope
**Future Features (order TBD):**

**Fluids:**
- Fluid pipes with Transfer API integration.
- Visible fluid movement (segments or packets).

**Power / Cost:**
- Add a "logistics cost" system (token/buffer).
- Optional bridges to tech mod power systems.

**Machines:**
- Custom logistics machines and processing blocks.
- Integration with pipe network.

**Balance & Polish:**
- Recipe progression and balancing.
- Performance optimization.
- Additional pipe variants and upgrades.

## Architecture Overview
### Phase 1 (Implemented)
**Blocks / Block Entities:**
- PipeBlock: thin model with 6-way connectivity via blockstate properties.
- PipeBlockEntity: stores traveling items list and module state (NBT).
  - Does NOT own connections (those are in blockstate).
  - Delegates tick logic to PipeRuntime.

**Pipe Module System:**
- Pipe: base class that composes Module instances.
- Module: interface for pipe behaviors (routing, speed, acceptance, etc.).
- PipeContext: provides modules access to world, pos, state, and entity for state storage.
- PipeRuntime: handles per-tick item movement, routing at center, and insertion at exits.

**ItemStack Lifecycle:**
1. A neighbor attempts to insert via Fabric Transfer API.
2. Modules may reject the transfer (ingress rules, filters, etc.).
3. Accepted stacks become a TravelingItem traveling from the entry face toward the pipe center.
4. When the traveling item reaches the pipe center (progress = 0.5), a RoutePlan is created and executed (see Routing Rules below).
5. Traveling continues toward the chosen exit direction.
6. Only when a traveling item reaches an exit (progress = 1.0), insertion is attempted via Transfer API into the adjacent storage along its direction.
7. If insertion fails at the exit, the stack is dropped as an item entity.

**Routing Rules:**
At the pipe center (progress = 0.5), modules are evaluated in order. Each module returns a RoutePlan:
- **PASS**: "No opinion" - next module decides. If all modules pass, PipeRuntime chooses randomly from valid directions.
- **DROP**: Drop item as entity at pipe location.
- **DISCARD**: Delete item immediately (void behavior).
- **REROUTE(directions)**: Provide list of candidate directions. PipeRuntime randomly chooses one. If list is empty, item is dropped.
- **SPLIT(items)**: Provide list of TravelingItems, each with its own direction, to send items in multiple directions simultaneously.

The first module to return something other than PASS wins. If the winning plan is REROUTE with an empty list, the item is dropped.

**Velocity Preservation:**
- TravelingItems carry their own velocity (speed field).
- All pipes share the same maximum speed; items maintain constant speed unless an acceleration value is applied.
- BoostModule (gold pipes) applies acceleration to TravelingItems when powered by redstone.
- When a TravelingItem moves from one pipe to another, its velocity is preserved via the overloaded `insert(TravelingItem)` method.
- External inserts (from inventories, hoppers, etc.) start with default initial speed.

**Module Composition Rules:**
- Routing: First module to return non-PASS wins.
- Acceptance: All modules must return true from canAcceptFrom() or the insert is rejected.
- State Storage: All per-pipe state lives in the PipeBlockEntity and is accessed through PipeContext using module-specific keys.

**Item Movement:**
- TravelingItem: represents item in transit with stack, direction, progress, and speed.
- Movement handled by PipeRuntime.tick() with acceleration and speed control.
- PipeBlockEntityRenderer: renders traveling items with client-side interpolation.

**Implemented Modules:**
- ExtractionModule: wooden pipes extract from inventories.
- MergerModule: iron pipes route all inputs to single output.
- SplitterModule: copper pipes distribute evenly (round-robin).
- SmartSplitterModule: diamond pipes filter items by configured rules.
- BoostModule: gold pipes accelerate items when powered.
- ComparatorModule: quartz pipes emit redstone signal.
- VoidModule: void pipes delete items.
- PipeOnlyModule: restricts ingress to pipe-to-pipe only.

### Phase 2+ (Future)
**Planned Systems:**
- RequestTableBlockEntity: UI + request handling.
- PipeNetworkGraph: per-dimension graph for pathfinding (not needed for Phase 1).
- RoutingEngine: long-distance pathfinding with priorities and filters.
- ProviderPipe/RequesterNode/CrafterNode: logistics system components.

## Rendering Plan (Items)
**Phase 1 (Implemented):**
- TravelingItem stores: stack, direction, progress (0.0-1.0), and speed.
- PipeBlockEntityRenderer renders items with client-side interpolation between ticks.
- Items accelerate/decelerate smoothly based on pipe speed and acceleration values.
- Rendering uses trapezoidal integration for smooth partial-tick positioning.

**Phase 2+ (Future):**
- Potential visual improvements: particle effects, pipe flow indicators, etc.

## Routing Plan
**Phase 1 (Implemented - Local Routing):**
- Items route at pipe center (progress = 0.5).
- Modules make local routing decisions via Module.route():
  - Random selection (default transport pipes).
  - Single-direction (iron/merger pipes).
  - Round-robin (copper pipes).
  - Filter-based (diamond pipes).
  - Discard (void pipes).
- No path planning or global graph search.
- Items rejected if no valid route (dropped as item entity).

**Phase 2+ (Future - Pathfinding):**
- PipeNetworkGraph: per-dimension graph of connected pipes.
- RoutingEngine: long-distance pathfinding with:
  - Edge weight by distance and congestion.
  - Node priority (provider preference).
  - Filter match constraints.
  - Support for request/provider logistics system.

## Interop / Integration
- Fabric Transfer API (ItemStorage/FluidStorage).
- Expose pipe endpoints as storages for other mods.
- Optional compat hooks for common tech mods (future).

## Data / Persistence
**Phase 1 (Implemented):**
- PipeBlockEntity NBT stores:
  - TravelingItems list (stack, direction, progress, speed).
  - ModuleState compound for per-pipe module data.
- Modules use PipeContext.getOrCreateModuleState(key) for state storage.
- Examples: round-robin index, extraction cooldown, active face.

**Phase 2+ (Future):**
- Network graph cache for pathfinding.
- Provider/requester persistent state.

## UX / Tools
**Phase 1 (Implemented):**
- Wrench tool for pipe configuration.
  - Iron pipes: cycle output direction.
  - Wooden pipes: cycle extraction face.
  - Diamond pipes: open filter GUI.

**Phase 2+ (Future):**
- Debug overlay for routing and pipe contents.
- Connection toggles (individual side enable/disable).

## Milestones
**Completed:**
1. ✅ Fabric mod scaffold + basic pipe placement (Phase 1).
2. ✅ Item movement + visible rendering (Phase 1).
3. ✅ Inventory extraction/insertion (Phase 1).

**Next Up:**
4. 🚧 Request Table + provider pipes + sink pipes (Phase 2).
5. Autocraft via vanilla Crafter (Phase 2).

**Future:**
- Fluids
- Power/cost system
- Custom machines
- Recipe balancing and progression
- Performance optimization

## Open Questions
**Resolved:**
- ✅ Visual style for pipes: BuildCraft-inspired thin pipes with opaque/transparent textures.
- ✅ Item rendering: floating items with smooth acceleration/deceleration.

**Open:**
- Request Table UX for Phase 2: list vs search-first interface.
- Extraction upgrades: tier system or separate pipe types?

