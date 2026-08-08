#!/usr/bin/env python3
"""Render a recipe-chain diagram from the extracted recipe graph, via Graphviz.

Run `extract.py` first to (re)generate `data/items.json` / `data/recipes.json`.
Requires the Graphviz `dot` binary on PATH (`brew install graphviz` on macOS;
`apt install graphviz` on Linux) -- this script shells out to it rather than
depending on the `graphviz` PyPI package, so the only new dependency is the
one system binary.

Each item becomes one composite "card" node (a Graphviz HTML-like label):
a machine banner on top (icon + name), the item's own icon in the middle,
and a footer with RF cost / yield / byproduct chance. An item with more than
one producing recipe in the current query picks the cheapest as its
displayed face and notes "+N more recipes" in the footer -- edges are drawn
only for that recipe's inputs, so what's on the card always matches what
feeds it. A raw/base resource (no producing recipe in scope) renders as a
plain icon + name, no banner/footer. Byproduct edges are drawn dashed.

Item icons come from the mod's own flat 16x16 textures where `items.json`
found one; for ids with no flat texture (mostly machine blocks -- see
`extract.py`'s README caveat) this falls back to an on-demand isometric
render via the sibling `render_blocks.py`, cached under `data/icon_cache/`.

Usage:
    python3 tools/recipe-graph/diagram.py chain logistics:core/tar \\
        --direction descendants --out tar_chain.svg

    python3 tools/recipe-graph/diagram.py chain logistics:core/fuel_oil \\
        --direction ancestors --rate 60 --out fuel_oil_chain.svg

Pure stdlib; shells out to the system `dot` binary and (for the icon
fallback) to `render_blocks.py`.
"""

import argparse
import shutil
import subprocess
import sys
from html import escape as html_escape
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from graph import RecipeGraph  # noqa: E402

TOOLS_DIR = Path(__file__).resolve().parents[1]
RENDER_BLOCKS = TOOLS_DIR / "render_blocks.py"
DEFAULT_MOD_ROOT = TOOLS_DIR.parents[1] / "logistics-mc-26.2"
ICON_CACHE_DIR = Path(__file__).resolve().parent / "data" / "icon_cache"

# machine key -> (block id for the isometric-icon fallback, display name).
# Machines with no mod block model (plain crafting/vanilla furnace paths,
# and the synthetic Fuel Engine/Reaction sink edges) get a text-only banner.
MACHINE_INFO = {
    "macerator": ("logistics:automation/macerator", "Macerator"),
    "sawmill": ("logistics:automation/sawmill", "Sawmill"),
    "alloy_smelter": ("logistics:automation/alloy_smelter", "Alloy Smelter"),
    "crucible": ("logistics:automation/crucible", "Crucible"),
    "refinery": ("logistics:automation/refinery", "Refinery"),
    "fabricator": ("logistics:automation/sequential_fabricator", "Fabricator"),
    "transposer": ("logistics:automation/transposer", "Transposer"),
    "kiln": ("logistics:automation/kiln", "Kiln"),
    "reaction": (None, "Reaction Engine"),
    "crafting_table": (None, "Crafting"),
    "blast_furnace": (None, "Blast Furnace"),
    "fuel_engine": (None, "Fuel Engine"),
}


def item_label(g, item_id):
    item = g.item(item_id)
    return item["display_name"] if item else item_id


def render_isometric_icon(block_id, assets_root):
    """Render (and cache) an isometric icon for a mod block via render_blocks.py.

    Returns a path to the cached PNG, or None if there's no model for this
    id or rendering failed (e.g. render_blocks.py isn't available here).
    """
    if not block_id or not RENDER_BLOCKS.exists():
        return None
    namespace, path = block_id.split(":", 1)
    if namespace != "logistics":
        return None
    model_path = f"block/{path}"
    model_file = assets_root / "models" / f"{model_path}.json"
    if not model_file.exists():
        return None

    ICON_CACHE_DIR.mkdir(parents=True, exist_ok=True)
    cache_file = ICON_CACHE_DIR / f"{path.replace('/', '_')}.png"
    if cache_file.exists():
        return cache_file

    result = subprocess.run(
        [sys.executable, str(RENDER_BLOCKS), "--assets", str(assets_root), "--model", model_path, "--out", str(cache_file), "--size", "128"],
        capture_output=True,
        text=True,
    )
    return cache_file if result.returncode == 0 and cache_file.exists() else None


def first_frame(path):
    """Crop an animated fluid-still texture (a tall NxN*frames strip) to its first frame.

    Falls back to the untouched path if Pillow isn't available or the image
    isn't actually a strip (height <= width).
    """
    try:
        from PIL import Image
    except ImportError:
        return path
    try:
        with Image.open(path) as im:
            if im.height <= im.width:
                return path
            ICON_CACHE_DIR.mkdir(parents=True, exist_ok=True)
            cache_file = ICON_CACHE_DIR / f"frame0_{path.stem}.png"
            if not cache_file.exists():
                im.crop((0, 0, im.width, im.width)).convert("RGBA").save(cache_file)
            return cache_file
    except OSError:
        return path


def item_icon(g, item_id, assets_root):
    item = g.item(item_id)
    if not item:
        return None
    if item.get("icon_path"):
        path = assets_root.parent / item["icon_path"]
        if path.exists():
            return first_frame(path) if item["kind"] == "fluid" else path
    return render_isometric_icon(item_id, assets_root)


def machine_icon(machine, assets_root):
    block_id, _ = MACHINE_INFO.get(machine, (None, machine))
    return render_isometric_icon(block_id, assets_root)


def format_amount(entry):
    if entry["kind"] == "fluid":
        return f"{entry['amount']}mB"
    count = entry.get("count", 1)
    return "" if count == 1 else f"x{count}"


def choose_primary_recipe(g, item_id, candidate_recipe_ids):
    """Among this item's producing recipes in scope, pick one deterministically to be the card's face."""
    candidates = []
    for rid in candidate_recipe_ids:
        recipe = g.recipe(rid)
        out_entry = next((e for e in recipe["outputs"] if e["id"] == item_id), None)
        is_byproduct = recipe["byproduct"] and recipe["byproduct"]["id"] == item_id
        if out_entry is None and not is_byproduct:
            continue
        # Skip a recipe whose own inputs include this same item id -- the
        # Transposer's bucket-fill/empty pair shares one id between a fluid
        # and its bucket item (see extract.py's `also_item_form`), which
        # would otherwise look like a free/self-producing "cheapest" recipe.
        if any(e["id"] == item_id for e in recipe["inputs"] if e["kind"] != "tag"):
            continue
        rf = recipe["energy_rf"] or recipe["rf_output"] or 0
        candidates.append((rf, rid, recipe, out_entry, is_byproduct))
    if not candidates:
        return None, None, None, len(candidates)
    candidates.sort(key=lambda c: (c[0], c[1]))
    rf, rid, recipe, out_entry, is_byproduct = candidates[0]
    return recipe, out_entry, is_byproduct, len(candidates) - 1


def card_html(g, item_id, recipe, out_entry, is_byproduct, alt_count, assets_root, machines_needed):
    name = html_escape(item_label(g, item_id))
    icon = item_icon(g, item_id, assets_root)
    # No repeating `name` here as a fallback -- the name already gets its own row below.
    icon_cell = f'<IMG SRC="{icon}" SCALE="TRUE"/>' if icon else ""

    if recipe is None:
        # Raw/base resource: no producing recipe in this query's scope.
        return (
            '<TABLE BORDER="1" CELLBORDER="0" CELLSPACING="0" CELLPADDING="6" '
            f'STYLE="ROUNDED" BGCOLOR="white"><TR><TD WIDTH="56" HEIGHT="56">{icon_cell}</TD></TR>'
            f'<TR><TD>{name}</TD></TR></TABLE>'
        )

    machine = recipe["machine"]
    _, machine_name = MACHINE_INFO.get(machine, (None, machine))
    m_icon = machine_icon(machine, assets_root)
    m_icon_cell = f'<IMG SRC="{m_icon}"/>' if m_icon else ""
    banner = (
        '<TABLE BORDER="0" CELLBORDER="0" CELLSPACING="4"><TR>'
        f'<TD WIDTH="16" HEIGHT="16">{m_icon_cell}</TD><TD>{html_escape(machine_name)}</TD>'
        '</TR></TABLE>'
    )

    rf = recipe["energy_rf"] or recipe["rf_output"]
    footer_bits = []
    if rf:
        footer_bits.append(f"{rf:,} RF")
    if is_byproduct:
        footer_bits.append(f"{recipe['byproduct']['chance'] * 100:g}% byproduct")
    elif out_entry:
        amt = format_amount(out_entry)
        if amt:
            footer_bits.append(amt)
    if item_id in machines_needed:
        footer_bits.append(f"×{machines_needed[item_id]:g}")
    footer = html_escape(" · ".join(footer_bits)) if footer_bits else "&nbsp;"
    footer_row = f'<TR><TD BGCOLOR="#eeeeee">{footer}</TD></TR>'
    if alt_count:
        footer_row += f'<TR><TD><FONT POINT-SIZE="8" COLOR="#888888">+{alt_count} more recipe{"s" if alt_count > 1 else ""}</FONT></TD></TR>'

    return (
        '<TABLE BORDER="1" CELLBORDER="0" CELLSPACING="0" CELLPADDING="4" STYLE="ROUNDED" BGCOLOR="white">'
        f'<TR><TD BGCOLOR="#eeeeee">{banner}</TD></TR>'
        f'<TR><TD WIDTH="56" HEIGHT="56">{icon_cell}</TD></TR>'
        f'<TR><TD>{name}</TD></TR>'
        f'{footer_row}'
        '</TABLE>'
    )


def build_dot_cards(g, root, items, recipes, rate_rows, assets_root):
    lines = ["digraph chain {", '  rankdir="LR";', "  node [fontname=Helvetica, fontsize=11, shape=plaintext];", "  edge [fontname=Helvetica, fontsize=9];"]
    machines_needed = {row["item"]: row["machines_needed"] for row in rate_rows} if rate_rows else {}

    faces = {}  # item_id -> (recipe_or_None, out_entry, is_byproduct, alt_count)
    for item_id in items:
        candidates = [rid for rid in recipes if _produces(g, rid, item_id)]
        faces[item_id] = choose_primary_recipe(g, item_id, candidates)

    edges = []  # (from_id, to_id, amount_label)
    for item_id in items:
        recipe, _, _, _ = faces[item_id]
        if recipe is None:
            continue
        for entry in recipe["inputs"]:
            if entry["kind"] == "tag" or entry["id"] not in items:
                continue
            edges.append((entry["id"], item_id, format_amount(entry)))

    # An item pulled in by the raw ancestors()/descendants() BFS (which
    # explores *every* producer/consumer) can end up with no edge at all
    # once each card settles on a single "primary" recipe -- e.g. Oil Sand
    # and Oil Shale are alternate Bitumen sources, but if Oil Red Sand's
    # recipe wins as Bitumen's primary, the other two would otherwise render
    # as disconnected orphan cards. Keep only what's actually reachable from
    # `root` through the edges we're drawing (undirected -- covers
    # ancestors/descendants/both alike).
    adjacency = {}
    for a, b, _ in edges:
        adjacency.setdefault(a, set()).add(b)
        adjacency.setdefault(b, set()).add(a)
    connected = {root}
    frontier = [root]
    while frontier:
        nxt = [n for c in frontier for n in adjacency.get(c, ()) if n not in connected]
        connected.update(nxt)
        frontier = nxt

    for item_id in sorted(connected):
        recipe, out_entry, is_byproduct, alt_count = faces[item_id]
        html = card_html(g, item_id, recipe, out_entry, is_byproduct, alt_count, assets_root, machines_needed)
        lines.append(f'  "{item_id}" [label=<{html}>];')
    for a, b, amount in edges:
        if a in connected and b in connected:
            lines.append(f'  "{a}" -> "{b}" [label="{html_escape(amount)}"];')

    lines.append("}")
    return "\n".join(lines), len(connected)


def _produces(g, recipe_id, item_id):
    recipe = g.recipe(recipe_id)
    if recipe is None:
        return False
    if any(e["id"] == item_id for e in recipe["outputs"]):
        return True
    return bool(recipe["byproduct"] and recipe["byproduct"]["id"] == item_id)


def render(dot_source, out_path):
    dot_bin = shutil.which("dot")
    if not dot_bin:
        print("error: the Graphviz `dot` binary is not on PATH.", file=sys.stderr)
        print("  macOS: brew install graphviz", file=sys.stderr)
        print("  Linux: apt install graphviz", file=sys.stderr)
        return False
    fmt = out_path.suffix.lstrip(".") or "svg"
    result = subprocess.run(
        [dot_bin, f"-T{fmt}", "-o", str(out_path)],
        input=dot_source,
        text=True,
        capture_output=True,
    )
    if result.returncode != 0:
        print(result.stderr, file=sys.stderr)
        return False
    return True


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--data-dir", type=Path, default=None)
    parser.add_argument("--mod-root", type=Path, default=DEFAULT_MOD_ROOT, help="mod worktree for icons/models (default: sibling ../logistics-mc-26.2)")
    sub = parser.add_subparsers(dest="command", required=True)

    p_chain = sub.add_parser("chain", help="render an item's production/consumption chain")
    p_chain.add_argument("item", help="item/fluid id, e.g. logistics:core/fuel_oil")
    p_chain.add_argument(
        "--direction", choices=["ancestors", "descendants", "both"], default="ancestors"
    )
    p_chain.add_argument("--depth", type=int, default=None, help="BFS hop limit (default: unlimited)")
    p_chain.add_argument("--rate", type=float, default=None, help="annotate machine counts for this rate, units/min (ancestors only)")
    p_chain.add_argument(
        "--exclude", default=None,
        help="comma-separated item ids to prune from traversal (e.g. common hub items like minecraft:bucket)",
    )
    p_chain.add_argument("--out", type=Path, required=True, help="output path, extension selects format (.svg/.png/...)")
    p_chain.add_argument("--dot-out", type=Path, default=None, help="also write the raw DOT source here")

    args = parser.parse_args()
    g = RecipeGraph.load(args.data_dir) if args.data_dir else RecipeGraph.load()
    assets_root = args.mod_root / "common/src/main/resources/assets/logistics"

    if args.item not in g.items:
        print(f"error: unknown item id {args.item!r} (see data/items.json)", file=sys.stderr)
        sys.exit(1)

    exclude = set(args.exclude.split(",")) if args.exclude else frozenset()

    items, recipes = set(), set()
    if args.direction in ("ancestors", "both"):
        a_items, a_recipes = g.ancestors(args.item, args.depth, exclude)
        items |= a_items
        recipes |= a_recipes
    if args.direction in ("descendants", "both"):
        d_items, d_recipes = g.descendants(args.item, args.depth, exclude)
        items |= d_items
        recipes |= d_recipes

    rate_rows = g.machine_ratios(args.item, args.rate) if args.rate else []

    dot_source, kept = build_dot_cards(g, args.item, items, recipes, rate_rows, assets_root)
    if args.dot_out:
        args.dot_out.write_text(dot_source + "\n")

    if not render(dot_source, args.out):
        sys.exit(1)
    print(f"wrote {args.out} ({kept} of {len(items)} traversed items rendered)", file=sys.stderr)


if __name__ == "__main__":
    main()
