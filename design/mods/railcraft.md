# Railcraft

*Rails, advanced minecarts, signals, and the steam/steel processing chain — inter-base transport and bulk logistics. Entirely greenfield and the most clearly separable module. This is Phase 3.*

**Source era:** 1.7.10–1.12.2 (Railcraft).
**Logistics module:** `logistics-transport` (new) — the strongest standalone candidate.
**Phase:** 3.

See [`../principles.md`](../principles.md) for the table legend. Railcraft is *enormous*; the calls below **consolidate the huge track/cart taxonomy** into a coherent set and reconsider its many multiblocks.

## Tracks

| Feature | What it did (1.7.10–1.12.2) | Decision | Modern take / balance notes | Status | Maps to |
|---|---|---|---|---|---|
| Basic/reinforced/wooden/high-speed track | Tiered rails (durability, speed) | Modernize | Keep a small tier set: basic → reinforced → high-speed | — | Phase 3 — tracks |
| Switch / junction / wye / turnout | Direction control | Port | Essential for any rail network | — | Phase 3 — tracks |
| Powered / booster track | Accelerate carts (replaces vanilla powered rail at scale) | Port | Core movement | — | Phase 3 — tracks |
| Detector tracks | Emit redstone on cart conditions | Port | Automation glue | — | Phase 3 — tracks |
| Control tracks (boarding/holding/locking/one-way/buffer/launcher) | Fine cart control + fun (launcher) | Modernize | Consolidate the dozen+ control tracks into a smaller, configurable set | — | Phase 3 — tracks |
| Routing track + ticket | Route carts by destination | Port | Pairs naturally with the logistics theme | — | Phase 3 — routing |
| Electric track | Power electric locomotives | Skip | Out of scope for now — a long way out if ever (maintainer + contributor call, Jun 2026). Revisit only if electric locomotives are ever pursued | ❌ | — |

## Minecarts

| Feature | What it did | Decision | Modern take / balance notes | Status | Maps to |
|---|---|---|---|---|---|
| Locomotive (steam / electric / creative) | Self-powered engine that pulls carts | Port | The headline: automated trains. Start with steam; **electric is out of scope** (see Tracks → Electric track) | — | Phase 3 — locomotives |
| Tank cart | Mobile fluid transport | Port | Depends on the fluid layer | — | Phase 3 — carts |
| Chest / work carts (energy/anchor) | Mobile inventory + utility carts | Port | Mobile storage transport; pairs with loaders | — | Phase 3 — carts |
| Loaders / unloaders (item & fluid) | Auto-load/unload carts at stations | Port | Connects rails to the pipe/fluid networks | — | Phase 3 — loaders |
| Fun carts (TNT / gift / undercutter) | Novelty/utility carts | Modernize | Keep a couple (TNT cart); drop the rest | — | Phase 3 — carts |
| Tunnel Bore | Cart-mounted automatic tunneling | Modernize | Cool but complex; possible alt to/with the Laser Quarry | — | Later |
| Cart dispenser | Auto-place carts onto track | Port | Station automation | — | Phase 3 — loaders |

## Signals

| Feature | What it did | Decision | Modern take / balance notes | Status | Maps to |
|---|---|---|---|---|---|
| Block / distant / token signals + controllers/receivers | Automated train spacing & control | Modernize | High value for automated networks, but historically fiddly — **simplify** to an approachable signal set | — | Phase 3 — signals |
| Signal tuner | Pair signals | Port | Needed if signals ship | — | Phase 3 — signals |

## Steam, steel & bulk processing

| Feature | What it did | Decision | Modern take / balance notes | Status | Maps to |
|---|---|---|---|---|---|
| Coke Oven | Multiblock: coal → coke + creosote oil | Modernize | Coke + creosote feed steel + fuel/treated-wood. **Reconsider the multiblock** — prefer single block or tileable (scale optional) | — | Phase 3 — steam chain |
| Blast Furnace | Multiblock: iron + coke → steel | Modernize | Steel is the module's key material; **single-block machine** preferred over the 34-block structure | — | Phase 3 — steel |
| Steam Boiler (solid/liquid, HP/LP) | Multiblock: fuel + water → steam | TBD | Steam as a power tier, but the big multiblock fights the no-multiblock stance. **Leaning: lean on Create for steam via a compat layer** rather than build our own boiler; else single-block tier or cut. See [RFC 0003](../rfcs/0003-railcraft-multiblocks.md) | — | Phase 3 — steam power |
| Steam Turbine / steam engines | Steam → RF/work | Modernize | Unify with the engine line (steam-tier engine) | — | `power` / engines |
| Iron / Steel Tank | Multiblock bulk fluid storage | Modernize | The classic "Iron Tanks" role. *Scale is the feature* — a valid multiblock exception, or tileable tank blocks | — | Phase 3 — tanks (fluids) |
| Rock Crusher | Multiblock ore processing | Skip | Covered by the Macerator | ❌ | [`thermal-expansion.md`](thermal-expansion.md) |
| Rolling Machine | Cart/rail crafting | Modernize | Fold rail crafting into normal/vanilla-Crafter recipes | — | — |
| Water Tank (multiblock) | Collect water for boilers | Modernize | Only if boilers need it; prefer simple | — | — |

## Materials & utility

| Feature | What it did | Decision | Modern take / balance notes | Status | Maps to |
|---|---|---|---|---|---|
| Steel | Blast-furnace alloy; rails/tools | Port | Core `logistics-transport` material | — | Phase 3 — steel |
| Creosote oil | Wood preservative (treated wood) + fuel | Port | From the coke oven; treated wood for tracks/ties | — | Phase 3 — steam chain |
| Coke / charcoal | Smelting fuel | Port | From the coke oven | — | Phase 3 — steam chain |
| World Anchor (chunkloading) | Keep cart/quarry chunks loaded | TBD | Useful for quarries/farms but sensitive (server perf); needs a Discussion | — | Later |
| Engraving / emblems / firestone / misc | Cosmetic & niche extras | Skip | Out of scope | ❌ | — |

> TODO: the multiblock calls (coke oven, blast furnace, boiler, tank) are the biggest design tension in this module vs. the no-multiblock stance — decide per-block (single vs. tileable vs. true multiblock) via a Discussion before scheduling. See [RFC 0003 — Railcraft Multiblocks & Steam](../rfcs/0003-railcraft-multiblocks.md).
> TODO: confirm scope of the steam power chain — whether steam is a real `power` tier (boiler→turbine→RF), a **Create compat layer** (the current maintainer leaning), or simplified away; it couples to the engine-line unification in [`buildcraft.md`](buildcraft.md) / [`thermal-expansion.md`](thermal-expansion.md). See [RFC 0003 — Railcraft Multiblocks & Steam](../rfcs/0003-railcraft-multiblocks.md).
> Decided (Jun 2026): **electric locomotives/tracks are out of scope** — Railcraft transport is steam-only for v1.
