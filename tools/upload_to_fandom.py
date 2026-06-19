#!/usr/bin/env python3
"""
Upload the Logistics wiki (pages + media) to a Fandom / MediaWiki site via the API.

It does two jobs:
  * Pages  — every `wiki/*.txt` becomes one page. Filename (minus `.txt`) is the title,
             `Template_X.txt` -> `Template:X`, and `main.txt` -> `Main Page`.
  * Media  — every `wiki/media/*.png` is uploaded as `File:<filename>` (the files are
             already named `Grid <Name>.png`). `--used-only` limits to icons the pages
             reference. (Legacy: `--from-map` uploads via `ASSET_UPLOAD_MAP.md` instead.)

Both passes are idempotent and safe to re-run:
  * pages skip when the on-wiki text already matches (unless --force),
  * media skip when the on-wiki file's SHA1 already matches the local file (unless --force).
Interrupted runs just resume — already-done items are skipped on the next run.

------------------------------------------------------------------------------------
SETUP
  1. pip install requests
  2. On your wiki, go to  Special:BotPasswords , create a bot with the
     "Edit existing pages", "Create, edit, and move pages", and "Upload new files /
     Upload, replace, and move files" grants. You get a username like
     `YourName@logisticsbot` and a generated password.
  3. Export them (avoids leaking creds into shell history):
        export FANDOM_USER='YourName@logisticsbot'
        export FANDOM_PASSWORD='the-generated-password'

USAGE
  # dry-run everything first (no writes):
  python tools/upload_to_fandom.py --site https://yourwiki.fandom.com --all --dry-run

  # 1) pages (creates the 4 templates first, then Main Page, then content):
  python tools/upload_to_fandom.py --site https://yourwiki.fandom.com --pages

  # 2) media — uploads wiki/media/*.png as File:<name> (generate it first with
  #    tools/upscale_icons.py). --used-only limits to icons the pages reference:
  python tools/upload_to_fandom.py --site https://yourwiki.fandom.com --media
  python tools/upload_to_fandom.py --site https://yourwiki.fandom.com --media --used-only

NOTES
  * Uploads are sequential with a throttle (--delay) and exponential backoff on rate
    limits — Fandom will captcha/refuse parallel floods, so don't lower --delay much.
  * Non-English Fandom wikis live at .../<lang>/api.php — pass --api explicitly then.
"""

import argparse
import hashlib
import os
import re
import sys
import time

try:
    import requests
except ImportError:
    sys.exit("This script needs 'requests'.  Install it with:  pip install requests")


USER_AGENT = "LogisticsWikiUploader/1.0 (https://github.com/Indemnity83/logistics; bot)"
WRITE_OK = True  # flipped off by --dry-run


# --------------------------------------------------------------------------- API

class Wiki:
    def __init__(self, api_url, delay=1.0, maxlag=5, max_retries=6):
        self.api = api_url
        self.delay = delay
        self.maxlag = maxlag
        self.max_retries = max_retries
        self.csrf = "+\\"
        self.s = requests.Session()
        self.s.headers["User-Agent"] = USER_AGENT

    def _call(self, params, post=False, files_factory=None):
        """One API call with retry/backoff. files_factory() returns a fresh files dict."""
        params = dict(params, format="json", formatversion="2")
        attempt = 0
        while True:
            attempt += 1
            try:
                if post or files_factory:
                    files = files_factory() if files_factory else None
                    r = self.s.post(self.api, data=params, files=files, timeout=60)
                else:
                    r = self.s.get(self.api, params=params, timeout=60)
                if r.status_code in (429, 502, 503):
                    raise _Retry(r.headers.get("Retry-After"))
                r.raise_for_status()
                data = r.json()
            except (requests.RequestException, _Retry, ValueError) as e:
                if attempt > self.max_retries:
                    raise
                wait = _retry_after(e) or min(2 ** attempt, 30)
                print(f"    … transient error ({e}); retry {attempt} in {wait}s")
                time.sleep(wait)
                continue

            err = data.get("error")
            if err and err.get("code") in ("maxlag", "ratelimited"):
                if attempt > self.max_retries:
                    raise RuntimeError(f"API error: {err}")
                wait = min(2 ** attempt, 30)
                print(f"    … {err['code']}; backing off {wait}s")
                time.sleep(wait)
                continue
            return data

    def login(self, user, password):
        tok = self._call({"action": "query", "meta": "tokens", "type": "login"})
        token = tok["query"]["tokens"]["logintoken"]
        res = self._call({"action": "login", "lgname": user,
                          "lgpassword": password, "lgtoken": token}, post=True)
        if res.get("login", {}).get("result") != "Success":
            sys.exit(f"Login failed: {res.get('login')}\n"
                     "Check the bot username (Name@botname) and password from Special:BotPasswords.")
        self.csrf = self._call({"action": "query", "meta": "tokens"})["query"]["tokens"]["csrftoken"]
        print(f"Logged in as {user}")

    # -- reads --
    def page_text(self, title):
        d = self._call({"action": "query", "prop": "revisions", "rvprop": "content",
                        "rvslots": "main", "titles": title})
        pages = d.get("query", {}).get("pages", [])
        if not pages or pages[0].get("missing"):
            return None
        try:
            return pages[0]["revisions"][0]["slots"]["main"]["content"]
        except (KeyError, IndexError):
            return None

    def file_sha1(self, filename):
        d = self._call({"action": "query", "prop": "imageinfo", "iiprop": "sha1",
                        "titles": f"File:{filename}"})
        pages = d.get("query", {}).get("pages", [])
        if not pages or pages[0].get("missing"):
            return None
        try:
            return pages[0]["imageinfo"][0]["sha1"]
        except (KeyError, IndexError):
            return None

    # -- writes --
    def edit(self, title, text, summary, force=False):
        current = self.page_text(title)
        if current is not None and not force and current.rstrip("\n") == text.rstrip("\n"):
            return "unchanged"
        if not WRITE_OK:
            return "would-create" if current is None else "would-edit"
        data = {"action": "edit", "title": title, "text": text, "summary": summary,
                "token": self.csrf, "bot": "1", "assert": "user", "maxlag": self.maxlag}
        res = self._call(data, post=True)
        if "error" in res:
            if res["error"].get("code") == "badtoken":
                self.csrf = self._call({"action": "query", "meta": "tokens"})["query"]["tokens"]["csrftoken"]
                data["token"] = self.csrf
                res = self._call(data, post=True)
            if "error" in res:
                raise RuntimeError(f"edit {title}: {res['error']}")
        time.sleep(self.delay)
        return res.get("edit", {}).get("result", "ok").lower().replace("success", "edited")

    def upload(self, filename, path, summary, force=False):
        local = _sha1(path)
        if not force and self.file_sha1(filename) == local:
            return "skip"
        if not WRITE_OK:
            return "would-upload"
        data = {"action": "upload", "filename": filename, "comment": summary,
                "ignorewarnings": "1", "token": self.csrf, "assert": "user"}
        res = self._call(data, files_factory=lambda: {
            "file": (filename, open(path, "rb"), "application/octet-stream")})
        if "error" in res:
            if res["error"].get("code") == "badtoken":
                self.csrf = self._call({"action": "query", "meta": "tokens"})["query"]["tokens"]["csrftoken"]
                data["token"] = self.csrf
                res = self._call(data, files_factory=lambda: {
                    "file": (filename, open(path, "rb"), "application/octet-stream")})
            if "error" in res:
                raise RuntimeError(f"upload {filename}: {res['error']}")
        time.sleep(self.delay)
        return res.get("upload", {}).get("result", "ok").lower()


class _Retry(Exception):
    def __init__(self, retry_after=None):
        self.retry_after = retry_after


def _retry_after(e):
    if isinstance(e, _Retry) and e.retry_after:
        try:
            return int(e.retry_after)
        except ValueError:
            return None
    return None


def _sha1(path):
    h = hashlib.sha1()
    with open(path, "rb") as fh:
        for chunk in iter(lambda: fh.read(65536), b""):
            h.update(chunk)
    return h.hexdigest()


# ----------------------------------------------------------------------- mapping

def page_title(filename):
    stem = filename[:-4]  # drop .txt
    if stem == "main":
        return "Main Page"
    if stem.startswith("Template_"):
        return "Template:" + stem[len("Template_"):]
    return stem


def page_sort_key(filename):
    stem = filename[:-4]
    if stem.startswith("Template_"):
        return (0, stem)            # templates first — pages depend on them
    if stem == "main":
        return (1, stem)            # then the Main Page
    return (2, stem)


MAP_ROW = re.compile(r"^\|\s*`([^`]+\.png)`\s*\|\s*`File:([^`]+\.png)`\s*\|")


def parse_asset_map(map_path, icons_dir):
    """Return list of (target_filename, source_path) for rows whose source file exists."""
    pairs, seen, missing = [], set(), 0
    with open(map_path, encoding="utf-8") as fh:
        for line in fh:
            m = MAP_ROW.match(line)
            if not m:
                continue
            source, target = m.group(1), m.group(2)  # target already has File: stripped
            if target in seen:
                continue
            src_path = os.path.join(icons_dir, source)
            if not os.path.isfile(src_path):
                missing += 1
                continue
            seen.add(target)
            pairs.append((target, src_path))
    return pairs, missing


GRID_LITERAL = re.compile(r"Grid ([^\n|}\]=]+?)\.png")
GRID_TMPL = re.compile(r"\{\{Grid\|([^}|]+)")
GRID_TABLE = re.compile(r"\{\{Grid Crafting Table(.*?)\}\}", re.S)
TABLE_PARAM = re.compile(r"\|\s*(?:A[123]|B[123]|C[123]|Output)\s*=\s*([^|}\n]+)")


def referenced_files(wiki_dir):
    """Set of 'Grid <name>.png' filenames actually used across the wiki pages."""
    needed = set()
    for fn in os.listdir(wiki_dir):
        if not fn.endswith(".txt") or fn.startswith("Template_"):
            continue  # templates only hold placeholder examples, not real icon references
        text = open(os.path.join(wiki_dir, fn), encoding="utf-8").read()
        for name in GRID_LITERAL.findall(text):
            needed.add(f"Grid {name.strip()}.png")
        for name in GRID_TMPL.findall(text):
            needed.add(f"Grid {name.strip()}.png")
        for block in GRID_TABLE.findall(text):
            for val in TABLE_PARAM.findall(block):
                needed.add(f"Grid {val.strip()}.png")
    return needed


# -------------------------------------------------------------------------- main

def main():
    ap = argparse.ArgumentParser(description="Upload the Logistics wiki to Fandom/MediaWiki.")
    ap.add_argument("--site", help="Wiki base URL, e.g. https://yourwiki.fandom.com")
    ap.add_argument("--api", help="Full api.php URL (overrides --site; needed for /<lang>/ wikis)")
    ap.add_argument("--user", default=os.environ.get("FANDOM_USER"))
    ap.add_argument("--password", default=os.environ.get("FANDOM_PASSWORD"))
    ap.add_argument("--pages", action="store_true", help="upload wiki/*.txt pages")
    ap.add_argument("--media", action="store_true", help="upload icons from the asset map")
    ap.add_argument("--all", action="store_true", help="pages and media")
    ap.add_argument("--used-only", action="store_true",
                    help="media: upload only icons referenced by the pages")
    ap.add_argument("--force", action="store_true", help="re-write/re-upload even if unchanged")
    ap.add_argument("--dry-run", action="store_true", help="show actions, write nothing")
    ap.add_argument("--delay", type=float, default=1.0, help="seconds between writes (default 1.0)")
    repo = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    ap.add_argument("--wiki-dir", default=os.path.join(repo, "wiki"))
    ap.add_argument("--media-dir", default=os.path.join(repo, "wiki", "media"),
                    help="folder of ready-named Grid <Name>.png files to upload")
    ap.add_argument("--from-map", action="store_true",
                    help="legacy: upload via ASSET_UPLOAD_MAP.md from --icons-dir instead of --media-dir")
    ap.add_argument("--icons-dir", default=os.path.join(repo, "docs", "assets", "icons"))
    ap.add_argument("--map", default=os.path.join(repo, "wiki", "ASSET_UPLOAD_MAP.md"))
    args = ap.parse_args()

    do_pages = args.pages or args.all
    do_media = args.media or args.all
    if not (do_pages or do_media):
        ap.error("choose --pages, --media, or --all")
    if not args.api and not args.site:
        ap.error("pass --site https://yourwiki.fandom.com (or --api .../api.php)")
    if not args.user or not args.password:
        ap.error("set FANDOM_USER / FANDOM_PASSWORD env vars (or --user/--password)")

    global WRITE_OK
    WRITE_OK = not args.dry_run
    api_url = args.api or args.site.rstrip("/") + "/api.php"

    wiki = Wiki(api_url, delay=args.delay)
    print(f"{'DRY-RUN — ' if args.dry_run else ''}Target: {api_url}")
    wiki.login(args.user, args.password)

    if do_pages:
        files = sorted([f for f in os.listdir(args.wiki_dir) if f.endswith(".txt")],
                       key=page_sort_key)
        print(f"\n=== Pages: {len(files)} ===")
        tally = {}
        for i, fn in enumerate(files, 1):
            title = page_title(fn)
            text = open(os.path.join(args.wiki_dir, fn), encoding="utf-8").read()
            try:
                result = wiki.edit(title, text, "Import from logistics wiki source", args.force)
            except Exception as e:
                result = "FAILED"
                print(f"  !! {title}: {e}")
            tally[result] = tally.get(result, 0) + 1
            print(f"  [{i}/{len(files)}] {result:12} {title}")
        print("  pages:", dict(sorted(tally.items())))

    if do_media:
        if args.from_map:
            pairs, missing = parse_asset_map(args.map, args.icons_dir)
            src_desc = f"{missing} map rows skipped — no local source file"
        else:
            pairs = []
            if os.path.isdir(args.media_dir):
                for fn in sorted(os.listdir(args.media_dir)):
                    if fn.lower().endswith(".png"):
                        pairs.append((fn, os.path.join(args.media_dir, fn)))
            else:
                print(f"  (no media dir at {args.media_dir})")
            src_desc = f"from {args.media_dir}"
        if args.used_only:
            needed = referenced_files(args.wiki_dir)
            pairs = [(t, p) for (t, p) in pairs if t in needed]
            print(f"\n=== Media (--used-only): {len(pairs)} of the referenced set ===")
        else:
            print(f"\n=== Media: {len(pairs)} icons ({src_desc}) ===")
        tally = {}
        for i, (target, src) in enumerate(pairs, 1):
            try:
                result = wiki.upload(target, src, "Import item icon", args.force)
            except Exception as e:
                result = "FAILED"
                print(f"  !! {target}: {e}")
            tally[result] = tally.get(result, 0) + 1
            if result != "skip" or i % 50 == 0:
                print(f"  [{i}/{len(pairs)}] {result:12} File:{target}")
        print("  media:", dict(sorted(tally.items())))

    print("\nDone.")


if __name__ == "__main__":
    main()
