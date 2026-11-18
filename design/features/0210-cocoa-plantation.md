# Cocoa Plantation

> **Status:** 🚧 In review — tend model ([ADR 0001](../../docs/adr/0001-tend-only-farms.md)) · **Phase:** 2 — Forestry · **Module:** `logistics-forestry` (`forestry` domain)
> **Source:** [`../mods/forestry.md`](../mods/forestry.md) (Farm (single-block)) · **Depends on:** [`0201-farm-framework.md`](0201-farm-framework.md) · **Maps to (roadmap):** Phase 2 — Farms

Tends the cocoa the player grows on jungle logs around it: picks ripe pods and replants a bean on the same log face. The player places the logs and the first beans; the farm never builds, breaks or replaces a log.

## Requirements

- **Reach:** the shared 7×7 Reach from [ADR 0001](../../docs/adr/0001-tend-only-farms.md), from one block below the farm to one above it. A pod is tended when it **or the log it hangs on** is in the Reach, so a pod on the outer face of an edge log, one block outside, counts too. Basic tier only.
- **Germlings:** cocoa beans.
- **Ground:** logs in `logistics:forestry/cocoa_plantation_ground` (`#minecraft:jungle_logs`). A pod hangs *beside* its log, so the Plot's ground is the log the pod faces, not the block under it.
- **Ripe:** a cocoa pod at its maximum age, hanging on a tended log. Unripe pods on a tended log are taken on as Plots before their first harvest.
- **Harvest:** break the pod; replant a bean facing the same log, from the pod's beans first, then from the inventory. Spare beans go to the 15-slot inventory; what does not fit drops at the pod.
- **Empty Plots:** a picked pod with no bean to replant leaves an Empty Plot, which waits for beans in the inventory. A Plot whose log is gone, or whose space is taken by anything but its pod, is forgotten.
- **Never:** places, breaks or repairs a log, or plants anywhere it has not harvested or found a pod.
- **Upkeep:** `rf_upkeep_cocoa_plantation`, 0.2 RF/t per Plot (see Decisions).
- **Costs:** a pick is a crop harvest (`rf_harvest_crop`) and a replant a plant (`rf_plant`). Nothing is tilled.
- **Recipe:** standard farm recipe with the **bronze valve**, jungle log and cocoa beans.

## Design sketch

- `CocoaTendLogic` on the shared `FarmBlockEntity`, `CocoaPlantationBlock` registered through `registerFarm`.
- Framework seam: `TendLogic#support(world, plot)` names the block a Plot grows on. It defaults to `plot.below()`, which keeps the Crop Farm and Arboretum unchanged; cocoa answers the log its pod faces, or for an Empty Plot the first tended log beside it. `TendFarmComponent` reads and repairs ground there instead of hard-coding the block below, and `TendLogic#plantState` (which already took the world and plot) faces the new pod towards that log.
- Framework seam: `TendLogic#attached(world, pos)` names neighbours of a Reach position whose plant belongs to it (none by default; for cocoa, the pods hanging on a tended log). When the scan visits a Reach position it also visits each neighbour just outside the Reach that is attached there or already a Plot, so an out-of-Reach pod is harvested, taken on, refilled and forgotten like any other Plot. Farms that attach nothing never have a Plot outside the Reach, so they are unchanged.

## Scope & non-goals

- **In:** the farm block and logic, picking and replanting cocoa on logs the player places.
- **Out:** placing or maintaining support logs, growing jungle trees (the [Arboretum](0203-arboretum.md) does that), other side-mounted plants.

## Decisions

- **The pod or its log must be in the Reach.** A log inside the perimeter reads as part of the plantation, so the pods on its outer face, one block outside the Reach, are picked and refilled too (as the Gourd Farm takes fruit outside the Reach off a stem inside it). A pod inside the Reach on a log outside it is tended as well, since the farm never touches the log. A pod outside the Reach on a log outside it is left alone. Out-of-Reach Plots pay upkeep and show in the HUD like any other.
- **An Empty Plot between two logs** is replanted against the first tended log beside it, north, east, south, then west. Each Plot remembers only its germling, not the face it was planted on.
- **Upkeep 0.2 RF/t per Plot.** The Crop Farm's 0.25 RF/t over its roughly 50-Plot field comes to about 13 RF/t: more than one Stirling engine, within one warm Fuel engine. A standard plantation holds 64 pods, so 64 × 0.2 = 12.8 RF/t lands on the same target.
- **No new action costs.** A pick is charged as `HARVEST_CROP` and a replant as `PLANT`; a pod is a crop block, and adding a cost would add a config key nobody needs to tune separately.
- **Bronze valve.** Tin and copper went to the Crop Farm and Arboretum; bronze is the next Basic-tier valve made from the same two metals. The recipe's farm-specific parts are a jungle log and two cocoa beans.
- **Body:** the concept's bare jungle log on a Humus bed with three pods, no foliage. The pergola layout stays an idea for later.

## Done when

- JUnit over `FakeFarmWorld`: a ripe pod is picked and replanted facing the same log; unripe pods become Plots; pods on other logs are left alone; logs are never touched; an Empty Plot is refilled from the inventory; a Plot whose log is removed is forgotten; upkeep is charged per Plot; a pod outside the Reach on a log inside it is picked, taken on, refilled and forgotten, while one whose log is outside too is left alone.
- GameTests on both loaders: ripe pods are picked and replanted while a hopper under the farm pulls the beans into a chest, logs untouched; Empty Plots are replanted from beans a hopper feeds in from above; a pod outside the Reach on a log inside it is picked and, once the player breaks it, refilled; nothing happens without power.
- Regression tests pin the Crop Farm and Arboretum ground to the block under the Plot, and keep a ripe crop just outside the Reach untouched.

## References

- Breakdown: [`../mods/forestry.md`](../mods/forestry.md) → Farms
- Related: [ADR 0001](../../docs/adr/0001-tend-only-farms.md), [0201 Farm Framework](0201-farm-framework.md), [0209 Berry Farm](0209-berry-farm.md)
