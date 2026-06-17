# Fluid transport design

Status: agreed direction, not yet implemented. Replaces the head/relaxation model with a
two-layer design: a passive cellular gravity layer (free) and an active frontier-solver
extractor (costs energy).

## Core principle

Pipes are **cellular buffers**, not one logical tank — a connected graph component is not a
connected body of fluid (an inverted-U holds two independent slugs). Behaviour splits along
the existing invariant *distribution is passive (free), extraction is active (costs energy)*.

All movement is computed from a **per-tick snapshot** and applied as deltas (see
[invariant](#invariant-layers-must-not-fight)).

## Passive layer (free, every tick)

Per-pipe gravity + spread. Priority is purely directional; neighbour *kind* never enters it:

1. **down** — greedy pour
2. **sideways** — spread

**Never up.** A pipe only accepts flow when it has space, so a full vertical column is
stable; when its bottom drains, the column above collapses down (siphon-break for free).

Transfer *amount* per target:
- **into a pipe:** equalize — move half the level difference (communicating vessels).
- **into a tank/handler:** greedy — give it all it has space for. (Equalizing never fills a
  tank.)

Equalize only ever runs pipe↔pipe at equal 250 mB capacity, so the by-amount
`FluidPipe.push` stays correct and "level across unequal capacities" never arises.

### Tanks: filled free, emptied for energy

- **Insertion is passive-only.** The extractor never targets a tank; passive down/sideways
  rules fill it.
- **Extraction is active-only.** Only the extractor empties a tank (draining it as a source
  handler). Passive never pulls from a tank.

Consequence: a tank fills only from its **sides or top**, never from directly below.
(Optional later exception: let an elevated tank gravity-drain straight down. Default no.)

### Junction split

The per-tick budget is **divided evenly across eligible outflow arms** (those that can
accept), not spent sequentially. **Down takes its greedy share first**; the sideways split
divides the remainder. Each arm applies its own rule within its share.

**Water-filling, multi-pass:** push the whole budget while any arm can still take it — split
the remaining budget evenly across not-yet-capped arms, apply each arm's cap (tank: free
space; pipe: half the snapshot level difference), repeat. Terminates in ≤(#arms) passes;
leftover budget stays in the junction. Caps come from the snapshot, so it's computed and
applied once. This replaces the current order-dependent sequential side-loop in
`FluidDistribution.tick`.

## Active layer (extractor, costs energy)

The only thing that lifts fluid. Solver/frontier based, not "pull into my buffer and
dribble":

- pull from the adjacent source (bounded by energy + transfer rate)
- advance a **frontier cursor** through the cached component graph, priming bottom-up with
  no gaps, **into pipe cells only — never tanks/handlers**
- stop when out of fluid, energy, or network space

Frontier tracking (not destination search) makes the inverted-U correct and gives
backpressure for free — when the frontier can't advance, extraction stops.

## Dropped: the `head` concept

Delete `FluidHead.java`, the `head` field, `computeHead`/`neighbourHead`, and
`FluidPipe.lift`; `FluidDistribution.tick` collapses to down→sides.

**Infinite head:** the only ceiling on lift is fluid availability, energy, and network
space. Deliberate balance choice — energy is the honest limiter (more intuitive). A single
powered extractor with steady supply fills an arbitrarily tall riser.

## Invariant: layers must not fight

With head gone, fluid the solver places in an elevated cell is not at equilibrium — gravity
wants it back. Stability depends on *every cell beneath the frontier being full*. So:

- The solver primes bottom-up; one gap mid-column collapses everything above it.
- Both layers share the **same capacity-bounded fill**, applied from a per-tick snapshot.
  Snapshot-then-apply is a **prerequisite**: it protects this invariant against same-tick
  iteration order and kills the existing read-while-mutating directional bias.

## Tuning knob (not structural)

Transfer rate vs. 250 mB segment capacity sets whether the frontier creeps (~1 cell/tick)
or rushes (many cells/tick).

## Reuse

Cache the component graph, invalidate on connection change, mirroring the item-pipe layer
(`NetworkRegistry`/`NetworkGraph`). The frontier is a small per-extractor cursor over it —
never a per-tick flood.

## Under consideration: momentum / "sloshing" (NOT baseline)

Level-equalize is diffusion: it settles into a stair-step ramp instead of sloshing. A
traveling/bouncing wave needs a second state variable — a velocity that persists and decays.

**Model — flux per edge.** Store a signed `flux` per pipe↔pipe edge alongside `level`. Each
tick from the snapshot:

1. `flux[e] += k * (level[i] − level[j])`   (pressure)
2. `flux[e] *= (1 − friction)`             (energy loss)
3. `level[i] −= flux[e]; level[j] += flux[e]`, clamped + symmetric (conserves fluid)

This deletes the baseline's "never raise a neighbour above the source" rule — that overshoot
*is* the slosh. Dead-end = reflecting (wave bounces); tank = absorbing (drains); solver
injection seeds flux for free.

**Caveats:** per-*edge* (per-pipe is ambiguous at junctions); CFL-unstable if `k` too high
(keep `friction > 0`, clamp); flux is transient (don't persist; waves die on reload, fine);
costs the "layers never fight" guarantee — keep it **horizontal-only** and watch for solver
resonance.

**Spectrum:** if the goal is only to kill the ugly stair-step, do it with zero new state
(handle the integer residual / over-relax toward the run average). The flux model is for
making fluid *feel alive*.