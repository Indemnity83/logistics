# Logistics (Fabric 1.21) - Technical Design

## Vision
Build a modern, Fabric-based logistics/pipe mod inspired by BuildCraft and Logistics Pipes, with authentic in-pipe item motion, mod interoperability, and a request/autocrafting system.

## Architecture Philosophy: The Three-Tier Model

Logistics is structured around three conceptual tiers inspired by the OSI network model. Each tier provides a layer of abstraction, and higher tiers build on lower ones without replacing them.

### Tier 1: Passive Movement (Physical + Data Link Layer)
Pipes that move and shape item flow with mechanical operations but no item-aware decisions.

**Examples:**
- Transport pipes (backbone connectivity - stone/copper variants)
- Acceleration pipes (speed boost - gold)
- Merger pipes (convergence—all inputs → single output - iron)
- Extractor pipes (mechanical extraction from inventories - wood)

**Characteristics:**
- React to flow with mechanical operations, don't make decisions
- No item-aware logic or filtering
- Operations happen regardless of item type
- Like Ethernet: connectivity and frame forwarding without addressing
- Material-based visual identity for inventory clarity

### Tier 2: Active Control (Network + Transport Layer)
Pipes that make decisions based on item type, count, or conditional logic.

**Examples:**
- Filter pipes (item-aware routing - diamond)
- Sensor pipes (monitoring and conditional signals - quartz)

**Characteristics:**
- Assert decisions based on item type or conditional logic
- Item-aware or count-aware behavior
- Like IP/TCP: routing decisions, conditional behavior
- May use advanced components (circuits, redstone) in recipes

### Tier 3: Network Logistics (Session + Presentation + Application Layer)
Abstract inventory representation with global routing and high-level services.

**Examples:**
- Request Tables (service requests)
- Provider modules (inventory abstraction)
- Chassis & network cores (modular interfaces)
- Crafting logistics (application-level protocols)

**Characteristics:**
- Inventories become abstract resources, not physical locations
- Global pathfinding and request/provider model
- Like HTTP/FTP: application-layer services
- Pipes become **ports** into a logistics system, not the system itself

### Design Rules
- **Don't mix layers**: Tier 1 pipes shouldn't make routing decisions; Tier 3 components shouldn't care about physical pipe layout
- **Progressive abstraction**: Each tier hides complexity from the next
- **All tiers interconnect**: This is a layered system, not segregated eras—higher tiers build on and connect to lower tiers
- **Resource-gated progression**: Access to higher tiers requires game progression (advanced materials)

## Guiding Principles
- **Material-based identity**: Each pipe type uses distinct vanilla materials (stone, wood, copper, iron, gold, diamond, etc.)
- **Visual clarity**: Pipes are easily distinguishable in inventory and world by their material appearance
- **Simple Tier 1 recipes**: Basic pipes use material + glass, no complex components
- **Progressive complexity**: Tier 2 adds decision-making with additional recipe components
- **Layered abstraction**: Three tiers separate physical, control, and network logic
- **Authentic visuals**: Items travel continuously through thin pipes with visible speed
- **Interop first**: Integrate with other mods via Fabric Transfer API (ItemStorage/FluidStorage)
- **Modular systems**: Item transport first, fluids next, power/cost later
- **Classic ergonomics**: Simple placement, visible connections, easy debugging

## Progression System

### Tier 1: Material Progression
Basic pipes use vanilla materials directly:
- **Stone**: Cheap entry point, slower transport
- **Wood**: Extraction mechanism (with hopper)
- **Copper**: Main backbone transport
- **Iron**: Convergence/merger
- **Gold**: Speed boost

Recipe pattern: `Material + Glass (+ Optional Component) → 8 pipes`

### Tier 2: Advanced Materials + Components
Decision-making pipes require rare materials and additional components:
- **Diamond**: Precision filtering (with advanced components)
- **Quartz**: Monitoring/redstone integration

### Tier 3: End-Game Materials
Network logistics requires end-game resources:
- Nether stars, shulker shells, etc.
- Complex crafting chains
- Modular construction

## MVP Scope (Phase 1-2)
### Phase 1: Pipes + Items (Tier 1-2) ✅ (Complete in v0.1.0)
- Thin pipe block with 6-way connections (BuildCraft-like geometry)
- Server-side traveling item simulation with continuous progress
- Client-side interpolation for smooth visuals
- Extraction from adjacent inventories (Tier 1: extractor pipes)
- Insertion into adjacent inventories at pipe exits
- Local routing with multiple behaviors:
  - **Tier 1 behaviors**: Transport (random routing), Merger (convergence), Acceleration (speed boost when powered), Extractor (mechanical extraction from inventories)
  - **Tier 2 behaviors**: Filter (item-aware routing), Sensor (count-aware redstone output)
- Wrench tool for pipe configuration

### Phase 2: Network Logistics (Tier 3 Transition)
This phase represents the shift from physical pipes to abstract network logistics:
- **Request Table block**: High-level service requests for items
- **Provider pipes**: Expose inventory contents as abstract resources
- **Global pathfinding**: Network-layer routing across the pipe graph
- **Sink pipes**: Provide overflow/destination routing for unmatched items
- **Priority system**: Request Table > Sink pipes
  - Requests are fulfilled first
  - Overflow/unmatched items route to sink pipes
- **Request flow**: request → routing → provider → dispatch traveling items
- **Autocraft support**: Integrate with vanilla Crafter (no custom crafting table)

**Key concept**: At Tier 3, pipes become **ports** into a logistics system rather than the system itself. Inventories are represented abstractly, and global routing occurs at the network level.

## Post-MVP Scope
**Future Features (order TBD):**

**Fluids:**
- Fluid pipes with Transfer API integration
- Visible fluid movement (segments or packets)

**Power / Cost:**
- Add a "logistics cost" system (token/buffer)
- Optional bridges to tech mod power systems

**Machines:**
- Custom logistics machines and processing blocks
- Integration with pipe network

**Balance & Polish:**
- Recipe progression and balancing
- Performance optimization
- Additional pipe variants and upgrades

## Architecture Overview
### Phase 1 (Implemented - Tier 1-2)
**Blocks / Block Entities:**
- PipeBlock: thin model with 6-way connectivity via blockstate properties
- PipeBlockEntity: stores traveling items list and module state (NBT)
  - Does NOT own connections (those are in blockstate)
  - Delegates tick logic to PipeRuntime

**Pipe Module System:**
- Pipe: base class that composes Module instances
- Module: interface for pipe behaviors (routing, speed, acceptance, etc.)
- PipeContext: provides modules access to world, pos, state, and entity for state storage
- PipeRuntime: handles per-tick item movement, routing at center, and insertion at exits

**ItemStack Lifecycle:**
1. A neighbor attempts to insert via Fabric Transfer API
2. Modules may reject the transfer (ingress rules, filters, etc.)
3. Accepted stacks become a TravelingItem traveling from the entry face toward the pipe center
4. When the traveling item reaches the pipe center (progress = 0.5), a RoutePlan is created and executed (see Routing Rules below)
5. Traveling continues toward the chosen exit direction
6. Only when a traveling item reaches an exit (progress = 1.0), insertion is attempted via Transfer API into the adjacent storage along its direction
7. If insertion fails at the exit, the stack is dropped as an item entity

**Routing Rules:**
At the pipe center (progress = 0.5), modules are evaluated in order. Each module returns a RoutePlan:
- **PASS**: "No opinion" - next module decides. If all modules pass, PipeRuntime chooses randomly from valid directions.
- **DROP**: Drop item as entity at pipe location.
- **DISCARD**: Delete item immediately (void behavior).
- **REROUTE(directions)**: Provide list of candidate directions. PipeRuntime randomly chooses one. If list is empty, item is dropped.
- **SPLIT(items)**: Provide list of TravelingItems, each with its own direction, to send items in multiple directions simultaneously.

The first module to return something other than PASS wins. If the winning plan is REROUTE with an empty list, the item is dropped.

**Velocity Preservation:**
- TravelingItems carry their own velocity (speed field)
- All pipes share the same maximum speed; items maintain constant speed unless an acceleration value is applied
- Acceleration modules (gold pipes) apply acceleration to TravelingItems when powered by redstone
- When a TravelingItem moves from one pipe to another, its velocity is preserved via the overloaded `insert(TravelingItem)` method
- External inserts (from inventories, hoppers, etc.) start with default initial speed

**Module Composition Rules:**
- Routing: First module to return non-PASS wins
- Acceptance: All modules must return true from canAcceptFrom() or the insert is rejected
- State Storage: All per-pipe state lives in the PipeBlockEntity and is accessed through PipeContext using module-specific keys

**Item Movement:**
- TravelingItem: represents item in transit with stack, direction, progress, and speed
- Movement handled by PipeRuntime.tick() with acceleration and speed control
- PipeBlockEntityRenderer: renders traveling items with client-side interpolation

**Implemented Behaviors (by Tier):**

**Tier 1 (Passive Movement):**
- Transport: Random routing at junctions (stone/copper materials)
- Acceleration: Speed boost when powered by redstone (gold material)
- Merger: All inputs route to single output (iron material)
- Extractor: Mechanical extraction from adjacent inventories (wood material)
- Void: Mechanical deletion at pipe center (obsidian material)
- Pipe-only ingress: Restricts external inserts (used by transport/merger)

**Tier 2 (Active Control):**
- Filter: Item-aware routing based on per-side filters (diamond material)
- Sensor: Count-aware redstone signal based on item count (quartz material)

**Implementation Modules:**

*Tier 1 (Mechanical):*
- ExtractionModule: Implements extractor behavior
- MergerModule: Implements merger behavior
- BoostModule: Implements acceleration behavior
- VoidModule: Implements void behavior
- PipeOnlyModule: Restricts ingress to pipe-to-pipe only

*Tier 2 (Decision-making):*
- SmartSplitterModule: Implements filter behavior (item-aware)
- ComparatorModule: Implements sensor behavior (count-aware)

### Phase 2+ (Future - Tier 3)
**Planned Systems:**
- RequestTableBlockEntity: UI + request handling
- PipeNetworkGraph: per-dimension graph for pathfinding
- RoutingEngine: long-distance pathfinding with priorities and filters
- ProviderPipe/RequesterNode/CrafterNode: logistics system components

## Rendering Plan (Items)
**Phase 1 (Implemented):**
- TravelingItem stores: stack, direction, progress (0.0-1.0), and speed
- PipeBlockEntityRenderer renders items with client-side interpolation between ticks
- Items accelerate/decelerate smoothly based on pipe speed and acceleration values
- Rendering uses trapezoidal integration for smooth partial-tick positioning

**Phase 2+ (Future):**
- Potential visual improvements: particle effects, pipe flow indicators, etc.

## Routing Plan
**Phase 1 (Implemented - Local Routing):**
- Items route at pipe center (progress = 0.5)
- Modules make local routing decisions via Module.route():
  - Random selection (default transport pipes)
  - Single-direction (merger pipes)
  - Filter-based (filter pipes)
  - Discard (void pipes)
- No path planning or global graph search
- Items rejected if no valid route (dropped as item entity)

**Phase 2+ (Future - Pathfinding):**
- PipeNetworkGraph: per-dimension graph of connected pipes
- RoutingEngine: long-distance pathfinding with:
  - Edge weight by distance and congestion
  - Node priority (provider preference)
  - Filter match constraints
  - Support for request/provider logistics system

## Interop / Integration
- Fabric Transfer API (ItemStorage/FluidStorage)
- Expose pipe endpoints as storages for other mods
- Optional compat hooks for common tech mods (future)

## Data / Persistence
**Phase 1 (Implemented):**
- PipeBlockEntity NBT stores:
  - TravelingItems list (stack, direction, progress, speed)
  - ModuleState compound for per-pipe module data
- Modules use PipeContext.getOrCreateModuleState(key) for state storage
- Examples: round-robin index, extraction cooldown, active face

**Phase 2+ (Future):**
- Network graph cache for pathfinding
- Provider/requester persistent state

## UX / Tools
**Phase 1 (Implemented):**
- Wrench tool for pipe configuration
  - Merger pipes: cycle output direction
  - Extractor pipes: cycle extraction face
  - Filter pipes: open filter GUI

**Phase 2+ (Future):**
- Debug overlay for routing and pipe contents
- Connection toggles (individual side enable/disable)

## Milestones
**Completed:**
1. ✅ Fabric mod scaffold + basic pipe placement (Phase 1)
2. ✅ Item movement + visible rendering (Phase 1)
3. ✅ Inventory extraction/insertion (Phase 1)
4. ✅ Tier 1-2 pipe behaviors implemented (Phase 1)

**Next Up:**
5. 🚧 Request Table + provider pipes + sink pipes (Phase 2 - Tier 3 transition)
6. Autocraft via vanilla Crafter (Phase 2)

**Future:**
- Fluids
- Power/cost system
- Custom machines
- Recipe balancing and progression
- Performance optimization

## Open Questions
**Resolved:**
- ✅ Visual style for pipes: BuildCraft-inspired thin pipes with material-based appearance
- ✅ Item rendering: floating items with smooth acceleration/deceleration
- ✅ Tier model: OSI-inspired three-tier architecture
- ✅ Naming approach: Behavior-centric in code, material-based appearance in-game
- ✅ Connection rules: All tiers interconnect
- ✅ Progression: Material-based for visual clarity and inventory readability

**Open:**
- Specific recipe components for Tier 2 pipes (circuits, redstone, etc.)
- Request Table UX for Phase 2: list vs search-first interface
- Extraction upgrade tiers: speed-wrapped versions vs separate pipe types
