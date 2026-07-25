# Logistics Pipes

*The "glue" of a classic tech base — request/provider network logistics layered on top of BuildCraft pipes. In Logistics this is the most complete system: the entire three-tier pipe model and network are implemented.*

**Source era:** 1.7.10 (Logistics Pipes for BuildCraft).
**Logistics module:** `logistics-automation` (pipe domain) · code in `common/src/main/java/com/logistics/pipe/`.
**Phase:** 0 (Foundation) — largely ✅ done.

See [`../principles.md`](../principles.md) for the table legend.

## Network logistics pipes

| Feature | What it did (1.7.10) | Decision | Modern take / balance notes | Status | Maps to |
|---|---|---|---|---|---|
| Basic Logistics Pipe | Network backbone; routes addressed items, optional filtering | Port | Implemented with filtering + default route | ✅ Done | `pipe` / Basic Logistics Pipe |
| Request Pipe | Pull specific items from the network on demand | Port | Requester pipe + screen | ✅ Done | `pipe` / Requester Logistics Pipe |
| Provider Pipe | Advertise an attached inventory's contents to the network | Port | Provider pipe (energy-enabled) | ✅ Done | `pipe` / Provider Logistics Pipe |
| Supplier Pipe | Keep a target inventory stocked to configured levels | Port | Supplier pipe + passive/active supplier modules | ✅ Done | `pipe` / Supplier Logistics Pipe |
| Crafting Pipe | On-demand autocrafting fulfilled by the network | Modernize | Reuses the **vanilla Crafter** as the crafting block instead of BC assembly table | ✅ Done | `pipe` / Crafting Logistics Pipe |
| Satellite Pipe | Named endpoint for addressed routing | Port | Satellite pipe + named routing | ✅ Done | `pipe` / Satellite Logistics Pipe |
| Chassis Pipe (Mk1–5) | Modular pipe with N module slots | Port | Chassis MkI–V (1/2/3/4/8 slots) | ✅ Done | `pipe` / Chassis MkI–V |
| Process/Request crafting routing | Route in-progress crafting to dedicated machines | Port | Process Logistics Pipe | ✅ Done | `pipe` / Process Logistics Pipe |

## Modules (for chassis pipes)

| Feature | What it did | Decision | Modern take / balance notes | Status | Maps to |
|---|---|---|---|---|---|
| ItemSink / default route | Accept overflow / unaddressed items | Port | Sink + Polymorphic + Enchantment + Mod sinks | ✅ Done | `pipe` / Sink modules |
| Provider module (tiers) | Advertise inventory at tiers | Port | Provider I / II | ✅ Done | `pipe` / Provider modules |
| Extractor module (tiers) | Pull from adjacent inventory at speeds | Port | Basic / MkII / Advanced (MkIII) | ✅ Done | `pipe` / Extractor modules |
| Supplier module (passive/active) | Push stock to requesters | Port | Passive + Active supplier | ✅ Done | `pipe` / Supplier modules |
| Crafter module (tiers) | Fulfill crafting at speeds/parallelism | Port | Crafter I / II / III | ✅ Done | `pipe` / Crafter modules |
| QuickSort module | Route items by type to sorted destinations | Port | Quicksort module | ✅ Done | `pipe` / QuickSort module |
| Terminus module | Terminal endpoint; stop routing past it | Port | Terminus module (color slots) | ✅ Done | `pipe` / Terminus module |

## Mechanical / smart pipes (BuildCraft-adjacent base)

*The transport tiers Logistics Pipes sat on top of. Tracked here because they're part of the same domain; BuildCraft pipe lineage is in [`buildcraft.md`](buildcraft.md).*

| Feature | What it did | Decision | Modern take / balance notes | Status | Maps to |
|---|---|---|---|---|---|
| Transport pipes | Move items, random routing | Modernize | Stone + Copper transport pipes (material identity) | ✅ Done | `pipe` / transport pipes |
| Extractor pipe | Pull from inventories | Port | Item Extractor Pipe (energy-gated) | ✅ Done | `pipe` / Item Extractor Pipe |
| Merger / passthrough / void | Combine / pipe-only / delete | Port | Merger, Passthrough, Void pipes | ✅ Done | `pipe` / smart pipes |
| Filter / insertion pipe | Item-aware routing | Port | Item Filter Pipe, Item Insertion Pipe | ✅ Done | `pipe` / smart pipes |
| Speed boost (gold) | Redstone-powered acceleration | Port | Golden Transport Pipe | ✅ Done | `pipe` / Golden Transport Pipe |
| Pipe marking / color | Visually group networks | Modernize | Marking fluid (16 dye colors) + copper weathering flavor | ✅ Done | `pipe` / marking module |

## Gaps & not-yet-ported

| Feature | What it did | Decision | Modern take / balance notes | Status | Maps to |
|---|---|---|---|---|---|
| Remote Orderer | Handheld GUI to request from the network anywhere in range | Modernize | A handheld access item (probe-like) opening the request UI; balance the range/cost. **Exploratory** — leaned on an Ender Chest-style companion to be broadly useful ([ROADMAP](../../ROADMAP.md) → Exploring/RFC) | — | Exploring — logistics QoL |
| Power gating for operations | LP operations needed power (Power Junction / supplier) | Port | Done — pipe operations consume RF; pipes render green/red by power state (#464/#465/#469). Refined in v0.8.0: a network is now powered through a dedicated **Power Junction** block (`pipe/power_junction`) rather than a battery on the pipe | ✅ Done | `pipe` / Power Junction |
| Fluid logistics | Liquid supplier/provider/request over the network | Modernize | Fluid pipes shipped (v0.7.3) — now unblocked; will be delivered via a fluid↔item packaging machine (the Fluid Transposer, not yet built) so the network stays item-based | — | Phase 1 — fluids |
| Firewall pipe | Isolate/segment a sub-network | TBD | **Deferred** — no solid use case yet (maintainer, Jul 2026). Network segmentation *sounds* useful for large bases, but the concrete player need isn't established; parked until it earns its place. Not a 1.0 item | — | Deferred |
| Logistics disk / network mgmt | Save/load network config, naming | Modernize | Data-component-based config item; revisit need | — | — |
| Security station | Per-player network permissions | Skip | Heavy, niche; out of scope for now | ❌ | — |

> TODO: confirm whether the original "default route" + sink priority behavior fully matches the current Sink module priority model, or if any edge cases differ.
