# Fluids Foundation

> **Status:** 🚧 Planned (keystone) · **Phase:** 1 — Automation core · **Module:** `logistics-automation` (new `fluid` domain)
> **Source:** [`../mods/buildcraft.md`](../mods/buildcraft.md) (waterproof pipes, pump), [`../mods/thermal-expansion.md`](../mods/thermal-expansion.md) (magma crucible, transposer) · **Depends on:** nothing (the platform layer already exists)
> **Maps to (roadmap):** Phase 1 — 🔑 Fluids foundation · **Unblocks:** Combustion Engine, Pump, Magma Crucible, Fluid Transposer, fluid logistics, biofuel bottling (Phase 2), tank carts/tanks (Phase 3)

The single highest-leverage item in Phase 1. Roughly half the remaining Phase 1/2/3 work is gated on fluid handling. **The platform abstraction is already built and wired** — this feature is the *content* that sits on it: a fluid transport pipe, a tank block, and a contract for machine fluid I/O.

## Reality check: what already exists

The hard, loader-specific part is **done**. Verified in the codebase:

| Layer | Class(es) | Location | State |
|---|---|---|---|
| Core storage contract | `IFluidStorage` (insert/extract/contents, simulate semantics) | `core/lib/fluids/` | ✅ Implemented |
| Fluid identity | `IFluidKey`, `IFluidView` | `core/lib/fluids/` | ✅ Implemented |
| Ready-made tank | `FluidTankComponent` (single-variant, NBT-persistent) | `core/lib/fluids/` | ✅ Usable as-is |
| BE capability marker | `HasFluidStorage` (`fluidStorage(side)`) | `core/lib/block/capability/` | ✅ Implemented |
| Neighbor lookup SPI | `FluidStorageLookup.find(level, pos, dir)` | `core/lib/fluids/` | ✅ Wired both loaders |
| Fabric adapter | `FabricFluidStorage`, `FabricFluidKey`, `FluidStorageAccess` (SIDED fallback) | `fabric/.../fluids`, `fabric/.../capability` | ✅ Implemented + registered |
| NeoForge adapter | `NeoForgeFluidStorage`, `NeoForgeFluidKey` | `neoforge/.../fluids` | ✅ Implemented |
| NeoForge machine wiring | `NeoForgeCapabilityRegistration.registerFluids(event, type)` | `neoforge/...` | ⚠️ Template present, currently commented out — uncomment + call per fluid BE type |

This mirrors the item-storage stack 1:1 (`IItemStorage`/`HasItemStorage`/`ItemStorageLookup` → `IFluidStorage`/`HasFluidStorage`/`FluidStorageLookup`). It is the **same pattern as Team Reborn Energy**: common code talks to an SPI; each loader supplies the native bridge (Fabric Transfer API `Storage<FluidVariant>`, NeoForge `ResourceHandler<FluidResource>`). No new external dependency is needed.

> **Doc-accuracy note:** the roadmap and the BuildCraft/TE breakdowns describe fluids as "needs a fluid-transport layer (platform fluid API)." That platform layer is already present. Update those rows from "needs the fluid layer" to "needs fluid *content* (pipes/tanks); platform layer done" when this feature is scheduled.

## Problem & goal

Logistics moves items and energy but cannot move or store **fluids**. That blocks the entire liquid-fuel power tier (Combustion/Magmatic engines), world fluid extraction (Pump), several Thermal machines (Magma Crucible, Fluid Transposer), fluid logistics over the network, and downstream Forestry/Railcraft fluid features.

**Goal:** ship a minimal, complete fluid-handling slice — *move, store, and feed fluids into machines* — that later features consume without further platform work. Match the visible, material-identity feel of item pipes.

## Requirements

### Functional
- **Fluid transport pipe(s)** — standalone fluid pipes with simple pressure/equalization flow (BuildCraft waterproof-pipe style), carrying one fluid type along a line. Connect to adjacent `HasFluidStorage` block entities (any mod's fluid inventory via `FluidStorageLookup`) and to each other. Foundation set: a base pipe, an energy-gated **extractor** pipe, and a **void** pipe (see the roster in Design sketch).
- **Tank block** — a single-block buffer backed by `FluidTankComponent`. Stores one fluid variant up to a capacity; accepts insert/extract on all sides; shows fill level (block state stages like the Battery's `CHARGE`, plus a GUI or in-world fluid render).
- **Machine fluid I/O contract** — machines that hold fluid implement `HasFluidStorage`; the NeoForge `registerFluids(...)` template is enabled and called for each such BE type (Fabric auto-discovers via the `FluidStorageAccess` SIDED fallback).
- **Container interop** — buckets (and ideally any fluid-container item) can fill/drain a tank by hand. (Fluid Transposer automates this later; manual bucket support is the minimum.)
- **First consumer = Pump** *(thin validation slice, may be its own doc/PR)* — a block that extracts a fluid source block from the world into an adjacent tank/fluid pipe, proving the whole chain end to end.

### Balance — anchored on BuildCraft

Start from BuildCraft's real numbers (it *is* the lineage) and tune from there, rather than inventing rates. Reference: **BuildCraft 7.1.27 / MC 1.7.10** (`../buildcraft` checkout). Unit: **1 bucket = 1000 mB** — vanilla and BC agree, so this is the base unit throughout.

**Pipe throughput.** BC derives every fluid pipe's rate from a single **base flow rate (default 10 mB/t)** times a per-pipe multiplier (`PipeTransportFluids` `fluidCapacities`):

| BC fluid pipe(s) | Multiplier | Rate @ base 10 | Role |
|---|---|---|---|
| Cobblestone · Wooden · Void | 1× | 10 mB/t | base / extractor / void |
| Stone · Sandstone | 2× | 20 mB/t | mid |
| Iron · Clay · Quartz · Emerald | 4× | 40 mB/t | high / routing / filtered-extract |
| Diamond · Gold | 8× | 80 mB/t | sorting / max-speed |

(Per-pipe internal buffer = `25 × base` = 250 mB; higher rate ⇒ lower travel latency. The *ratios* are what matter — keep the 1×/2×/4×/8× ladder even if we pick a higher base than 10 to suit modern pacing.)

- **Fluid pipe:** start with **one base pipe at BC's base (10–20 mB/t)**; add tiers on the 1×/2×/4×/8× ladder only as demand shows.
- **Tank:** **16,000 mB (16 buckets)** — exactly BC's `TileTank`. Port the **vertical stack-and-merge** behavior (stacked tanks form one logical reservoir, fill bottom-up); it's cheap, loved, and is the "scale is the feature" tank done as tileable blocks rather than a multiblock.
- **Pump:** BC's `TilePump` = **100 RF per source block drained**, 16,000 mB internal buffer, pumps 1000 mB/operation on a 16-tick cycle (~62.5 mB/t), pushes up to 400 mB/t into the network. Anchor here; couples to the engine line — no free pumping.
- **Extractor pipe:** BC's wooden pipe ties extraction to engine power (≈`5 × base` RF per mB/t of pull). Mirror the existing **Item Extractor Pipe**'s energy gating rather than copying the exact RF math.
- **Refinery (later, fuel chain):** for context — BC tanks 4000 mB, ~3 oil : 1 fuel; revisit in the fuel-chain brief.

## Design sketch

New `fluid` domain, parallel to `pipe`, registered as a `DomainBootstrap` (`LogisticsFluid implements DomainBootstrap`, listed in `META-INF/services/com.logistics.core.bootstrap.DomainBootstrap`).

```
common/src/main/java/com/logistics/fluid/
├── LogisticsFluid.java            # DomainBootstrap: BLOCK / ITEM / ENTITY / MENU registrars
├── pipe/                          # fluid transport pipe (see note below)
├── tank/
│   ├── TankBlock.java             # extends MachineBlock or BaseEntityBlock; FILL_LEVEL state
│   └── TankBlockEntity.java       # extends BaseBlockEntity implements HasFluidStorage
│                                  #   wraps a FluidTankComponent; saveLogisticsData/loadLogisticsData
└── pump/PumpBlock(+Entity).java   # world source → adjacent fluid storage
common/src/client/java/com/logistics/fluid/   # tank fill render, pipe fluid render
```

**Fluid pipes are standalone — fluid never joins the logistics graph (decided).** Fluid pipes run their own **simple pressure/equalization transport** (BuildCraft waterproof-pipe style): fluid flows toward lower-fill / sink neighbors, capped per tick by the pipe's rate. They are *not* modeled as modules on the item `Pipe`/`NetworkGraph`, and there is **no planned fluid-network refactor**. Keep BC's *flowRate-as-hard-cap* as the throughput contract; we do **not** need to copy BC's full section/TTL/input-output-mode machinery unless a simple equalizer proves insufficient.

**How fluid "logistics" happens later — bridge fluids to items, don't teach the network about fluids.** When network-level fluid distribution is wanted, the answer is a **packaging machine** that fills/empties containers (the **Fluid Transposer** / a canning machine) — converting fluid ↔ a filled-container *item*. The existing item logistics network then provides / requests / routes those items exactly like any other item. **The logistics network stays item-based forever.** Fluids get local transport (pipes) + a bridge (the transposer); they never become a network primitive. This makes fluid pipes a self-contained, bounded feature.

**Fluid pipe set — BuildCraft roster → Logistics identity.** BC shipped ~11 waterproof pipe variants; per *"features earn their place,"* collapse to a tight set mapped onto Logistics' existing item-pipe identity (Stone/Copper movers, Extractor, Filter, Void, Golden speed):

| Need | BC origin | Logistics fluid pipe | Ship when |
|---|---|---|---|
| Base transport | Cobblestone / Stone | Stone (or Copper) Fluid Pipe | foundation |
| Pull from tanks/machines | Wooden | Fluid Extractor Pipe (energy-gated; mirrors Item Extractor Pipe) | foundation |
| Destroy fluid | Void | Void Fluid Pipe | foundation (trivial) |
| Higher throughput | Stone 2× / Iron 4× / Gold 8× | a tier or two (e.g. Copper → Golden) | fast-follow |
| Sort by fluid type | Diamond | Fluid Filter Pipe | later (only if demand) |
| Forced directional output | Iron | — likely skip (use placement / wrench) | — |

Note the fluid pipe is a **new continuous-flow transport**, unlike item pipes (discrete `TravelingItem`s) — it shares the *domain and rendering style* of item pipes, not their movement model.

**Wiring a machine for fluid I/O:**
1. BE implements `HasFluidStorage`, returns its `FluidTankComponent` (or a sided wrapper) from `fluidStorage(side)`.
2. Fabric: nothing extra — `FluidStorageAccess` SIDED fallback already exposes any `HasFluidStorage` BE.
3. NeoForge: enable the commented `registerFluids(event, TYPE)` template in `NeoForgeCapabilityRegistration` and call it for the BE type.

## Scope & non-goals

- **In:** the foundation fluid-pipe set (base + extractor + void), one tank (with vertical stack-and-merge), the machine fluid-I/O contract, manual bucket interop, and the Pump as the validating consumer.
- **Fast-follow (this feature grows into):** higher pipe tiers on the 1×/2×/4×/8× ladder, a fluid filter pipe — added as demand shows, not gated in v1.
- **Out (separate features):** Combustion/Magmatic engines, Magma Crucible, the **Fluid Transposer / packaging machine** (the fluid↔item bridge — its own brief), the oil/biofuel fuel chain, true-multiblock bulk tanks (Railcraft, Phase 3 — our tank is *tileable*, not a schematic).
- **Out (decided, not merely deferred):** any model that puts fluids *on the logistics network*. Fluid distribution across a base is delivered later by the packaging machine + the existing **item** network — there is no fluid-network primitive, ever.
- **Out:** inventing a fluid registry or fluid types of our own — use vanilla `Fluid`s (water/lava) and whatever fuels the fuel-chain feature defines.

## Open questions

- ~~Fluid pipe approach (parallel vs network-integrated)~~ — **resolved:** standalone equalization pipes; fluid logistics later via a fluid↔item packaging machine, network stays item-based.
- **Base flow rate:** keep BC's 10 mB/t, or pick a higher base (≈20–40) for modern pacing? Keep the 1×/2×/4×/8× tier *ratios* regardless.
- **Flow model fidelity:** is a simple "flow toward lower-fill/sink, capped by rate" equalizer enough, or do we need BC's section + travel-delay machinery for stable multi-source/multi-sink behavior? Prototype the simple one first.
- Which pipes are genuinely in the foundation (base + extractor + void) vs. fast-follow (tiers, filter)?
- Tank fill visualization: block-state stages (cheap, like Battery `CHARGE`) vs. a dynamic in-world fluid quad (prettier, more client code). Start with stages?
- Should the base tank be sided-configurable (in/out per face) now, or flat all-sides until the machine-upgrade/config story lands?
- Pump scope here vs. its own brief — leaning "own brief, but built immediately after so the foundation is proven."

## Done when

- A fluid pipe carries water/lava between a tank and a vanilla/other-mod fluid inventory on **both loaders**.
- A tank stores a fluid across save/load, fills/drains from a bucket, and reports level to adjacent pipes.
- A machine BE exposing `HasFluidStorage` is readable/writable by the fluid pipe on both loaders (NeoForge `registerFluids` enabled).
- Pump moves a world fluid source into the network, consuming RF.

## References

- Roadmap: [`../roadmap.md`](../roadmap.md) → Phase 1 → 🔑 Fluids foundation
- Breakdowns: [`../mods/buildcraft.md`](../mods/buildcraft.md) (Fluid pipes, Pump, Oil/Refinery), [`../mods/thermal-expansion.md`](../mods/thermal-expansion.md) (Magma Crucible, Fluid Transposer)
- Code precedent (item side, mirror it): `core/lib/storage/IItemStorage`, `core/lib/block/capability/HasItemStorage`, `core/lib/storage/ItemStorageLookup`, `fabric/.../capability/ItemStorageAccess`, `neoforge/.../NeoForgeCapabilityRegistration`
- Already-built fluid layer: `core/lib/fluids/*`, `core/lib/block/capability/HasFluidStorage`, `fabric/.../fluids/*`, `neoforge/.../fluids/*`
- BuildCraft balance anchor (`../buildcraft`, v7.1.27 / MC 1.7.10): `common/buildcraft/transport/PipeTransportFluids.java` (base rate 10 mB/t + per-pipe multipliers; 250 mB sections), `common/buildcraft/transport/pipes/PipeFluids*.java` (the waterproof-pipe roster), `common/buildcraft/factory/TileTank.java` (16,000 mB, stack-merge), `common/buildcraft/factory/TilePump.java` (100 RF/source block, 16-tick cycle), `common/buildcraft/factory/TileRefinery.java` (fuel chain, later)
