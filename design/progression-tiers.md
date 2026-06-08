# Progression Tiers

The mod's **canonical tier ladder** — one ordered vocabulary of vanilla-anchored materials that *every* tiered line draws from, so "the gold tier" means the same progression position whether it's a cable, a gear, or a battery.

This expands two ideas already in [`principles.md`](principles.md): **material-based identity** (each tier reads from its vanilla material at a glance) and **respect modern progression tiers** (map onto the vanilla ladder, not legacy metals). Before this doc, the lines diverged — gears ran a 9-step ladder (incl. `tin`), cables ran copper/gold/ender, cores/valves ran copper/bronze. This is the shared spine.

## The rule

- There is **one canonical ladder** (below). Every tiered line picks a **subset of it, in order**, using these materials and names.
- A line uses the **flavor-appropriate** tiers — cables use conductors, gears use structural metals — but never invents a parallel ordering or off-ladder names.
- **Not every line needs many tiers.** Two is fine; the point is consistency, not maximalism.

## Progression phases (unlock points)

Materials map to five game phases. Ordering is by **intended tier/theme**, not strictly vanilla acquisition difficulty (e.g. ender pearls are overworld-obtainable but are treated as the "End / dimensional" tier; echo shards don't require the End but are the deepest, ultimate tier).

| Phase | Unlock gate | Headline tiers | Feel |
|---|---|---|---|
| **Early game** | surface / shallow overworld mining | Copper, Iron *(Wood/Stone pre-tier)* | first machines, mechanical, basic RF |
| **Mid game** | deep mining · alloy smelting · geodes | Bronze, Gold, Diamond, Amethyst | tech ramp, ore processing, resonance/precision |
| **Nether** | nether portal | Netherite *(Blaze/Quartz accents)* | heat, durable endgame craft |
| **End** | dimensional access | Ender | wireless / dimensional logistics |
| **Deep dark** | ancient city | Echo Shard | ultimate, sculk/data |

## The canonical material ladder

| Rank | Material | Phase | Flavor / function | Acquisition |
|---|---|---|---|---|
| 0 | Wood / Stone | early | crude, pre-power | start |
| 1 | **Copper** | early | basic metal · entry conductor | early |
| 2 | **Iron** | early | structural staple | early-mid |
| 3 | **Bronze** | mid | early tech-alloy (our signature) | mid (tin + smelt) |
| 4 | **Gold** | mid | superior conductor · premium mid | mid |
| 5 | **Diamond** | mid | precision · high quality | mid-late |
| 6 | **Amethyst** | mid | resonant · crystal | mid-late (geodes) |
| 7 | **Netherite** | nether | durable endgame *(the "nether" slot)* | late |
| 8 | **Ender** | end | dimensional · wireless | late |
| 9 | **Echo Shard** | deep dark | deep · sculk/data · ultimate | endgame |

**Function accents (not core ranks).** Some vanilla materials carry a *function* rather than a progression rank — keep them out of the spine and use them for the system they evoke:
- **Redstone** → signal / active (reserve for gates/logic; *not* power cables, which would muddy the redstone-as-signal identity).
- **Lapis** → enchant · **Emerald** → trade · **Blaze/Quartz** → nether heat/components.

**Alloys occupy ranks.** Our crafted alloys slot into the ladder by crafting difficulty: **Bronze** is rank 3; **Invar** (see [`features/0104-alloy-smelter.md`](features/0104-alloy-smelter.md)) is a structural variant around rank 3–4, used for frames/precision components. They keep their own names but respect the ordering.

## Per-line mapping

Each line picks its subset in canonical order. *(Illustrative subsets — refine per line as it's built.)*

| Line | Flavor | Canonical subset |
|---|---|---|
| **Cables** | conductivity | Copper · Gold · **Amethyst** · Ender |
| **Gears** | mechanical | Wood · Stone · Copper · Iron · Bronze · Gold · Diamond · Netherite *(drop Tin)* |
| **Batteries** ([0106](features/0106-tiered-batteries.md)) | storage | Copper · Gold · Ender |
| **Cores / Valves** | electronics | Copper · Bronze · Gold · Amethyst |
| **Machine frames** (future) | structural | Iron · Bronze · Diamond · Netherite |
| **Chassis MkI–V** | slot count | *intentional exception — numeric, not a material tier* |

## Retrofit plan (implementation)

Per the maintainer call, **existing lines conform too** (not just new work). These are implementation tasks for later PRs on the `mc/*` branches — this doc records the decision; the code changes are separate:

- **Cables** — add an **Amethyst** tier → `Copper 30 / Gold 60 / Amethyst 120 / Ender 240` RF/t (keeps the ×2 ladder; Amethyst takes the old Ender rate and Ender rises for late-game headroom — see [`features/0106-tiered-batteries.md`](features/0106-tiered-batteries.md)). Numbers tunable against the RF curve. Touches `power/cable/CableTier` + an `amethyst_cable` block/model/recipe.
- **Gears** — remove `tin_gear`; reorder to canonical Wood · Stone · Copper · Iron · Bronze · Gold · Diamond · Netherite. *Migration:* `tin_gear` removal is breaking for existing worlds — acceptable pre-1.0, or remap `tin_gear → bronze_gear` via a data-fixer/recipe.
- **Cores / Valves** — currently Copper · Bronze; extend toward Copper · Bronze · Gold · Amethyst as the electronics system grows (couples to the deferred programmable-behavior work — [`rfcs/0001-programmable-behavior.md`](rfcs/0001-programmable-behavior.md)). No forced change now.

## References

- [`principles.md`](principles.md) — material-based identity, "respect modern progression tiers" (this doc is the concrete realization)
- [`vision.md`](vision.md) — material-based identity / layered progression principles
- Applied in: [`features/0106-tiered-batteries.md`](features/0106-tiered-batteries.md) (cables/batteries), [`features/0104-alloy-smelter.md`](features/0104-alloy-smelter.md) (Bronze/Invar ranks)
