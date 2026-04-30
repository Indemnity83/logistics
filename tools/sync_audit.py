#!/usr/bin/env python3
"""
Branch sync audit script.

Compares commit subjects across the three Logistics release branches and reports
any commits that appear on some branches but not others (potential missed ports).

Usage:
    python tools/sync_audit.py

Requires all three branch worktrees to exist at their expected paths relative to
this repository:
    ../logistics-mc-1.21.11   (mc/1.21.11 branch)
    ../logistics-mc-1.21.1    (mc/1.21.1 branch)
    ../logistics-mc-26.1      (mc/26.1 branch)
"""

import re
import shutil
import subprocess
import sys
from pathlib import Path

GIT_BIN = shutil.which("git")
if not GIT_BIN:
    print("ERROR: git not found on PATH — cannot run sync audit.", file=sys.stderr)
    raise SystemExit(1)

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------

SCRIPT_DIR = Path(__file__).parent.resolve()
REPO_ROOT = SCRIPT_DIR.parent

PARENT = REPO_ROOT.parent

WORKTREES = {
    "mc/1.21.11": PARENT / "logistics-mc-1.21.11",
    "mc/1.21.1":  PARENT / "logistics-mc-1.21.1",
    "mc/26.1":    PARENT / "logistics-mc-26.1",
}

# Path to the file that documents intentionally-diverged commit subjects.
# Each non-blank, non-comment line is treated as a known gap and suppressed.
KNOWN_GAPS_FILE = SCRIPT_DIR / "sync_known_gaps.txt"

# Subjects matching any of these patterns are branch-specific by design and
# are excluded from the gap report.
SKIP_PATTERNS = [
    # Release-please chore commits always differ between branches
    r"^chore: release \d+\.\d+\.\d+",
    # Branch-specific CI/tooling that can legitimately differ
    r"\b1\.21\.11\b",
    r"\b1\.21\.1\b",
    r"\b26\.1\b",
    r"\bmc1\.",
    r"\bmc26\.",
    # Java version bumps (each branch may be on a different Java)
    r"\bjava (version|21|25)\b",
    # loom version (MC 26.1 uses a different loom version)
    r"\bloom\b",
]

SKIP_RE = [re.compile(p, re.IGNORECASE) for p in SKIP_PATTERNS]

PR_REF_RE = re.compile(r" \(#\d+\)$")


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def git_current_branch(worktree: Path) -> str:
    """Return the name of the currently checked-out branch in worktree."""
    result = subprocess.run(  # noqa: S603
        [GIT_BIN, "rev-parse", "--abbrev-ref", "HEAD"],
        capture_output=True,
        text=True,
        cwd=worktree,
    )
    return result.stdout.strip() if result.returncode == 0 else ""


def git_log_subjects(worktree: Path) -> list[str]:
    """Return all commit subjects (no PR refs) for the worktree's current branch."""
    result = subprocess.run(  # noqa: S603
        [GIT_BIN, "log", "--format=%s", "--no-merges"],
        capture_output=True,
        text=True,
        cwd=worktree,
    )
    if result.returncode != 0:
        print(f"  ERROR: git log failed in {worktree}: {result.stderr.strip()}", file=sys.stderr)
        sys.exit(1)

    subjects = []
    for line in result.stdout.splitlines():
        subject = PR_REF_RE.sub("", line).strip()
        if subject:
            subjects.append(subject)
    return subjects


def load_known_gaps() -> set[str]:
    """Load intentionally-diverged commit subjects from sync_known_gaps.txt."""
    if not KNOWN_GAPS_FILE.exists():
        return set()
    known = set()
    for line in KNOWN_GAPS_FILE.read_text().splitlines():
        line = line.strip()
        if line and not line.startswith("#"):
            known.add(line)
    return known


def is_branch_specific(subject: str) -> bool:
    return any(pat.search(subject) for pat in SKIP_RE)


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main() -> int:
    print("Branch Sync Audit")
    print("=" * 60)

    # Verify worktrees exist and collect subjects
    subjects_by_branch: dict[str, set[str]] = {}
    missing_trees = []

    for branch, path in WORKTREES.items():
        if not path.exists():
            print(f"  ✗ {branch}: worktree not found at {path}")
            missing_trees.append(branch)
            continue
        actual_branch = git_current_branch(path)
        if actual_branch != branch:
            print(f"  ✗ {branch}: wrong branch checked out (expected '{branch}', got '{actual_branch}') — skipping")
            missing_trees.append(branch)
            continue
        subjects = git_log_subjects(path)
        subjects_by_branch[branch] = set(subjects)
        print(f"  ✓ {branch}: {len(subjects)} commits  ({path})")

    if missing_trees:
        print(f"\nERROR: {len(missing_trees)} worktree(s) not found or on wrong branch: {', '.join(missing_trees)}")
        return 1

    if len(subjects_by_branch) < 2:
        print("ERROR: Need at least two branches to compare.")
        return 1

    all_branches = list(subjects_by_branch.keys())
    all_subjects: set[str] = set()
    for subs in subjects_by_branch.values():
        all_subjects.update(subs)

    known_gaps = load_known_gaps()
    if known_gaps:
        print(f"  (suppressing {len(known_gaps)} known gap(s) from sync_known_gaps.txt)")

    # Find gaps: subjects present on ≥1 branch but not all
    gaps: list[tuple[str, list[str], list[str]]] = []
    for subject in sorted(all_subjects):
        if is_branch_specific(subject):
            continue
        if subject in known_gaps:
            continue
        present_on = [b for b in all_branches if subject in subjects_by_branch.get(b, set())]
        missing_on = [b for b in all_branches if b not in present_on]
        if missing_on and len(present_on) >= 1:
            gaps.append((subject, present_on, missing_on))

    print()
    if not gaps:
        print("✓ No sync gaps detected — all branches are in parity.")
        return 0

    print(f"✗ {len(gaps)} sync gap(s) detected:\n")
    for subject, present, missing in gaps:
        print(f"  Subject : {subject!r}")
        print(f"  Present : {', '.join(present)}")
        print(f"  Missing : {', '.join(missing)}")
        print()

    print("Review each gap and either:")
    print("  • Port the missing fix to the other branch(es), or")
    print("  • Document why it's intentionally absent.")
    return 1


if __name__ == "__main__":
    sys.exit(main())
