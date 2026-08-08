"""In-memory recipe graph built from `data/items.json` + `data/recipes.json`.

Importable library, no CLI of its own -- see `balance.py` and `diagram.py`
for the tools built on top of it. Run `extract.py` first to (re)generate the
two JSON tables this module reads.

Core model: items are nodes, recipes are hyperedges (multiple inputs to
multiple outputs). A recipe's byproduct is treated as an extra output edge
with an associated `chance`. `#tag` inputs are expanded to their concrete
member items (via `items.json`'s `members` list) so querying a concrete
item's consumers also finds recipes that consume it only through a tag.

Pure stdlib; no external dependencies.
"""

import json
from pathlib import Path

DATA_DIR = Path(__file__).resolve().parent / "data"


class RecipeGraph:
    def __init__(self, items, recipes):
        self.items = items  # id -> item record
        self.recipes = {r["recipe_id"]: r for r in recipes}

        # item id -> set of recipe ids producing / consuming it (concrete
        # items only; tag membership is resolved on the fly in producers()
        # /consumers() via tag_consumers/tag_producers below).
        self._produced_by = {}
        self._consumed_by = {}
        self._tag_consumed_by = {}  # tag id -> set of recipe ids

        for r in recipes:
            rid = r["recipe_id"]
            for entry in r["outputs"]:
                self._produced_by.setdefault(entry["id"], set()).add(rid)
            if r["byproduct"]:
                self._produced_by.setdefault(r["byproduct"]["id"], set()).add(rid)
            for entry in r["inputs"]:
                if entry["kind"] == "tag":
                    self._tag_consumed_by.setdefault(entry["id"], set()).add(rid)
                else:
                    self._consumed_by.setdefault(entry["id"], set()).add(rid)

        # concrete item id -> set of tag ids it's a member of (only tags we
        # could resolve locally -- external tags can't be expanded).
        self._member_of_tags = {}
        for tag_id, item in self.items.items():
            if item["kind"] != "tag" or not tag_id.startswith("#"):
                continue
            for member_id in item.get("members") or []:
                self._member_of_tags.setdefault(member_id, set()).add(tag_id[1:])

    @classmethod
    def load(cls, data_dir=DATA_DIR):
        items = json.loads((data_dir / "items.json").read_text())
        recipes = json.loads((data_dir / "recipes.json").read_text())
        return cls(items, recipes)

    def item(self, item_id):
        return self.items.get(item_id)

    def recipe(self, recipe_id):
        return self.recipes.get(recipe_id)

    def producers(self, item_id):
        """Recipe records that output (or byproduct-output) `item_id`."""
        return [self.recipes[rid] for rid in sorted(self._produced_by.get(item_id, ()))]

    def consumers(self, item_id):
        """Recipe records that consume `item_id`, directly or via a tag it belongs to."""
        rids = set(self._consumed_by.get(item_id, ()))
        for tag_id in self._member_of_tags.get(item_id, ()):
            rids |= self._tag_consumed_by.get(f"#{tag_id}", set())
        return [self.recipes[rid] for rid in sorted(rids)]

    def _walk(self, item_id, expand, depth, exclude=frozenset()):
        """BFS from item_id; expand(item_id) -> iterable of (recipe, item_id) edges.

        `exclude` prunes traversal at the given item ids -- neither the item
        nor the edge into it is added. Useful for hub items with very wide
        fan-out (e.g. `minecraft:bucket`, shared by every fluid's Transposer
        fill/empty recipe) that would otherwise dominate the diagram.
        """
        visited_items = {item_id}
        visited_recipes = set()
        frontier = [item_id]
        level = 0
        while frontier and (depth is None or level < depth):
            next_frontier = []
            for current in frontier:
                for recipe, neighbor in expand(current):
                    if neighbor in exclude:
                        continue
                    visited_recipes.add(recipe["recipe_id"])
                    if neighbor not in visited_items:
                        visited_items.add(neighbor)
                        next_frontier.append(neighbor)
            frontier = next_frontier
            level += 1
        return visited_items, visited_recipes

    def ancestors(self, item_id, depth=None, exclude=frozenset()):
        """(items, recipes) reachable by walking backward through producers -- "how do I make this"."""

        def expand(current):
            for recipe in self.producers(current):
                for entry in recipe["inputs"]:
                    if entry["kind"] == "tag":
                        for member_id in (self.items.get(entry["id"], {}) or {}).get("members") or []:
                            yield recipe, member_id
                    else:
                        yield recipe, entry["id"]

        return self._walk(item_id, expand, depth, exclude)

    def descendants(self, item_id, depth=None, exclude=frozenset()):
        """(items, recipes) reachable by walking forward through consumers -- "what can this become".

        Includes byproduct edges, so e.g. Bitumen -> Tar (macerator byproduct)
        -> sticky piston / fluid pipe crafting (Tar consumers) shows up here.
        """

        def expand(current):
            for recipe in self.consumers(current):
                for entry in recipe["outputs"]:
                    yield recipe, entry["id"]
                if recipe["byproduct"]:
                    yield recipe, recipe["byproduct"]["id"]

        return self._walk(item_id, expand, depth, exclude)

    def cheapest_chain(self, item_id, _stack=None):
        """Recursive RF-cost-per-unit rollup to raw/base resources.

        Returns {"item": id, "rf_per_unit": rf_or_None, "recipe": recipe_id_or_None,
        "inputs": [subchains...]}, normalized to the cost of *one* unit of
        `item_id` (one item, or 1mB for a fluid) -- a recipe's per-run cost is
        divided by how many units it outputs, so e.g. a 4-bitumen-per-run
        macerator recipe correctly costs 1/4 per bitumen, not the whole run.
        Picks the cheapest producing recipe at each step. Base resources (no
        producing recipe) get rf_per_unit=0.

        An item only ever produced as a *byproduct* (chance < 1) is priced by
        dividing the run cost by that chance -- an upper-bound approximation
        that treats the byproduct as the recipe's sole reason to run (it
        ignores the primary output's value, so it overstates cost when the
        primary output is also useful).

        A recipe whose own inputs recurse back to `item_id` (e.g. the
        Transposer's bucket-empty/fill pair, which shares one resource id
        between the fluid and its bucket item -- see extract.py's
        `also_item_form`) is excluded rather than treated as a free/zero-cost
        loop; rf_per_unit=None means every producer was circular or unpriced.
        """
        stack = _stack or set()
        if item_id in stack:
            return {"item": item_id, "rf_per_unit": None, "recipe": None, "inputs": []}
        producers = [r for r in self.producers(item_id) if r["energy_rf"] is not None]
        if not producers:
            return {"item": item_id, "rf_per_unit": 0, "recipe": None, "inputs": []}

        best = None
        for recipe in producers:
            out_entry = next((e for e in recipe["outputs"] if e["id"] == item_id), None)
            byproduct_chance = None
            if out_entry is not None:
                out_amount = out_entry.get("count") or out_entry.get("amount") or 1
            elif recipe["byproduct"] and recipe["byproduct"]["id"] == item_id:
                out_amount = 1
                byproduct_chance = recipe["byproduct"]["chance"]
            else:
                continue

            subchains = []
            circular = False
            run_rf = recipe["energy_rf"]
            for entry in recipe["inputs"]:
                if entry["kind"] == "tag":
                    continue  # ambiguous which member; cost left unresolved for this input
                sub = self.cheapest_chain(entry["id"], stack | {item_id})
                subchains.append(sub)
                if sub["rf_per_unit"] is None:
                    circular = True
                    continue
                input_amount = entry.get("count") or entry.get("amount") or 1
                run_rf += input_amount * sub["rf_per_unit"]
            if circular:
                continue

            if byproduct_chance:
                run_rf = run_rf / byproduct_chance
            rf_per_unit = run_rf / out_amount
            if best is None or rf_per_unit < best["rf_per_unit"]:
                best = {
                    "item": item_id,
                    "rf_per_unit": rf_per_unit,
                    "recipe": recipe["recipe_id"],
                    "inputs": subchains,
                }
        return best or {"item": item_id, "rf_per_unit": None, "recipe": None, "inputs": []}

    def machine_ratios(self, item_id, rate_per_min):
        """Machine-count solve for a target output rate, walking the cheapest chain.

        Returns a flat list of {"item", "recipe", "machine", "rate_per_min",
        "machines_needed"} rows, one per recipe in the cheapest chain, sized
        to sustain `rate_per_min` of `item_id`. Requires every recipe's
        `time_ticks` to be known; a machine produces `output_count *
        (1200 / time_ticks)` per minute (1200 ticks/min at 20 tps).
        """
        chain = self.cheapest_chain(item_id)
        rows = []

        def visit(node, needed_per_min):
            if not node["recipe"]:
                return
            recipe = self.recipes[node["recipe"]]
            out_entry = next((e for e in recipe["outputs"] if e["id"] == node["item"]), None)
            if out_entry is None or not recipe["time_ticks"]:
                return
            output_per_run = out_entry.get("count") or out_entry.get("amount") or 1
            runs_per_min = 1200 / recipe["time_ticks"]
            output_per_min_per_machine = output_per_run * runs_per_min
            machines_needed = needed_per_min / output_per_min_per_machine
            rows.append(
                {
                    "item": node["item"],
                    "recipe": recipe["recipe_id"],
                    "machine": recipe["machine"],
                    "rate_per_min": needed_per_min,
                    "machines_needed": round(machines_needed, 3),
                }
            )
            for entry, subchain in zip((e for e in recipe["inputs"] if e["kind"] != "tag"), node["inputs"]):
                input_amount = entry.get("count") or entry.get("amount") or 1
                per_run_needed = input_amount / output_per_run
                visit(subchain, needed_per_min * per_run_needed)

        visit(chain, rate_per_min)
        return rows
