---
name: ship-pr-queue
description: >-
  Work through every open pull request targeting an mc/** branch, one at a time. A mergeable
  PR is squash-shipped with its changelog body and then ported to the other version branches;
  a non-mergeable PR has its failing checks and review findings addressed and pushed. Use when
  asked to work the PR queue, clear the open PRs, ship what is ready, or drive the backlog
  toward merged. Skips release-please PRs, which belong to the release cycle.
---

# Ship the PR queue

One pass = one PR taken as far as it can go. Repeat until the queue is empty. Do **not**
batch operations across PRs: every push re-triggers a review that costs roughly $1, so
wasted pushes are wasted budget.

## Build the queue

```bash
gh pr list --state open --limit 100 \
  --json number,title,baseRefName,headRefName,isDraft,labels,mergeStateStatus \
  --jq '.[] | select(.baseRefName|startswith("mc/"))
            | select(.headRefName|startswith("release-please")|not)
            | select([.labels[].name]|index("autorelease: pending")|not)
            | select(.isDraft|not)
            | "#\(.number)\t\(.mergeStateStatus)\t\(.baseRefName)\t\(.title)"'
```

Release-please PRs are excluded on both signals (head branch and label) because they are
merged deliberately as part of the release cycle, not by this loop.

**`UNKNOWN` is not a state — it means GitHub has not computed mergeability yet.** Asking is
what triggers the computation, so the first query after a base moves usually returns
`UNKNOWN` for most of the queue. Run the query again a few seconds later and it resolves.
Never classify a PR off an `UNKNOWN`; you would treat a clean PR as broken and "fix" a
problem that does not exist.

Report the queue to the user before acting, then take PRs in order, `CLEAN` ones first —
shipping those shrinks the queue fastest and moves the base for the rest.

## Classify

`mergeStateStatus` is the primary signal:

| Status | Meaning | Branch |
|---|---|---|
| `CLEAN` | green and no blockers | **Ship** |
| `BLOCKED` | a required check is pending/failing, or a review thread is unresolved | **Diagnose first** (below) |
| `DIRTY` | merge conflict with base | **Fix** (merge base in) |
| `BEHIND` | base moved | `gh pr update-branch <n>` |
| `UNSTABLE` | non-required check failing | judge: usually **Fix** |

**`BLOCKED` does not imply broken.** A required check that is still *pending* blocks the
PR exactly like a failing one, and a review takes ~2 minutes. Before changing anything,
separate the three causes:

```bash
gh pr checks <n> | awk -F'\t' '$2!="pass" && $2!=""{printf "%-34s %s\n",$1,$2}'
gh api graphql -f query='{repository(owner:"<owner>",name:"<repo>"){pullRequest(number:<n>){
  reviewThreads(first:100){nodes{isResolved}}}}}' \
  --jq '[.data.repository.pullRequest.reviewThreads.nodes[]|select(.isResolved==false)]|length'
```

- Only `pending` entries → **wait and re-probe.** Do not push anything; a pointless commit
  costs another review.
- `fail` entries → fix the checks.
- 0 non-pass checks but unresolved threads > 0 → address the findings.

The `Code Review / summary` status says what the reviewer actually did — read it rather
than inferring from the green `review (claude)` check, which is green in four different
situations including "reviewed nothing":

```bash
gh pr checks <n> | awk -F'\t' '{printf "%-34s %-6s %s\n", $1, $2, $5}'
```

`No issues found` / `N findings posted` / `No review needed: trivial, or reviewed earlier`
/ `Skipped: …` are self-explanatory. A PR that modifies `.github/workflows/**` cannot
review itself (the action refuses to run a workflow differing from the default branch's
copy) — review those yourself before shipping.

## Branch A — Ship a mergeable PR

### 1. Assemble the squash body

The subject is the PR title; **`-m` supplies only the body.** Passing a full message
duplicates the subject into the body and produces duplicate changelog entries.

The body holds *additional* Conventional Commit lines when one PR makes several
player-facing changes — read the PR body and its discussion for lines the author called
out (often under a "Suggested squash commit" heading). Each `type(scope): description`
line becomes its own changelog entry. See CLAUDE.md "Multi-change squash commits".

- Single change → `git town ship -m ""`
- Multiple → one Conventional Commit line per additional change, newline-separated

Only carry lines that are genuinely additional changelog entries. Do not restate the
title, and do not invent entries the PR does not support.

### 2. Ship

```bash
git checkout <feature-branch>
git town ship -m "<additional lines, or empty>"
git checkout <base> && git pull --ff-only origin <base>
```

Then **verify the commit actually landed as intended** before porting:

```bash
git log -1 --format='%s%n---%n%b'
```

Subject must be the PR title; body must contain exactly the additional lines and no
duplicate of the subject. If it is wrong, stop and tell the user — a bad squash message
produces a wrong changelog, and it is far easier to fix before the port fans it out.

If `git town ship` complains about a leftover run or a missing interactive terminal, a
stale runstate is blocking it: delete `runstate.json` under
`~/Library/Application Support/git-town/<repo-slug>/` (branches are untouched) and retry.

### 3. Port to the other version branches

Read the branch roles from CLAUDE.md; never hardcode which branch is main. From the
default branch, port **down** to the maintenance branches and **up** to the pre-release
branch. Ports are cherry-pick plus direct push — no PR — using the sibling worktrees.

Decide applicability first. Port player-facing fixes/features and internal work that keeps
the branches converging. Do **not** port a change that is version-specific: if it touches
an API or asset that only exists on some branches, adapt it or skip it, and say which.

**For anything beyond a clean cherry-pick, use the `backport` skill** (`.claude/skills/backport/`).
It covers already-present detection, resolving conflicts toward structural convergence, the
per-version API cheat-sheet, the verification suites, and working without per-branch worktrees.
The steps below are the short path for a port that applies cleanly.

```bash
p=/Users/kklaus/Dev/Minecraft/logistics-<version>
git -C "$p" fetch origin <branch> -q && git -C "$p" merge --ff-only origin/<branch> -q
git -C "$p" cherry-pick <sha>
git -C "$p" push origin HEAD:<branch>
```

- Conflicts are expected on legacy branches; resolve them for that branch's API, and keep
  the commit message identical so the branches read as the same change.
- The `pre-push` hook runs the unit tests for any `mc/*` destination. Let it. Only
  `--no-verify` when the change contains no code at all, and say so. Pre-release
  branches skip the hook automatically (decided from `minecraft_version`), so a
  push there running no checks is expected, not a bypass.
- Commits touching `.github/workflows/**` cannot be pushed over HTTPS (the token lacks
  `workflow` scope). Push those over SSH; if port 22 times out, use port 443 —
  see the `env_push_workflow_files_needs_ssh` memory for the exact incantation.
- A change that must go **up into the default branch** cannot be direct-pushed; open a PR
  for that hop.

Then confirm the branches actually converged:

```bash
for b in <all mc branches>; do
  printf "%-12s %s\n" "$b" "$(git show origin/$b:<file> | shasum | cut -c1-10)"
done
```

For `.github/workflows/code-review.yml` this is mandatory, not cosmetic: if it differs
from the default branch's copy, the action silently green-skips every review on that
branch.

## Branch B — Fix a non-mergeable PR

Work the PR's own feature branch. Push follow-up commits and let the squash collapse them:
**never amend or force-push** a PR under review — it detaches the review threads.

### Failing checks

```bash
gh pr checks <n> | awk -F'\t' '$2!="pass" && $2!=""'
gh run view <run-id> --log-failed | tail -40
```

Fix the cause, not the symptom. If a check fails for a reason unrelated to the PR (flaky
infrastructure, an upstream artifact that vanished), say so and re-run the job rather than
editing code to route around it.

### Review findings

List the unresolved threads:

```bash
gh api graphql -f query='{repository(owner:"<owner>",name:"<repo>"){pullRequest(number:<n>){
  reviewThreads(first:100){nodes{id isResolved isOutdated path line
    comments(first:1){nodes{author{login} body}}}}}}}' \
  --jq '.data.repository.pullRequest.reviewThreads.nodes[]
        | select(.isResolved==false) | {id, path, line, body: .comments.nodes[0].body[0:200]}'
```

For each, **verify the finding against the current code before acting on it** — reviewers
are wrong sometimes, and a finding that quotes a rule should be checked against the rule.

- **Valid** → fix minimally, commit, push. Then resolve the thread.
- **Wrong or already handled** → reply on the thread saying why, then resolve it. Do not
  change code to satisfy a finding you believe is incorrect; say so instead.
- **Real but out of scope** → reply, leave it unresolved or file an issue, and tell the
  user. Do not silently expand the PR.

Resolve a thread with:

```bash
gh api graphql -f query='mutation{resolveReviewThread(input:{threadId:"<id>"}){thread{isResolved}}}'
```

Unresolved threads block merging (the ruleset requires thread resolution), so a PR is not
shippable until every thread is either addressed or explicitly answered and resolved.

### After pushing

The push re-triggers a review, which now runs on every push rather than once per PR. Wait
for it, then re-classify. Do not ship on the previous review's verdict.

## Stop and ask

- The PR's intent is unclear, or the fix would change behaviour the PR did not set out to change.
- A finding is arguably valid but the fix is large — propose it rather than doing it.
- A port needs non-trivial adaptation and you are unsure the adapted form is correct.
- Anything wants a force-push, a direct push to the default branch, or a ruleset bypass.

## Done when

Every non-release open PR against `mc/**` is either merged and ported with branches
verified identical, or left with a clear statement of what blocks it and what you tried.
