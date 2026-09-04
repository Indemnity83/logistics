---
name: fix-bug
description: >-
  Fix one bug test-first — prove it with a failing test, fix it, watch the test go green — from a
  GitHub issue number or an inline description. Use when asked to fix a bug, work an issue, or
  address a defect. Weighs the maintenance cost of the test it adds rather than always adding one,
  and can optionally carry the change all the way to merged and ported via `ship-pr-queue`.
---

# Fix a bug

One pass = one bug, taken from a report to a PR (and optionally to merged). The shape is
**prove it, fix it, confirm it** — but the proof is not always a permanent test, and deciding
which it is, is part of the job.

**Input** is either a GitHub issue (`/fix-bug 1014`) or a plain description
(`/fix-bug the quarry drops its frame when the chunk unloads`). With an issue, read the whole
thing including linked issues; the body carries context the title does not.

**Scope is one bug.** If you find a second while fixing the first, file it or say so — do not
quietly widen the PR.

## 1. Verify the report before writing anything

**Issue bodies are wrong often enough that you must check.** They are usually right about
*symptom* and less reliable about *mechanism* and *prescribed fix*. Real examples from this repo:

- #993 named the wrong exception type — the mechanism was right, the class was not.
- #931 proposed guarding on `pipePos`, which cannot work: the client has no pipe position until
  the very packet being guarded arrives.
- #1014's fix shape was right, but its "no test-first shape" assumption was wrong.
- #1026 was correct, but the bug did not exist on `mc/1.21.1` at all — that menu has no
  `handlePlacement`.

So: read the code the issue points at, confirm the mechanism yourself, and **say in the PR when
the issue was wrong about something.** If a reviewer's or reporter's prescription is wrong, fix
the bug correctly and explain the divergence rather than implementing a broken prescription.

If you cannot reproduce or locate the defect, stop and say so. Do not fix code speculatively to
close an issue.

## 2. Decide where the fix originates

Read the branch roles from `CLAUDE.md`; never hardcode which branch is main.

- Bug exists on the default branch → start there (`git town hack <name>`), PR into it.
- **Legacy-only bug** (does not reproduce on the newest branches) → originate on the highest
  *affected* branch and cherry-pick down. This is the exception; most bugs are not this.

New work never starts directly on an `mc/*` branch. Feature branch, then PR.

## 3. Write the failing test first

The test is the specification of the bug: it should read as "what should have happened".

- **Plain JUnit in `src/test`** for logic, accounting, state machines, and anything a reviewer
  should be able to run in seconds.
- **GameTest** only for feature-level claims that must be proven through real blocks — a real
  Hopper, Cable or Pipe — rather than by poking a capability interface directly. GameTests are
  slow and cannot run many-way parallel, so they are the exception.
- Assert the *invariant*, not the mechanism. "No item is destroyed" survives a refactor;
  "`insert` returns 44" does not.

**Then prove it fails.** Run it against the code *before* the fix and keep the output:

```bash
git stash            # or check out the pre-fix commit
./gradlew :common:test --tests '*YourNewTest*'
git stash pop
```

A test you did not watch fail is not evidence — it may be asserting something that was already
true. Paste the failure into the PR body.

## 4. Weigh the test debt — do not reflexively keep the test

A test is not free. It is code that must be maintained, ported to every other `mc/*` branch, and
kept green forever. Usually it earns that; sometimes it does not, and noticing which is the point
of this step.

Ask two questions:

- **What is the ratio of test (plus harness) to fix?** A 200-line harness guarding a one-line
  constant is a bad trade — the guard becomes the thing most likely to break.
- **Does this class of bug recur?** Check the history, don't guess:
  `git log --since='12 months ago' -p -- <path> | grep '^+.*<pattern>'`. A defect that has
  happened repeatedly justifies a permanent guard; a genuine one-off usually does not.

Three legitimate outcomes:

| Outcome | When | What you leave behind |
|---|---|---|
| **Permanent test** | Behavioural bug in code that changes. The default. | The test, committed. |
| **Throwaway proof** | The fix is real but a permanent guard needs disproportionate scaffolding. | No test. The before/after output pasted in the PR body as the evidence. |
| **No test** | Config/constant with no behaviour to assert; verification is direct observation (a CI job log, a rendered screenshot). | An explicit note in the PR saying how it was verified and why there is no test. |

**Warning sign:** if you are writing a test *framework* rather than a test, you are probably in
row 2 or 3. A cautionary case from this repo — #1014 was a one-line action pin; the first attempt
carried a 93-line linter plus a 136-line test suite, and review found **two correctness bugs in
the linter itself**. The guard was more defect-prone than the thing it guarded. The enforcement
question was split into its own issue and the PR shipped as one line.

**When the test is substantially larger than the fix, surface the trade-off rather than deciding
alone.** Give the size numbers and the recurrence data, recommend, and let the user choose.

## 5. Fix the bug

Minimal change; fix the cause, not the symptom. Match the surrounding code's idiom.

Prefer the ordering the rest of the codebase already uses — e.g. check affordability *before*
committing to an irreversible extraction, rather than extracting and refunding.

## 6. Beware the vacuous pass

**After the fix, re-read the tests that already existed around it.** A change in approach can
make a previously meaningful test trivially true.

Real case: `ProviderModuleRefundTest` asserted "every extracted ingot still exists somewhere".
Once the fix stopped the extraction happening at all, that assertion passed *because nothing was
extracted* — it no longer tested the refund it was written for.

The check is mechanical, and worth doing on the key tests every time:

```bash
# revert only the production change, keep the tests
git stash push -- <production files>
./gradlew :common:test --tests '*Relevant*'   # MUST fail
git stash pop
```

If a test still passes with the fix reverted, it is no longer testing anything. Re-target it at
the new contract instead of leaving it green.

## 7. Confirm

```bash
./gradlew :common:test spotlessCheck
```

Plus the loader modules if you touched them (`:fabric:`, `:neoforge:`), and compile the client
source sets when the change reaches them.

## 8. Open the PR

Follow `CLAUDE.md` ("Pull Requests", "Release notes and PR title strategy"):

- **Title:** scoped conventional commit, player-facing wording, 4–8 words after the colon.
  `fix(routing): stop Providers destroying items on an unpowered network`.
- **Body:** release notes — Summary / Changes / Notes / Tests / Porting. Include `Fixes #N`.
- **Evidence:** the failing-before output, and what verified it if there is no committed test.
- **Porting:** state which other `mc/*` branches carry the defect, and which do not and why.
  Check, don't assume — the same file may not have the same bug.
- Several player-facing changes in one PR → one Conventional Commit line each in the squash body.

Commits touching `.github/workflows/**` push over plain HTTPS like anything else; confirm the
token's scopes with `gh auth status` if a push is ever refused.

## 9. Optionally: carry it to merged

Only when the user asked for end-to-end. Hand off to **`ship-pr-queue`**, which squash-merges the
PR with its changelog body, then ports it to the other `mc/*` branches and verifies they
converged. For a port needing real adaptation it defers to **`backport`**.

Stop at the PR by default — a fix wants a review before it is merged, and the review is what
catches the misread issue.

## Stop and ask

- The defect does not reproduce, or the issue names code that does not exist.
- The correct fix would change behaviour beyond what the issue describes.
- The test (or its harness) is substantially larger than the fix — give the numbers and recommend.
- The issue's prescribed fix is wrong; propose the alternative rather than silently diverging.
- The fix is version-specific and you are unsure the adaptation is right on another branch.

## Done when

The bug is fixed, the proof is recorded (as a committed test or as evidence in the PR body), the
full suite and `spotlessCheck` are green, and the PR is open with its porting applicability
stated — or, when asked for end-to-end, merged and ported with the branches verified identical.
