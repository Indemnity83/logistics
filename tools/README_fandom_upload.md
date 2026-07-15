# Uploading the wiki to Fandom

`upload_to_fandom.py` pushes the `wiki/*.txt` pages and the item icons to a Fandom
(MediaWiki) site over the API. It is idempotent — re-running only sends what changed.

## 1. Requirements
Pure Python 3 standard library — no `pip install`, no virtualenv.

## 2. Create a bot password
On your wiki, open **`Special:BotPasswords`** and create a bot with these grants:
- Edit existing pages
- Create, edit, and move pages
- Upload new files / Upload, replace, and move files

You'll get a username like `YourName@logisticsbot` and a generated password. Export them
so they don't land in your shell history:
```bash
export FANDOM_USER='YourName@logisticsbot'
export FANDOM_PASSWORD='the-generated-password'
```

## 3. Run it (recommended order)
Replace the URL with your wiki. Always dry-run first.

```bash
# preview everything, write nothing
python tools/upload_to_fandom.py --site https://yourwiki.fandom.com --all --dry-run

# 1) pages — uploads the 4 templates FIRST, then Main Page, then all content
python tools/upload_to_fandom.py --site https://yourwiki.fandom.com --pages

# 2) media — only the ~130 icons the pages actually reference (fast, high-value)
python tools/upload_to_fandom.py --site https://yourwiki.fandom.com --media --used-only

# 3) media — the full ~1,850-icon set (so every recipe ingredient has an icon)
python tools/upload_to_fandom.py --site https://yourwiki.fandom.com --media
```

## How things map
| Source | Becomes |
| --- | --- |
| `wiki/Item Extractor Pipe.txt` | page **Item Extractor Pipe** |
| `wiki/Template_Grid.txt` | **Template:Grid** |
| `wiki/main.txt` | the wiki's **configured main page** (from siteinfo, e.g. *Logistics Wiki*) — gets the full-width, no-rail main-page layout |
| `wiki/Template_Grid.styles.txt` | **Template:Grid/styles.css** |
| `wiki/Module_ItemLink.txt` | **Module:ItemLink** |
| `wiki/media/Grid <Name>.png` | **File:Grid \<Name\>.png** (produced by the icon tools) |

## Good to know
- **Idempotent:** pages skip when the on-wiki text already matches; icons skip when the
  on-wiki file's SHA1 matches the local file. Interrupted runs resume cleanly.
- **Throttled:** writes are sequential with a `--delay` (default 1s) and back off on rate
  limits. Don't parallelize — Fandom will captcha/refuse floods.
- **Pages work before icons exist:** `{{Grid}}` falls back to a text link via `#ifexist`,
  so you can do the pages pass and fill in media later.
- **Icons** are produced into `wiki/media/` by the companion tools (`render_blocks.py`,
  `upscale_icons.py`, `vanilla_icons.py`, `fluid_icons.py`); anything without a rendered
  `Grid <Name>.png` simply stays a text-link fallback until its icon is produced and uploaded.
- **Non-English wikis** live at `.../<lang>/api.php`; pass `--api` explicitly then.

## Flags
`--all` (pages+media) · `--pages` · `--media` · `--used-only` · `--force` (re-send even if
unchanged) · `--dry-run` · `--delay SECONDS` · `--site` / `--api` · `--user` / `--password`
(or the env vars) · `--wiki-dir` / `--media-dir` (path overrides).
