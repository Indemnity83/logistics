# Architecture: Domains & Module Split

> **Status: shape decided, seams provisional.** The module *count and lines* are settled — **four modules: `core` / `automation` / `forestry` / `transport`** (Jun 2026). What's still open is the precise seam placement (*where exactly does each module end so it stands alone?*) and the timing — packaging only becomes real once Forestry and Transport exist.

## Today: domains in one jar

The mod currently ships as one artifact, internally organized into **decoupled domains** that depend only on shared abstractions in `core.lib` (Dependency Inversion):

```text
common/src/main/java/com/logistics/
├── core/         # shared lib + materials, tools, Macerator; core.lib abstractions
├── pipe/         # item transport + logistics network (Logistics Pipes)
├── power/        # engines, cables, battery, energy abstraction
└── automation/   # Kiln, Laser Quarry, markers
```

Domains don't import each other; they talk through `core.lib` (`PlatformService`, energy/item/fluid lookups, network contracts, `DomainBootstrap`, etc.). This decoupling is what makes a future split *possible* — the seams already mostly exist.

## Target: a small family of modules

The aim is to let players install **only what they want** — not a monolith. Recreating five mods' worth of content in one giant jar would undercut the "install the parts you want" flexibility that defined classic tech packs.

Finalized split, roughly along the source-mod lines:

```text
logistics-core        # shared library: core.lib abstractions, energy/item/fluid APIs,
                      #   base materials (tin/bronze/dusts/gears), tools (wrench/probe).
                      #   Every other module depends on this; it ships nothing "fun" alone.

logistics-automation  # BuildCraft + Logistics Pipes + Thermal Expansion:
                      #   pipes & the logistics network, engines & cables, RF machines
                      #   (macerator, kiln, quarry). The current "main" content.

logistics-forestry    # Forestry (industrial side): farms, processing, electron tubes, the worktable.
                      #   Genetics (bees/trees/butterflies) is out of scope.

logistics-transport   # Railcraft: rails, advanced carts, tanks, signals, bulk processing.
```

### Decided: four modules, not finer

**The direction is the four-module split above — `core` + `automation` + `forestry` + `transport`** (maintainer + contributor call, Jun 2026). We will *not* carve `logistics-automation` further into `pipes` / `power` / `machines`.

A finer split was considered:

```text
logistics-pipes    # transport + logistics network (the "glue")
logistics-power    # engines, cables, battery, energy distribution
logistics-machines # RF machines: macerator, kiln, quarry, future Thermal-style machines
```

It was rejected (for now) because:
- The extra granularity buys little real player choice — pipes are far more useful *with* power once operations cost energy (see the power-gated pipe work), so the pieces don't cleanly stand alone.
- It adds inter-module API surface to keep stable for no current payoff.

This isn't urgent either way: **packaging only becomes a real question once Forestry and Transport actually exist**, so the four-module shape stays the working target and `automation` is treated as one bucket until evidence says otherwise. If the internal `core.lib` seams later prove clean enough that pipes/power/machines genuinely stand alone, revisit via Discussion.

## Principles for keeping modules separable

- **`logistics-core` owns every cross-cutting contract.** Anything two modules both need (energy unit, item/fluid transfer, the network graph interfaces, shared materials) lives in core. Modules never depend on each other directly.
- **Each module degrades gracefully alone.** A module must be playable with only `logistics-core` present. Optional cross-module synergies (e.g. Forestry machines accepting Logistics pipes) are *detected*, not *required*.
- **Loader specifics stay in adapters.** The existing multiloader rule holds across modules: `common` is loader-agnostic; Fabric/NeoForge wiring lives in loader source sets.
- **Find the edges deliberately.** Before splitting, write down what API each side needs from the other; if that surface is large or unstable, the seam isn't ready.

## Relationship to the build today

No packaging change is required to start the roadmap — the domain structure already mirrors the target. The split becomes a *build/Gradle* concern (separate artifacts) once the content justifies it. Until then, treat the module names above as **labels for groups of domains**, and keep new code on the right side of the seam.
