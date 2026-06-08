# Decision Principles

This file defines **how we decide** what to bring over from each source mod, and the shared table every [`mods/`](mods/) breakdown uses. The goal is consistent, defensible calls that add up to a balanced, classic-feeling whole.

## The three decisions

Every feature from a source mod gets exactly one decision:

- **Port** — bring it over essentially as-is. The mechanic still makes sense in modern Minecraft and fits the balance. (May still get a fresh texture/recipe.)
- **Modernize** — keep the *intent*, change the *form* to fit modern vanilla. Use this when the original clashes with current Minecraft content, conventions, or balance.
- **Skip** — don't bring it. Either modern vanilla/another mod already covers it, it breaks balance, it's out of scope, or it's not worth the complexity (often: monolithic multiblocks).

When a decision is genuinely unsettled, mark the row's decision as **TBD** and open a Discussion (Ideas/Polls) — don't silently default it.

## Modernization rules

When **Modernize** is chosen, prefer these conventions so the result feels native to modern Minecraft:

- **Use vanilla materials that now exist.** Copper is a first-class progression metal now — use it (we already do for cables and copper pipe). Don't invent a parallel to something vanilla added.
- **Match modern vanilla naming/items.** Trees drop *resin* now, not generic "sap"; honey/honeycomb exist; amethyst, echo shards, etc. are fair game. Align with these instead of reintroducing legacy item names.
- **Reuse vanilla systems where they fit.** The vanilla **Crafter** block, smelting recipes (Kiln reuses these), tags, and data components are leverage — prefer them over bespoke reimplementations.
- **Respect modern progression tiers.** Stone → copper → iron → gold → diamond → netherite is the vanilla ladder; map tiers onto it rather than onto legacy (e.g. tin/bronze stay as flavor, not as a mandatory gate unless it earns it). The concrete, mod-wide ladder + unlock-point phases (early/mid/nether/end/deep-dark) + the per-line "pick a subset in order" convention live in [`progression-tiers.md`](progression-tiers.md) — every tiered line must draw from it.
- **Keep RF as the energy unit** via the existing loader-agnostic energy abstraction (`core.lib`), interoperable with Team Reborn Energy (Fabric) and NeoForge energy.

## Balance philosophy

- **Preserve the 1.7.10-era curve.** Ore doubling, engine throughput, machine speeds, and automation gating should *feel* like the classic pack's pace — meaningful early grind, satisfying mid-game automation, abstract endgame logistics.
- **No free lunches early.** Powerful conveniences (network logistics, autocrafting, bulk mining) sit behind earlier mechanical/energy investment.
- **Each tier should obsolete the grind, not the gameplay.** Later tools remove tedium (faster extraction, larger buffers), not the systems players learned.

## The multiblock stance

Default: **avoid monolithic multiblock structures.** They're tedious to build, fiddly to debug, and clash with the "simple placement, visible connections" principle.

Exceptions are allowed when a multiblock genuinely earns it (e.g. a Railcraft-style tank or boiler where *scale itself is the feature*). When porting something that was a multiblock:

- Prefer a **single block** + optional satellites/markers (as the Laser Quarry already does with marker blocks).
- If scale is the point, prefer **modular/tileable** blocks over a rigid required schematic.
- Document the call in the breakdown's notes.

## The decision-table template

Every `mods/*.md` breakdown uses this table. Copy it verbatim:

```markdown
| Feature | What it did (1.7.10–1.12.2) | Decision | Modern take / balance notes | Status | Maps to |
|---|---|---|---|---|---|
```

### Column legend

- **Feature** — the concrete block/item/mechanic from the source mod.
- **What it did** — one-line description of the classic behavior (era 1.7.10–1.12.2).
- **Decision** — `Port` · `Modernize` · `Skip` · `TBD`.
- **Modern take / balance notes** — how we'd realize it (if Port/Modernize) or why not (if Skip); balance considerations.
- **Status** — current implementation state:
  - ✅ **Done** — implemented and in the build.
  - 🚧 **Planned** — accepted, scheduled (has/will have a Project issue).
  - **—** Not started — accepted in principle, not yet scheduled.
  - ❌ **Won't port** — decided Skip.
- **Maps to** — the codebase location (e.g. `power` / engines) or the roadmap epic it belongs to. For ✅ Done rows, this should point at a real implemented block/item/module.

### Conventions

- Keep one feature per row; split umbrella features into sub-rows if their decisions differ.
- A ✅ Done row must be verifiable against the actual codebase (`common/src/main/java/com/logistics/`).
- Flag uncertain historical specifics with a blockquote: `> TODO: confirm <thing>`.
