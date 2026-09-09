---
name: backport
description: >-
  Port already-merged commits between the repo's mc/* branches in any direction
  (backport down to older branches, forward-port up to newer, or sideways), one
  commit at a time, adapting for per-version API differences. Use when the user
  asks to backport/forward-port/port commits, a PR, or a range across mc/*
  branches, or wants feature parity across versions. The end state is the same
  commit messages on each target branch (commits already present on a branch are
  skipped, not re-added as empty commits).
---

# Port commits across mc/* branches

Port already-merged commits from a **source** `mc/*` branch to one or more **target** `mc/*`
branches. Cherry-pick is the primary mechanism; conflicts are resolved by adapting
version-specific APIs while keeping behavior identical.

**Direction is whatever the task needs** — the most common case is backporting from the main
branch *down* to older maintenance branches, but the same process forward-ports *up* (e.g. a
legacy-only fix that originated on the highest affected branch) or sideways. Only the *direction
of API translation* changes (see the cheat-sheet).

**Branch roles are not hardcoded here** — they advance over time (the main branch moves to the
newest MC release, e.g. `mc/26.1` → `mc/26.2`, and older branches age out). Read the current
roles from `CLAUDE.md` ("Branch Strategy" / "Cross-Version Workflow"); discover the actual
branches and worktree paths with `git`.

## Ground rules

- **Sync with remote FIRST.** Before scoping or cherry-picking, `git fetch` and fast-forward
  every branch involved — the source *and* each target — to its `origin/<branch>` tip. The local
  worktrees drift behind while you work; porting onto a stale tip means the final `push` is
  rejected non-fast-forward and you must rebase your picks onto the moved remote anyway (`git
  rebase --onto origin/<branch> <old-base> <branch>`) — and worse, a "missing" commit may already
  be on the real remote tip. Do the sync up front so the base is current. Re-fetch right before
  pushing too, in case the remote moved again mid-port.
- **One target at a time:** finish one target branch fully, then move to the next. When
  backporting down, go newest-MC target first (e.g. `mc/26.1 → mc/1.21.11 → mc/1.21.1`); adjust
  to the actual source/targets the task names.
- **Order within a branch:** oldest commit → newest (so each builds on the last).
- **Cherry-pick directly onto the target branch** — no feature branch, no PR. This is the
  sanctioned exception to branch protection for porting already-merged commits.
  **Except into the default branch.** Its ruleset carries no bypass, so a direct push there is
  refused for everyone including admins and agent sessions. A change that must go *up* into the
  default branch needs a normal PR for that one hop; from there it ports outward as usual. Down
  and sideways ports are unaffected. Discover the default branch with
  `git rev-parse --abbrev-ref origin/HEAD` — never hardcode it.
- **Push when verification passes; stop when it cannot.** Finish the cherry-picks, run the
  verification below, then push (`git -C <worktree> push origin <branch>`) — an unpushed port
  leaves the branches divergent, and divergence is what makes the *next* port conflict.
  **Stop and hand off before pushing** when the change alters in-game behaviour that the
  automated suites do not cover, when you had to guess at an adaptation, or when the user asked
  to game-test first. Say which case applies.
- **The `pre-push` hook runs the unit tests** for any `mc/*` destination, so a broken port is
  caught before it lands. Let it run. Pre-release branches are exempt automatically (decided
  from `minecraft_version`; see "Branch stage" below), so a push there running no checks is
  expected rather than a bypass. Only `--no-verify` when the commit contains no code at all,
  and say so.
- **Identical messages, not empty commits.** The goal is the same commit *messages* on each
  branch. If a commit's content is already present (see "already-present detection"), **skip
  it** — do not record an empty placeholder commit (it would create duplicate changelog
  entries). Confirm this skip policy with the user if a commit turns out already-present.

## Branch stage

Some steps treat pre-release branches differently. **Derive the stage from metadata, never from
a branch list** — the roles rotate, and today's alpha is next year's main:

```bash
git show "origin/<branch>:gradle.properties" | sed -n 's/^minecraft_version=//p'
```

Digits and dots only (`26.2`, `1.21.11`) → **released**. Anything carrying a qualifier
(`26.3-snapshot-7`, `26.3-pre-1`, `26.3-rc1`, `24w14a`) → **pre-release**. The whole alpha
lifecycle stays pre-release until the version goes bare.

A pre-release branch tracks upstream snapshot artifacts that get rotated and deleted, so its
Gradle build often cannot resolve dependencies at all — and a failed *configuration* fails every
task, not just the one you wanted. When the verification steps below cannot run there, say so
and rely on CI rather than forcing them.

## Where to work: worktrees, or without them

Each `mc/*` branch may have its own sibling worktree (e.g. `../logistics-mc-1.21.11`).
**Discover them — don't assume paths or versions:** `git worktree list`. They share one `.git`,
so a commit in one is visible to the others. Operate on each via `git -C <abs-path>` and absolute
paths (the sandboxed shell resets cwd between calls, and `cd` can trigger a prompt).

**Do not require them.** A clone with no per-branch worktrees is a perfectly normal setup, and
the whole procedure works there. Resolve the working directory for a target branch like this:

```bash
# 1. Is there already a worktree on this branch?
wt=$(git worktree list --porcelain | awk -v b="refs/heads/$BRANCH" '
  /^worktree /{p=$2} $0=="branch "b{print p; exit}')

# 2. If not, make a temporary one. It shares the same .git, costs one checkout,
#    and leaves the user's current checkout untouched.
if [ -z "$wt" ]; then
  wt=$(mktemp -d)/port-$BRANCH
  git worktree add "$wt" "$BRANCH"
fi
```

Do the lookup first and do not skip it: `git worktree add` **refuses a branch that is already
checked out anywhere**, including in the user's main checkout, and fails with
`fatal: '<branch>' is already used by worktree at ...`. Step 1 is what makes step 2 safe.

Clean up any worktree you created (`git worktree remove "$wt"`) once the port is pushed — but
only ones you created, and never one the user is working in.

A temporary worktree is strongly preferred over checking the branch out in place: switching
branches under the user's feet loses their working state, and a mid-port conflict would strand
them on the wrong branch. If you genuinely cannot create one, stash-and-restore explicitly and
say that you did.

Gradle builds in a fresh worktree start cold, so the verification steps are slower there than in
an established one. That is a reason to allow more time, not to skip them.

### ⚠️ When sibling sessions port the same commit in parallel

A fan-out port (one agent per target branch) means several Minecraft dev servers run at once out of
sibling worktrees. Two ways to trash a sibling's run — both observed:

- **Never `pkill` on an unanchored worktree path.** `pkill -f "logistics-mc-1.21.1"` also matches
  `logistics-mc-1.21.11` (prefix!) and kills that session's server mid-run. Anchor with a trailing
  slash (`logistics-mc-1.21.1/`), or better, kill by the Gradle-reported PID or use
  `pgrep -fl <pattern>` to *inspect* the match list before killing anything. Generic patterns
  (`devlaunchinjector`, `GradleWorkerMain`) hit every worktree — never use them.
- **Dedicated servers collide on port 25565.** A second `runServer` fails to bind and dies, which
  looks like a broken port rather than a busy port. Either serialize the server runs across
  sessions, or give each worktree a distinct `server-port` in `<wt>/fabric/run/server.properties`.
  Always grep your own log for `FAILED TO BIND|BindException` and for the
  `Done (…)! For help, type` ready line before trusting a "clean" run.

## Procedure

### 0. Scope it
0. **Sync first (see ground rule):** `git fetch origin`, then fast-forward the source and every
   target worktree to `origin/<branch>` (`git -C <wt> merge --ff-only origin/<branch>`, or `git -C
   <wt> pull --ff-only`). Scope and cherry-pick onto the *current* remote tip, not a stale local one.
1. Find the unported commits: `git log --oneline --reverse <last-shared>..<source-branch>`.
   A target branch's tip is often a squashed feature port (e.g. `#511`) that already bundled
   several later commits — so the count of "missing" commits is usually smaller than the raw
   range.
2. Filter to the set the user wants. Default to **player-facing** `feat`/`fix`/`perf`. Also
   include **`refactor`s that touch shared code** — porting them keeps the branches structurally
   aligned, which is what makes *future* cherry-picks across that code apply cleanly (a refactor
   left on one branch makes every later port through those files conflict). Pure infra
   (`chore`/`docs`/`ci`/`build`) usually stays on its origin branch unless it changes shared
   structure. Confirm the set with the user.
3. Track per `(branch, commit)` with the task tools — it's many steps.

### 1. Per commit, oldest → newest (do this loop for each target branch)

For each commit SHA:

1. **Cherry-pick:** `git -C <wt> cherry-pick -x <sha>` (the `-x` adds a "cherry picked from" trailer).
2. **Already-present detection (critical):** after resolving any conflicts to HEAD, check
   `git -C <wt> diff HEAD --stat`. If the **net diff is empty**, the commit's content is already
   on the branch (bundled in an earlier squash) → `git -C <wt> cherry-pick --skip` and move on.
   - *Don't trust shortcuts:* reverse-`git apply --check` and patch-id (`git cherry`) give false
     "missing" verdicts here because package renames (e.g. `fluid/` → `pipe/`) defeat them. The
     real apply (empty net diff) is the only reliable signal.
   - For a feature that *looks* present, verify it's actually registered (block/item registration
     + assets) before skipping — the earlier squash may have bundled it incompletely.
3. **Resolve conflicts.** Keep behavior identical and bias toward *structural convergence* —
   minimizing how much the target file's shape drifts from the source is what makes *future*
   cherry-picks apply cleanly.
   - **Default: resolve hunk-by-hunk.** Accept the source commit's structural changes wherever
     they apply, and hand-translate only the version-specific API lines inside each conflicting
     hunk (see cheat-sheet). This pulls the target toward the source's structure without
     discarding anything — best for both alignment and regression-safety. The whole-file
     `--ours`/`--theirs` moves below are fallbacks for when the 3-way merge is too tangled to
     resolve line-by-line. (In a cherry-pick, `--ours` = the target branch / HEAD; `--theirs` =
     the **source commit's entire version** of the file, not just its change.)
   - **Classify why the file conflicts — forced vs. incidental divergence:**
     - **Incidental** (same logic; conflict is drift, import/package, nearby edits): the two
       structures *should* converge. Pull toward the source — hunk-level, or whole-file
       `git checkout --theirs <file>` + re-fix the few API lines **only if** the file is
       structurally near-identical AND has no target-only changes. ⚠️ `--theirs` silently reverts
       any target-only history in that file (legacy-only fixes that never existed on the source),
       so re-verify rather than trusting a clean compile.
     - **Forced** (the platform gives the target a *different API shape* for the same job — e.g.
       the render path, NBT getters): the source's code can't compile on the target, so `--theirs`
       imports non-compiling code you'd rewrite anyway. Keep the target's idiom: `git checkout
       --ours <file>` + re-apply just the commit's *semantic* change by hand, or rewrite mirroring
       a sibling (see cheat-sheet). Structural alignment is impossible per-commit here.
   - **Flag every forced-divergence conflict** as an abstraction candidate. The durable fix for
     forced divergence is a shared shim that makes the file identical across versions
     (`ResourceId`, `NbtCompat`, `MachineModels` are existing examples) — *not* re-resolving the
     same conflict on every future port. Note which file/API forced the divergence, and **surface
     these in the final report** so the user can decide whether to introduce/extend an abstraction
     (especially if the same file forces it repeatedly).
4. **Build + run the full test suite:** `./gradlew build` (compiles all loaders + runs unit
   tests). Loader builds skip `:common:test` source set quirks, but `build` covers it here. Fix
   every compile error and test failure.
   ⚠️ **Whether `build` also runs the gametests differs per branch** — it does on `mc/1.21.1`, it
   does NOT on `mc/26.2` (separate tasks there). Never read a green `build` as green gametests.
   **Always run both suites explicitly and check the counts:** `./gradlew :fabric:runGameTest`
   and `./gradlew :neoforge:runGameTestServer`. Counts drift as tests are added, so compare
   against a baseline run on the branch tip *before* your cherry-pick rather than a number
   memorised here. A NeoForge-only failure is invisible to the Fabric suite — see the
   mock-player entry in the cheat-sheet for a case that passes Fabric and fails NeoForge.
5. **Boot each loader's server and scan the log.** Compile + unit tests + gametests miss
   *data-load* problems — missing/malformed recipes, loot tables, tags, advancements, broken
   model/blockstate references. These only surface when a server loads the data packs, and they
   often log as **non-fatal errors that don't fail the build** (the recipe `JsonParseException`
   in the cheat-sheet was *logged*, not thrown). So run a server per loader far enough to load
   registries + data, then stop it, and grep the log:
   - Fabric `./gradlew :fabric:runServer`, NeoForge `./gradlew :neoforge:runServer` (accept the
     EULA). The fabric gametest already boots a Fabric server, but NeoForge isn't gametested on
     the legacy branches, so run it explicitly. Run in the background, wait for the
     `Done (…)! For help, type` ready line (or a short timeout), capture stdout/stderr to a log,
     then stop it (`stop` to stdin, or kill the background task).
   - Scan for `ERROR` / `WARN` / `Exception` / `Failed to parse` / `Missing` /
     `Unknown recipe|loot|tag`. If unsure whether a line is pre-existing, diff against a baseline
     run on the branch tip before the cherry-pick.
   - **Fix errors caused by the cherry-picked commit** (e.g. the 1.21.1 recipe key format, an
     `items/**` model path 1.21.1 ignores). **Flag anything unrelated or ambiguous to the user** —
     don't silently fix pre-existing or out-of-scope log noise; let them decide.
6. **Stage everything** (`git -C <wt> add -A`), including new/renamed/deleted files, then
   `GIT_EDITOR=true git -C <wt> cherry-pick --continue` (preserves the original message).
   - A clean cherry-pick auto-commits; only conflicted ones need `--continue`.
7. **Format + amend (only if not pushed):** spotless is skipped mid-cherry-pick, so after the
   commit run `./gradlew spotlessApply`; if it changed anything, `git commit --amend --no-edit
   --no-verify`. Finish with `spotlessCheck`. Fold any post-commit bug fixes into the commit via
   `--amend` too (the commit isn't pushed yet).

### 2. Finish
- Verify each branch: the same set of commit messages on top of the shared base, no cherry-pick in progress
  (`.git/sequencer` gone, no `CHERRY_PICK_HEAD`), clean working tree.
- Report what landed vs. skipped-as-already-present, and the per-version adaptations made.
- **List the forced-divergence conflicts you flagged** (file + API that forced it) as
  abstraction candidates — call out any file that forced divergence on more than one commit, and
  consider recording it in auto-memory for the next port.
- **Push, unless the ground rule says to stop.** Re-fetch first in case the remote moved.

### 3. Verify the branches actually converged

A port is not done because the cherry-pick succeeded — it is done when the branches agree. For
every file the port touched, compare the blob across all `mc/*` branches:

```bash
for b in $(git branch -r --list 'origin/mc/*' | sed 's|.*origin/||'); do
  printf "%-12s %s\n" "$b" "$(git show "origin/${b}:<file>" | shasum | cut -c1-10)"
done
```

Identical hashes mean the port landed. Differing hashes are fine **only** when you can name the
version-specific reason — otherwise you have just created the drift that makes the next port
conflict.

For `.github/workflows/code-review.yml` this is mandatory rather than cosmetic: the review
action refuses to run when that file differs from the copy on the default branch, and it fails
*green*, so a mismatched branch silently stops being reviewed with nothing to notice.

## Cross-version API deltas cheat-sheet

Different MC versions use different Minecraft/Fabric APIs. Each entry below states what each
version uses — when porting, translate from the **source** version's form to the **target**
version's form (this flips depending on direction). Reach for the project's abstractions first;
adapt at the boundary only where unavoidable. **When you discover a new delta, add it here and to
auto-memory.** The older the MC version, the more divergent (today `mc/1.21.1` is the most so:
older render + data + gametest APIs).

**Project abstractions (use these; don't reintroduce raw types):**
- **`core.lib.resource.ResourceId`** — hides `Identifier` (1.21.11/26.x) vs `ResourceLocation`
  (1.21.1). Use `ResourceId` everywhere; never "fix" a port by swapping the raw type. Enforced by
  the `checkImportBoundaries` lint (`// raw-id-ok` opt-out). See memory `feedback_use_resourceid`.
- **`core.lib.compat.NbtCompat`** — hides NBT getters returning `Optional<T>` (1.21.5+, incl.
  1.21.11/26.x) vs primitives with implicit defaults (1.21.1–1.21.4). **Gotcha:** unlike
  ResourceId this is *not* transparent — `NbtCompat.java`'s body uses the Optional API, so when
  the file itself is backported to `mc/1.21.1` each method must be swapped to the primitive form.
  The per-method javadoc spells out the exact 1.21.1 replacement.

**Rendering (`mc/1.21.1` uses a different BER API entirely):**
- 26.x / 1.21.11: `BlockEntityRenderer` with `createRenderState`/`extractRenderState`/`submit`,
  `SubmitNodeCollector.submitBlockModel(...)`, `CameraRenderState`, `RenderTypes`,
  `BlockStateModelPart`. `MachineModels.model(key)` → `BlockStateModel`;
  `submitBlockModel(matrices, layer, model, r,g,b, light, overlay, 0)`.
- 1.21.1: classic `render(entity, partialTick, PoseStack, MultiBufferSource, light, overlay)`,
  drawing `BakedQuad`s via `VertexConsumer.putBulkData`. No render-state classes (delete them).
  `MachineModels.quads(key)` → `List<BakedQuad>`; `bufferSource.getBuffer(RenderType.cutout())`;
  light via `LightTexture.pack(level.getBrightness(BLOCK,pos), level.getBrightness(SKY,pos))`.
  `CameraRenderState` lives at `net.minecraft.client.renderer.state.CameraRenderState` on 1.21.11.
  Mirror an existing sibling renderer (`FluidPipeBlockEntityRenderer`, `CableBlockEntityRenderer`).
  See memory `project_render_chain_porting`, `project_code_render_refactor`.

**Fluid sprite rendering (`FluidBoxRenderer` — 26.x vs *both* 1.21.x):** 26.x resolves a fluid's still
sprite + tint through the unified vanilla fluid model — a private `resolveModel(Fluid)` doing
`Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluidState)` →
`model.stillMaterial().sprite()` (with a `Resolved`/`FluidModel` record). **1.21.11 AND 1.21.1** have no
`getFluidStateModelSet`; `FluidBoxRenderer` instead delegates the sprite/tint lookup to the
loader-specific `FluidSpriteLookup.resolve(fluid, level, pos)` from its public `resolve(...)` (no
`resolveModel`/`Resolved`). So a change to 26.x's `resolveModel` conflicts on 1.21.x — re-apply the
equivalent at the shared `FluidBoxRenderer.resolve` entry point (both `resolve` and `resolveForGui` funnel
through it). E.g. #696's empty-fluid guard `if (fluid == Fluids.EMPTY) return null;` lives in
`resolveModel` on 26.x but in `resolve` on 1.21.x (add the `Fluids` import there). Note this is a delta
against **both** legacy branches, not just 1.21.1 — and it's *distinct* from the custom-fluid
registration/tint divergence in `project_crucible_epic_backport`. Abstraction candidate: a shared
fluid-sprite resolution seam. When the same conflict repeats across branches, `git rerere` can auto-apply
your first resolution to identical later ones (it did here for 1.21.1 after 1.21.11).

**GUI slot tooltips (`mc/1.21.1`):** on 1.21.1, `AbstractContainerScreen.render()` does **not**
auto-draw hovered-slot item tooltips — every container screen must override
`render(GuiGraphics, mouseX, mouseY, delta)` to call `super.render(...)` then
`renderTooltip(graphics, mouseX, mouseY)`. On 1.21.11/26.x the base class draws slot tooltips
itself (26.x via the new render-state `extractTooltip` path), so a screen overriding only
`renderBg`/`extractBackground` still shows tooltips. **Consequence when forward-porting a machine
screen UP to 1.21.1** (or adding one there): without the manual override, item tooltips silently
vanish while the GUI otherwise looks fine. Mirror the existing `MaceratorScreen` override.

**Container-menu click type (`AbstractContainerMenu.clicked`):** the action-type parameter is
`net.minecraft.world.inventory.ContainerInput` on **26.x** and
`net.minecraft.world.inventory.ClickType` on **both 1.21.x** (same `QUICK_MOVE` constant). It sits
in an `@Override` signature, so a static helper can't hide it. **Quarantined** by the shared base
`common/.../pipe/ui/CustomSlotScreenHandler` (#749) — it owns the one `clicked` override and
delegates to a version-stable hook `handleCustomSlotClick(int slotIndex, int button, boolean
quickMove, Player player)` (return `true` to swallow, `false` to fall through to `super.clicked`).
The 8 pipe UI `*ScreenHandler`s implement the hook and are byte-identical across branches; only the
base file carries the token (import + parameter type + `QUICK_MOVE` — 3 lines). When porting the
base down to 1.21.x, swap those three `ContainerInput` references to `ClickType`; the subclasses
port clean. ⚠️ Resolve conflicts by checking out only the **specific files the commit touched** from
a converge tree — a whole-`pipe/ui`-dir `checkout` clobbers 1.21.1's own `*Inventory.java` files
(registry-lookup `Optional`-vs-nullable divergence) with the wrong version. See memory
`project_clicktype_containerinput_delta`.

**Recipe `assemble` arity (`Recipe#assemble`):** the vanilla override is `assemble(I input)` on
**26.x** and `assemble(I input, HolderLookup.Provider provider)` on **1.21.x** (the body ignores
both — machine recipes return a fixed result). Another `@Override`-signature delta. **Quarantined**
by the generic base `core/lib/recipe/AbstractLogisticsRecipe<I extends RecipeInput>` — it owns the
version-specific `assemble` override and delegates to a version-stable `assembleResult()` hook that
subclasses implement (`AlloySmelterRecipe`, `CrucibleRecipe`, `FabricatorRecipe`,
`MaceratorRecipeWrapper`, `RefineryRecipe`, `SawmillRecipe`). For **call sites that invoke a vanilla
recipe's** `assemble` (e.g. `SmeltingRecipeResolver` on a vanilla `SmeltingRecipe`), route through
`core/lib/compat/RecipeCompat.assemble(recipe, input, level)` — 26.x `recipe.assemble(input)`, 1.21.x
`recipe.assemble(input, level.registryAccess())`. When porting the base/compat down, add the
`HolderLookup.Provider` param and the `level.registryAccess()` arg. ⚠️ On **1.21.1** the recipe
classes ALSO carry a *separate, much larger* divergence — the whole old vanilla `Recipe` interface
(`group`/`placementInfo`/`recipeBookCategory`/`display`/`showNotification`/`getResultItem`/
`canCraftInDimensions`) vs the new 26.x/1.21.11 one — so the `assemble` base alone does NOT converge
1.21.1's recipe files; that interface delta is its own (bigger) cluster. See memory
`project_recipe_assemble_arity_delta`.

**Chat link events (`ClickEvent`/`HoverEvent`):** 26.x/1.21.11 use sealed-record subtypes —
`new ClickEvent.RunCommand(cmd)`, `new ClickEvent.OpenUrl(URI)`, `new HoverEvent.ShowText(component)`,
and pattern-match them (`x instanceof ClickEvent.RunCommand run` → `run.command()`; `ClickEvent.OpenUrl
o` → `o.uri()`). **1.21.1 predates the records:** single-class form `new ClickEvent(ClickEvent.Action
.RUN_COMMAND, cmd)` / `new ClickEvent(ClickEvent.Action.OPEN_URL, urlString)` / `new HoverEvent(HoverEvent
.Action.SHOW_TEXT, component)`; read back via `getAction()` + `getValue()` (a String — OPEN_URL takes/
returns a **String**, not a `URI`, so drop `URI.create(...)`). Hit `CrashReportNotifier` + its test on
the #633 port.

**Operator permission check:** 26.x uses `Commands.LEVEL_GAMEMASTERS.check(player.permissions())`
(LEVEL_GAMEMASTERS is a permission-check object). **1.21.1:** `LEVEL_GAMEMASTERS` is a plain `int` and
`player.permissions()` doesn't exist — use `player.hasPermissions(Commands.LEVEL_GAMEMASTERS)` (or
`source.hasPermission(Commands.LEVEL_GAMEMASTERS)` for a `CommandSourceStack`).

**NeoForge `FMLEnvironment`:** 26.x exposes methods `getDist()` / `isProduction()`; **1.21.1 exposes
fields** `dist` / `production` (so `FMLEnvironment.dist.isClient()`, `!FMLEnvironment.production`). Also
`SharedConstants.getCurrentVersion().id()` (26.x) does not resolve on 1.21.1 — get the MC version via
`ModList.get().getModContainerById("minecraft").map(c -> c.getModInfo().getVersion().toString())`
(mirrors the NeoForge `modVersion()` idiom). See memory `project_crash_reporting`.

**World height:** 1.21.1 has `Level.getMinBuildHeight()` / `getMaxBuildHeight()` (the latter is
**exclusive**); 26.x/1.21.11 renamed them to `getMinY()` / `getMaxY()` (`getMaxY()` is inclusive).
So `> getMaxY()` becomes `>= getMaxBuildHeight()`.

**Lake feature config (`LakeFeature.Configuration`):** the record constructor is **5-arg on mc/26.2**
— `(fluid, barrier, canPlaceFeature, canReplaceWithAirOrFluid, canReplaceWithBarrier)` — but **2-arg
`(fluid, barrier)` on mc/26.1 AND both 1.21.x** (the three placement flags were added in MC 26.2).
Note this is one of the few deltas that splits **26.1 from 26.2**, not just 26.x from 1.21.x.
Quarantined by `core/lib/compat/LakeConfigCompat.withBarrier(base, barrier)` (used by
`OilSeepFeature`) — 26.2's body forwards `base.canPlaceFeature()/canReplaceWithAirOrFluid()/
canReplaceWithBarrier()`; the legacy body just `new LakeFeature.Configuration(base.fluid(), barrier)`.
See memory `project_lakeconfig_arity_delta`.

**Chunk tickets:** 26.x/1.21.11 share the new API — `TicketType` is a `BuiltInRegistries.TICKET_TYPE`
record (`new TicketType(timeout, FLAG_*)`), add via `chunkCache.addTicket(new Ticket(type, level), pos)`
with `level = ChunkLevel.byStatus(FullChunkStatus.BLOCK_TICKING)`. `mc/1.21.1` is a FORCED rewrite:
no TICKET_TYPE registry, generic `TicketType<T>` made (not registered) by
`TicketType.create(name, Comparator<T>, timeoutTicks)`, add via
`chunkCache.addRegionTicket(type, pos, radius, value)` — which builds a ticket at level
`ChunkLevel.byStatus(FULL) - radius`, so `radius = byStatus(FULL) - byStatus(targetStatus)` (BLOCK_TICKING→1).
`ChunkPos` accessors also differ on BOTH legacy branches: fields `.x`/`.z` + `new ChunkPos(BlockPos)`
(not 26.x's `.x()`/`.z()`/`ChunkPos.containing()`). See memory `project_chunk_ticket_api_delta`.

**Recipes (`mc/1.21.1`):** ingredients must be objects `{"item":"..."}`, not the bare strings `"..."`
allowed on 1.21.4+. This applies to shaped-recipe `key` entries AND to custom recipe types with an
`Ingredient` codec field (e.g. the `logistics:macerator` `ingredient` field — the 1.21.1 `Ingredient`
codec rejects bare strings). A bad recipe throws `JsonParseException` at data load (caught at gametest
server startup). Cross-check the bare-string vs object form against an *existing* recipe on the target
branch before continuing. Reconfirmed on the `logistics:sawmill` `ingredient` field (#824's five
per-seed recipes) — **watch out for truncated log capture hiding multiple failures**: piping a full
`./gradlew build` through `tail -N` can drop all but the last "Parsing error loading recipe" line even
though every bad file failed independently (RecipeManager logs-and-skips per file, so the build itself
still succeeds); redirect to a file and `grep` the whole thing, and sanity-check the final
`Loaded N recipes` count against a pre-fix baseline rather than trusting an empty grep on a truncated tail.

**Item models (`mc/1.21.1`):** ignores `assets/<ns>/items/**` (the 1.21.4+ item-model-definition
system). Put the item model at `assets/<ns>/models/item/.../<name>.json`
(`{"parent":"<ns>:block/..."}`) and drop the `items/**` file. See memory
`project_mc1_21_1_no_item_model_defs`.

**Gametests (`mc/1.21.1`):** annotation is vanilla `net.minecraft.gametest.framework.GameTest`
(not Fabric `net.fabricmc.fabric.api.gametest.v1.GameTest`); requires
`@GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = N)` (`timeoutTicks`, not
`maxTicks`). `GameTestHelper.getBlockEntity(pos)` returns raw `BlockEntity` → cast (no typed
two-arg overload). No `assertionException(String)` → use `assertTrue(boolean, String)` (this also
works as a drop-in replacement for `throw context.assertionException(msg)` inside a
`succeedWhen(() -> {...})` polling callback — `assertTrue(false, msg)` throws the same way, so
polling retries exactly like the original). Mirror an existing 1.21.1 gametest (`PipeFlowGameTest`,
`CableGameTest`).

**Mock players in gametests (`makeMockServerPlayer` is 26.2-ONLY):** confirmed absent via `javap` on
mc/26.1, mc/1.21.11 AND mc/1.21.1 — `GameTestHelper.makeMockServerPlayer(GameType)` was added in 26.2.
⚠️ **The obvious substitute is a trap that fails on NeoForge only.**
`context.makeMockServerPlayerInLevel()` builds a real `Connection` + `EmbeddedChannel` and calls
`PlayerList.placeNewPlayer` — a *genuine login* — which fires `PlayerEvent.PlayerLoggedInEvent` →
`NeoForgePlayerJoinEvents.onLogin` → a JEI `logistics:automation/sync_machine_recipes` payload sent
over a channel the test connection never negotiated. **NeoForge rejects it** ("Payload … may not be
sent to the client!"); **Fabric tolerates it**. So that path compiles and passes the Fabric gametests
while failing the NeoForge ones — you will not catch it without running
`./gradlew :neoforge:runGameTestServer`.
**Working fix** (identical code on 26.1 and 1.21.11): construct a **no-login** `ServerPlayer` inline —
`new ServerPlayer(MinecraftServer, ServerLevel, GameProfile, ClientInformation.createDefault())` with
`gameMode()` **overridden** to return the desired `GameType`, then apply
`GameType.updatePlayerAbilities(Abilities)` by hand (this mirrors what 26.2's helper does internally).
`setGameMode` is unusable without a connection. All four constituent APIs exist unchanged on 26.1 and
1.21.11. Never use `makeMockPlayer(GameType)` — it is NOT a real `ServerPlayer`, so loader-side
`instanceof ServerPlayer` branches (e.g. Fabric's `FluidStorageUtil` creative-inventory-grant path)
silently take the wrong branch. **Abstraction candidate:** this single file
(`GlassTankBucketGameTestBody`) has now forced the same divergence on two branches with an identical
fix — a shared `GameTestPlayers.mockServerPlayer(context, gameType)` helper in the gametest source set
would make it identical across 26.2/26.1/1.21.11. See memory
`project_mock_server_player_gametest_delta`.

**Data-driven GameTest registry stack (`TEST_FUNCTION` / `TestEnvironmentDefinition`):** the
DeferredRegister-based NeoForge GameTest model (PR #875) works on **26.2, 26.1 and 1.21.11** — all
three have `BuiltInRegistries.TEST_FUNCTION`, `GameTestInstance`, `FunctionGameTestInstance`,
`TestData` (5-arg ctor `(EnvironmentType, Identifier, int, int, boolean)`), and a NeoForge
`RegisterGameTestsEvent` exposing `registerEnvironment(...)` / `registerTest(...)`. The **only** delta:
`TestEnvironmentDefinition` is **generic on 26.2** (`TestEnvironmentDefinition<?>`) but **non-generic on
1.21.11** — a 2-line change confined to `GameTestRegistrationSupport.java`.
**`mc/1.21.1` is the hard cutoff:** it has *none* of those classes and no `TEST_FUNCTION` registry key,
and its NeoForge (21.1.x) `RegisterGameTestsEvent` exposes only the legacy `register(Class)` /
`register(Method)`. There, NeoForge gametests require the legacy `@GameTestHolder(MODID)` +
`@PrefixGameTestTemplate(false)` model with vanilla per-method `@GameTest` on `public static` methods,
`LogisticsGameTestMod` reduced to a bare `@Mod` shell, and `GameTestCase`/`GameTestFunctions`/
`GameTestRegistrationSupport` deleted outright. Docs:
https://docs.neoforged.net/docs/1.21.1/misc/gametest — **but two of its claims are wrong for this repo:**
`setForceExit false` is NeoGradle-only (moddev-gradle 2.0.141's `RunModel` has no `forceExit`; runs exit
fine without it), and it omits that **`neoforge.enabledGameTestNamespaces` is mandatory** — without it the
run reports zero tests and still **exits green**. Shipped on mc/1.21.1 as `a39d3f9b` (Fabric 205/205,
NeoForge 189/189).
That model also **mandates a real `.nbt` structure** under `data/<ns>/structure/` (singular `structure`,
no auto-empty fallback). Fabric's `fabric-gametest-api-v1:empty` is unreachable — it's loaded by a Fabric
*mixin* from `gametest/structure/*.snbt`, not shipped as an `.nbt`. Author one instead: an all-air
structure in the vanilla palette/blocks format, **DataVersion 3955** for 1.21.1 (8×8×8 works).
**Confirmed non-deltas on 1.21.11:** the typed `getBlockEntity(pos, Class)` overload and Fabric's
`net.fabricmc.fabric.api.gametest.v1.GameTest` annotation both work — the warnings above are
1.21.1-only. CI pins `java-version: '21'` on 1.21.11 vs `'25'` on 26.2.

**⚠️ A "hung" gametest server is a FAILED TEST, not a scale/timeout limit.** When a test doesn't
complete, `GameTestRunner` re-queues its whole batch forever inside
`createStructuresForBatch → StructureUtils.clearSpaceForStructure`, starving the server tick and
emitting ~150k `Running test batch` lines — **often with no failure ever printed**. This convincingly
masquerades as "too many tests at once" (small batches pass, large ones wedge, because the large ones
happen to contain the bad test). Diagnose with a thread dump of the wedged server, then bisect by
running classes in isolation. Give each registration class a distinct `batch` name so a wedged run
names its own culprit — batch names are loader/version-specific anyway, so this costs no cross-branch
convergence.

**Out-of-bounds block placement wedges 1.21.1 gametests** (same symptom as above). Placing a block at
relative **z = −1** hangs the run; **x = −1 is tolerated** (Kiln/Crucible/AlloySmelter/QuarryMining all
do it and pass). Fix by seating the machine further into the structure (e.g. `z=3`). Hit in
`RefineryGameTestBody` and `FluidPumpGameTestBody` — those coordinate shifts are worth forward-porting
to the other branches so the shared bodies stay identical.

**`ServerPlayer.gameMode()` doesn't exist on 1.21.1** — it's a `ServerPlayerGameMode` *field*, and both
`setGameMode` and `changeGameModeForPlayer` route through `onUpdateAbilities()` → the absent connection.
So the no-login mock player (see the mock-player entry above) must override **`isCreative()` /
`isSpectator()`** instead of `gameMode()`. `ClientInformation.createDefault()` and the 4-arg
`ServerPlayer` constructor do exist on 1.21.1.

**Jade compat:** the Jade dep uses Loom's **remapped** configurations on 1.21.x, so every
`compileOnly`/`runtimeOnly`/`clientCompileOnly`/`clientRuntimeOnly` in a 26.x `fabric/build.gradle`
Jade hunk becomes `modCompileOnly`/`modRuntimeOnly`/`modClientCompileOnly`/`modClientRuntimeOnly`.
Cross-check against the JEI dep a few lines above in the same file — it already uses the `mod*`
names, so it proves which names exist on that branch's Loom. Jade 15.x (1.21.1) lacks `JadeUI`
(use `IElementHelper`), but `IWailaPlugin`/`@WailaPlugin`/`IWailaCommonRegistration`/
`IWailaClientRegistration` and `ServiceLoader`-based plugin splitting all work unchanged on 15.10.x.
Full gotchas in memory `project_jade_cross_version_port`.
⚠️ A 26.2 Jade hunk in `fabric/build.gradle` usually conflicts *together with the adjacent Spark
profiler dep*, which is 26.2-only (no `spark_fabric_version` on 1.21.1) — drop that half as context
bleed rather than porting it.

**Loader-entrypoint / source-set bugs are invisible to build + unit tests + gametests.** Loom stamps
client-source-set classes into the jar manifest as `Fabric-Loom-Client-Only-Entries`, and Fabric
Loader hides those on a dedicated server — so a `fabric.mod.json` entrypoint (which has **no
per-entrypoint `environment` field**) pointing at a client-source class throws
`ClassNotFoundException` on servers only. To verify such a port:
- Unfold the manifest and assert the class moved *out* of the client-only list:
  `unzip -p <jar> META-INF/MANIFEST.MF` then re-join continuation lines (`re.sub(r'\r?\n ','',raw)`)
  before splitting `Fabric-Loom-Client-Only-Entries` on `;` — the manifest wraps at 72 cols, so a
  naive grep silently misses entries.
- Boot `:fabric:runServer`, wait for `Done (…)! For help, type`, then grep the **whole** log file
  (the failure can appear more than once; `tail` hides it).
- For a positive client-side signal, wipe `fabric/run/config/jade/` and A/B it: a server-only boot
  regenerates `sort-order.json` with just the server-data-provider UIDs, and a subsequent
  `runClient` adds exactly the client component UIDs. Diffing that one file proves both halves
  register on the right side. (`run/` is gitignored; restore the user's config afterwards.)
- Dev servers only reproduce this if Jade is actually on the server classpath. A jar dropped in
  `<wt>/fabric/run/mods/` puts it there regardless of `modClientRuntimeOnly` — confirm by checking
  the version the log loaded against `gradle.properties` (a mismatch proves it came from `run/mods`,
  so the baseline is genuine and needs no synthetic dependency edit).

**Profiler / tick-section instrumentation (`LogisticsProfiler`):** `net.minecraft.util.profiling.Profiler`
with the static thread-local accessor `Profiler.get()` exists on **26.x and 1.21.11** but **NOT on
1.21.1** (older MC has no ambient profiler accessor — the `ProfilerFiller` was threaded through the
level/server). `core/LogisticsProfiler.java` wraps `Profiler.get()`, so it ports byte-identical to
26.1/1.21.11 (pure drift — the call sites `LogisticsProfiler.push/popPush/pop` around tick sections
converge cleanly), but on **1.21.1 it's a forced quarantine**: give it no-op method bodies (empty
`push`/`popPush`/`pop`) so the call sites still compile and stay identical, accepting that profiling
is inert on 1.21.1. The `#630` origin commit also added spark build deps + `PROFILING.md` — those are
version-specific infra, keep them out of a code-only port. See memory `project_profiler_get_1_21_1_delta`.

**Block-entity type constants (`BlockEntityType` → `BlockEntityTypes`):** MC **26.2** moved the
vanilla constant holders out of `BlockEntityType` into a new
`net.minecraft.world.level.block.entity.BlockEntityTypes` class — so `BlockEntityType.FURNACE` (mc/26.1
+ 1.21.x) is `BlockEntityTypes.FURNACE` on **26.2**. Some constants were dropped entirely: **26.2 has
no `BlockEntityType.CHEST`** — look it up via
`BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("chest"))`. Splits
**26.1 from 26.2**. Seen mainly in test setup so far (`BaseBlockEntityPersistenceTest`,
`AbstractBatteryBlockEntityTest`, etc.); if it reaches main code, a small compat helper (per-version
constant accessors) would quarantine it. See memory `project_blockentitytypes_rename_delta`.

**Render layer:** 26.x auto-derives the cutout layer from sprite transparency; 1.21.x needs manual
`BlockRenderLayerMap` registration (Fabric) — legacy-only, so such fixes originate on the highest
affected legacy branch (today 1.21.11) and port down, never up to 26.x.

**Fabric API packet registration naming (`PayloadTypeRegistry`):** same class, same methods, different
names depending on the Fabric API version pulled in per branch — **mc/26.2 uses**
`PayloadTypeRegistry.serverboundPlay()` / `.clientboundPlay()`; **mc/1.21.11 and mc/1.21.1 use the
older** `PayloadTypeRegistry.playC2S()` / `.playS2C()` (same signatures, same registration pattern —
naming only). Hit in `FabricPacketRegistration.java` porting a new packet registration down; resolve
by keeping whichever name the target file already uses elsewhere in the same file (a mixed file is a
sign the wrong name was picked). Confirm mc/26.1's convention when next touched — not yet checked.

**Player overlay/action-bar message:** `ServerPlayer.sendOverlayMessage(Component)` is **26.2-only**.
mc/26.1, mc/1.21.11, and mc/1.21.1 all use the older two-arg form
`player.displayClientMessage(Component, boolean actionBar)` (pass `true` for actionBar to match
`sendOverlayMessage`'s behavior). Hit porting `FluidSupplierScreenHandler#onGaugeClicked`'s
locked/filter-set/cleared messages down to 1.21.11 and 1.21.1 — translate every call site in the
conflicting hunk, not just the first (easy to miss a sibling `if`/`else` branch in the same method).

## Resources
- `CLAUDE.md` — branch strategy, cross-version workflow, build/release commands.
- Auto-memory (`feedback_use_resourceid`, `project_render_chain_porting`,
  `project_mc1_21_1_no_item_model_defs`, `project_jade_cross_version_port`,
  `project_engine_heat_tint_1_21_x`, `feedback_verify_common_test`).
- **FabricMC blog — https://fabricmc.net/blog/** — authoritative per-version "what changed"
  notes (render pipeline, data formats, NBT). Use it to *diagnose a new delta*, then record the
  concrete fix in the cheat-sheet above + memory. Don't mirror the blog wholesale here (it's
  broad and goes stale); capture only deltas this codebase actually hits.
