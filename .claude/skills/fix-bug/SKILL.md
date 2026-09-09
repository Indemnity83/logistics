---
name: fix-bug
description: >-
  Fix one bug from a GitHub issue number or inline description. Verify the report,
  establish proof of the defect before changing production code, make the smallest
  correct fix, and confirm the proof goes green. Prefer permanent regression tests
  for behavioral bugs, but weigh their maintenance cost rather than adding tests
  mechanically. Opens a PR by default and can optionally hand off to ship-pr-queue
  for merge and porting.
---

# Fix a bug

Fix **one bug per pass**.

The workflow is:

**verify → prove → fix → confirm → PR**

The proof is usually a regression test, but it may instead be a throwaway test or direct
verification when a permanent test would cost more to maintain than the behavior it protects.

Input may be:

```text
/fix-bug 1014
/fix-bug the quarry drops its frame when the chunk unloads
```

For a GitHub issue, read the entire issue and relevant linked issues before changing code.
The issue is evidence, not ground truth.

Do not quietly widen scope. If you discover another defect while fixing this one, report or
file it separately.

---

## 1. Verify the report

Before writing a test or changing production code, inspect the implementation and confirm:

1. The reported symptom can actually occur.
2. The suspected mechanism is correct, or determine the real mechanism.
3. The bug exists on the branch where the issue claims it exists.
4. The requested behavior is consistent with the surrounding system.

Issue reports are often right about the symptom and wrong about the mechanism or prescribed
fix.

Examples seen in this repository include:

- an issue naming the wrong exception type while correctly identifying the failure path;
- a proposed guard depending on state the client cannot yet possess;
- an issue claiming there was no reasonable test shape when one existed;
- a bug affecting a newer Minecraft branch but not an older branch because the relevant code
  did not exist there.

Do not implement a reporter's proposed patch merely because it appears in the issue.

If the report is wrong but the correct fix is clear, remains within the described behavior,
and does not introduce a product decision, fix the actual defect and explain the divergence
in the PR.

Stop when:

- the defect cannot be reproduced or located;
- the expected behavior itself is ambiguous;
- the correct fix would intentionally change behavior beyond the issue's scope.

Do not make speculative production changes just to close an issue.

---

## 2. Determine affected branches

Read branch roles from `CLAUDE.md`. Do not hardcode which branch is current or primary.

Determine where the defect actually exists before choosing the origin branch.

### Normal case

If the bug exists on the default development branch:

```bash
git town hack <name>
```

Fix it there and PR into that branch.

### Legacy-only case

If newer branches do not contain the defect, originate the fix from the **highest affected
branch**, then port downward as appropriate.

Never start new work directly on an `mc/*` branch when the repository workflow requires a
feature branch and PR.

Record which branches are:

- affected;
- unaffected;
- structurally different enough to require adaptation.

Check this from code. Do not infer it solely from branch ancestry.

---

## 3. Choose the proof strategy

Before building test infrastructure, decide what kind of proof this bug deserves.

The default for a behavioral bug is a permanent regression test.

There are three valid proof strategies.

| Strategy | Use when | Repository result |
| --- | --- | --- |
| **Permanent regression test** | Behavioral bug in code likely to change or regress. Default. | Commit the test. |
| **Throwaway proof** | The defect can be demonstrated automatically, but keeping the required harness would create disproportionate maintenance debt. | Keep before/after output in the PR; remove the temporary test/harness. |
| **Direct verification** | The defect is a constant, configuration, generated output, workflow behavior, rendering issue, or similar case where an automated behavioral assertion adds little value. | Document the verification method and why no regression test was retained. |

### Test selection

Prefer:

- **Plain JUnit under `src/test`** for logic, accounting, state transitions, serialization,
  algorithms, and behavior that can be tested without launching Minecraft.
- **GameTest** only when the contract depends on actual game behavior involving real blocks,
  entities, capabilities, worlds, or loader integration.

GameTests are expensive and serialize more of the build, so do not use them merely because the
code happens to run inside Minecraft.

### Assert the contract

Test what must remain true, not the implementation used to achieve it.

Prefer:

```text
No item is destroyed when the network cannot accept it.
```

over:

```text
insert() returns 44.
```

A good regression test should survive a reasonable refactor of the implementation.

---

## 4. Check whether the proof is worth keeping

A regression test is production code with maintenance cost.

Before creating substantial harness infrastructure, consider:

### Test-to-fix ratio

Compare the amount and complexity of:

- the production fix;
- the regression test;
- any new harness, fake implementation, parser, linter, fixture system, or framework required
  solely to support that test.

A large test is not automatically wrong. A large **new testing abstraction protecting a tiny,
stable behavior** is a warning sign.

If you find yourself building a test framework rather than a test, reconsider whether the proof
should be temporary.

### Recurrence

Look for evidence that this class of defect has happened before.

For example:

```bash
git log --since='12 months ago' -p -- <path>
```

Search the relevant history for the mistake or pattern rather than guessing about recurrence.

Repeated defects strongly favor a permanent guard.

### Escalate disproportionate test debt

If the test or harness is substantially larger or more complex than the fix, do not silently
choose.

Report:

- approximate production-fix size;
- approximate test/harness size;
- whether similar bugs have recurred;
- your recommendation.

Let the user choose between a permanent regression guard and a throwaway proof when the trade-off
is genuinely significant.

Do this **before** investing heavily in the harness.

---

## 5. Establish the failure before fixing production code

For an executable proof, demonstrate that the current code fails.

A test that was never observed failing is not evidence of the bug.

Run the smallest targeted proof possible:

```bash
./gradlew :common:test --tests '*YourNewTest*'
```

For a GameTest, run the smallest appropriate GameTest target.

Capture the relevant failure output for the PR.

The failure should demonstrate the reported behavioral contract, not merely fail because of a
broken fixture or missing setup.

If the test unexpectedly passes, stop and investigate. Do not weaken or distort the assertion
just to manufacture a red test.

For direct verification, capture equivalent before-state evidence such as:

- CI output;
- generated configuration;
- rendered output;
- reproducible logs;
- deterministic inspection of the incorrect value.

---

## 6. Fix the cause

Make the smallest change that correctly fixes the underlying defect.

Prefer:

- fixing the cause rather than masking the symptom;
- the idiom already used by nearby code;
- existing abstractions over new ones;
- ordering that avoids irreversible work before validation.

For example, when surrounding code validates affordability before extraction, follow that
pattern rather than extracting first and attempting to refund afterward.

Do not refactor unrelated code merely because you are nearby.

If fixing the bug exposes an unrelated cleanup opportunity, leave it for another change unless
the cleanup is necessary to make the bug fix correct.

---

## 7. Confirm the proof goes green

Run the same proof that failed before the fix.

```bash
./gradlew :common:test --tests '*YourNewTest*'
```

The evidence should now show the exact same behavioral claim succeeding.

For throwaway proof:

1. preserve the before/after result for the PR;
2. remove the temporary harness or test;
3. verify the production fix remains present;
4. run the normal repository validation.

For direct verification, repeat the same observation after the fix and record the changed result.

---

## 8. Check for vacuous tests

After changing behavior, inspect the existing tests closest to the modified contract.

A formerly meaningful test can become trivially true after the implementation changes.

Example:

A test asserts:

```text
Every extracted ingot still exists somewhere.
```

If the new fix prevents extraction entirely, the assertion may succeed because zero ingots were
extracted. The test is green but no longer proves the behavior its name claims.

For important affected tests, verify that they can still detect a regression.

One useful technique is to temporarily remove only the production fix while keeping the tests:

```bash
git stash push -- <production-files>
./gradlew :common:test --tests '*Relevant*'
git stash pop
```

At least the regression proof must fail without the production fix.

Do not mechanically require every neighboring test to fail against every reverted implementation;
the goal is to identify tests whose asserted contract has become vacuous.

If an existing test no longer exercises its claimed behavior, retarget it to the correct contract.

---

## 9. Run repository validation

At minimum:

```bash
./gradlew :common:test spotlessCheck
```

Also run the affected loader modules when applicable:

```text
:fabric:
:neoforge:
```

Compile client source sets when client code is touched.

Use the narrow test during development, then the repository-level validation before opening the
PR.

Do not report the fix as complete while required validation is known to be failing.

---

## 10. Open the PR

Follow the repository rules in `CLAUDE.md`, especially:

- Pull Requests
- Release notes
- PR title strategy
- branch and porting conventions

### Title

Use a scoped Conventional Commit with concise player-facing behavior.

Example:

```text
fix(routing): stop Providers destroying items
```

Prefer roughly 4–8 words after the colon. Describe the corrected behavior rather than the
implementation detail.

### Body

Use the repository's release-note structure, including:

- Summary
- Changes
- Notes
- Tests
- Porting

Include:

```text
Fixes #N
```

when applicable.

### Evidence

For a permanent regression test, include:

- what failed before;
- the relevant failure;
- what passes afterward.

For a throwaway proof, explicitly say that the test was temporary and why retaining its harness
would create disproportionate maintenance cost.

For direct verification, state:

- why an automated regression test was not appropriate;
- the exact before/after verification performed.

### Divergence from the issue

If the issue's diagnosis or prescribed implementation was wrong, say so plainly in the PR.

Explain:

- what the issue claimed;
- what inspection showed;
- why the implemented fix differs.

### Porting

State explicitly which `mc/*` branches:

- contain the defect;
- do not contain the defect;
- require adaptation.

Do not say "all branches" without checking them.

Several independently player-facing fixes should normally be separate PRs. If repository policy
allows several release-note-worthy changes in one PR, preserve each Conventional Commit line in
the squash body as required by `CLAUDE.md`.

If a workflow push under `.github/workflows/**` is rejected, inspect:

```bash
gh auth status
```

and verify the token has the necessary scopes rather than changing repository behavior to work
around authentication.

---

## 11. Stop at the PR unless asked to ship

Opening the PR is the default endpoint.

A bug fix benefits from review, especially because review can catch:

- a misread issue;
- an incorrect behavioral assumption;
- an overbuilt test;
- a branch-specific difference;
- an accidental scope expansion.

When the user explicitly asks for end-to-end delivery, hand off to:

```text
ship-pr-queue
```

That workflow is responsible for:

1. merging using the correct squash/changelog body;
2. porting to the applicable `mc/*` branches;
3. verifying convergence.

If a port requires semantic adaptation rather than a clean mechanical port, defer that branch to:

```text
backport
```

Do not silently improvise a risky version-specific adaptation.

---

# Stop conditions

Stop and report the situation when:

- the defect cannot be reproduced or located;
- the expected behavior is ambiguous;
- the correct fix would expand product behavior beyond the issue;
- the proof unexpectedly passes before the production fix;
- a permanent test requires disproportionate scaffolding and the user should choose whether to
  retain it;
- a branch-specific adaptation is unclear or behaviorally different.

You do **not** need to stop merely because the issue's proposed implementation is wrong.

If the intended behavior is clear and the correct fix remains within scope, implement the correct
fix and explain the discrepancy.

---

# Done when

For the default workflow:

- the defect has been independently verified;
- the affected branches have been identified;
- the defect was proven before the fix;
- the smallest correct production change was made;
- the same proof succeeds afterward;
- retained regression tests are meaningful rather than vacuous;
- repository validation and formatting checks are green;
- the PR is open;
- the PR records the evidence and branch applicability.

For end-to-end delivery, it is done only when the PR is merged and the applicable branches have
been ported and verified.
