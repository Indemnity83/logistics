# Plantation

> **Status:** 🚧 In review — draft PR, waiting on a playtest · **Phase:** 2 — Forestry · **Module:** `logistics-forestry` (`forestry` domain)
> **Source:** [`../mods/forestry.md`](../mods/forestry.md) (Farm (single-block)) · **Depends on:** [`0201-farm-framework.md`](0201-farm-framework.md), [ADR 0001](../../docs/adr/0001-tend-only-farms.md) (tend-only farms) · **Maps to (roadmap):** Phase 2 — Farms

The stalk farm (Forestry's reeds and succulent farms in one block): it tends the **sugar cane, bamboo and cactus** the player plants within its 7×7 Reach, harvesting each stalk from the top down to its base and leaving the base to grow again. It absorbs the planned [Desert Farm](0213-desert-farm.md): under the tend model the player builds the field, water channels and cactus spacing included, so one block covers all three plants.

## Problem & goal

Sugar cane, bamboo and cactus all grow the same way, upward from a base that keeps regrowing, and players automate them with observer-and-piston contraptions that are ugly, noisy and hard to fit into a build. **Goal:** one block that sits in the player's own cane, bamboo or cactus field and keeps it cut, without ever reshaping the field.

## Requirements

### Functional
- **Reach and model:** the shared tend model from [ADR 0001](../../docs/adr/0001-tend-only-farms.md). The Reach runs 3 blocks out on every side, corners included, from Y−1 to Y+1. The farm tends what the player planted there, never clears or lays anything, and is Basic tier only.
- **Plants:** the block tag `logistics:forestry/plantation_stalks` (sugar cane, bamboo, bamboo shoot, cactus).
- **Base:** a stalk block standing on `logistics:forestry/plantation_ground` (`#minecraft:sand`, `#minecraft:dirt`, gravel). A stalk block standing on another stalk block is never a base.
- **Harvest:** every stalk block stacked on a base, broken from the top down, one block per action, plus one block from `logistics:forestry/plantation_toppings` crowning the stack. The base is never broken. A stalk is ripe as soon as anything stands on its base.
- **Above the Reach:** only the **base** must be inside the Reach. The stack above it is harvested however tall it is (bamboo reaches 16), up to 31 blocks above the base.
- **Cactus flower:** the only topping. It exists only on 26.x, so the tag lists it as `"required": false`, and the same file and code load on every branch. A flower is picked first, and a flower on a bare cactus is picked on its own, which also lets that cactus grow again.
- **Plots:** each base is a Plot. A bare base is taken on by the scan (`growing`), and a harvested one is kept. A bamboo shoot counts as planted bamboo. The upkeep and the status lamp work as they do for every other farm.
- **No replant after a harvest:** the base stays, so there is nothing to replant. If the base itself goes missing, the Plot becomes an Empty Plot and is replanted from the inventory like any other farm's. Bamboo goes back as a shoot.
- **Never:** builds or refills water channels, lays sand, repairs ground, or breaks a base. Sugar cane's water and cactus's spacing are the player's to provide.
- **Output:** the shared 15-slot inventory. When it is full, the farm keeps working and drops what does not fit at the base.
- **Recipe:** the standard farm recipe with the **copper valve**, sugar cane, bamboo and cactus around the Machine Core.

### Balance
- **Harvest:** `rf_harvest_crop` (100 RF) per block, shared with the Crop Farm; no new cost key.
- **Replant of a missing base:** `rf_plant` (50 RF).
- **Upkeep:** `rf_upkeep_plantation`, **0.375 RF/t per Plot**. That is sized to the Crop Farm's target, where a full field costs about 13 RF/t: more than one Stirling engine (10 RF/t peak), and within one warm Fuel engine (10–40). A 7×7 cane field needs water beside every base, so two channels leave about 35 bases, which comes to about 13 RF/t. A bamboo field needs no water and fits about 48 bases (18 RF/t). A cactus field needs a checkerboard and fits about 24 (9 RF/t).
- **Throughput:** vanilla growth sets it. Cane and cactus grow a block about every 18 minutes per base and bamboo about every 3.5 minutes, so harvest RF is small next to upkeep.

## Design sketch

- `forestry/farm/plantation/PlantationTendLogic` implements the `TendLogic` seam. `PlantationBlock` is a `FarmMachineBlock` on the shared `FarmBlockEntity`, registered through `LogisticsForestry.BLOCK.registerFarm` like the Crop Farm and Arboretum.
- A harvest is `new Harvest(base, stackTopDown, germling)`: its plot is the base, and its blocks are everything above it.
- **Framework change:** `TendLogic.Harvest#keepsPlot()` is true when a harvest's blocks leave out its plot. `TendFarmComponent.startHarvest` reserves the replant's RF only for a harvest that does not keep its plot. The rest of the framework already did the right thing. At the end of the harvest, the replant fails harmlessly on the occupied base, and the Plot is remembered as planted, the same path as a refused break. The [Gourd Farm](0212-gourd-farm.md) (fruit off a stem that stays) uses the same seam.
- `isPlanted` is overridden so a bamboo shoot counts as bamboo; `plantState` plants bamboo as a shoot.
- Tags, not classes, decide what is tended, so modded stalks can be added by tag.

## Scope & non-goals

- **In:** the Plantation block, logic, tags, the glass-case model with the stalk-field body (cane and a bamboo stalk either side of a water channel, plus a flowering cactus), recipe, lang, JEI info, GameTests on both loaders.
- **Out:** placing water or sand; kelp, seagrass and other underwater plants; chorus fruit; vines; tiers above Basic.

## Decisions

- **Desert Farm merged in.** Cactus differs from cane only in what the player has to build (sand and spacing instead of water). Under the tend model the farm never builds the field, so a separate block would add nothing. `0213` stays as a stub pointing here.
- **Harvest at any height.** A stalk is ripe once anything stands on its base. Waiting for full height gains nothing, because vanilla stalks only grow at the top, and it would mean guessing bamboo's random 12–16 cap.
- **Reach covers bases only.** A base must be in the Y−1..Y+1 Reach; the rest of its stack is harvested wherever it reaches. Bamboo grows far past the Reach, and a taller Reach would scan mostly air.
- **The base is the Plot and the planted plant.** That gives stalks upkeep, Empty Plots and the status lamp with no special case in the framework beyond `keepsPlot`.
- **A missing base is replanted from the inventory.** This is the standard Empty Plot rule. It is not a replant after harvest: a harvest never breaks the base.
- **Ground tag, no repair.** `plantation_ground` stops a Plot from outliving its ground, so turning sand to stone forgets the Plot. Vanilla's own survival rules (water for cane, free sides for cactus) still decide whether a stalk can stand.
- **Shared harvest cost.** A stalk block reuses `rf_harvest_crop` rather than adding a cost key.
- **Copper valve** in the recipe.

## Open questions

- **Cactus overflow.** A full farm drops overflow at the base, and a cactus destroys items it touches, so cactus overflow is lost. Keeping the output pulled avoids it. Should cactus overflow drop at the farm instead?
- **Upkeep at 0.375 RF/t per Plot.** Bamboo fields run at the top of the band (18 RF/t) and cactus fields at the bottom (9 RF/t). Is one rate for all three right?
- **A dry Empty Plot.** A cane base lost because its water was removed is replanted only when the cane can survive there; until then the Plot is an Empty Plot that keeps costing upkeep.

## Done when

- JUnit (`PlantationTendTest`, over `FakeFarmWorld`) covers: cane harvested top-down with its base kept as a planted Plot; bamboo harvested far above the Reach; a topping picked first, and alone on a bare base; a bare base and a bamboo shoot taken on as Plots, with upkeep paid; a stack counted as one Plot; stalks on other ground left alone; a missing base replanted from the inventory; a harvest started with only its break's RF; a Plot forgotten when its ground goes.
- GameTests on both loaders, with real blocks and a hopper-to-chest output: sugar cane (on sand by water), bamboo (on dirt, two blocks above the Reach) and cactus are harvested into the chest, and every base and its ground stay. A full farm drops the cane at its base. A broken bamboo base is replanted as a shoot from the inventory.

## References

- Breakdown: [`../mods/forestry.md`](../mods/forestry.md) → Farms
- Model: [ADR 0001](../../docs/adr/0001-tend-only-farms.md); glossary terms Reach, Plot and Empty Plot in [`../../GLOSSARY.md`](../../GLOSSARY.md)
- Code precedents: `forestry/farm/crop/CropTendLogic`, `forestry/farm/arboretum/ArboretumTendLogic`
- Related: [0201 Farm Framework](0201-farm-framework.md), [0212 Gourd Farm](0212-gourd-farm.md), [0213 Desert Farm](0213-desert-farm.md) (merged here)
