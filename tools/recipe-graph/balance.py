#!/usr/bin/env python3
"""Balance-evaluation CLI over the extracted recipe graph.

Run `extract.py` first to (re)generate `data/items.json` / `data/recipes.json`.

Subcommands:
    cost <item>              Full RF-cost-per-unit breakdown to raw resources.
    chain <item> --rate N     Machine-count solve for a target output rate (units/min).
    outliers                 Flag recipes whose RF-cost-per-output looks out
                              of line with same-machine recipes at a similar
                              graph depth (internal-consistency check --
                              design/progression-tiers.md defines a material
                              tier ladder, not a global numeric RF curve, so
                              there's no external target to check against yet).

Usage:
    python3 tools/recipe-graph/balance.py cost logistics:core/fuel_oil
    python3 tools/recipe-graph/balance.py chain logistics:core/fuel_oil --rate 60
    python3 tools/recipe-graph/balance.py outliers [--machine crucible]

Pure stdlib; no external dependencies.
"""

import argparse
import statistics
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from graph import RecipeGraph  # noqa: E402


def name_of(g, item_id):
    item = g.item(item_id)
    return item["display_name"] if item else item_id


def print_chain(g, node, indent=0):
    label = name_of(g, node["item"])
    rf = node["rf_per_unit"]
    rf_str = "unresolved" if rf is None else f"{rf:g} RF/unit"
    via = f" via {node['recipe']}" if node["recipe"] else " (base resource)"
    print("  " * indent + f"- {label} [{node['item']}]: {rf_str}{via}")
    for child in node["inputs"]:
        print_chain(g, child, indent + 1)


def cmd_cost(args, g):
    node = g.cheapest_chain(args.item)
    if node["rf_per_unit"] is None:
        print(f"{args.item}: cost unresolved (no priced, non-circular production path found)")
        return 1
    print_chain(g, node)
    return 0


def cmd_chain(args, g):
    rows = g.machine_ratios(args.item, args.rate)
    if not rows:
        print(f"{args.item}: no timed production path found (missing time_ticks/recipe)")
        return 1
    width = max(len(name_of(g, r["item"])) for r in rows)
    print(f"Target: {args.rate:g}/min of {name_of(g, args.item)}\n")
    for r in rows:
        label = name_of(g, r["item"]).ljust(width)
        print(f"  {label}  {r['rate_per_min']:>8.2f}/min  {r['machines_needed']:>7.3f}x {r['machine']:<14} ({r['recipe']})")
    return 0


def cmd_outliers(args, g):
    depth_memo = {}

    def item_depth(item_id, stack=frozenset()):
        if item_id in depth_memo:
            return depth_memo[item_id]
        if item_id in stack:
            return 0
        producers = g.producers(item_id)
        if not producers:
            depth_memo[item_id] = 0
            return 0
        best = None
        for r in producers:
            input_depth = 0
            for e in r["inputs"]:
                if e["kind"] == "tag":
                    continue
                input_depth = max(input_depth, item_depth(e["id"], stack | {item_id}))
            candidate = input_depth + 1
            best = candidate if best is None else min(best, candidate)
        depth_memo[item_id] = best or 0
        return depth_memo[item_id]

    # (machine, depth) -> [(rf_per_unit, recipe_id, output_item), ...]
    buckets = {}
    for recipe in g.recipes.values():
        if args.machine and recipe["machine"] != args.machine:
            continue
        if recipe["energy_rf"] is None or not recipe["outputs"]:
            continue
        out = recipe["outputs"][0]
        amount = out.get("count") or out.get("amount") or 1
        rf_per_unit = recipe["energy_rf"] / amount
        depth = item_depth(out["id"])
        buckets.setdefault((recipe["machine"], depth), []).append((rf_per_unit, recipe["recipe_id"], out["id"]))

    findings = []
    for (machine, depth), rows in buckets.items():
        if len(rows) < args.min_bucket:
            continue
        values = [v for v, _, _ in rows]
        median = statistics.median(values)
        if median == 0:
            continue
        for rf_per_unit, recipe_id, item_id in rows:
            ratio = rf_per_unit / median
            if ratio >= args.threshold or ratio <= 1 / args.threshold:
                findings.append((ratio, machine, depth, recipe_id, name_of(g, item_id), rf_per_unit, median))

    findings.sort(key=lambda f: -max(f[0], 1 / f[0]))
    if not findings:
        print("No outliers found at the current threshold.")
        return 0
    print(f"{'ratio':>7}  {'machine':<14} {'depth':>5}  {'item':<28} {'rf/unit':>10}  {'bucket median':>14}  recipe")
    for ratio, machine, depth, recipe_id, item_name, rf_per_unit, median in findings:
        print(f"{ratio:>6.2f}x  {machine:<14} {depth:>5}  {item_name:<28} {rf_per_unit:>10.1f}  {median:>14.1f}  {recipe_id}")
    return 0


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--data-dir", type=Path, default=None)
    sub = parser.add_subparsers(dest="command", required=True)

    p_cost = sub.add_parser("cost", help="RF-cost-per-unit breakdown for an item")
    p_cost.add_argument("item", help="item/fluid id, e.g. logistics:core/fuel_oil")

    p_chain = sub.add_parser("chain", help="machine-count solve for a target rate")
    p_chain.add_argument("item")
    p_chain.add_argument("--rate", type=float, required=True, help="target output rate, units/min")

    p_out = sub.add_parser("outliers", help="flag recipes with out-of-line RF cost")
    p_out.add_argument("--machine", default=None)
    p_out.add_argument("--threshold", type=float, default=3.0, help="flag ratio vs bucket median (default 3x)")
    p_out.add_argument("--min-bucket", type=int, default=3, help="minimum bucket size to compare against (default 3)")

    args = parser.parse_args()
    g = RecipeGraph.load(args.data_dir) if args.data_dir else RecipeGraph.load()

    if args.command == "cost":
        sys.exit(cmd_cost(args, g))
    elif args.command == "chain":
        sys.exit(cmd_chain(args, g))
    elif args.command == "outliers":
        sys.exit(cmd_outliers(args, g))


if __name__ == "__main__":
    main()
