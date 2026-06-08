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
- **Fluid transport pipe** — a pipe variant that connects to adjacent `HasFluidStorage` block entities and to other fluid pipes, moving one fluid type along the line. Connects to any mod's fluid inventory via `FluidStorageLookup`.
- **Tank block** — a single-block buffer backed by `FluidTankComponent`. Stores one fluid variant up to a capacity; accepts insert/extract on all sides; shows fill level (block state stages like the Battery's `CHARGE`, plus a GUI or in-world fluid render).
- **Machine fluid I/O contract** — machines that hold fluid implement `HasFluidStorage`; the NeoForge `registerFluids(...)` template is enabled and called for each such BE type (Fabric auto-discovers via the `FluidStorageAccess` SIDED fallback).
- **Container interop** — buckets (and ideally any fluid-container item) can fill/drain a tank by hand. (Fluid Transposer automates this later; manual bucket support is the minimum.)
- **First consumer = Pump** *(thin validation slice, may be its own doc/PR)* — a block that extracts a fluid source block from the world into an adjacent tank/fluid pipe, proving the whole chain end to end.

### Balance
- Fluid pipe throughput in **mB/tick**, tiered later if needed; start with a single sensible rate (anchor to a bucket = 1000 mB; e.g. ~100 mB/t base).
- Tank capacity in whole buckets (e.g. 16 buckets = 16,000 mB for the base tank) — generous enough to buffer an engine, not so large it replaces dedicated storage mods.
- Pump consumes RF per source block removed (couples to the engine line); no infinite free pumping.

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

**Fluid pipe — reuse the item-pipe network or run parallel?** Two options, decide before building:
- **(A) Parallel minimal pipe** — a standalone fluid pipe with simple pressure/equalization flow (BuildCraft waterproof-pipe style), *not* on the logistics graph. Simpler; ships sooner. Fluid *logistics* (provider/requester over the network) becomes a later, separate feature.
- **(B) Extend the module/network system** — model fluid endpoints as modules on the existing `Pipe`/`NetworkGraph`. Heavier, but fluid logistics falls out naturally. The `Module`/`RoutingModule`/`DispatchableModule` abstractions are item-typed today, so this is a real refactor.

Recommendation: **(A) first** to unblock engines/machines fast, then evaluate (B) when fluid logistics is scheduled. Document the call here.

**Wiring a machine for fluid I/O:**
1. BE implements `HasFluidStorage`, returns its `FluidTankComponent` (or a sided wrapper) from `fluidStorage(side)`.
2. Fabric: nothing extra — `FluidStorageAccess` SIDED fallback already exposes any `HasFluidStorage` BE.
3. NeoForge: enable the commented `registerFluids(event, TYPE)` template in `NeoForgeCapabilityRegistration` and call it for the BE type.

## Scope & non-goals

- **In:** one fluid pipe, one tank, machine fluid-I/O contract, manual bucket interop, Pump as the validating consumer.
- **Out (separate features):** Combustion/Magmatic engines, Magma Crucible, Fluid Transposer, fluid *logistics* (network provider/requester/supplier), multiblock bulk tanks (Railcraft, Phase 3), fluid pipe tiers/filters.
- **Out:** inventing a fluid registry or fluid types of our own — use vanilla `Fluid`s (water/lava) and whatever fuels the fuel-chain feature defines.

## Open questions

- Fluid pipe approach **(A) parallel vs (B) network-integrated** — gates how fluid logistics later works. **Decide first.**
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
