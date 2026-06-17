# Firewall Pipe

> **Status:** 🚧 Planned · **Phase:** 1 — Automation core · **Module:** `logistics-automation` (`pipe` domain)
> **Source:** [`../mods/logistics-pipes.md`](../mods/logistics-pipes.md) (Firewall pipe — network segmentation) · **Depends on:** nothing
> **Maps to (roadmap):** Phase 1 — logistics advanced (Firewall pipe)

A pipe that segments a logistics network — controlling what (items, requests, provider advertisements, crafting) is allowed to cross between two sub-networks. The tool for keeping large bases sane: isolate a sub-base without physically disconnecting it.

## Problem & goal

Once a network gets large, everything sees everything: providers advertise base-wide, requests route anywhere, sorting sprawls. The Logistics Pipes Firewall let you place a controlled boundary so two halves of a contiguous pipe run behave as separate-ish networks.

**Goal:** a pipe that acts as a configurable one-way/selective boundary between network segments, so builders can partition a network without cutting it. This is the most design-sensitive of the three pipe features because it touches the **network graph**, so the approach choice matters.

## Requirements

### Functional
- A Firewall pipe sits in a pipe run and **filters what crosses it** between the two sides. Configurable (per-category toggles): block/allow **items**, **requests**, **provider advertisements**, **crafting** propagation across the boundary.
- Directionality: at minimum a symmetric block; ideally per-direction (allow A→B, block B→A) like a real firewall.
- Configured via wrench/GUI; state persists in the pipe BE.
- The two sides remain physically connected (it's still a pipe) but logically partitioned per the rules.

### Balance
- A pure organizational/QoL block — no power cost needed, no throughput change. Its value is control, not speed.
- Must not create routing dead-ends or lost items: blocked items shouldn't vanish — they should simply not be offered a route across (stay/return), or be rejected cleanly.

## Design sketch

The explore surfaced **two viable approaches** — this is the key decision to make before building.

**(A) Routing-gate module (lighter).** A `FirewallModule implements RoutingModule` (+ config state) that lives on a normal pipe. The network stays one graph; the module vetoes crossings at routing time: in `route(...)`, reject/return items that came from the disallowed side, and have the module's presence filter request/provider propagation queries that pass through it.
- *Pros:* no `NetworkGraph`/`PipeNetwork` changes; uses the existing `Module` + `RoutingModule` hooks; ships fast.
- *Cons:* "segmentation" is emulated at the routing layer, not the topology — provider/request *advertisement* filtering needs the network controller to consult the firewall along paths, which the current `NetworkController`/`NetworkGraph` may not expose cleanly. Item-flow blocking is easy; advertisement/request partitioning is the hard part.

**(B) True graph segmentation (heavier, more correct).** Treat firewall pipes as typed edges in the graph so `NetworkPathfinder`/`NetworkGraph` can compute reachability *subject to* firewall rules — effectively sub-networks that share structure. Provider visibility and request routing naturally respect the boundary.
- *Pros:* semantically correct; advertisement/request/crafting partitioning falls out of pathfinding.
- *Cons:* invasive — `INetworkGraph`/`NetworkGraph`/`NetworkController` changes; risk to a system that's currently complete and stable.

**Recommendation:** start with **(A)** for *item-flow* and *request* blocking (the most-wanted behaviors), and explicitly scope provider-advertisement partitioning as the part that may need a slice of **(B)**. Validate against the actual `NetworkController` request/provider query paths before committing — if (A) can't filter advertisements without ugly hacks, escalate that sub-feature to (B). **Decide via a short spike before scheduling the full feature.**

- Module + pipe registration follow the standard pattern: `FirewallModule` in `pipe/modules/`, pipe in `PipeTypes`, block/item in `LogisticsPipe`. Config UI mirrors `RequesterModule.onWrench` → `openMenu` with a small toggle screen.

## Scope & non-goals

- **In:** a configurable boundary pipe; per-category cross toggles (items, requests, advertisements, crafting) to the extent the chosen approach supports; per-direction if feasible.
- **Out:** per-player security/permissions (that's the skipped Security Station), encryption/locking, multiple named firewall "channels," cross-dimension boundaries.
- **Out (gated on approach):** full topology-level sub-networks if we ship (A) — document what's emulated vs. real.

## Open questions

- **Approach (A) routing-gate vs (B) graph segmentation** — the central call. Needs a spike against `NetworkController`/`NetworkGraph` to confirm whether provider-advertisement filtering is doable in (A). **Resolve before scheduling.**
- Which cross-categories ship in v1 — likely **items + requests** first, advertisements/crafting as the harder follow.
- Symmetric vs per-direction config for v1. **Lean: symmetric first.**
- Exact failure semantics for a blocked item mid-transit (return to sender? hold? reject at the boundary face?).

## Done when

- A Firewall pipe in a contiguous run prevents the configured categories from crossing, both loaders.
- Blocked items are not lost (clean rejection/return).
- Config persists and is editable via wrench/GUI.
- The behavior is documented honestly as routing-emulated vs. topology-real per the chosen approach.

## References

- Roadmap: [`../delivery-plan.md`](../delivery-plan.md) → Phase 1 → logistics advanced; [`../mods/logistics-pipes.md`](../mods/logistics-pipes.md) → Firewall pipe row
- Code: `core/lib/pipe/{Module,RoutingModule}`, `pipe/modules/*` (e.g. `NetworkRouterModule`, `VoidModule`), `pipe/network/{NetworkRegistry,PipeNetwork,NetworkController}`, `core/lib/network/{INetworkGraph,NetworkGraph,NetworkPathfinder,ILogisticsNetwork}`, `pipe/ui/RequesterScreenHandler` (config-UI pattern), `pipe/PipeTypes`, `LogisticsPipe.java`
