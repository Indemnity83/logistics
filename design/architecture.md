# Architecture: Domains & Module Split

> **Status: provisional.** This is a target to steer toward and revisit, not a committed boundary. The hard question — *where exactly do the seams go so each module stands alone?* — is still open.

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

Proposed provisional split, roughly along the source-mod lines:

```text
logistics-core        # shared library: core.lib abstractions, energy/item/fluid APIs,
                      #   base materials (tin/bronze/dusts/gears), tools (wrench/probe).
                      #   Every other module depends on this; it ships nothing "fun" alone.

logistics-automation  # BuildCraft + Logistics Pipes + Thermal Expansion:
                      #   pipes & the logistics network, engines & cables, RF machines
                      #   (macerator, kiln, quarry). The current "main" content.

logistics-forestry    # Forestry: bees, trees, farms, electron tubes, the worktable.

logistics-transport   # Railcraft: rails, advanced carts, tanks, signals, bulk processing.
```

### Open question: split `logistics-automation` further?

`logistics-automation` is the biggest bucket and arguably three mods in a trench coat. A finer split could be:

```text
logistics-pipes    # transport + logistics network (the "glue")
logistics-power    # engines, cables, battery, energy distribution
logistics-machines # RF machines: macerator, kiln, quarry, future Thermal-style machines
```

**Trade-off:**
- *Finer split* → maximum player choice (pipes without machines, etc.), but more inter-module API surface to keep stable, and harder to "find the edges" so each stands alone (e.g. pipes are far more useful *with* power once operations cost energy — see the power-gated pipe work).
- *Coarser split* (single `logistics-automation`) → simpler edges, fewer cross-module contracts, but less granular choice.

**Provisional recommendation:** ship the coarse split first (`core` + `automation` + `forestry` + `transport`), and only carve `automation` apart if/when the internal `core.lib` seams prove clean enough that pipes/power/machines genuinely stand alone. Decide via Discussion before committing.

## Principles for keeping modules separable

- **`logistics-core` owns every cross-cutting contract.** Anything two modules both need (energy unit, item/fluid transfer, the network graph interfaces, shared materials) lives in core. Modules never depend on each other directly.
- **Each module degrades gracefully alone.** A module must be playable with only `logistics-core` present. Optional cross-module synergies (e.g. Forestry machines accepting Logistics pipes) are *detected*, not *required*.
- **Loader specifics stay in adapters.** The existing multiloader rule holds across modules: `common` is loader-agnostic; Fabric/NeoForge wiring lives in loader source sets.
- **Find the edges deliberately.** Before splitting, write down what API each side needs from the other; if that surface is large or unstable, the seam isn't ready.

## Relationship to the build today

No packaging change is required to start the roadmap — the domain structure already mirrors the target. The split becomes a *build/Gradle* concern (separate artifacts) once the content justifies it. Until then, treat the module names above as **labels for groups of domains**, and keep new code on the right side of the seam.
