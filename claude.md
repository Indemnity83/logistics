# Logistics Wiki (documentation)

## Overview
This is the **documentation worktree** for the Logistics Minecraft mod. It holds the source for the
mod's user-facing wiki, published to **Fandom** at <https://logistics.fandom.com/>.

The wiki is authored as MediaWiki **wikitext** files under `wiki/` and pushed to Fandom over the
MediaWiki API with `tools/upload_to_fandom.py`. The old self-hosted Zensical/MkDocs static site has
been **retired** — GitHub Pages now serves only a static redirect (`redirect/`, deployed by
`.github/workflows/docs.yml`) pointing at the Fandom wiki. There is no Markdown site or `docs/`
content anymore; ignore any lingering references to Zensical, `zensical.toml`, or a domain-foldered
`docs/` tree.

## Relationship to the mod
- **This worktree**: branch `docs` — wiki source only (`wiki/`, `tools/`, `redirect/`, `.github/`). No mod code.
- **Mod worktrees**: `../logistics-mc-26.2/` (primary/newest), plus `../logistics-mc-26.1`, `../logistics-mc-1.21.11`, `../logistics-mc-1.21.1`. These are the **source of truth** for recipes, IDs, lang strings, worldgen, and mechanics — read them (data JSON + `assets/logistics/lang/en_us.json` + Java) rather than guessing. New content lands on `mc/26.2` first; when documenting it, read and render against `../logistics-mc-26.2`.

## Repository layout (docs branch)
```
wiki/
  <Page Title>.txt          # one wikitext file per wiki page (~200 content pages)
  Template_<Name>.txt       # → Template:<Name>
  Template_<Name>.styles.txt# → Template:<Name>/styles.css   (TemplateStyles CSS)
  Module_<Name>.txt         # → Module:<Name>                (Scribunto Lua)
  main.txt                  # → the Main Page ("Logistics Wiki")
  media/                    # icons + animations uploaded as File:<name>
tools/                      # the Python toolchain (stdlib + Pillow; no venv needed)
redirect/                   # static GitHub Pages redirect to Fandom (index.html, 404.html)
.env                        # FANDOM_SITE / FANDOM_USER / FANDOM_PASSWORD (bot password) + Modrinth/CurseForge tokens
```

Filename → wiki-title mapping (handled by the upload tool): spaces are literal in filenames;
`Template_X.txt` → `Template:X`, `Template_X.styles.txt` → `Template:X/styles.css`,
`Module_X.txt` → `Module:X`, `main.txt` → the Main Page. Everything else → a page of that title.

## Toolchain (`tools/`)
All are plain Python 3 (stdlib; the icon tools also use Pillow). See `tools/README_fandom_upload.md`.

- **`upload_to_fandom.py`** — push/pull pages and media over the MediaWiki API. Idempotent: pages
  skip when unchanged, media skips when the on-wiki SHA1 matches.
  - `--pull [--dry-run]` — download live wikitext into `wiki/*.txt`. **Run `--pull --dry-run`
    before editing/pushing** to catch contributor edits made on the live wiki (this wiki has other
    editors); reconcile/commit those first so a push never clobbers them.
  - `--pages` — upload pages (templates/modules first, then Main, then content).
  - `--media --used-only` — upload only icons the pages reference (fast). Plain `--media` uploads all of `media/`.
  - `--only <stem…>` (restrict to page file stems), `--force` (re-send despite SHA/dupe), `--dry-run`.
  - Credentials come from `.env` (or `--site`/env). The reference scanner recognizes the recipe
    templates and `[[File:…]]` / infobox `image=`/`image2=` refs, so gifs and `Bucket of …` icons
    upload under `--used-only`.
- **`render_blocks.py`** — 3D isometric **block** icons from the mod's block/item models. Single
  model: `--model block/automation/crucible --out "wiki/media/Grid Crucible.png"`; `--batch` renders
  all; `--rot rx,ry,rz` overrides the default GUI rotation. Point `--assets` at a mod worktree's
  `assets/logistics` (default is 26.1 — pass `../logistics-mc-26.2/.../assets/logistics` for new content).
- **`upscale_icons.py`** — flat **item** sprite icons (nearest-neighbor upscale; PIL fallback for
  palette/grayscale PNGs). `NAME_OVERRIDES` renames, `SKIP_ITEMS` excludes (e.g. fluid buckets).
- **`vanilla_icons.py`** — vanilla Minecraft ingredient icons, only the referenced-but-missing ones,
  from a Minecraft client jar's `assets/minecraft` (`--assets`).
- **`fluid_icons.py`** — per custom fluid, emits three assets: `Grid <Fluid>.png` (static swatch,
  recipe-list icon), `Fluid <Fluid>.gif` (animated hero for the infobox), `Bucket of <Fluid>.png`
  (the filled-bucket item). Renders against 26.2 fluid textures.
- **`gen_item_links.py`** — regenerates `wiki/Module_ItemLink.txt` from the mod's `en_us.json` (the
  set of mod item/block/fluid display names). **Rerun when items are added.**

## Icons & media
- Recipe/list icons are `File:Grid <Display Name>.png`; `{{Grid|Name}}` and `{{Grid Cycle|A;B}}`
  look them up (graceful text fallback if missing). Vanilla ingredient icons are generated too, so
  recipe grids show real sprites.
- Fluids: animated `Fluid <Fluid>.gif` in the infobox `image`; bucket `Bucket of <Fluid>.png` in
  `image2`; the Crucible recipe **output** slot shows the animated gif (never the bucket).
- Regenerate against the worktree that has the content (26.2 for the newest), write into
  `wiki/media/`, then `--media --used-only`.
- Fandom serves uploads as WebP — a CDN fetch returning `image/webp` is normal, not corruption. If a
  just-uploaded icon renders as a missing-file redlink that a purge/null-edit won't clear, force a
  new file revision (re-encode the PNG so bytes differ, then re-upload).

## Page structure & templates
Every recipe uses a **template — never a hand-built wikitable.** The families:
- **Infoboxes** (`Module:Infobox`): `{{Item}}`, `{{Block}}` — fields like `image`/`image2`/
  `imagesize`/`caption`/`type`/`hardness`/`blastresistance`/`added`/`id`.
- **Recipes**: `{{Crafting}}` (grid), `{{Grinding}}` (Macerator), `{{Milling}}` (Sawmill),
  `{{Smelting}}`, `{{Crucible}}` (item→fluid, mB), `{{Alloy Smelter}}` (2-input→output+byproduct).
  Each is a wrapper `Template` + a `Module` + a `{{Grid <X> Table}}` widget + a `.styles.css`.
- **Data/meta**: `{{ID}}` (namespaced id + translation key — copy the real key from lang, don't
  infer), `{{History}}`, `{{Breaking}}` (mining-time table), `{{About}}`/`{{Hatnote}}`,
  `{{Disambig}}`, `{{LootChestItem}}` (chest-loot table via `Module:LootChest`).
- **Linking**: `{{Grid}}`/`Module:Cycle` route each item's link through **`Module:ItemLink`** — mod
  items → their local page, vanilla items → the Minecraft Wiki. `{{Mcw|Page|text}}` links vanilla
  items in prose (uses Fandom's built-in `w:c:minecraft:` interwiki; needs no wiki config). Keep
  `Module:ItemLink` in sync via `gen_item_links.py`.

A typical item/block page: infobox → intro sentence (with links) → `== Obtaining ==`
(Breaking/Crafting/Grinding/…) → `== Usage ==` → `== Data values ==` (`{{ID}}`) → `== History ==`
→ `== See also ==` → `[[Category:…]]`.

## Wiki writing conventions (established, follow exactly)
- **One page per item/block/concept**, heavily cross-linked; concise wiki-style (facts, not tutorials).
- **Body states CURRENT behavior.** "Moved from / renamed / now / no longer / in vX" belongs **only**
  in the `{{History}}` section.
- **All recipes use the recipe templates**, never wikitables. Don't restate a recipe in prose
  ("Requires/Yields X + Y") — the grid already shows it; only add what the grid can't.
- `{{History}}` "Added" entries lead with the item's **32px icon**, inline: `[[File:Grid X.png|32px]] Added '''X'''.`
- **Overview / disambiguation pages get NO `== History ==`** (only concrete item/block/machine pages do).
- Link vanilla items to the Minecraft Wiki (`{{Mcw}}` in prose; automatic in recipe grids); link mod
  items and concepts internally.
- Naming: the melting machine is **"Crucible"**, never "Magma Crucible" (matches the shipped lang).
- **Fandom caps expensive parser functions at 100/page.** `{{#ifexist:}}` counts; a big recipe page
  is already near the cap. Never add a per-item `#ifexist` — use a table lookup (that's why
  `Module:ItemLink` exists).

## Multi-version
The mod ships for Minecraft 1.21.1, 1.21.11, 26.1, and 26.2 on Fabric & NeoForge. Document the
**current behavior** and keep version numbers out of prose where possible — the Installation page is
written version-agnostically and points at the download pages (Modrinth/CurseForge) as the source of
truth for supported versions. Note version-specific differences only where they matter to a player.

## Workflow
1. `--pull --dry-run` to check for contributor drift; reconcile/commit any live edits first.
2. Edit `wiki/*.txt`. Regenerate/refresh icons if new items were added
   (`render_blocks`/`upscale_icons`/`vanilla_icons`/`fluid_icons` against `../logistics-mc-26.2`,
   plus `gen_item_links.py`).
3. `--pull --dry-run` again (still clean), then `--pages` and `--media --used-only`.
4. Optionally verify live via the API (`action=parse`) — check for Script errors, unexpanded
   `{{…}}`, and missing-file redlinks.
5. Commit on `docs`, push only when asked.

Pre-commit / pre-push hooks run `./gradlew` (Spotless) and **fail in this docs-only worktree** —
bypass with `git commit --no-verify` / `git push --no-verify`.

## Notes for Claude
- Source material is the sibling mod worktrees (primarily `../logistics-mc-26.2`); read the data/lang, don't guess recipes or IDs.
- Wiki-style, not guide-style: factual, concise, one page per thing, link everything.
- When adding a machine with a new recipe shape, add a matching recipe-template family (wrapper +
  `Module` + `{{Grid <X> Table}}` widget + styles); keep new machine modules self-contained rather
  than editing the shared `Module:Grinding` (large blast radius).
