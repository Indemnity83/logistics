"""Squash-merge a PR with its changelog lines, read from the PR body.

A PR with several player-facing changes lists every changelog line, the PR title first, in an
override block in its body:

    BEGIN_COMMIT_OVERRIDE
    feat(automation): add the sawmill (#1234)
    change(automation): rename Wood Pulp to Sawdust (#1234)
    END_COMMIT_OVERRIDE

Release Please reads that block for the branch the PR merged into. A ported commit is not linked to
the PR it came from, so the other branches read the commit message instead: this script writes the same
lines into the squash commit, the title as its subject and the rest as its body. Every line gets
the PR's `(#N)`, in the PR body too, so each entry links back to the PR.

A PR with no block ships with its title alone.

Usage: python3 tools/ship_pr.py <PR number> [--dry-run]

Ships with `git town ship` when the PR's branch is checked out here, else with `gh pr merge`.
Pure stdlib; needs `gh` and `git` (and `git town` for the first path).
"""

import json
import re
import subprocess
import sys

BEGIN = "BEGIN_COMMIT_OVERRIDE"
END = "END_COMMIT_OVERRIDE"
ENTRY = re.compile(r"^[a-z]+(\([a-z0-9-]+\))?!?: \S")


def run(*args, capture=True):
    result = subprocess.run(args, capture_output=capture, text=True)
    if result.returncode != 0:
        if capture:
            sys.stderr.write(result.stderr)
        sys.exit(f"ship_pr: `{' '.join(args)}` failed")
    return result.stdout if capture else ""


def override_lines(body):
    """The block's non-empty lines, or None when the body has no block.

    Release Please starts the block at the first BEGIN anywhere in the body, so a BEGIN in prose,
    or a second block, would hijack the changelog: both are refused.
    """
    lines = body.replace("\r", "").split("\n")
    if body.count(BEGIN) == 0:
        return None
    if body.count(BEGIN) > 1 or [line.strip() for line in lines].count(BEGIN) != 1:
        sys.exit(f"ship_pr: {BEGIN} must appear exactly once, alone on its line, opening the block")
    if [line.strip() for line in lines].count(END) != 1:
        sys.exit(f"ship_pr: the block must close with {END} alone on its line")
    block = body.split(BEGIN, 1)[1].split(END, 1)[0]
    return [line.strip() for line in block.replace("\r", "").split("\n") if line.strip()]


def with_ref(line, ref):
    return line if line.endswith(ref) else f"{line} {ref}"


def without_ref(line, ref):
    return line[: -len(ref)].rstrip() if line.endswith(ref) else line


def message(title, lines, number):
    """The squash subject and body for a PR titled `title` with override `lines` (or None)."""
    ref = f"(#{number})"
    subject = with_ref(title, ref)
    if lines is None:
        return subject, ""
    bad = [line for line in lines if not ENTRY.match(line)]
    if bad:
        sys.exit("ship_pr: not a `type(scope): description` line:\n  " + "\n  ".join(bad))
    if not lines or without_ref(lines[0], ref) != without_ref(title, ref):
        sys.exit(f"ship_pr: the override block must start with the PR title:\n  {title}")
    return subject, "\n".join(with_ref(line, ref) for line in lines[1:])


def linked_body(body, number):
    """`body` with `(#N)` on every override line."""
    ref = f"(#{number})"
    head, rest = body.split(BEGIN, 1)
    block, tail = rest.split(END, 1)
    lines = [with_ref(line.strip(), ref) if line.strip() else line for line in block.replace("\r", "").split("\n")]
    return head + BEGIN + "\n".join(lines) + END + tail


def main():
    args = [a for a in sys.argv[1:] if a != "--dry-run"]
    dry_run = "--dry-run" in sys.argv[1:]
    if len(args) != 1 or not args[0].isdigit():
        sys.exit(__doc__)
    number = args[0]

    pr = json.loads(run("gh", "pr", "view", number, "--json", "title,body,headRefName,baseRefName,state"))
    if pr["state"] != "OPEN":
        sys.exit(f"ship_pr: #{number} is {pr['state']}")
    lines = override_lines(pr["body"])
    subject, body = message(pr["title"], lines, number)
    here = run("git", "branch", "--show-current").strip()
    path = "git town ship" if here == pr["headRefName"] else "gh pr merge --squash"

    print(f"#{number} -> {pr['baseRefName']} via {path}\n---\n{subject}" + (f"\n\n{body}" if body else ""))
    if dry_run:
        return

    if lines is not None:
        fixed = linked_body(pr["body"], number)
        if fixed != pr["body"]:
            run("gh", "pr", "edit", number, "--body", fixed)

    if path == "git town ship":
        run("git", "town", "ship", "-m", subject + (f"\n\n{body}" if body else ""), capture=False)
    else:
        run("gh", "pr", "merge", number, "--squash", "--subject", subject, "--body", body, capture=False)

    run("git", "fetch", "-q", "origin", pr["baseRefName"])
    print(run("git", "log", "-1", "--format=landed %h%n%s%n---%n%b", f"origin/{pr['baseRefName']}"))


if __name__ == "__main__":
    main()
