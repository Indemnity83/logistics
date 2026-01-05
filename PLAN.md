# Logistics (Fabric 1.21) - Project Plan

## Vision
Build a modern, Fabric-based logistics/pipe mod inspired by BuildCraft and Logistics Pipes, with authentic in-pipe item motion, mod interoperability, and a request/autocrafting system. The mod name is temporarily "Logistics".

## Guiding Principles
- Authentic visuals: items travel continuously through thin pipes with visible speed.
- Interop first: integrate with other mods via Fabric Transfer API (ItemStorage/FluidStorage).
- Modular systems: item transport first, fluids next, power/cost later.
- Classic ergonomics: simple placement, visible connections, easy debugging.

## MVP Scope (Phase 1-2)
### Phase 1: Pipes + Items
- Thin pipe block with 6-way connections (BuildCraft-like geometry).
- Server-side traveling item simulation with continuous progress along edges.
- Client-side interpolation for smooth visuals.
- Basic extraction/insertion into adjacent inventories.
- Simple routing: pathing to a destination with filters/priorities.

### Phase 2: Logistics Basics
- Request Table block + GUI.
- Provider pipes expose inventory contents.
- Request flow: request -> routing -> provider -> dispatch traveling items.
- Autocraft support via vanilla Crafter (no custom crafting table).

## Post-MVP Scope
### Phase 3: Fluids
- Fluid pipes with Transfer API integration.
- Visible fluid movement (segments or packets).

### Phase 4: Power / Cost
- Add a "logistics cost" system (token/buffer).
- Optional bridges to tech mod power systems.

## Architecture Overview
### Blocks / Block Entities
- PipeBlock: thin model with 6-way connectivity.
- PipeBlockEntity: owns connections, upgrades, and per-tick movement queues.
- RequestTableBlockEntity: UI + request handling.

### Core Systems
- PipeNetworkGraph: per-dimension graph of connected pipes.
- RoutingEngine: selects path by distance, priority, filter match, and load.
- TravelingItem: server data for item stack, path, and progress.
- TravelingItemRenderer: client interpolation and in-pipe rendering.

### Logistics Nodes
- ProviderPipe: scans adjacent ItemStorage and advertises contents.
- RequesterNode (via Request Table): collects requests, submits to routing.
- CrafterNode: wraps vanilla Crafter as a crafting provider.

## Rendering Plan (Items)
- Represent each traveling item as a packet with `edge`, `progress`, and `speed`.
- Render in pipe-local coordinates along edge center line.
- Client interpolates between server snapshots for smooth movement.

## Routing Plan (Items)
- Graph search (Dijkstra/A*) with:
  - Edge weight by distance and congestion.
  - Node priority (provider preference).
  - Filter match constraints.

## Interop / Integration
- Fabric Transfer API (ItemStorage/FluidStorage).
- Expose pipe endpoints as storages for other mods.
- Optional compat hooks for common tech mods (future).

## Data / Persistence
- Per-pipe NBT: upgrades, filters, local settings.
- Network graph cache rebuilt on topology change.

## UX / Tools
- Wrench tool for connection toggles and info.
- Debug overlay for routing and pipe contents (later).

## Milestones
1) Fabric mod scaffold + basic pipe placement.
2) Item movement + visible rendering.
3) Inventory extraction/insertion.
4) Request Table + provider pipes.
5) Autocraft via vanilla Crafter.
6) Fluids.
7) Power/cost system.

## Open Questions
- Final mod name and ID ("logistics" for now).
- Request Table UX: list vs search-first.
- Visual style for pipes and traveling items.

