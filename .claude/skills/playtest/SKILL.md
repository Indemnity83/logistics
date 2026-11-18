---
name: playtest
description: >-
  Launch the dev client on a PR (or several PRs together, or an mc/* branch) so the user can try
  it in game, then report what the mod logged. Use when the user asks to playtest, launch, or try
  out a PR or branch in the client, or to check a fix visually.
---

# Playtest

`tools/playtest.py` does the whole run. It blocks until the user closes the client, so run it in
the background and wait for the completion notice:

```bash
python3 tools/playtest.py <PR> [<PR> ...] [--loader fabric|neoforge]
```

- **One version at a time.** All targets must share a base branch. Each version has its own playtest
  worktree, `../logistics-playtest-<version>`, holding one world that carries over between runs.
- **Several PRs** are merged locally there (never pushed), so one world can hold every feature under
  test. A conflict stops the run and names the files; shipping those PRs in sequence will hit the
  same conflict.
- **No branch juggling.** The PR head is checked out detached, so a branch checked out in an
  agent's worktree needs no freeing. Never check a PR branch out in a worktree you don't own to
  playtest it.
- **`--prepare-only`** sets up the checkout without launching, e.g. to confirm several PRs merge.

While the client starts, tell the user what to look at: the player-visible changes in the PR, and
the edge cases its tests could not show (art, rendering, feel, balance).

When the run ends, relay the report:

- **Mod warnings, errors and exceptions** are new findings unless the PR explains them. Known noise
  (oversized-texture mip warnings, JEI duplicate-item notes, world-icon size) is counted, not listed.
- **Dropped content** means the world had blocks or items this build does not register, usually
  from a different PR's earlier playtest. It is expected after switching PRs; suggest playtesting
  the PRs together instead.

Then ask how it went. A playtest is the user's sign-off: never mark a PR ready, ship it, or port
it on the strength of a clean log alone.
