#!/usr/bin/env python3
"""Render a recipe-chain diagram from the extracted recipe graph.

Run `extract.py` first to (re)generate `data/items.json` / `data/recipes.json`.
Requires the Graphviz `dot` binary on PATH (`brew install graphviz` on macOS;
`apt install graphviz` on Linux) -- this script shells out to it rather than
depending on the `graphviz` PyPI package, so the only new dependency is the
one system binary.

Two output modes, chosen by `--out`'s extension:

- `.svg`/`.png`/... : `dot` renders the whole diagram itself, using its
  (fairly limited) HTML-like node labels for the composite cards.
- `.html`: `dot` is used ONLY to compute layout (`-Tplain`: node positions,
  edge splines) -- the actual visual is real HTML/CSS (absolutely
  positioned card `<div>`s, an SVG overlay for edges/arrows/labels),
  self-contained with icons inlined as data URIs. Crisper text, hover
  affordance, and full CSS control that Graphviz's own renderer can't give;
  prefer this mode. `layout_chain()`/`render_html_page()` implement it.

Each item becomes one composite "card": a machine banner on top (icon +
name), the item's own icon in the middle, and a footer with RF cost /
yield / byproduct chance. An item with more than one producing recipe in
the current query picks the cheapest as its displayed face and notes "+N
more recipes" in the footer -- edges are drawn only for that recipe's
inputs, so what's on the card always matches what feeds it. A raw/base
resource (no producing recipe in scope) renders as a plain icon + name, no
banner/footer.

Item icons come from the mod's own flat 16x16 textures where `items.json`
found one; for ids with no flat texture (mostly machine blocks -- see
`extract.py`'s README caveat) this falls back to an on-demand isometric
render via the sibling `render_blocks.py`, cached under `data/icon_cache/`.

Usage:
    python3 tools/recipe-graph/diagram.py chain logistics:core/tar \\
        --direction descendants --out tar_chain.html

    python3 tools/recipe-graph/diagram.py chain logistics:core/fuel_oil \\
        --direction ancestors --rate 60 --out fuel_oil_chain.svg

Pure stdlib; shells out to the system `dot` binary and (for the icon
fallback) to `render_blocks.py`.
"""

import argparse
import base64
import json
import shlex
import shutil
import subprocess
import sys
from html import escape as html_escape
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from graph import DATA_DIR, RecipeGraph  # noqa: E402

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


def card_html(g, item_id, recipe, out_entry, is_byproduct, alt_count, assets_root, machines_needed, name_override=None, icon_override=None):
    name = html_escape(name_override if name_override else item_label(g, item_id))
    icon = icon_override if name_override else item_icon(g, item_id, assets_root)
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


def load_variant_groups(data_dir):
    path = data_dir / "variant_groups.json"
    return json.loads(path.read_text()) if path.exists() else {}


def compute_chain(g, root, items, recipes, rate_rows, groups=None):
    """Shared by both renderers: pick each item's card face, its edges, and prune to what's connected.

    Returns (connected_items, faces, edges, machines_needed, group_members)
    where faces is item_id -> (recipe_or_None, out_entry, is_byproduct,
    alt_count), edges is [(from_id, to_id, amount_label), ...], and
    group_members is group_node_id -> [member item_id, ...] for any variant
    group that ended up in the render (see `variant_groups.json`).

    `root` may itself land inside a returned group; callers should check
    `group_members` before treating `root` as a plain item id.
    """
    machines_needed = {row["item"]: row["machines_needed"] for row in rate_rows} if rate_rows else {}
    groups = groups or {}

    faces = {}
    for item_id in items:
        candidates = [rid for rid in recipes if _produces(g, rid, item_id)]
        faces[item_id] = choose_primary_recipe(g, item_id, candidates)

    edges = []
    for item_id in items:
        recipe, _, _, _ = faces[item_id]
        if recipe is None:
            continue
        for entry in recipe["inputs"]:
            if entry["kind"] == "tag" or entry["id"] not in items:
                continue
            edges.append((entry["id"], item_id, format_amount(entry)))

    # Fold raw/leaf variant-group members (e.g. Oil Sand / Oil Red Sand /
    # Oil Shale -- interchangeable inputs, not worth three near-duplicate
    # cards) onto one synthetic "group:<id>" node before pruning, so a
    # sibling with no edge of its own (its recipe lost the primary-recipe
    # tie-break to another member -- see choose_primary_recipe) still shows
    # up on the merged card rather than vanishing. Only raw items (no
    # producing recipe in scope) are grouped -- merging items that *do* have
    # their own recipe would need to reconcile differing footers too.
    member_to_group = {}
    for item_id in items:
        if faces[item_id][0] is not None:
            continue
        for gid, group in groups.items():
            if item_id in group["members"]:
                member_to_group[item_id] = f"group:{gid}"
                break

    def remap(item_id):
        return member_to_group.get(item_id, item_id)

    edges = [(remap(a), remap(b), amt) for a, b, amt in edges]
    edges = list(dict.fromkeys(edges))  # de-dupe while keeping first-seen order
    root = remap(root)

    # An item pulled in by the raw ancestors()/descendants() BFS (which
    # explores *every* producer/consumer) can end up with no edge at all
    # once each card settles on a single "primary" recipe -- e.g. if Oil Red
    # Sand's recipe wins as Bitumen's primary over its ungrouped siblings',
    # they'd otherwise render as disconnected orphan cards. Keep only what's
    # actually reachable from `root` through the edges we're drawing
    # (undirected -- covers ancestors/descendants/both alike).
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

    edges = [(a, b, amt) for a, b, amt in edges if a in connected and b in connected]

    group_members = {}
    for item_id, gid in member_to_group.items():
        if gid in connected:
            group_members.setdefault(gid, []).append(item_id)
    for gid in group_members:
        faces[gid] = (None, None, None, 0)

    return connected, faces, edges, machines_needed, group_members


def build_dot_cards(g, root, items, recipes, rate_rows, assets_root, groups=None):
    connected, faces, edges, machines_needed, group_members = compute_chain(g, root, items, recipes, rate_rows, groups)

    lines = ["digraph chain {", '  rankdir="LR";', "  node [fontname=Helvetica, fontsize=11, shape=plaintext];", "  edge [fontname=Helvetica, fontsize=9];"]
    for item_id in sorted(connected):
        recipe, out_entry, is_byproduct, alt_count = faces[item_id]
        if item_id in group_members:
            gid = item_id.split(":", 1)[1]
            members = group_members[item_id]
            name = (groups or {}).get(gid, {}).get("display_name") or " / ".join(item_label(g, m) for m in members)
            icon = item_icon(g, members[0], assets_root)  # first member's icon; the HTML renderer shows all of them
            html = card_html(g, item_id, recipe, out_entry, is_byproduct, alt_count, assets_root, machines_needed, name, icon)
        else:
            html = card_html(g, item_id, recipe, out_entry, is_byproduct, alt_count, assets_root, machines_needed)
        lines.append(f'  "{item_id}" [label=<{html}>];')
    for a, b, amount in edges:
        lines.append(f'  "{a}" -> "{b}" [label="{html_escape(amount)}"];')
    lines.append("}")
    return "\n".join(lines), len(connected)


DPI = 96  # px per inch, matching dot's -Tplain coordinate convention
CARD_W_IN, CARD_H_IN = 190 / DPI, 210 / DPI
RAW_W_IN, RAW_H_IN = 140 / DPI, 120 / DPI


def data_uri(path):
    if not path:
        return None
    try:
        data = base64.b64encode(Path(path).read_bytes()).decode()
        return f"data:image/png;base64,{data}"
    except OSError:
        return None


def card_content(g, item_id, recipe, out_entry, is_byproduct, alt_count, assets_root, machines_needed):
    """Card data for the HTML renderer -- the same face-selection logic as card_html, real values not markup."""
    content = {
        "name": item_label(g, item_id),
        "icon": data_uri(item_icon(g, item_id, assets_root)),
        "raw": recipe is None,
    }
    if recipe is None:
        return content

    machine = recipe["machine"]
    _, machine_name = MACHINE_INFO.get(machine, (None, machine))
    content["machine_name"] = machine_name
    content["machine_icon"] = data_uri(machine_icon(machine, assets_root))

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
    content["footer"] = " · ".join(footer_bits)
    content["alt_count"] = alt_count
    return content


def group_card_content(g, gid, group_def, member_ids, assets_root):
    """Card data for a merged variant-group node (e.g. Oil Sand / Oil Red Sand / Oil Shale)."""
    name = (group_def or {}).get("display_name") or " / ".join(item_label(g, m) for m in member_ids)
    return {
        "name": name,
        "icons": [data_uri(item_icon(g, m, assets_root)) for m in member_ids],
        "raw": True,
        "group": True,
    }


def layout_chain(connected, edges, faces, group_members=None):
    """Run `dot -Tplain` on a minimal (unstyled, correctly-sized) graph to get real positions.

    Returns (nodes, edge_paths, width_px, height_px) where nodes is
    item_id -> (cx, cy, w, h) in px (top-left origin, y down) and edge_paths
    is [(from_id, to_id, [(x,y),...] spline points, label, (lx, ly) or None), ...].
    """
    dot_bin = shutil.which("dot")
    if not dot_bin:
        return None
    group_members = group_members or {}

    lines = ["digraph g {", "  rankdir=LR;", "  nodesep=0.35;", "  ranksep=0.55;", "  node [shape=box, fixedsize=true, label=\"\"];"]
    for item_id in connected:
        recipe = faces[item_id][0]
        w, h = (RAW_W_IN, RAW_H_IN) if recipe is None else (CARD_W_IN, CARD_H_IN)
        if item_id in group_members:
            w += (len(group_members[item_id]) - 1) * (36 / DPI)  # room for each extra icon in the strip
        lines.append(f'  "{item_id}" [width={w:.3f}, height={h:.3f}];')
    for a, b, amount in edges:
        label = f' [label="{amount}"]' if amount else ""
        lines.append(f'  "{a}" -> "{b}"{label};')
    lines.append("}")

    result = subprocess.run([dot_bin, "-Tplain"], input="\n".join(lines), text=True, capture_output=True)
    if result.returncode != 0:
        print(result.stderr, file=sys.stderr)
        return None

    graph_h_in = 0
    nodes = {}
    edge_paths = []
    for line in result.stdout.splitlines():
        if not line.strip():
            continue
        tokens = shlex.split(line)
        kind = tokens[0]
        if kind == "graph":
            graph_h_in = float(tokens[3])
        elif kind == "node":
            name, x, y, w, h = tokens[1], *map(float, tokens[2:6])
            nodes[name] = (x, y, w, h)
        elif kind == "edge":
            tail, head, n = tokens[1], tokens[2], int(tokens[3])
            coords = list(map(float, tokens[4 : 4 + 2 * n]))
            points = list(zip(coords[0::2], coords[1::2]))
            rest = tokens[4 + 2 * n :]
            label, lx, ly = None, None, None
            if len(rest) >= 3 and rest[0] not in ("solid", "dashed", "bold"):
                label, lx, ly = rest[0], float(rest[1]), float(rest[2])
            edge_paths.append((tail, head, points, label, (lx, ly) if lx is not None else None))

    def to_px(x, y):
        return x * DPI, (graph_h_in - y) * DPI

    px_nodes = {}
    max_x = max_y = 0.0
    for name, (x, y, w, h) in nodes.items():
        cx, cy = to_px(x, y)
        px_nodes[name] = (cx, cy, w * DPI, h * DPI)
        max_x, max_y = max(max_x, cx + w * DPI / 2), max(max_y, cy + h * DPI / 2)

    px_edges = []
    for tail, head, points, label, lpos in edge_paths:
        px_points = [to_px(x, y) for x, y in points]
        px_lpos = to_px(*lpos) if lpos else None
        px_edges.append((tail, head, px_points, label, px_lpos))

    return px_nodes, px_edges, max_x + 40, max_y + 40


def spline_path(points):
    """dot -Tplain gives points as: 1 start point, then groups of 3 forming cubic Beziers."""
    if not points:
        return ""
    path = f"M {points[0][0]:.1f} {points[0][1]:.1f} "
    i = 1
    while i + 2 < len(points):
        p1, p2, p3 = points[i], points[i + 1], points[i + 2]
        path += f"C {p1[0]:.1f} {p1[1]:.1f} {p2[0]:.1f} {p2[1]:.1f} {p3[0]:.1f} {p3[1]:.1f} "
        i += 3
    return path


CARD_CSS = """
:root {
  --bg: #16181d; --surface: #1f232b; --surface-2: #262b35; --border: #343b47;
  --ink: #e8eaed; --ink-secondary: #a8b0bd; --ink-muted: #707886; --accent: #e2a03f;
  --edge: #566072;
}
:root[data-theme="light"] {
  --bg: #f3f4f6; --surface: #ffffff; --surface-2: #f0f1f4; --border: #d8dbe1;
  --ink: #1c1f26; --ink-secondary: #4b5261; --ink-muted: #7b8394; --accent: #b3720c;
  --edge: #9aa3b2;
}
@media (prefers-color-scheme: light) {
  :root:not([data-theme="dark"]) {
    --bg: #f3f4f6; --surface: #ffffff; --surface-2: #f0f1f4; --border: #d8dbe1;
    --ink: #1c1f26; --ink-secondary: #4b5261; --ink-muted: #7b8394; --accent: #b3720c;
    --edge: #9aa3b2;
  }
}
* { box-sizing: border-box; }
body { margin: 0; background: var(--bg); color: var(--ink); font-family: -apple-system, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; }
h1 { position: sticky; top: 0; margin: 0; padding: 14px 20px; font-size: 1.05rem; background: var(--bg); border-bottom: 1px solid var(--border); z-index: 2; }
.canvas { position: relative; overflow: auto; }
img { image-rendering: pixelated; display: block; }
.edges { position: absolute; top: 0; left: 0; overflow: visible; }
.edge-label {
  position: absolute; transform: translate(-50%, -50%); background: var(--bg); color: var(--ink-secondary);
  font-size: 11px; padding: 1px 5px; border-radius: 4px; white-space: nowrap;
}
.card {
  position: absolute; display: flex; flex-direction: column; background: var(--surface);
  border: 1px solid var(--border); border-radius: 10px; overflow: hidden; text-align: center;
  transition: transform 0.12s ease, box-shadow 0.12s ease; cursor: default;
}
.card:hover { transform: scale(1.04); box-shadow: 0 6px 18px rgba(0,0,0,0.35); z-index: 3; }
.card.raw { align-items: center; justify-content: center; gap: 6px; padding: 8px; }
.card .banner { display: flex; align-items: center; gap: 6px; background: var(--surface-2); padding: 4px 8px; border-bottom: 1px solid var(--border); font-size: 11px; color: var(--ink-secondary); flex: 0 0 auto; }
.card .banner img { width: 16px; height: 16px; object-fit: contain; }
.card .icon { width: 48px; height: 48px; margin: 10px auto 4px; flex: 0 0 auto; }
.card .icon img { width: 100%; height: 100%; object-fit: contain; }
.card .icon.icon-strip { width: auto; display: flex; gap: 4px; justify-content: center; }
.card .icon.icon-strip img { width: 40px; height: 40px; flex: 0 0 auto; }
.card .name { font-weight: 600; font-size: 0.86rem; padding: 0 6px; flex: 0 0 auto; }
.card .footer { margin-top: auto; background: var(--surface-2); border-top: 1px solid var(--border); font-size: 11px; color: var(--ink-secondary); padding: 4px 6px; flex: 0 0 auto; }
.card .alt { font-size: 9px; color: var(--ink-muted); padding-bottom: 4px; flex: 0 0 auto; }
"""


def page_title(g, root, group_members, groups):
    if root in group_members:
        gid = root.split(":", 1)[1]
        return (groups or {}).get(gid, {}).get("display_name") or " / ".join(item_label(g, m) for m in group_members[root])
    return item_label(g, root)


def render_html_page(g, root, connected, faces, edges, machines_needed, assets_root, out_path, group_members=None, groups=None):
    group_members = group_members or {}
    layout = layout_chain(connected, edges, faces, group_members)
    if layout is None:
        return False
    nodes, px_edges, width, height = layout

    cards = []
    for item_id in connected:
        recipe, out_entry, is_byproduct, alt_count = faces[item_id]
        cx, cy, w, h = nodes[item_id]
        style = f"left:{cx - w / 2:.0f}px; top:{cy - h / 2:.0f}px; width:{w:.0f}px; height:{h:.0f}px;"

        if item_id in group_members:
            gid = item_id.split(":", 1)[1]
            content = group_card_content(g, gid, (groups or {}).get(gid), group_members[item_id], assets_root)
            icons = "".join(f'<img src="{icon}" alt="">' for icon in content["icons"] if icon)
            cards.append(
                f'<div class="card raw" style="{style}" title="{html_escape(content["name"])}">'
                f'<div class="icon icon-strip">{icons}</div><div class="name">{html_escape(content["name"])}</div></div>'
            )
            continue

        content = card_content(g, item_id, recipe, out_entry, is_byproduct, alt_count, assets_root, machines_needed)
        icon_img = f'<img src="{content["icon"]}" alt="">' if content["icon"] else ""
        if content["raw"]:
            cards.append(
                f'<div class="card raw" style="{style}" title="{html_escape(content["name"])}">'
                f'<div class="icon">{icon_img}</div><div class="name">{html_escape(content["name"])}</div></div>'
            )
            continue
        m_icon_img = f'<img src="{content["machine_icon"]}" alt="">' if content.get("machine_icon") else ""
        alt_html = ""
        if content.get("alt_count"):
            n = content["alt_count"]
            alt_html = f'<div class="alt">+{n} more recipe{"s" if n > 1 else ""}</div>'
        cards.append(
            f'<div class="card" style="{style}" title="{html_escape(content["name"])}">'
            f'<div class="banner">{m_icon_img}<span>{html_escape(content["machine_name"])}</span></div>'
            f'<div class="icon">{icon_img}</div>'
            f'<div class="name">{html_escape(content["name"])}</div>'
            f'<div class="footer">{html_escape(content.get("footer") or "&nbsp;")}</div>'
            f'{alt_html}</div>'
        )

    edge_svg = []
    edge_labels = []
    for tail, head, points, label, lpos in px_edges:
        edge_svg.append(f'<path d="{spline_path(points)}" fill="none" stroke="var(--edge)" stroke-width="1.6" marker-end="url(#arrow)"/>')
        if label and lpos:
            edge_labels.append(f'<div class="edge-label" style="left:{lpos[0]:.0f}px; top:{lpos[1]:.0f}px;">{html_escape(label)}</div>')

    title = page_title(g, root, group_members, groups)
    html = f"""<title>{html_escape(title)} chain</title>
<style>{CARD_CSS}</style>
<h1>{html_escape(title)} &mdash; recipe chain ({len(connected)} items)</h1>
<div class="canvas" style="width:{width:.0f}px; height:{height:.0f}px;">
  <svg class="edges" width="{width:.0f}" height="{height:.0f}">
    <defs><marker id="arrow" markerWidth="8" markerHeight="8" refX="7" refY="3" orient="auto"><path d="M0,0 L8,3 L0,6 Z" fill="var(--edge)"/></marker></defs>
    {''.join(edge_svg)}
  </svg>
  {''.join(edge_labels)}
  {''.join(cards)}
</div>
"""
    out_path.write_text(html)
    return True


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
    p_chain.add_argument(
        "--no-group", action="store_true",
        help="don't merge variant-group items (see data/variant_groups.json) onto one card",
    )

    args = parser.parse_args()
    data_dir = args.data_dir or DATA_DIR
    g = RecipeGraph.load(data_dir)
    assets_root = args.mod_root / "common/src/main/resources/assets/logistics"
    groups = {} if args.no_group else load_variant_groups(data_dir)

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

    if args.out.suffix.lower() == ".html":
        connected, faces, chain_edges, machines_needed, group_members = compute_chain(g, args.item, items, recipes, rate_rows, groups)
        if not render_html_page(g, args.item, connected, faces, chain_edges, machines_needed, assets_root, args.out, group_members, groups):
            sys.exit(1)
        print(f"wrote {args.out} ({len(connected)} of {len(items)} traversed items rendered)", file=sys.stderr)
        return

    dot_source, kept = build_dot_cards(g, args.item, items, recipes, rate_rows, assets_root, groups)
    if args.dot_out:
        args.dot_out.write_text(dot_source + "\n")

    if not render(dot_source, args.out):
        sys.exit(1)
    print(f"wrote {args.out} ({kept} of {len(items)} traversed items rendered)", file=sys.stderr)


if __name__ == "__main__":
    main()
