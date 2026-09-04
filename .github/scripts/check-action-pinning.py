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


def entry(node, key):
    """The value node for `key` in a mapping node, or None."""
    if not isinstance(node, yaml.MappingNode):
        return None
    for name, value in node.value:
        if isinstance(name, yaml.ScalarNode) and name.value == key:
            return value
    return None


def refs(root):
    """(ref, line) for every `uses:` a workflow can carry.

    Walked structurally rather than by text search, so a `with:` input named
    `uses` is not mistaken for an action, and each occurrence carries its own
    line even when the same action is referenced twice in one file.
    """
    jobs = entry(root, "jobs")
    if not isinstance(jobs, yaml.MappingNode):
        return
    for _, job in jobs.value:
        called = entry(job, "uses")  # reusable-workflow call
        if isinstance(called, yaml.ScalarNode):
            yield called.value, called.start_mark.line + 1
        steps = entry(job, "steps")
        if not isinstance(steps, yaml.SequenceNode):
            continue
        for step in steps.value:
            used = entry(step, "uses")
            if isinstance(used, yaml.ScalarNode):
                yield used.value, used.start_mark.line + 1


def main():
    unpinned = []
    for path in sorted(
        glob.glob(".github/workflows/*.yml") + glob.glob(".github/workflows/*.yaml")
    ):
        with open(path, encoding="utf-8") as handle:
            root = yaml.compose(handle)
        if root is None:
            continue
        for ref, line in refs(root):
            # Local actions and container images have no commit to pin to.
            if ref.startswith("./") or ref.startswith("docker://"):
                continue
            if not SHA.match(ref.rpartition("@")[2]):
                unpinned.append((path, line, ref))

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
