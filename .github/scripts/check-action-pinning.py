#!/usr/bin/env python3
"""Fail if any workflow action is referenced by a mutable ref.

A tag or branch can be repointed at new code, so an action pinned to one runs
whatever its owner publishes next — with the calling workflow's credentials.
Only a full commit SHA is immutable.
"""

import glob
import re
import sys

import yaml

SHA = re.compile(r"^[0-9a-f]{40}$")


def refs(doc):
    """Every `uses:` a workflow can carry: reusable-workflow calls and step actions."""
    for name, job in (doc.get("jobs") or {}).items():
        if not isinstance(job, dict):
            continue
        if isinstance(job.get("uses"), str):
            yield job["uses"]
        for step in job.get("steps") or []:
            if isinstance(step, dict) and isinstance(step.get("uses"), str):
                yield step["uses"]


def line_of(path, ref):
    with open(path, encoding="utf-8") as handle:
        for number, text in enumerate(handle, 1):
            if ref in text:
                return number
    return 1


def main():
    unpinned = []
    for path in sorted(
        glob.glob(".github/workflows/*.yml") + glob.glob(".github/workflows/*.yaml")
    ):
        with open(path, encoding="utf-8") as handle:
            doc = yaml.safe_load(handle)
        if not isinstance(doc, dict):
            continue
        for ref in refs(doc):
            # Local actions and container images have no commit to pin to.
            if ref.startswith("./") or ref.startswith("docker://"):
                continue
            if not SHA.match(ref.rpartition("@")[2]):
                unpinned.append((path, line_of(path, ref), ref))

    for path, line, ref in unpinned:
        print(
            f"::error file={path},line={line}::"
            f"'{ref}' is not pinned to a full commit SHA."
        )

    if unpinned:
        print(
            f"\n{len(unpinned)} action(s) are pinned to a mutable ref. Pin each to the "
            "full commit SHA with a trailing '# vX.Y' comment, matching the other "
            "actions in this repo; Dependabot then keeps it updated.",
            file=sys.stderr,
        )
        return 1

    print("All workflow actions are pinned to a full commit SHA.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
