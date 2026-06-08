# BuildCraft

*The foundation of classic tech: pipes, engines, the quarry, and gate-based automation. Logistics already covers the pipe and engine core and a modernized quarry; the open frontier is fluids, combustion-tier power, and gate automation.*

**Source era:** 1.7.10–1.12.2 (BuildCraft).
**Logistics module:** `logistics-automation` (pipe + power + automation domains).
**Phase:** 0 (done parts) / 1 (gaps).

See [`../principles.md`](../principles.md) for the table legend. Pipe transport lineage is detailed in [`logistics-pipes.md`](logistics-pipes.md); this file focuses on engines, fluids, quarry/automation, and gates.

## Pipes

| Feature | What it did (1.7.10–1.12.2) | Decision | Modern take / balance notes | Status | Maps to |
|---|---|---|---|---|---|
| Item transport pipes (wood/cobble/stone/iron/gold/diamond/obsidian/etc.) | Move/sort/extract items by material | Modernize | Covered by the Logistics transport + smart pipe set with vanilla-material identity | ✅ Done | `pipe` / transport + smart pipes |
| Diamond pipe (sorting) | Per-side item sorting | Port | Item Filter Pipe | ✅ Done | `pipe` / Item Filter Pipe |
| Obsidian pipe (world pickup) | Suck up dropped items/entities | Port | Worth adding — vacuum behavior; balance range | — | Phase 1 — pipes |
| Cobblestone/lapis/quartz flavor pipes | Routing-speed/color variants | Modernize | Covered/absorbed by current material tiers + marking fluid | ✅ Done | `pipe` / pipes + marking |
| Kinesis (power) pipes | Transport engine power (MJ) | Modernize | Replaced by RF **cables** (copper/gold/ender) | ✅ Done | `power` / cables |
| Fluid (waterproof) pipes | Transport liquids | Port | Needs a fluid-transport layer (platform fluid API) | — | Phase 1 — fluids |

## Engines & power

| Feature | What it did | Decision | Modern take / balance notes | Status | Maps to |
|---|---|---|---|---|---|
| Redstone Engine | Weak power from redstone; safe | Port | Implemented; never overheats | ✅ Done | `power` / Redstone Engine |
| Stirling Engine | Burns solid fuel; medium power; heat | Port | Implemented with heat stages | ✅ Done | `power` / Stirling Engine |
| Combustion Engine | Burns liquid fuel + needs water cooling; high power; explodes if mismanaged | Modernize | The big power-tier gap. Needs fluid fuel + coolant; keep the "manage or it blows" tension | — | Phase 1 — engines |
| Engine heat / overheat | Visual heat stages, safe shutdown vs explosion | Port | Implemented (COLD→OVERHEAT) | ✅ Done | `power` / engine heat |
| MJ power unit | BuildCraft's energy currency | Modernize | Standardized on **RF** via `core.lib` energy abstraction | ✅ Done | `power` / energy API |

## Quarry & fluid automation

| Feature | What it did | Decision | Modern take / balance notes | Status | Maps to |
|---|---|---|---|---|---|
| Quarry | Frame-bounded automatic mining | Modernize | **Laser Quarry**: marker-bounded, energy-scaled, no monolithic frame build | ✅ Done | `automation` / Laser Quarry |
| Landmarks / markers | Define work areas | Modernize | Marker blocks (solo + connected bounds) | ✅ Done | `automation` / Marker |
| Pump | Pump fluids from world into pipes/tanks | Port | Needs the fluid layer; classic oil/water/lava pumping | — | Phase 1 — fluids |
| Oil & Refinery & Fuel | Oil lakes → refine to fuel for combustion engines | Port | Bring it over as-is: oil world-gen → Refinery → fuel. Couples to the Combustion Engine; Forestry biofuel is a *parallel* fuel, not a replacement | — | Phase 1 — fuels |
| Filler | Auto-fill/clear areas with patterns | Skip | Construction automation; tedious, overlaps Create/Schematica niche | ❌ | — |
| Builder / Architect / Blueprints / Library | Save & auto-build structures | Skip | Heavy; modern Schematica-likes cover this; out of scope | ❌ | — |

## Gates, wiring & automation logic

| Feature | What it did | Decision | Modern take / balance notes | Status | Maps to |
|---|---|---|---|---|---|
| Gates (basic/iron/gold/diamond) | Programmable trigger→action logic on pipes/machines | TBD | High value but complex. Possible modern take: a compact "logic gate" pluggable reading machine/pipe state. Big design effort — needs a Discussion | — | Phase 1 — automation (RFC) |
| Pipe wiring (red/blue/green/yellow) | Carry gate signals between pipes | TBD | Only meaningful if gates are ported; could lean on vanilla redstone instead | — | Phase 1 — automation (RFC) |
| Autarchic gate | Self-pulsing redstone engine | Modernize | Could fold into engine/extractor config rather than a separate gate | — | — |
| Assembly Table + Laser | Laser-powered crafting of BC chipsets/gates | TBD | Revisit as we approach the recipes it produced — parts already changed, but the balance is still uncertain. Decide Port vs. Modernize closer in | — | Phase 1 — revisit |
| Facades | Cosmetic covers over pipes | Port | Strong feature — camouflage pipes/cables with block appearances via a data component. Scheduled **post-1.0** | — | Post-1.0 |
| Robots & stations | Programmable mobile workers | Skip | Very complex; out of scope | ❌ | — |
| Wrench | Configure/rotate machines | Port | Implemented | ✅ Done | `core` / Wrench |

> TODO: confirm the exact combustion-engine coolant/explosion behavior we want to mirror (water-cooled vs. simplified), and decide oil sourcing (world-gen vs. crafted) — these two couple together and gate the fluids epic.
> TODO: the gates question is the biggest open design call from BuildCraft — flag for an Ideas/Polls Discussion before scheduling. See [RFC 0001 — Programmable Behavior](../rfcs/0001-programmable-behavior.md).
