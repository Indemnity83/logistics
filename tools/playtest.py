"""Launch a dev client on one or more PRs, then report what the mod logged.

Usage:
    python3 tools/playtest.py <PR|mc/branch> [<PR> ...] [--loader fabric|neoforge] [--prepare-only]

The client runs in a playtest worktree per Minecraft version, `../logistics-playtest-<version>`,
created on first use. The PR's head is checked out *detached* there, so a branch that is checked
out in another worktree (an agent's, say) needs no freeing, and the version keeps one world from
session to session. Several PRs, all against the same base, are merged locally in that worktree
(never pushed) so one world holds every feature under test; a conflict between them stops the run
and names the PRs, which is also what shipping them in sequence will hit.

The command blocks until the client closes, then prints the mod's warnings, errors and exceptions,
leaving out known noise, plus the saved blocks and items the world dropped because this build does
not register them. The full log stays under `build/playtest/` in the playtest worktree.
`--prepare-only` sets up the checkout (or reports the merge conflict) without launching.
Pure stdlib; needs `gh` and `git`.
"""

import datetime
import json
import os
import re
import subprocess
import sys

LOADERS = ("fabric", "neoforge")

# Lines that mention the mod but are understood and harmless; counted, not listed.
KNOWN = [
    re.compile(r"Texture logistics:\S+ with size \d+x\d+ limits mip level"),
    re.compile(r"duplicate items were found in 'Logistics"),
    re.compile(r"Mod logistics uses the version"),
    re.compile(r"Invalid icon for world|Couldn't load server icon|Invalid world icon size"),
]

LEVEL = re.compile(r"/(ERROR|WARN)\]")
UNKNOWN_ID = re.compile(r"Unknown registry key in ResourceKey\[minecraft:root / minecraft:(block|item)\]: (logistics:[\w/]+)")
TIMESTAMP = re.compile(r"^\[(\d{2}:\d{2}:\d{2})\]")
# A dropped block leaves its saved block entity standing on air.
ORPHANED_ENTITY = re.compile(r"Invalid block entity logistics:\S+ .* got Block\{minecraft:air\}")


def run(*args, cwd=None, check=True):
    result = subprocess.run(args, cwd=cwd, capture_output=True, text=True)
    if check and result.returncode != 0:
        sys.stderr.write(result.stdout + result.stderr)
        sys.exit(f"playtest: `{' '.join(args)}` failed")
    return result


def resolve(target):
    """(base branch, head ref to check out, label) for a PR number or an mc/* branch."""
    if target.isdigit():
        pr = json.loads(run("gh", "pr", "view", target, "--json", "headRefName,baseRefName,state").stdout)
        if pr["state"] != "OPEN":
            sys.exit(f"playtest: #{target} is {pr['state']}")
        return pr["baseRefName"], pr["headRefName"], f"#{target}"
    if target.startswith("mc/"):
        return target, target, target
    sys.exit(f"playtest: '{target}' is neither a PR number nor an mc/* branch")


def playtest_worktree(base):
    common = run("git", "rev-parse", "--path-format=absolute", "--git-common-dir").stdout.strip()
    here = run("git", "rev-parse", "--show-toplevel").stdout.strip()
    path = os.path.join(os.path.dirname(here), "logistics-playtest-" + base.removeprefix("mc/"))
    if not os.path.isdir(path):
        run("git", "fetch", "-q", "origin", base)
        run("git", "--git-dir", common, "worktree", "add", "-q", "--detach", path, f"origin/{base}")
    elif run("git", "status", "--porcelain", "--untracked-files=no", cwd=path).stdout.strip():
        sys.exit(f"playtest: {path} has uncommitted changes; resolve them first")
    return path


def checkout(path, heads, labels):
    run("git", "fetch", "-q", "origin", *heads, cwd=path)
    run("git", "switch", "-q", "--detach", f"origin/{heads[0]}", cwd=path)
    for head, label in zip(heads[1:], labels[1:]):
        merged = run("git", "-c", "user.name=playtest", "-c", "user.email=playtest@localhost",
                     "merge", "-q", "--no-edit", f"origin/{head}", cwd=path, check=False)
        if merged.returncode != 0:
            conflicted = run("git", "diff", "--name-only", "--diff-filter=U", cwd=path, check=False).stdout.split()
            run("git", "merge", "--abort", cwd=path, check=False)
            sys.exit(f"playtest: {label} conflicts with {', '.join(labels[:labels.index(label)])} in:\n  "
                     + "\n  ".join(conflicted))


def report(log, returncode):
    lines = open(log, encoding="utf-8", errors="replace").read().splitlines()
    times = [m.group(1) for m in (TIMESTAMP.match(line) for line in lines) if m]
    findings, known, dropped, exceptions, orphaned = [], 0, {}, [], 0
    header = None
    for line in lines:
        if not line.startswith(("\t", " ")):
            header = line
        if (m := UNKNOWN_ID.search(line)):
            dropped.setdefault(m.group(2), set()).add(m.group(1))
            continue
        if "at com.logistics" in line or "at knot//com.logistics" in line:
            if header and ORPHANED_ENTITY.search(header):
                orphaned += 1
            elif header and header not in exceptions:
                exceptions.append(header)
            continue
        if LEVEL.search(line) and "logistics" in line.lower():
            if any(p.search(line) for p in KNOWN):
                known += 1
            elif "Recoverable errors when loading section" not in line and "Failed to create block entity" not in line:
                findings.append(TIMESTAMP.sub("", line).strip())

    print(f"\n=== playtest report ({times[0] if times else '?'} -> {times[-1] if times else '?'}, "
          f"gradle exit {returncode}) ===")
    print(f"log: {log}")
    if dropped:
        print("\nSaved content this build does not register, dropped from the world:")
        for key in sorted(dropped):
            print(f"  {key} ({', '.join(sorted(dropped[key]))})")
        if orphaned:
            print("  (their block entities were discarded with them)")
    print(f"\nMod warnings and errors ({len(findings)}, plus {known} known):")
    for finding in dict.fromkeys(findings):
        print(f"  {finding}")
    print(f"\nExceptions through mod code ({len(exceptions)}):")
    for exception in exceptions:
        print(f"  {exception.strip()}")


def main():
    args = sys.argv[1:]
    loader = "fabric"
    prepare_only = "--prepare-only" in args
    args = [a for a in args if a != "--prepare-only"]
    if "--loader" in args:
        i = args.index("--loader")
        loader = args[i + 1] if i + 1 < len(args) else ""
        del args[i:i + 2]
    if loader not in LOADERS or not args:
        sys.exit(__doc__)

    resolved = [resolve(target) for target in args]
    bases = {base for base, _, _ in resolved}
    if len(bases) != 1:
        sys.exit(f"playtest: these target different branches ({', '.join(sorted(bases))}); "
                 "playtest one Minecraft version at a time")
    base = bases.pop()
    heads = [head for _, head, _ in resolved]
    labels = [label for _, _, label in resolved]

    path = playtest_worktree(base)
    checkout(path, heads, labels)
    if prepare_only:
        head = run("git", "log", "-1", "--format=%h %s", cwd=path).stdout.strip()
        print(f"playtest: {' + '.join(labels)} on {base} ready in {path} at {head}")
        return
    stamp = datetime.datetime.now().strftime("%Y%m%d-%H%M%S")
    log = os.path.join(path, "build", "playtest", f"{stamp}-{loader}.log")
    os.makedirs(os.path.dirname(log), exist_ok=True)

    print(f"playtest: {' + '.join(labels)} on {base}, {loader} client, in {path}\nlog: {log}", flush=True)
    with open(log, "w", encoding="utf-8") as out:
        code = subprocess.run(["./gradlew", f":{loader}:runClient"], cwd=path, stdout=out,
                              stderr=subprocess.STDOUT).returncode
    report(log, code)


if __name__ == "__main__":
    main()
