# Releases: versioning, release cycle, hotfixes

Every `mc/*` branch releases on its own through **Release Please**, with SemVer build metadata
carrying the Minecraft version and loader. How a PR title becomes a changelog line is in
[`changelog.md`](changelog.md).

## Versioning

1. A PR's conventional title becomes its squash commit (`tools/ship_pr.py`).
2. Release Please reads the commits and opens or updates a release PR per branch
   (`chore(release): release X.Y.Z for mcV`, labelled `autorelease: pending`).
3. Merging the release PR tags and creates the GitHub release.
4. `build-release.yml` builds each loader's jar and publishes to Modrinth and CurseForge.

**Bumps before 1.0.0** (`bump-minor-pre-major` and `bump-patch-for-minor-pre-major` in
`release-please-config.json`):

- `fix:` and `feat:` → patch (0.4.0 → 0.4.1)
- `feat!:` or `BREAKING CHANGE:` → minor (0.4.0 → 0.5.0)

After 1.0.0, `feat:` → minor and `feat!:` → major (standard SemVer).

**Naming:**

- Git tags: `mc{version}-v{semver}` (e.g. `mc1.21.11-v0.4.0`)
- Artifacts: `logistics-{semver}+mc{version}.{loader}.jar`, built to `<loader>/build/libs/`
  (a local build is `logistics-dev.jar`)
- Published version: `{semver}+mc{version}.{loader}` (e.g. `0.4.0+mc1.21.11.fabric`)
- Display name: `Logistics v{semver} for {loader} {version}`

Release Please owns the version numbers. To set one by hand, edit `.release-please-manifest.json`
and commit it.

**Files:** `.release-please-manifest.json` (current version per branch),
`release-please-config.json` (types, changelog sections, bump rules),
`.github/workflows/prepare-release.yml` (runs Release Please), `build-release.yml` (build and
publish), `build-rc.yml` (alpha and RC pre-releases).

## Release cycle

**The version boundary problem:** once any `feat:` PR merges, the pending release grows to include
it, so a quick patch after features land needs the [hotfix workflow](#hotfix-workflow).

Three tools manage this:

1. **`up next` label** — on PRs that are approved but intentionally held past the current patch
   release. While a release PR for the current version is open, label `feat:` PRs `up next`
   instead of merging them. When the patch ships, strip the label: those PRs are now in scope.
2. **Release candidate** (`build-rc.yml`, stage `rc`) — when ready to commit to the release,
   publish `vX.Y.Z-rc.1`. It builds the base branch merged with the open release PR and publishes
   it as a beta pre-release: the Field Test phase. When satisfied, merge the release PR.
3. **Alpha early access** (`build-rc.yml`, stage `alpha`) — when a feature on the pending release
   needs player feedback before its mechanics settle, publish `vX.Y.Z-a.1`, then `-a.2` and so on.
   Same build as an RC, but on the Modrinth/CurseForge **alpha** channel, with notes warning it
   carries no world-compatibility promise. Alphas sort before RCs (`-a.N` < `-rc.N` < release)
   and leave the release PR and manifest alone. A block, item or save format that only ever
   shipped in an alpha owes no alias or migration under the backward-compatibility policy.

Full cycle:

```
fix: PRs merge → release-please opens vX.Y.Z patch PR
Apply "up next" label to any feat: PRs that should wait
Merge patch release PR → vX.Y.Z ships → port to other branches
Strip "up next" from held PRs
Merge feat: PRs → release-please opens the next release PR (leave it open)
Publish -rc.1 via build-rc.yml → Field Test gate
Fix bugs found in testing; re-publish rc.2, rc.3 as needed
Merge the release PR → ships → port
```

## Hotfix workflow

Use this only when a release is already out **and** features have since merged, so a patch for
the shipped version cannot come from the branch head.

It branches off a release tag, not main, so it is plain `git` (no `git town hack`), and the hotfix
branch is pushed and tagged directly with no PR. **Confirm with the user before each push.**

1. Branch from the last release tag:
   ```bash
   git checkout -b hotfix/X.Y.Z mc{version}-vX.Y.Z
   # e.g. git checkout -b hotfix/0.4.1 mc1.21.11-v0.4.0
   ```
2. Apply the fix and commit (imperative subject, no prefix).
3. Tag it: `git tag mc{version}-vX.Y.Z` (e.g. `mc1.21.11-v0.4.1`).
4. Push the branch and the tag, one command each:
   ```bash
   git push origin hotfix/X.Y.Z
   git push origin mc{version}-vX.Y.Z
   ```
5. Run **Actions → Build (Release)** (`build-release.yml`) by hand with `tag` = the new tag and
   `publish` = `true`.
6. Cherry-pick the fix back onto its `mc/*` branch.
7. **Bump the manifest:** in `.release-please-manifest.json` set that branch's version to
   `X.Y.Z` and commit `Bump release-please manifest to X.Y.Z after hotfix`. Release Please reads
   the manifest for the current version; skip this and its next release PR targets the hotfix
   version again, producing a duplicate tag.
8. Delete the hotfix branch:
   ```bash
   git push origin --delete hotfix/X.Y.Z
   git branch -d hotfix/X.Y.Z
   ```
