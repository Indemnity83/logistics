---
name: pre-release-review
description: Review the open release PRs' changelogs as a player will read them, and fix their lines before release, at the source PR on the default branch and in the release PR itself on the other version branches.
disable-model-invocation: true
---

# Pre-release review

Every `mc/*` branch has its own release PR (`chore(release): release X.Y.Z for mcV`), labelled `autorelease: pending`. The newest block of its `CHANGELOG.md` becomes the GitHub release body, and `build-release.yml` publishes that body as the Modrinth and CurseForge changelog. The reader is a player, not a developer. What a line says, its type and its scope follow "PR titles" in `CLAUDE.md` and `docs/agents/changelog.md`; which types show, and under which heading, is `changelog-sections` in `release-please-config.json`.

release-please rebuilds a release PR from its branch's commits every time it runs, so an edit to a release PR lasts only until the next run. Where a line is fixed depends on the branch:

- **The default branch** (`git rev-parse --abbrev-ref origin/HEAD`): its commits are squash merges that GitHub links to their PR, so release-please reads a commit override in that PR's body. Fix lines at the source PR; the fix survives every rebuild.
- **Every other `mc/*` branch**: its commits are ports, cherry-picked and pushed directly. GitHub links them to no PR (`gh api repos/{owner}/{repo}/commits/<sha>/pulls` returns `[]`), so an override never reaches them. Fix lines in the release PR itself, right before it is merged.

## Steps

1. **Read the changelogs.** `gh pr list --state open --label "autorelease: pending" --json number,baseRefName,headRefName,body` (a search for `chore(release)` is less reliable than release-please's own label). For each release PR, take every line under every heading of the newest version block, and its source PR: the `(#N)` in the line, or, for a line with only a commit link on the default branch, `gh api repos/{owner}/{repo}/commits/<sha>/pulls --jq '.[0].number'`. A ported line with no `(#N)` has no source PR; note it. Done when every line has its source PR or is noted.
2. **Decide each line.** Read each branch's changelog as a player, then all of them together: does each release read as one coherent story, and does a change read the same on every branch? A line on only some branches needs a reason a player would accept (content locked to one Minecraft version, a fix only older versions needed). For each line, one decision:
   - **keep**
   - **reword**: player-facing, 4–8 words after the colon, the scope carrying the product noun
   - **retype**: the type is wrong for a player (a rework of content that only shipped in an alpha is `refactor`, a recipe or rate change is `balance`)
   - **link**: an override line missing its `(#N)`
   - **hide**: nothing a player sees changed; give it a hidden type (`refactor`, `chore`, `test`)
   - **merge**: several lines are one change to a player; one source carries the combined line, the others are hidden
   - **split**: one PR made two changes a player would name separately
   - **drop**: the line describes something a later change in the same release replaced (a feature reworked before it ever shipped); hide it

   Done when every line on every branch has a decision.
3. **Show the owner.** For each branch, the changelog as it is and as it would read. Then the source PR bodies that change, and the release PRs that will be edited. Editing a merged PR's body or a release PR is visible on GitHub: write nothing until the owner approves.
4. **Write the overrides (default branch).** For each source PR that changes, put one block at the top of its body, replacing any block already there. It stands in for every line that PR produces on the default branch:

   ```text
   BEGIN_COMMIT_OVERRIDE
   feat(farming): add the crop farm and arboretum (#1508)
   balance(farming): farms pay upkeep for every plot they tend (#1508)
   END_COMMIT_OVERRIDE
   ```

   Every line ends with its own `(#N)`: an override line without it links only the commit. Leave the rest of the body as it was.

   Then rebuild: `gh workflow run prepare-release.yml --ref <default branch>`, wait for the run to finish, and read the release PR again. Done when it matches what the owner approved, line for line; otherwise back to the start of this step.
5. **Edit the other release PRs**, as late as possible, since the next push to a branch rebuilds its release PR and undoes the edit. For each one:
   - Check out its head branch, rewrite the approved lines in the newest block of `CHANGELOG.md`, and commit with a single-line message such as `Reword the 0.8.13 changelog for players`. Push it to the release PR branch.
   - Put the same block into the release PR body (`gh pr edit <n> --body-file`), keeping release-please's header and footer around it.
   - Done when the PR's `CHANGELOG.md` block and its body both match what the owner approved. If release-please rebuilds the PR before it is merged, repeat this step.
6. **Hand back.** Report the final changelog for each branch. Run `credit-contributors` after this review, not before: it appends to the final lines, and a rebuild undoes it too. The owner merges the release PRs; this review never does.
