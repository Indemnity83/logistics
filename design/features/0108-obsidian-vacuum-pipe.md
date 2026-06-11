# Obsidian Vacuum Pipe

> **Status:** 🚧 Planned · **Phase:** 1 — Automation core · **Module:** `logistics-automation` (`pipe` domain)
> **Source:** [`../mods/buildcraft.md`](../mods/buildcraft.md) (Obsidian pipe — world pickup) · **Depends on:** nothing
> **Maps to (roadmap):** Phase 1 — pipes (Obsidian vacuum pipe)

A pipe that vacuums up dropped item entities from the world around it and injects them into the pipe network. A faithful, low-risk Port — the pipe/module system already has everything needed; this is a new module + pipe registration.

## Problem & goal

There's no way to ingest *world drops* (mob loot, broken-block items, mining debris, items tossed on the ground) into the logistics network. The BuildCraft Obsidian pipe was the canonical solution and a beloved classic block.

**Goal:** a pipe that pulls nearby `ItemEntity` drops into the network, with a balanced pickup range and (optionally) energy cost, reusing the existing module pattern.

## Requirements

### Functional
- A new pipe block that, each tick (or every N ticks), scans an AABB around itself for `ItemEntity` instances and pulls them in.
- Picked-up stacks become `TravelingItem`s injected into the pipe, then route normally (to sinks/providers/the network) — no special routing.
- Respects pickup delay (don't instantly re-grab items a pipe just dropped) and a TTL/age sanity check.
- Configurable/balanced **range** (small by default; see Balance).
- Connects to the network like any pipe; injection direction is into the pipe core.
- Optional: a filter (only vacuum matching items) — **defer to v2** unless trivial.

### Balance
- Default range **modest** (≈ 1–2 block radius around the pipe) — a collector, not a base-wide magnet. Larger range is a candidate for an upgraded variant or an augment later.
- Consider an **RF cost per pickup** to fit the power-gated-pipe direction (pipe operations already consume RF in this codebase). **Lean: small or zero cost in v1, revisit with the power-gating model.**
- Should not fight hoppers/players for the same items in a griefy way; pickup delay handles the worst of it.

## Design sketch

The closest precedent is the **Void pipe** (`VoidModule`) for "special routing pipe," plus the **Laser Quarry's** drop-collection for the world-entity scan — both verified in the codebase.

- **`ObsidianVacuumModule`** in `pipe/modules/`, implementing `TickingModule` (the scan) and `RoutingModule` (pass-through, so normal routing applies):
  - `onTick(PipeContext ctx)`: build an AABB around `ctx.pos()`, `level.getEntitiesOfClass(ItemEntity.class, box, predicate)`, and for each: take `itemEntity.getItem()`, create a `TravelingItem`, inject via the pipe's `addItem(...)` (or `PipeApi.forceInsert`), then `itemEntity.remove(DISCARDED)`. (Mirror of `PipeBlockEntity.dropItem(...)`, which does the reverse; `QuarryBlockBreaker` shows the scan idiom.)
  - `route(...)`: return `RoutePlan.pass()` so the network routes the captured item.
- **Register the pipe** in `PipeTypes` (`new Pipe(new ObsidianVacuumModule(), new TransportModule(...))`) and the block/item in `LogisticsPipe.BLOCK`/`.ITEM` (the standard `registerBlockWithItem` + `PipeBlock(props, PipeTypes.OBSIDIAN_VACUUM)` shape).
- Material identity: obsidian-styled texture (the name carries the lineage).
- Optional client `randomDisplayTick` suck particles (Void pipe has the precedent).

## Scope & non-goals

- **In:** the vacuum pipe, world `ItemEntity` pickup, normal routing of captured items, a sensible default range.
- **Out (v2 candidates):** XP/entity pickup, item filtering, large/area "magnet" range, dedicated upgrade tiers, sucking from a directional cone only.
- **Out:** picking up non-item entities or blocks.

## Open questions

- **Range** default and whether it's fixed, config-driven, or upgradeable. **Lean: small fixed default in v1, config value.**
- **RF cost per pickup** — couple to the existing pipe power-gating or keep free in v1? Resolve alongside the power model.
- Scan frequency (every tick vs every N ticks) for performance with many vacuum pipes — **lean: every few ticks**.
- Should it pull through a block face only (BuildCraft pointed the open face) or omnidirectionally? **Lean: omnidirectional radius for simplicity; revisit faced behavior if it feels off.**

## Done when

- Dropped items within range are pulled into the pipe and routed to the network on both loaders.
- Items the network just dropped aren't instantly re-grabbed (pickup delay respected).
- Performance is fine with several vacuum pipes loaded (throttled scan).

## References

- Roadmap: [`../roadmap.md`](../roadmap.md) → Phase 1 → pipes; [`../mods/buildcraft.md`](../mods/buildcraft.md) → Obsidian pipe row
- Code: `pipe/modules/VoidModule` (special pipe precedent), `core/lib/pipe/{Module,TickingModule,RoutingModule,TravelingItem}`, `pipe/block/entity/PipeBlockEntity#dropItem` (reverse of the operation), `automation/laserquarry/.../QuarryBlockBreaker` (entity-scan idiom), `pipe/PipeTypes`, `LogisticsPipe.java`
