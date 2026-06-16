# Fluid transport design — solver variant

Status: alternative to `transport-design.md`, for comparison. Same goal, different passive
layer. Here the passive layer is a **body-level solver** ("water finds its level") instead
of local directional rules, because local rules provably cannot do passive communicating-
vessels equalization (a normal U-tube). See [why local fails](#why-the-cellular-rules-cant-do-this).

The active layer (energy-gated lift), the snapshot discipline, and the tank in/out
asymmetry carry over unchanged from the cellular design.

## Core principle: fluid bodies, not graph components

A **fluid body** is a connected region of cells that water settles into when poured in:
starting from the filled cells, the surface rises and floods the lowest reachable cells
first — **including dry ones** — up to a single equilibrium height (priority-flood /
"trapping rain water"). A cell is reachable if a graph path to it runs through cells *at or
below the rising surface*.

The only thing that blocks is a dry cell **above the surface**: water can't climb over a dry
hump to reach the other side, so that severs the region into separate bodies. A dry cell
*below* the surface is no barrier — it just fills. ("Dry" alone never blocks; "dry **and
above the surface**" does.)

This is the "treat it as a tank" idea, corrected: the unit is the **body** (splits at air
gaps), not the graph component (which would wrongly merge an inverted-U's two legs).

### The four reality checks

| Shape | Connecting path | Result |
|---|---|---|
| **Normal U (∪)** | bottom run **wet** | one body spans both arms → **equalizes height (free)** |
| **Inverted U (∩)** | hump **dry** | body can't cross → **two independent pools** |
| **Tower** | — | fluid pools at the bottom; empty cells above are unreachable → **no free climb** |
| **Siphon** | pump wets the hump | the two pools **merge** into one body → flows over |

Prime and break are just body **merge** and **split**.

## Passive layer (free, every tick)

Each body settles toward its equilibrium level, spreading only through wet cells. This
*includes upward flow* — the far arm of a U rises for free, because that is equilibrium, not
lifting.

Per tick, from the snapshot:

1. **Identify bodies** — flood through wet cells (cached; re-flood only where fluid entered
   an empty cell or a cell emptied).
2. **Solve each body's level** — distribute the body's volume across its reachable cells by
   elevation, lowest first, to a common surface (priority-flood).
3. **Move toward the solved target, rate-limited** — don't teleport to equilibrium; move
   each cell toward its target by at most the transfer rate, so fluid visibly flows. (This
   is also where optional momentum/slosh would later live — overshoot the target.)

"Reachable" in step 2 means reachable through cells at or below the rising surface — an
empty cell below the surface, connected via wet cells, joins the body; a dry cell above the
surface does not (that's the inverted-U hump).

## Active layer (extractor, costs energy)

**Same contract as the cellular design, and actually simpler here.** In the cellular design
the frontier cursor had to prime bottom-up by hand because the passive layer couldn't
redistribute. With a passive solver that re-levels for free, the extractor only has to:

- pull from the adjacent source handler (bounded by energy + transfer rate)
- **raise fluid above the body's natural surface** into a dry cell (priming a hump, or
  pushing a column past where it would sit) — the newly-wet cell joins the body and the
  passive solver settles the rest. No frontier bookkeeping.
- stop when out of fluid, energy, or space

The crisp boundary is unchanged: **up to the body's own surface is free (passive); above the
surface costs energy (active).** A U-tube equalizes for free; a siphon over a dry hump costs
energy.

## Tanks: filled free, emptied for energy (same as cellular)

A tank/handler participates in the solve as an **absorbing boundary**, not a body member:

- The solver may push fluid **into** a tank (greedy — tanks are always thirsty) wherever a
  wet body cell is adjacent: sideways/down from any wet pipe, and **upward (filling a tank
  from below) once the pipe under it is full** — the same "fluid can go up" condition the
  body uses, so a pressurised/extractor-fed column fills a tank above it. Intake is paced at
  the transfer rate (capacity-capped per tick), not instant.
- The solver **never pulls fluid out** of a tank to satisfy a body's level. Emptying a tank
  stays active (extractor draining it as a source). Extractors never push into handlers, so
  they can't shove fluid back into their own source.

So a tank fills passively / empties actively, and can't be drained for free by being the
high point of a body.

## Invariant: snapshot-then-apply

Same prerequisite as the cellular design. The level solve reads a per-tick snapshot and
applies deltas, so it's order-independent and the active/passive layers compose without the
read-while-mutating bias. Body identification also reads the snapshot.

## Why the cellular rules can't do this

Communicating-vessels equalization is **non-local**. In a static U with the left arm tall
and the right empty, every submerged cell reads "full," so the bottom-right cell and the
empty cell above it have equal local hydraulic head — no gradient, no flow. What should
drive the right arm up is the *height of the left column*, several cells away. Local
adjacent-cell rules can't transmit that, so they can't distinguish a U (should rise) from a
tower (should not). A solver (or a propagated pressure field) is required. The deleted
`head` field wouldn't have helped: with no pump, head was 0 everywhere, so a passive U-tube
never equalized under it either.

## Cost & caching

- **Body identification:** O(N) flood, but incremental/cached — only re-flood a body when a
  cell it touches changes wet/dry. Most ticks touch nothing.
- **Level solve:** O(cells) per changed body with elevation bucketing (integer y).
- **Rate-limited application:** bounded by transfer rate, so big bodies settle over several
  ticks (also what makes it look like flow, not teleport).

For typical builds (tens–hundreds of cells) this is cheap; the worst case is a single huge
connected body churning every tick, which caching of unchanged bodies avoids.

## Comparison vs the cellular design (`transport-design.md`)

| Aspect | Cellular (local rules) | Solver (this doc) |
|---|---|---|
| Passive U-tube equalization | ✗ impossible (needs a pump) | ✓ free |
| Inverted-U independence | ✓ | ✓ (bodies split at dry hump) |
| Tower / no free pump | ✓ | ✓ |
| Per-tick cost | O(1) per cell, trivial | O(changed cells) + flood; cacheable |
| Conceptual model | several directional special-cases | one principle (bodies find their level) |
| Momentum/slosh add-on | per-edge flux | overshoot the solved target |
| Implementation risk | low | flood/level solver + cache invalidation |
| Main weakness | unintuitive (water doesn't find its level) | more CPU, solver complexity |

Open question to resolve before choosing: is the per-tick solver cost acceptable for the
largest realistic networks, given caching — or does the cellular model's O(1) simplicity win
despite losing free U-tube equalization?
