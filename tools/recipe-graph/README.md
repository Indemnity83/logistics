# Recipe graph tool

Extracts the mod's recipe data (all 8 custom machine types + the vanilla
crafting/smelting recipes it ships for its own items) into checked-in JSON
tables, then layers a balance-query CLI and a Graphviz chain-diagram
generator on top.

Lives here (in the docs worktree, branch `docs`) rather than in a mod
worktree, alongside the rest of the reporting/wiki tooling. It reads from a
sibling **mod** worktree over recipe/asset data rather than from local
source — same cross-worktree convention `render_blocks.py`'s `--assets`
already uses, and it borrows `render_blocks.py` directly for isometric
machine icons (see below). The mod's recipe JSON stays the single source of
truth; `extract.py` re-derives `data/*.json` from it rather than
hand-duplicating chains.

See `../../logistics-mc-26.2/design/progression-tiers.md` for the material
tier ladder this tool's balance output should be read against (it defines
tier identity, not a numeric RF-cost curve — see `balance.py outliers`'s
docstring).

## Setup

```bash
brew install graphviz   # only needed for diagram.py; extract.py/balance.py are pure stdlib
```

`diagram.py`'s icon fallback also wants Pillow (already a soft dependency of
this `tools/` directory's other icon scripts) to crop animated fluid
textures to their first frame; it degrades gracefully (uncropped strip)
without it.

## Usage

```bash
# 1. Regenerate data/items.json + data/recipes.json from a mod worktree's
#    recipe JSON (default: sibling ../logistics-mc-26.2). Re-run whenever
#    recipes change; safe to commit the diff.
python3 tools/recipe-graph/extract.py -v [--repo-root PATH]

# 2. Balance queries
python3 tools/recipe-graph/balance.py cost logistics:core/fuel_oil
python3 tools/recipe-graph/balance.py chain logistics:core/fuel_oil --rate 60
python3 tools/recipe-graph/balance.py outliers --machine crucible

# 3. Chain diagrams (--mod-root defaults to sibling ../logistics-mc-26.2, for icons/models).
#    --out's extension picks the renderer: .html is real HTML/CSS (recommended --
#    dot only computes layout); .svg/.png let dot render its own cards.
python3 tools/recipe-graph/diagram.py chain logistics:core/tar \
    --direction descendants --out tar_chain.html

python3 tools/recipe-graph/diagram.py chain logistics:core/fuel_oil \
    --direction ancestors --depth 3 --rate 60 \
    --exclude minecraft:bucket --out fuel_oil_chain.html
```

Item/fluid ids match the mod's own `logistics:<domain>/<name>` convention
(e.g. `logistics:core/crude_oil`); browse `data/items.json` for the full
list, or the mod's own lang file
(`../../logistics-mc-26.2/common/src/main/resources/assets/logistics/lang/en_us.json`).

## Card design

Each item in a chain diagram is one composite card (not a separate item box
+ machine diamond): a machine banner on top (icon + name), the item's own
icon in the middle, and a footer with RF cost / yield / byproduct chance.
Machine icons are rendered on demand via the sibling `render_blocks.py`
(isometric renders straight from the mod's block models) and cached under
`data/icon_cache/` (gitignored, regenerable — not checked in).

**`.html` output is the recommended mode.** `dot` is still used, but only to
compute layout (`-Tplain`: node positions + edge splines) — the actual
card/edge visuals are real HTML/CSS/SVG, not Graphviz's own (fairly
primitive) HTML-like node labels. That gets real fonts and kerning, a hover
affordance, and full CSS control; icons are inlined as base64 data URIs so
the page is a single self-contained file (portable — attach it, open it
directly, or publish it as a Claude Artifact). `.svg`/`.png` still work
(dot renders the whole thing itself) for a quick static snapshot.

An item with more than one producing recipe in the current query picks the
cheapest as its displayed face (excluding self-referential recipes like the
Transposer's bucket-fill/empty pair) and notes "+N more recipes" in the
footer; edges are drawn only for that chosen recipe's inputs, so what's on
the card always matches what feeds it. Items only reachable via one of
those un-shown alternate recipes are dropped from the render entirely
(rather than left as disconnected orphan cards) via a post-hoc connectivity
pass from the query's root item. A raw/base resource (no producing recipe
in scope) renders as a plain icon + name card, no banner/footer.

**Variant groups** (`data/variant_groups.json`) merge raw/leaf items that
play the same role in a chain onto one card with a side-by-side icon strip
-- e.g. Oil Sand / Oil Red Sand / Oil Shale are interchangeable macerator
inputs, so seeing three near-duplicate cards adds nothing. This is a
curated table, not an auto-detected heuristic: the mod's recipes for
"equivalent" variants aren't always byproduct-identical (Oil Shale's
macerator recipe drops Flint instead of Tar), so grouping is a deliberate
simplification call per group, not a fact derivable purely from the data.
Add entries as `{"group_id": {"display_name": "...", "members": ["id",
...]}}`; only items with no producing recipe in the current query's scope
are eligible (merging items that carry their own recipe/footer would need
to reconcile differing stats too). Pass `--no-group` to see every card
individually.

## Known limitations

- **Hub-item fan-out.** Items with very wide fan-in/out (`minecraft:bucket`,
  reached by every fluid's Transposer fill/empty recipe pair; common
  vanilla items like sticks) make unbounded `ancestors`/`descendants`
  queries balloon quickly. Use `--depth` and/or `--exclude` to keep
  diagrams legible — see the `fuel_oil` example above.
- **External tags are unresolvable.** `#tag` ingredients are expanded from
  tag JSON shipped *in the mod worktree*. Tags supplied by the base game
  itself (`minecraft:logs`, `minecraft:wool`, ...) or by a dependency mod
  (`c:sands`, `c:ores/iron`, ...) aren't present as files there and are
  recorded in `items.json` as `{"kind": "tag", "external": true, "members":
  null}` — traversal can't expand through them, and `balance.py cost`
  leaves such an input's contribution unpriced.
- **Byproduct-only items are cost-approximated.** An item only ever
  produced as a `chance < 1` byproduct (never a primary `result`) is priced
  by dividing the recipe's run cost by that chance — an upper bound that
  ignores the primary output's own value. See `RecipeGraph.cheapest_chain`'s
  docstring in `graph.py`.
- **No numeric balance target yet.** `design/progression-tiers.md` defines
  a material tier ladder, not a global RF-cost curve (the only hard number
  it states is the cable RF/t table), so `balance.py outliers` is a
  statistical/internal-consistency check against same-machine,
  similar-depth recipes — not a check against a design target.
- **Java-only constants need manual re-sync.** Per-machine RF/tick
  defaults and the Fuel Engine burn table live as small hardcoded tables at
  the top of `extract.py` (each commented with its Java source of truth)
  because they're config/code constants, not data files. Re-sync by hand if
  those Java values change.
- **Icon coverage is partial.** Flat item textures cover most items; the
  isometric `render_blocks.py` fallback fills in most of the rest (machine
  blocks). Pipe/cable items still commonly have neither (their models need
  more than the simple `block/<domain>/<name>` guess `diagram.py` tries) —
  those cards render with no icon, name only. Vanilla (`minecraft:`) items
  never get an icon here; `vanilla_icons.py` could fill that gap if wired in.

## Deferred (not built yet)

- A hand-laid "poster" reference sheet (grouped by material family, in the
  style of a Satisfactory cheat-sheet) — Graphviz auto-layout won't
  reproduce that grid; would need its own templated generator.
- Publishing generated diagrams to the wiki (`tools/upload_to_fandom.py`)
  — out of scope until asked.
