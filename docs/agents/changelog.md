# PR titles and changelog lines

Release Please turns each squash commit into a changelog line, and the changelog is what players
read on Modrinth and CurseForge. So a PR title is written for a **player**: it names the
player-visible effect. The format, the type list and the scope list are in `CLAUDE.md` under
"PR titles"; this file holds the rules for choosing among them, and the multi-change override
block. `check-pr.yml` enforces the allowed types and scopes, and `changelog-sections` in
`release-please-config.json` decides which types show and under which heading.

## Types

The changelog follows [Keep a Changelog](https://keepachangelog.com). Each visible type maps to a
section:

| Type | Changelog section | Use for |
|---|---|---|
| `feat` | Added | new player-facing capability, block, item, or machine |
| `balance` | Changed | recipe / cost / rate / output / power / progression tuning (buffs and nerfs) |
| `change` | Changed | other player-facing change (rename, restyle, behavior shift) |
| `perf` | Changed | player-visible performance improvement |
| `deprecate` | Deprecated | content/behavior marked for future removal |
| `remove` | Removed | removed content or behavior |
| `fix` | Fixed | corrected broken or unintended behavior |
| `revert` | Fixed | undo a shipped change that turned out to be wrong |
| `security` | Security | security-sensitive fix |

Hidden (developer-facing) types:

| Type | Use for |
|---|---|
| `refactor` | code restructuring with no player-visible change |
| `test` | test coverage |
| `build` | Gradle, publishing, or build system changes |
| `ci` | GitHub Actions or automation changes |
| `chore` | maintenance work |
| `docs` | documentation-only changes |

- Prefer `balance` over `change` for gameplay tuning (recipes, costs, rates, progression):
  `balance` is honest about both buffs and nerfs without implying an improvement.
- `revert` is visible on purpose: a revert is how a shipped regression gets withdrawn, and
  Release Please skips the release entirely when nothing in the range renders a changelog line, so
  a hidden `revert` would strand the withdrawal unreleased until an unrelated `fix:` landed.
- Keep the hidden types hidden: they never go into the changelog sections, and entries are never
  grouped under per-scope headings.

## Scopes

Scopes name the mod's product surface or implementation area.

Pipes:

- `transport` — basic item transport pipes
- `routing` — smart/routed/requested item logistics pipes
- `fluids` — fluid pipes, tanks, handlers, transfer rules, pipe sealant, and general fluid mechanics
- `pipes` — shared pipe framework code used across several pipe families (one family takes
  `transport`/`routing`/`fluids`)

Everything else:

- `farming` — farm blocks, soils and fertilizer.
- `automation` — only the shared machine framework, or behavior that crosses several machines
  (shared components, machine base classes, reusable output handling, chunk loading, upgrade
  seams). A single machine takes its own scope.
- `core` — low-level shared infrastructure that is not specific to machines.
- `common` — shared implementation code with no better product or framework scope.
- `fabric` / `neoforge` — loader-specific implementation work.
- `build` (Gradle, mappings, dependencies, publishing), `ci` (GitHub Actions, validation),
  `release` (Release Please, versions, changelog configuration).
- A `docs` change takes the scope of what it documents: a design brief or plan its product scope
  (`docs(farming)`), agent guidance and dev tooling `build`, branch and release process `release`.
  There is no `docs` scope.
- `logistics` is too broad (it is the mod name; smart-pipe behavior is `routing`), and
  `fluid-pipes` was folded into `fluids`.

## Changelog-readable subjects

Let the scope carry the main product noun, and keep enough context that the line stands on its
own: roughly **4–8 words** after the colon.

Good:

```text
balance(macerator): standardize recipe around machine components
change(kiln): restyle with machine shell
fix(pump): stop destroying waterlogged blocks
feat(transport): add gold item pipe
fix(routing): respect destination priority
feat(fluids): add void fluid pipe
refactor(pipes): share connection logic
refactor(automation): extract ChunkLoadingComponent
```

Too short (the changelog line is vague):

```text
balance(macerator): rework recipe
change(kiln): restyle machine
fix(automation): stop voiding byproducts
```

Too long (the scope already names the machine):

```text
balance(macerator): rework the macerator recipe to use the shared machine components
change(kiln): restyle the kiln to match the shared machine-component visual design
```

For the standardized machine recipes, use `balance(<machine>): standardize recipe around machine
components`. Those recipes share one structure: machine frame + redstone coil + machine-specific
ingredients (tanks/buckets for the pump, a pickaxe for the quarry, flint for the macerator).

## Multi-change PRs: the override block

When one PR makes several distinct player-facing changes, list every changelog line in an
override block in the PR body, the PR title first. Each line becomes its own changelog entry:

```text
BEGIN_COMMIT_OVERRIDE
feat(automation): add the sawmill
change(automation): rename Wood Pulp to Sawdust
balance(automation): move wood processing from the macerator to the sawmill
END_COMMIT_OVERRIDE
```

The lines reach every branch by two routes, and `tools/ship_pr.py` keeps them identical:

- On the branch the PR merged into, Release Please reads the block from the PR body.
- A ported commit is not linked to its source PR (GitHub associates it with that branch's open
  release PR), so the other branches read the commit message. The script writes the block's lines
  into the squash commit, and a plain `git cherry-pick` carries them across. `-x` adds a trailer
  after which Release Please keeps only the first extra line, so ports never use `-x`.

A PR with no block ships with its title alone. Write the two markers only as the block's own
lines: Release Please starts the block at the first marker anywhere in the body, prose included.
Edit the block before shipping: an edit after the merge reaches only that branch's changelog,
never the ports.

Confirm the squash commit before porting — a wrong message means a wrong changelog on every branch
the port reaches:

```bash
git log -1 --format='%s%n---%n%b'
```
