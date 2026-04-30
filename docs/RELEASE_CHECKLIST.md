# Release Checklist

Run through this checklist before merging any release PR. It takes about 45 minutes if
there are no issues — mostly dominated by the in-game smoke test.

---

## A. Branch Sync Audit (~5 min)

```
[ ] python tools/sync_audit.py
[ ] For each reported gap, determine: intentional divergence OR missed port?
[ ] If missed port: port the fix now, or document the gap and defer intentionally
[ ] All three branches have the same features reflected in their CHANGELOG entries
```

> **Tip:** The script skips release commits and version-string commits automatically.
> Any remaining gaps are real and need a decision.

---

## B. Data Safety Review (~10 min)

```
[ ] git log --oneline <prev-tag>..HEAD   (review all changes in this release)
```

**If any of these files changed since the last release, treat as high-risk:**

| File | Risk |
|------|------|
| `PipeBlockEntity.java` (`saveLogisticsData` / `loadLogisticsData`) | Items in transit |
| `TravelingItem.java` (`CODEC`) | Item codec format |
| `ItemFilterModule.java` (`getFilterSlots` / `setFilterSlots`) | Filter persistence |
| `AbstractEngineBlockEntity.java` (`saveAdditional` / `loadAdditional`) | Engine state |
| Any new `BlockEntity` subclass | First-time save/load |

**If any high-risk file changed:**
```
[ ] Load a world created with the previous release version
[ ] Verify: diamond pipe filters still show their configured items
[ ] Verify: stirling engine retains heat stage and fuel
[ ] Verify: items in pipes are not lost after reload
[ ] Run: ./gradlew test   (must pass, including golden fixture tests)
```

**Historical note:** The 0.5.4 release corrupted filter data because the save format changed
in some places but not others. Saved data loaded into defaults and was immediately
overwritten. The golden fixture tests in `ItemFilterModuleSerializationGoldenTest` directly
guard against this class of regression.

---

## C. In-Game Smoke Test (~30 min)

### Setup world (use the previous release JAR)

Build a test world with:
```
[ ] Two chests (A and B) connected by a mixed pipe run (stone + copper)
[ ] A diamond filter pipe between A and B with filters set on ≥ 2 sides
[ ] A stirling engine fully warmed up (HOT stage) with fuel remaining in slot
[ ] A laser quarry placed inside a valid frame, actively mining
[ ] At least one extraction pipe pulling from chest A
```

Save the world. Close Minecraft.

### Upgrade and verify

Install the new JAR, launch Minecraft, load the same world:

```
[ ] Diamond pipe: re-open filter GUI — filters are still configured (not empty)
[ ] Stirling engine: correct heat stage shows, fuel count is preserved
[ ] Laser quarry: resumes at the correct position and continues mining
[ ] Pipe network: items continue to flow through existing pipe runs
[ ] New pipe: place a new pipe adjacent to the existing run — it connects correctly
[ ] Wrench: reconfigure a pipe module, close and re-open GUI — setting is still there
[ ] No crashes in the Minecraft log during any of the above
```

---

## D. Beta Distribution (recommended, before stable release)

Use the **Build (Pre-release)** GitHub Actions workflow — not build-release. It runs
directly from the branch with no tag needed.

```
[ ] GitHub Actions → "Build (Pre-release)" → branch: mc/1.21.11 → number: 1 → Run
[ ] Repeat for mc/1.21.1 and mc/26.1
[ ] Verify the beta appears on Modrinth as "Beta" (not Release, not Featured)
[ ] Post in Discord #beta-testing: version, Modrinth link, and the smoke test steps above
[ ] Wait 3–7 days; address any reported issues
[ ] If issues fixed: re-run Build (Pre-release) with number: 2, repeat testing
```

---

## E. Merge

Once A–D are all checked:

```
[ ] Merge release PR for mc/1.21.11 (#296)
[ ] Merge release PR for mc/1.21.1 (#298)
[ ] Merge release PR for mc/26.1 (#299)
[ ] Confirm CI builds succeed and Modrinth shows new stable release
[ ] Confirm Discord #announcements receives the release webhook notification
```
