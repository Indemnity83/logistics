# Parallel sessions and the ship lock

Several sessions can work different PRs at once, each in its own worktree. Most of the work
parallelises cleanly; shipping and porting do not, and those two take the **ship lock**. The hard
constraints every session follows (one checkout per branch, never touch a worktree you do not own,
`pkill -f` prefixes, port 25565) are in `CLAUDE.md` under "Parallel sessions".

## Independent per session

Each session gets its own worktree and stays in it:

- Editing, committing, and pushing a feature branch.
- Addressing review findings and resolving threads.
- `git town sync` / `propose` on your own branch. Git Town's runstate is keyed by **worktree
  path**, not by repository, so sessions in different worktrees cannot corrupt each other's.
- `git fetch`, and reads generally. Git locks refs internally.
- Git Town's configuration. It is read from `.git-town.toml` in each worktree's own checkout
  rather than from shared git metadata, so a worktree sitting on an older branch can be a
  config revision behind until it syncs.
- Gradle daemons are per-worktree but share `~/.gradle`. Four or five concurrent builds are
  heavy; the ship lock already serialises the ones that run during a push.

## Serialised: take the ship lock first

- **Shipping** (`tools/ship_pr.py`, `git town ship`). Shipping moves the default branch, and every
  other session's feature branch immediately becomes "not in sync with its parent".
- **Porting.** Cherry-picking uses the shared `mc/*` worktrees; two sessions cherry-picking in
  the same worktree collide outright, and two pushing the same branch race for a
  non-fast-forward rejection.

The lock is one atomic `mkdir` in the shared git dir, so every worktree sees the same lock:

```bash
# --path-format=absolute matters: a bare --git-common-dir prints a *relative*
# `.git` when run from the main worktree, so the lock path would depend on cwd.
LOCK="$(git rev-parse --path-format=absolute --git-common-dir)/ship.lock"

if mkdir "$LOCK" 2>/dev/null; then
  echo "pr-<n>" > "$LOCK/owner"; date +%s > "$LOCK/since"

  # ... ship, then port to the other branches ...

  rm -rf "$LOCK"                             # release: only ever on this path
else
  owner=$(cat "$LOCK/owner" 2>/dev/null) || true
  since=$(cat "$LOCK/since" 2>/dev/null) || true
  if [ -n "$since" ]; then age="$(( $(date +%s) - since ))s"; else age="unknown age"; fi
  echo "held by ${owner:-unknown} ($age)"
  # Do not touch the lock. Wait and retry, or go work a different PR.
fi
```

The fallbacks on `owner`/`since` are not cosmetic: a lock directory can exist without its two
files — a session killed between the `mkdir` and the writes leaves exactly that — and
`$(( $(date +%s) - $(cat missing) ))` is a **syntax error** that aborts the whole `echo`, so the
losing session would print nothing at all instead of saying who holds the lock.

**Release only on the path that acquired it.** Putting `rm -rf "$LOCK"` after the `if`/`else`
instead of inside the success branch means the session that *lost* the race deletes the winner's
lock on its way past — the next contender then acquires it while the original holder is still
mid-ship, which is precisely the collision the lock exists to prevent.

Release even when the ship fails, but only if you are the holder. **The release is its own
step** — acquire, ship, port and release are separate commands in an agent session, each in its
own shell, so a `trap ... EXIT` on the acquiring command would fire the moment that command
returns and drop the lock immediately. That also means a ship that fails partway will strand the
lock rather than unwinding it: if a step fails, release explicitly before moving on.

Because a lock can be stranded that way, treat one held by a PR that is clearly finished, or one
that is hours old, as stale — say so, confirm the holder is really gone, then clear it. Waiting
is usually cheap: a ship plus a port is a few minutes.
