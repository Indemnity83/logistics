#!/usr/bin/env python3
"""Fixtures for check-action-pinning.py. Run with no arguments; exits non-zero on failure."""

import contextlib
import importlib.util
import io
import pathlib
import sys
import tempfile

HERE = pathlib.Path(__file__).parent
spec = importlib.util.spec_from_file_location(
    "checker", HERE / "check-action-pinning.py"
)
checker = importlib.util.module_from_spec(spec)
spec.loader.exec_module(checker)

SHA = "3d3c42e5aac5ba805825da76410c181273ba90b1"


def run(workflow):
    """Return (exit_code, [(line, ref)]) for a single workflow document."""
    with tempfile.TemporaryDirectory() as root:
        (pathlib.Path(root) / "w.yml").write_text(workflow, encoding="utf-8")
        out = io.StringIO()
        with contextlib.redirect_stdout(out), contextlib.redirect_stderr(io.StringIO()):
            code = checker.main(root)
    found = []
    for text in out.getvalue().splitlines():
        if text.startswith("::error"):
            location, _, message = text.partition("::")[2].partition("::")
            line = int(location.split("line=")[1])
            found.append((line, message.split("'")[1]))
    return code, found


CASES = []


def case(name):
    def register(fn):
        CASES.append((name, fn))
        return fn

    return register


@case("a SHA-pinned action passes")
def _():
    code, found = run(f"jobs:\n  a:\n    steps:\n      - uses: a/b@{SHA}\n")
    assert code == 0 and found == [], found


@case("a tag, a branch and a bare name are each rejected")
def _():
    code, found = run(
        "jobs:\n  a:\n    steps:\n"
        "      - uses: a/b@v1\n"
        "      - uses: c/d@main\n"
        "      - uses: e/f\n"
    )
    assert code == 1, code
    assert found == [(4, "a/b@v1"), (5, "c/d@main"), (6, "e/f")], found


@case("local actions and container images are skipped")
def _():
    code, found = run(
        "jobs:\n  a:\n    steps:\n"
        "      - uses: ./.github/actions/x\n"
        "      - uses: docker://alpine:3.20\n"
    )
    assert code == 0 and found == [], found


@case("a `with:` input named `uses` is not an action reference")
def _():
    code, found = run(
        f"jobs:\n  a:\n    steps:\n"
        f"      - uses: a/b@{SHA}\n"
        f"        with:\n          uses: not-an-action\n"
    )
    assert code == 0 and found == [], found


@case("the same ref twice reports each occurrence's own line")
def _():
    code, found = run(
        "jobs:\n  a:\n    steps:\n"
        "      - uses: a/b@v1\n"
        "      - uses: c/d@main\n"
        "      - uses: a/b@v1\n"
    )
    assert found == [(4, "a/b@v1"), (5, "c/d@main"), (6, "a/b@v1")], found


@case("an aliased ref is reported once, at the anchor it can be fixed at")
def _():
    code, found = run(
        "jobs:\n"
        "  a:\n    steps:\n      - uses: &pin a/b@v1\n"
        "  b:\n    steps:\n      - uses: *pin\n"
        "  c:\n    steps:\n      - uses: *pin\n"
    )
    # Still detected — aliasing cannot hide a violation.
    assert code == 1, code
    assert found == [(4, "a/b@v1")], found


@case("a reusable-workflow call is checked, and a local one skipped")
def _():
    code, found = run(
        "jobs:\n"
        "  a:\n    uses: o/r/.github/workflows/w.yml@v2\n"
        "  b:\n    uses: ./.github/workflows/local.yml\n"
    )
    assert found == [(3, "o/r/.github/workflows/w.yml@v2")], found


def main():
    failures = 0
    for name, fn in CASES:
        try:
            fn()
        except AssertionError as error:
            print(f"FAIL  {name}\n        {error}")
            failures += 1
        else:
            print(f"ok    {name}")
    if failures:
        print(f"\n{failures} of {len(CASES)} fixtures failed.", file=sys.stderr)
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
