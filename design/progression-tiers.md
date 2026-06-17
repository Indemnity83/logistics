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

Each rank has **two canonical labels**: the **material** (visual identity — what a thing is crafted from / looks like) and the **tier name** (the adjective — the durable name for the tier *concept*, and an option for item names). The **function** is the fixed flavor that label carries across every line — pick the material/tier when its function fits the line.

| Rank | Material | Tier name | Function (durable) | Phase |
|---|---|---|---|---|
| 0 | Wood / Stone | **Crude** | makeshift, pre-power | early |
| 1 | **Copper** | **Basic** | entry-level, cheap workhorse | early |
| 2 | **Iron** | **Sturdy** | structural, load-bearing | early |
| 3 | **Bronze** | **Industrial** | early machine-alloy | mid |
| 4 | **Gold** | **Conductive** | energy / signal throughput | mid |
| 5 | **Diamond** | **Precision** | accuracy, high quality | mid |
| 6 | **Amethyst** | **Resonant** | resonance / buffering / wireless-charge | mid |
| 7 | **Netherite** | **Infernal** | heat, durability | nether |
| 8 | **Ender** | **Dimensional** | teleport / cross-dimension / wireless transport | end |
| 9 | **Echo Shard** | **Deep** | sculk / sensing / data · ultimate | deep dark |

## Naming

Item display names can derive from **either** label. Default to the **material** name (Copper Cable, Bronze Gear — honors material identity); use the **tier adjective** when it reads better or the material is clunky (a "Resonant" amethyst tier, a "Dimensional" ender tier). Stay consistent within a line.

## Coverage — where every material sits

The spine lists only **progression-bearing** tiers. The system is *total*: every other material — vanilla or ours — falls into exactly one of these, so nothing is ambiguous about "what tier is this?"

- **Alloy feedstock (no rank):** **Tin**, **Nickel** — exist only to craft alloys (Bronze, Invar); never a tier on their own. (Macerator-byproduct nickel feeds Invar — see [`features/0105-alloy-smelter.md`](features/0105-alloy-smelter.md).)
- **Our alloys (occupy a rank):** **Bronze** = rank 3 (Industrial). **Invar** = a structural/precision alloy sitting around rank 3–4 (between Industrial and Conductive); its job is machine frames / precision components, so it participates as a *component material*, not a separate visual tier. New alloys take the rank of their role, keep their own name, and respect the ordering.
- **Function accents (no rank):** carry a *function*, not a progression step — use them for the system they evoke, not as tiers: **Redstone** → signal/logic (reserve for gates/circuits; *not* power cables), **Lapis** → enchant, **Emerald** → trade, **Blaze / Quartz / Glowstone** → nether components, **Obsidian** → containment/blast.
- **Out of scope:** purely decorative/world materials define no tier.

> Rule of thumb: a material is a **spine rank** (it gates progression), an **alloy** (occupies a rank), a **feedstock** (makes an alloy), an **accent** (a function, not a step), or **out of scope**.

## Per-line mapping

Each line picks its subset in canonical order. *(Illustrative subsets — refine per line as it's built.)* By tier name, the cable line reads **Basic → Conductive → Resonant → Dimensional**.

| Line | Flavor | Canonical subset |
|---|---|---|
| **Cables** | conductivity | Copper · Gold · **Amethyst** · Ender |
| **Gears** | mechanical | Wood · Stone · Copper · Iron · Bronze · Gold · Diamond · Netherite *(Tin is not a tier — see note)* |
| **Batteries** ([0107](features/0107-tiered-batteries.md)) | storage | Copper · Gold · Ender |
| **Cores / Valves** | electronics | Copper · Bronze · Gold · Amethyst |
| **Machine frames** (future) | structural | Iron · Bronze · Diamond · Netherite |
| **Chassis MkI–V** | slot count | *intentional exception — numeric, not a material tier* |

## Retrofit plan (implementation)

Per the maintainer call, **existing lines conform too** (not just new work). These are implementation tasks for later PRs on the `mc/*` branches — this doc records the decision; the code changes are separate:

- **Cables** — add an **Amethyst** tier → `Copper 30 / Gold 60 / Amethyst 120 / Ender 240` RF/t (keeps the ×2 ladder; Amethyst takes the old Ender rate and Ender rises for late-game headroom — see [`features/0107-tiered-batteries.md`](features/0107-tiered-batteries.md)). Numbers tunable against the RF curve. Touches `power/cable/CableTier` + an `amethyst_cable` block/model/recipe.
- **Gears** — the canonical ladder is Wood · Stone · Copper · Iron · Bronze · Gold · Diamond · Netherite; **Tin is not a tier.** The already-shipped `tin_gear` is **grandfathered legacy** — we are *not* doing a breaking removal/migration (maintainer + contributor call, Jun 2026: tin's only role is enabling Bronze, and a removal isn't worth the world-break). It simply stays as-is and the gear line gains no new tin content; treat tin everywhere else as a Bronze feedstock, not a rank.
- **Cores / Valves** — currently Copper · Bronze; extend toward Copper · Bronze · Gold · Amethyst as the electronics system grows (couples to the deferred programmable-behavior work — [`rfcs/0001-programmable-behavior.md`](rfcs/0001-programmable-behavior.md)). No forced change now.

## References

- [`principles.md`](principles.md) — material-based identity, "respect modern progression tiers" (this doc is the concrete realization)
- [`vision.md`](vision.md) — material-based identity / layered progression principles
- Applied in: [`features/0107-tiered-batteries.md`](features/0107-tiered-batteries.md) (cables/batteries), [`features/0105-alloy-smelter.md`](features/0105-alloy-smelter.md) (Bronze/Invar ranks)
