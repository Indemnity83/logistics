# Wiki audit — mc/26.3 @ `mc26.3-v0.8.9`

Audit of `wiki/` against the mod data/lang/Java at tag `mc26.3-v0.8.9`.

**Baseline:** the wiki was synced at **v0.8.8** on 2026-09-09 (commit `368fcb5c3`). This pass covers
the 27 player-facing commits between `mc26.2-v0.8.8` and `mc26.3-v0.8.9`, plus the surrounding
content on every page touched.

**Branch note:** `feat(common): run on Minecraft 26.3` exists only on `mc/26.3`. `mc/1.21.1` is
missing four fixes (quarry arm tick rate, stale machine textures, NeoForge wrapped-inventory slot
capacity, client menu blur), none of which change documented behaviour — no *"Since Minecraft …"*
markers were needed.

**Not audited on purpose:** three `fix(ci)` commits reach the changelog but are CI-only. All
`refactor` / `test` / `build` / `chore` / `docs` work is hidden from the changelog.

---

## A. Factually wrong — ✅ DONE

### A1. `Laser Quarry` — "any tool" was wrong, twice over

The page said *"can be broken with any tool, though a pickaxe is fastest."* The block carries
`requiresCorrectToolForDrops()`, so that claim was **already wrong at v0.8.8** — a pickaxe was
required. v0.8.9 adds it to `needs_stone_tool`, raising the bar to a stone pickaxe.

Fixed the prose and `{{Breaking|tier=}}` from `none` to `stone`.

### A2. `Power Junction` — was unharvestable, now mineable

The real content of `fix(common): stop pickaxes destroying blocks they should harvest`. The block
had `requiresCorrectToolForDrops()` but was in **no** `mineable/*` tag, so nothing dropped it
whatever you used. v0.8.9 adds it to both `mineable/pickaxe` and `needs_stone_tool`.

The page had no `=== Breaking ===` section at all; added one (`tier=stone`) and a history line.

### A3. `Creative Engine` — same class of error, pre-existing

Found by grepping for the A1 phrasing. Says *"any tool"*; the block requires a pickaxe of any tier.
Its `{{Breaking|tier=none}}` was already correct — only the prose was wrong. Fixed.

### A4. `Sawdust` — "nothing currently consumes it"

Made false by `feat(automation): let sawdust burn as furnace fuel`. Rewritten, infobox `type`
changed from `Item (Material)` to `Item (Fuel)`. See B2.

### A5. `Redstone Engine` — axe only

Said *"requires an axe to drop."* v0.8.9 adds it to `mineable/pickaxe` as well, so either tool
works. The `{{Breaking}}` template takes a single tool, so it keeps `tool=axe` (the faster of the
two) and the prose carries both.

---

## B. New player-facing content — ✅ DONE

### B1. Map colours — `Getting Started`

`feat(common): show ores, machines and oil lakes on maps`. The rule, from `BlockMapColorTest`:
every **solid** block is drawn; see-through blocks are not. Excluded are everything under `pipe/`
**except the Power Junction**, everything ending `_cable`, plus `core/marker`,
`core/quartz_crystal` and `automation/laser_quarry_frame`.

Written once, as a short `==On the map==` section. **Placement is the weak point of this pass:**
`Getting Started`, `main` and `Materials` are all link hubs with no prose sections, and there is no
world/exploration page. It reads acceptably under Core Concepts but a future *Exploration* or
*World generation* page would be a better home.

### B2. `Sawdust` — furnace fuel

100 ticks, read from `FurnaceFuels.BURN_TIMES` (coal is 1600 for reference). That is half a smelt,
so two pieces smelt one item. Framed as recovering value from the Sawmill's byproduct rather than a
fuel worth making deliberately.

### B3. `Glass Tank` — comparator output

Added a `=== Comparator output ===` section. The reading covers the **whole column**, not the block
the comparator touches — which matters because columns fill bottom-up, so a per-block reading would
report a full 15 for a reservoir a quarter full. That reasoning is lifted from the gametest.

### B4. `Laser Quarry` — frame maintenance

Two features in one squash commit (`c171edaec`): the quarry now repairs gaps punched in its
standing frame, and clears blocks that intrude into the frame band. Both pause mining and appear as
their own phases in the readout, so a quarry that looks stalled may be repairing itself.

---

## C. History lines only — ✅ DONE

A `{{History|v0.8.9|…}}` line and no prose change, on: `Crucible` (gauge scales to the server's
tank size), `Stirling Engine` (charge gauge scales to the buffer), `Glass Tank` (amounts above
32,767 mB), `Sequential Fabricator` (malformed byproduct chance reported as a recipe error),
`Oil Sand` / `Oil Shale` / `Oil Red Sand` (seeps no longer erase nearby deposits),
`Provider Module` (reserved slot protected by index), and the pages covered in A and B.

Twelve pages carry a v0.8.9 line.

---

## D. Recorded here, not on the wiki

* **`Modules.txt` already described a v0.8.9 change as if it shipped in v0.8.8.** The
  nearest-sink tie-break sentence matches `SinkPriority`'s **post**-fix javadoc. The v0.8.8 tag was
  cut 2026-09-08, `43e7beec9` landed after it, and the wiki line was written 2026-09-09 during the
  v0.8.8 pass. Players on v0.8.8 read a rule the mod did not follow; v0.8.9 makes it true. The
  prose is now correct and was left alone — `Modules.txt` and `Routing.txt` have no History
  sections, so there was nowhere to attribute it without inventing one.

  **Checked whether this was systemic: it is not.** 17 of the 27 v0.8.9 changes landed on or before
  the day of the v0.8.8 pass. Sawdust fuel, tank comparators, quarry frame maintenance, Crucible
  gauge scaling and map colours are all absent from the wiki, so only routing was documented early.
  Two apparent matches were false positives — the quarry hit is a v0.8.8 line about frame *removal*,
  the Crucible hit is generic gauge prose plus a v0.8.7 line.

  **Cause and fix:** the v0.8.8 pass read mainline rather than the tag. `claude.md` now says to read
  at the release tag.

* **Cable fixes** (`stop cables drawing a wrong connection arm`, `power the new network right after
  a pipe split`) — `Cables`, `Copper Cable`, `Gold Cable` and `Ender Cable` have no History
  sections, and neither fix contradicts anything the pages claim. No edit.

* **`fix(ui): show the server's config values on multiplayer clients`** — the `{{Config}}` values
  the wiki documents are the defaults and remain accurate; the fix is to the in-game display. No
  edit.

* **Pipe and cable harvest tags.** 39 blocks were added to `mineable/pickaxe`, which looks alarming
  in the diff, but only the two in A2/A5 changed what drops them. The rest never required a tool —
  a pickaxe is merely faster now. `Glass Tank`'s *"can be broken with any tool"* is still true and
  was left alone.

* **`Installation`** — deliberately says *"any version Logistics has a release for"* rather than
  listing versions, so Minecraft 26.3 support needs no edit.

---

## E. Housekeeping — ✅ DONE

`claude.md` named `logistics-mc-26.2` as the primary worktree. Updated to 26.3 throughout, and the
Minecraft version list now includes 26.3.

---

## Verified clean

* All 311 pages were in sync with the live wiki before editing (`--pull --dry-run`), so nothing
  written here clobbers a contributor edit.
* Every `{{History}}` block still has exactly one `head=1` and one `foot=1`.
* `Glass Tank`, `Transposer`, `Fluid Pump` and the ore pages were checked against the v0.8.9 tag
  data and need no correction beyond what is listed above.
