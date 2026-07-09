#!/usr/bin/env python3
"""Generate wiki/Module_ItemLink.txt — the mod-item name set that decides whether a recipe item's
icon/text links to a local wiki page (mod item) or out to the Minecraft Wiki (vanilla item).

The set is the mod's own item/block/fluid display names from en_us.json. Using a name set (a Lua
table lookup) instead of {{#ifexist:}} keeps the link decision OFF the page's expensive-parser-
function budget (Fandom caps it at 100/page), so large recipe tables don't hit the limit.

Regenerate whenever items are added:
  python tools/gen_item_links.py            # default 26.2 assets
  python tools/gen_item_links.py --assets <assets/logistics>
"""
import argparse
import json
import os
import sys

MODULE = '''\
-- Module:ItemLink — routes a recipe item's icon/text link to a local page (mod item) or to the
-- Minecraft Wiki (vanilla item). Mod items link to their own page; everything else links to
-- minecraft.fandom.com — a full URL for an image link= (which can't take an interwiki), or the
-- w:c:minecraft: interwiki for a text link.
--
-- The MOD set below is GENERATED from the mod's en_us.json by tools/gen_item_links.py; regenerate
-- it when items are added. A table lookup keeps this off the page's expensive-parser-function
-- budget, unlike {{#ifexist:}}.

local p = {}

local MCW = 'https://minecraft.fandom.com/wiki/'

local MOD = {
%s
}

-- link= target for an item: its own page name (mod item) or a full Minecraft Wiki URL (vanilla).
function p.targetFor(name)
\tname = mw.text.trim(name or '')
\tif name == '' then return '' end
\tif MOD[name] then return name end
\treturn MCW .. mw.uri.encode(name, 'WIKI')
end

-- a full wikitext link for an item: [[Page]] (mod) or the w:c:minecraft: interwiki (vanilla).
function p.linkFor(name)
\tname = mw.text.trim(name or '')
\tif name == '' then return '' end
\tif MOD[name] then return '[[' .. name .. ']]' end
\treturn '[[w:c:minecraft:' .. name .. '|' .. name .. ']]'
end

local function arg1(frame)
\treturn frame.args[1] or (frame:getParent() and frame:getParent().args[1]) or ''
end

function p.target(frame) return p.targetFor(arg1(frame)) end
function p.link(frame) return p.linkFor(arg1(frame)) end

return p
'''


def main():
    repo = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    default_assets = os.path.normpath(os.path.join(
        repo, "..", "logistics-mc-26.2", "common", "src", "main", "resources", "assets", "logistics"))
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--assets", default=default_assets)
    ap.add_argument("--out", default=os.path.join(repo, "wiki", "Module_ItemLink.txt"))
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()

    lang_path = os.path.join(args.assets, "lang", "en_us.json")
    if not os.path.isfile(lang_path):
        sys.exit(f"lang not found: {lang_path}\nPass --assets <path to assets/logistics>.")
    lang = json.load(open(lang_path, encoding="utf-8"))

    names = set()
    for k, v in lang.items():
        if k.startswith(("item.logistics.", "block.logistics.", "fluid.logistics.")):
            v = v.strip()
            if v:
                names.add(v)

    rows = "\n".join(f'\t[{json.dumps(n)}] = true,' for n in sorted(names))
    text = MODULE % rows

    if args.dry_run:
        print(f"would write {len(names)} mod names -> {args.out}")
    else:
        open(args.out, "w", encoding="utf-8").write(text)
        print(f"wrote {len(names)} mod names -> {args.out}")


if __name__ == "__main__":
    main()
