#!/usr/bin/env python3
"""Extract the mod's recipe data into normalized, checked-in JSON tables.

Lives in the docs worktree (branch `docs`) alongside the other reporting/
wiki tooling, but reads from a sibling **mod** worktree -- same
cross-worktree convention as `render_blocks.py`'s `--assets`. Reads every
recipe JSON under `common/src/main/resources/data/logistics/recipe/` in that
worktree (both the mod's own processing-machine types and the vanilla
crafting/smelting recipes it ships for its own items), expands `#tag`
ingredients via the game's tag data, resolves item/fluid display names from
the lang file, and locates each item's 16x16 icon texture. The mod's recipe
JSON stays the single source of truth; this script re-derives its dataset
from it rather than duplicating chains by hand.

Two small tables below mirror Java-only constants that cannot be read from
data files (per-machine RF/tick config, Fuel Engine burn values). Each is
commented with its Java source of truth -- re-sync by hand if those
constants change.

Usage:
    python3 tools/recipe-graph/extract.py [--repo-root PATH] [-v]

    --repo-root defaults to the sibling ../logistics-mc-26.2 worktree (the
    branch new content lands on first); pass another mod worktree to extract
    a different branch's recipes.

Outputs (checked into git):
    tools/recipe-graph/data/items.json
    tools/recipe-graph/data/recipes.json

Pure stdlib; no external dependencies.
"""

import argparse
import json
import re
import sys
from pathlib import Path

# --- Java-only constants, kept in sync by hand -----------------------------

# common/src/main/java/com/logistics/LogisticsAutomation.java, CONFIG class,
# `<machine>.defineLong("energy_per_tick", <n>L)` defaults.
MACHINE_RF_PER_TICK = {
    "macerator": 10,
    "kiln": 20,
    "sawmill": 20,
    "crucible": 40,
    "alloy_smelter": 20,
    "refinery": 20,
    "fabricator": 80,
    "transposer": 20,
}

# LogisticsAutomation.java CONFIG.KILN_RF_PER_COOK_TICK default: recipe
# energy for a vanilla smelting recipe = cookingtime * this.
KILN_RF_PER_COOK_TICK = 10

# common/src/main/java/com/logistics/power/engine/fuel/FuelEngineFuels.java
# BY_ID map: {rf_per_bucket, heat}. Emitted as synthetic sink recipes so
# fuel-chain diagrams can terminate at the engine.
FUEL_ENGINE_FUELS = {
    "logistics:core/crude_oil": {"rf_per_bucket": 40_000, "heat": 3.0},
    "logistics:core/bio_fuel": {"rf_per_bucket": 80_000, "heat": 1.5},
    "logistics:core/fuel_oil": {"rf_per_bucket": 150_000, "heat": 2.25},
}

REGISTRY_FILES = {
    "core": "common/src/main/java/com/logistics/LogisticsCore.java",
    "pipe": "common/src/main/java/com/logistics/LogisticsPipe.java",
    "power": "common/src/main/java/com/logistics/LogisticsPower.java",
    "automation": "common/src/main/java/com/logistics/LogisticsAutomation.java",
}

REGISTER_CALL_RE = re.compile(
    r"registerItem\(\s*\"([a-z0-9_]+)\"|registerBlockWithItem\(\s*\"([a-z0-9_]+)\"|registerBlock\(\s*\"([a-z0-9_]+)\""
)

TYPE_TO_MACHINE = {
    "logistics:macerator": "macerator",
    "logistics:sawmill": "sawmill",
    "logistics:alloy_smelter": "alloy_smelter",
    "logistics:crucible": "crucible",
    "logistics:refinery": "refinery",
    "logistics:fabricator": "fabricator",
    "logistics:transposer": "transposer",
    "logistics:reaction": "reaction",
    "minecraft:smelting": "kiln",
    "minecraft:blasting": "blast_furnace",
    "minecraft:crafting_shaped": "crafting_table",
    "minecraft:crafting_shapeless": "crafting_table",
}


def log(verbose, *args):
    if verbose:
        print(*args, file=sys.stderr)


# --- ingredient / result normalization --------------------------------------


def norm_id_ref(value):
    """A bare id string, `#tag` string, or {"id"/"ingredient"/"item": ..., "count": ...} object.

    Returns (id_or_tag, kind, count) where kind is "tag" for `#`-prefixed
    references and "item" otherwise.
    """
    count = 1
    if isinstance(value, dict):
        count = value.get("count", 1)
        value = value.get("id") or value.get("ingredient") or value.get("item")
    if value is None:
        return None
    if isinstance(value, list):
        # Flattened Ingredient codec: an inline "any of these items" list
        # (no tag involved). Modeled as a synthetic tag so graph traversal
        # treats it like any other multi-item ingredient.
        synthetic_id = "any:" + "|".join(sorted(value))
        return {"id": synthetic_id, "kind": "tag", "count": count}
    if isinstance(value, str) and value.startswith("#"):
        return {"id": value[1:], "kind": "tag", "count": count}
    return {"id": value, "kind": "item", "count": count}


def norm_fluid_ref(value):
    return {"id": value["fluid"], "kind": "fluid", "amount": value["amount"]}


def norm_result(value):
    """{"id"/"fluid": ..., "count"/"amount": ...} -> a normalized output entry."""
    if "fluid" in value:
        return {"id": value["fluid"], "kind": "fluid", "amount": value.get("amount", 1)}
    return {"id": value["id"], "kind": "item", "count": value.get("count", 1)}


def norm_byproduct(data):
    bp = data.get("byproduct")
    if not bp:
        return None
    return {"id": bp["id"], "chance": bp["chance"]}


# --- per-recipe-type parsers -------------------------------------------------
# Each returns (inputs, outputs, energy_rf, rf_output, extra) or None to skip.


def parse_macerator_like(data):
    ing = norm_id_ref(data["ingredient"])
    ing["count"] = data.get("count", ing.get("count", 1))
    inputs = [ing]
    outputs = [norm_result(data["result"])]
    extra = {}
    if "experience" in data:
        extra["experience"] = data["experience"]
    return inputs, outputs, data["energy"], None, extra


def parse_alloy_smelter(data):
    inputs = [norm_id_ref(v) for v in data["ingredients"]]
    outputs = [norm_result(data["result"])]
    return inputs, outputs, data["energy"], None, {}


def parse_crucible(data):
    inputs = [norm_id_ref(data["ingredient"])]
    outputs = [norm_result(data["result"])]
    return inputs, outputs, data["energy"], None, {}


def parse_refinery(data):
    inputs = [norm_fluid_ref(data["input"])]
    outputs = [norm_result(data["result"])]
    return inputs, outputs, data["energy"], None, {}


def parse_fabricator(data):
    inputs = [norm_id_ref(v) for v in data["ingredients"]]
    outputs = [norm_result(data["result"])]
    return inputs, outputs, data["energy"], None, {}


def parse_transposer(data):
    inputs, outputs = [], []
    if "input" in data:
        inputs.append(norm_id_ref(data["input"]))
    if "result" in data:
        outputs.append(norm_result(data["result"]))
    fluid = data.get("fluid")
    if fluid:
        amount = fluid["amount"]
        entry = {"id": fluid["fluid"], "kind": "fluid", "amount": abs(amount)}
        (inputs if amount < 0 else outputs).append(entry)
    return inputs, outputs, data["energy"], None, {}


def parse_reaction(data):
    reagent = norm_id_ref(data["reagent"])
    reactant = norm_fluid_ref(data["reactant"])
    inputs = [reactant, reagent]
    # Produces RF directly, not an item/fluid -- no outputs.
    return inputs, [], None, data["energy"], {"time_ticks": data["time"]}


def parse_kiln_smelting(data):
    ing = norm_id_ref(data["ingredient"])
    inputs = [ing]
    outputs = [norm_result(data["result"])]
    cooking_time = data.get("cookingtime", 200)
    energy_rf = cooking_time * KILN_RF_PER_COOK_TICK
    extra = {"vanilla_cooking_time": cooking_time}
    if "experience" in data:
        extra["experience"] = data["experience"]
    return inputs, outputs, energy_rf, None, extra


def parse_vanilla_crafting(data):
    inputs = []
    if "ingredients" in data:  # shapeless
        for v in data["ingredients"]:
            ref = norm_id_ref(v)
            if ref:
                inputs.append(ref)
    elif "key" in data:  # shaped: count symbol occurrences in the pattern
        pattern = "".join(data.get("pattern", []))
        counts = {}
        for ch in pattern:
            if ch != " ":
                counts[ch] = counts.get(ch, 0) + 1
        for symbol, value in data["key"].items():
            ref = norm_id_ref(value)
            if ref:
                ref["count"] = ref.get("count", 1) * counts.get(symbol, 1)
                inputs.append(ref)
    outputs = [norm_result(data["result"])]
    return inputs, outputs, None, None, {}


def parse_blasting(data):
    ing = norm_id_ref(data["ingredient"])
    outputs = [norm_result(data["result"])]
    return [ing], outputs, None, None, {}


PARSERS = {
    "macerator": parse_macerator_like,
    "sawmill": parse_macerator_like,
    "alloy_smelter": parse_alloy_smelter,
    "crucible": parse_crucible,
    "refinery": parse_refinery,
    "fabricator": parse_fabricator,
    "transposer": parse_transposer,
    "reaction": parse_reaction,
    "kiln": parse_kiln_smelting,
    "blast_furnace": parse_blasting,
    "crafting_table": parse_vanilla_crafting,
}


def extract_recipes(recipe_dir, verbose):
    recipes = []
    for path in sorted(recipe_dir.rglob("*.json")):
        rel = path.relative_to(recipe_dir)
        data = json.loads(path.read_text())
        recipe_type = data.get("type")
        machine = TYPE_TO_MACHINE.get(recipe_type)
        if machine is None:
            log(verbose, f"skip (unknown type {recipe_type!r}): {rel}")
            continue
        parser = PARSERS[machine]
        try:
            inputs, outputs, energy_rf, rf_output, extra = parser(data)
        except KeyError as e:
            log(verbose, f"skip (missing field {e}): {rel}")
            continue
        time_ticks = extra.pop("time_ticks", None)
        if time_ticks is None and energy_rf is not None and machine in MACHINE_RF_PER_TICK:
            time_ticks = round(energy_rf / MACHINE_RF_PER_TICK[machine])
        record = {
            "recipe_id": f"logistics:{rel.with_suffix('').as_posix()}",
            "machine": machine,
            "recipe_type": recipe_type,
            "inputs": inputs,
            "outputs": outputs,
            "byproduct": norm_byproduct(data),
            "energy_rf": energy_rf,
            "rf_output": rf_output,
            "time_ticks": time_ticks,
            "source_file": rel.as_posix(),
        }
        record.update(extra)
        recipes.append(record)

    # Synthetic Fuel Engine sink edges: bucket of fuel -> RF.
    for fluid_id, fuel in FUEL_ENGINE_FUELS.items():
        recipes.append(
            {
                "recipe_id": f"synthetic:fuel_engine/{fluid_id.split('/')[-1]}",
                "machine": "fuel_engine",
                "recipe_type": "synthetic:fuel_engine",
                "inputs": [{"id": fluid_id, "kind": "fluid", "amount": 1000}],
                "outputs": [],
                "byproduct": None,
                "energy_rf": None,
                "rf_output": fuel["rf_per_bucket"],
                "time_ticks": None,
                "heat": fuel["heat"],
                "source_file": None,
            }
        )
    return recipes


# --- tag expansion ------------------------------------------------------------


def load_tag_members(tag_id, data_root, cache, stack=None):
    """Resolve a `namespace:path` item tag to its concrete item ids.

    Returns None if the tag file isn't present in this repo (e.g. it's
    supplied by an external mod's convention-tag data at runtime).
    """
    if tag_id in cache:
        return cache[tag_id]
    if tag_id.startswith("any:"):
        # Synthetic "any of these items" tag from a flattened Ingredient
        # list (see norm_id_ref) -- members are encoded in the id itself.
        cache[tag_id] = set(tag_id[len("any:"):].split("|"))
        return cache[tag_id]
    stack = stack or set()
    if tag_id in stack:
        return set()  # cyclic tag reference; shouldn't happen, don't hang
    namespace, path = tag_id.split(":", 1)
    tag_file = data_root / namespace / "tags" / "item" / f"{path}.json"
    if not tag_file.exists():
        cache[tag_id] = None
        return None
    data = json.loads(tag_file.read_text())
    members = set()
    unresolved = False
    for value in data.get("values", []):
        if isinstance(value, dict):
            value = value.get("id")
        if not value:
            continue
        if value.startswith("#"):
            nested = load_tag_members(value[1:], data_root, cache, stack | {tag_id})
            if nested is None:
                unresolved = True
            else:
                members.update(nested)
        else:
            members.add(value)
    cache[tag_id] = None if (unresolved and not members) else members
    return cache[tag_id]


def collect_tag_ids(recipes):
    tag_ids = set()
    for r in recipes:
        for entry in r["inputs"] + r["outputs"]:
            if entry.get("kind") == "tag":
                tag_ids.add(entry["id"])
    return tag_ids


# --- items table --------------------------------------------------------------

LANG_PREFIXES = {"item": "item", "block": "item", "fluid": "fluid"}


def id_to_texture_candidates(item_id, textures_root, kind):
    namespace, path = item_id.split(":", 1)
    if namespace != "logistics":
        return []
    domain, _, name = path.partition("/")
    candidates = []
    if kind == "fluid":
        candidates.append(textures_root / "block" / domain / "fluid" / f"{name}_still.png")
    candidates.append(textures_root / "item" / domain / f"{name}.png")
    candidates.append(textures_root / "block" / domain / f"{name}.png")
    return candidates


def find_icon(item_id, textures_root, kind):
    for candidate in id_to_texture_candidates(item_id, textures_root, kind):
        if candidate.exists():
            return candidate.relative_to(textures_root.parents[1]).as_posix()
    return None


def humanize(item_id):
    name = item_id.split(":", 1)[1].rsplit("/", 1)[-1]
    return name.replace("_", " ").title()


def build_items_table(recipes, tag_members, repo_root, verbose):
    textures_root = repo_root / "common/src/main/resources/assets/logistics/textures"
    lang_path = repo_root / "common/src/main/resources/assets/logistics/lang/en_us.json"
    lang = json.loads(lang_path.read_text())

    # id -> {"item": name, "fluid": name} -- a custom fluid's bucket item and
    # the fluid itself can share one resource id (e.g. logistics:core/bio_fuel
    # names both the tank fluid and its filled-bucket item), each with its
    # own lang entry.
    display_names_by_kind = {}
    for key, name in lang.items():
        prefix, _, rest = key.partition(".")
        if prefix not in LANG_PREFIXES or not rest.startswith("logistics."):
            continue
        domain_and_path = rest[len("logistics."):]
        domain, _, item_path = domain_and_path.partition(".")
        item_id = f"logistics:{domain}/{item_path}"
        display_names_by_kind.setdefault(item_id, {})[LANG_PREFIXES[prefix]] = name

    # Every id referenced anywhere (inputs/outputs of real items+fluids, and tags).
    seen_kinds = {}  # id -> set of kinds observed ("item"/"fluid")
    for r in recipes:
        for entry in r["inputs"] + r["outputs"]:
            if entry["kind"] in ("item", "fluid"):
                seen_kinds.setdefault(entry["id"], set()).add(entry["kind"])
        if r["byproduct"]:
            seen_kinds.setdefault(r["byproduct"]["id"], set()).add("item")
    for item_id, names_by_kind in display_names_by_kind.items():
        seen_kinds.setdefault(item_id, set()).update(names_by_kind.keys())

    items = {}
    for item_id, kinds in sorted(seen_kinds.items()):
        # An id shared by a fluid and its bucket item (see above) is modeled
        # as one node, keyed under its "fluid" identity.
        kind = "fluid" if "fluid" in kinds else "item"
        namespace = item_id.split(":", 1)[0]
        domain = item_id.split(":", 1)[1].split("/", 1)[0] if "/" in item_id else None
        names_by_kind = display_names_by_kind.get(item_id, {})
        display_name = names_by_kind.get(kind) or next(iter(names_by_kind.values()), None) or humanize(item_id)
        items[item_id] = {
            "id": item_id,
            "kind": kind,
            "also_item_form": kind == "fluid" and "item" in kinds,
            "display_name": display_name,
            "domain": domain if namespace == "logistics" else namespace,
            "icon_path": find_icon(item_id, textures_root, kind) if namespace == "logistics" else None,
        }

    for tag_id, members in sorted(tag_members.items()):
        items[f"#{tag_id}"] = {
            "id": f"#{tag_id}",
            "kind": "tag",
            "display_name": f"Tag: {tag_id}",
            "domain": None,
            "icon_path": None,
            "members": sorted(members) if members is not None else None,
            "external": members is None,
        }
        if members is None:
            log(verbose, f"tag not resolvable locally (external mod data): #{tag_id}")

    return items


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    default_repo_root = Path(__file__).resolve().parents[2].parent / "logistics-mc-26.2"
    parser.add_argument(
        "--repo-root", type=Path, default=default_repo_root,
        help="mod worktree to read recipe/asset data from (default: sibling ../logistics-mc-26.2, "
        "matching render_blocks.py's convention -- pass another mod worktree to extract a different branch)",
    )
    parser.add_argument("-v", "--verbose", action="store_true")
    args = parser.parse_args()

    repo_root = args.repo_root
    recipe_dir = repo_root / "common/src/main/resources/data/logistics/recipe"
    data_root = repo_root / "common/src/main/resources/data"
    out_dir = Path(__file__).resolve().parent / "data"
    out_dir.mkdir(exist_ok=True)

    recipes = extract_recipes(recipe_dir, args.verbose)

    tag_cache = {}
    for tag_id in sorted(collect_tag_ids(recipes)):
        load_tag_members(tag_id, data_root, tag_cache)

    items = build_items_table(recipes, tag_cache, repo_root, args.verbose)

    (out_dir / "recipes.json").write_text(json.dumps(recipes, indent=2, sort_keys=False) + "\n")
    (out_dir / "items.json").write_text(json.dumps(items, indent=2, sort_keys=True) + "\n")

    print(f"wrote {len(recipes)} recipes, {len(items)} items to {out_dir}", file=sys.stderr)


if __name__ == "__main__":
    main()
