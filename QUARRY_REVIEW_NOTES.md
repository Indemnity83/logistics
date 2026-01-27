# Quarry Feature Review Notes

## Issues and Proposed Fixes

- **Pipe output duplication risk** (`src/main/java/com/logistics/quarry/entity/QuarryBlockEntity.java:594`)
  - **Issue:** `outputItem` uses `forceAddItem` to insert into a pipe, but on partial or full rejection the pipe code drops the full stack while `outputItem` continues to insert/drop the original stack, duplicating items.
  - **Proposed fix:** Treat pipe insertion as terminal. Change the pipe API to return the accepted/remainder stack (or add a new helper) and only continue with the remainder; if any pipe is present, do not attempt inventory insertion unless remainder exists.

- **Marker bounds can create invalid mining sizes** (`src/main/java/com/logistics/marker/MarkerManager.java:94`, `src/main/java/com/logistics/quarry/entity/QuarryBlockEntity.java:744`)
  - **Issue:** Tiny rectangles can yield zero or negative inner sizes, breaking mining traversal.
  - **Proposed fix:** Enforce minimum marker rectangle size of **3x3** at activation (fail with a clear message). Add a safety guard in quarry mining for innerSizeX/Z <= 0.

- **Frame decay is expensive** (`src/main/java/com/logistics/quarry/QuarryFrameBlock.java:128`)
  - **Issue:** Random tick scans a 64-radius cube per frame block, which can be costly with many frames.
  - **Proposed fix:** Track owning quarry position in frame state or a lightweight BE, or keep a registry of quarries by chunk and only check nearby candidates.

- **Nearby item collection can steal unrelated drops** (`src/main/java/com/logistics/quarry/entity/QuarryBlockEntity.java:520`)
  - **Issue:** `collectNearbyItems` grabs any `ItemEntity` in radius, not just ones spawned by the quarry.
  - **Proposed fix:** Avoid broad collection, or filter to items spawned during the block break (e.g., track block break time/position or use `Block.getDroppedStacks` only).

## Agreed Behavior

- Pipe above quarry **takes strict precedence** over inventory insertion.
- Minimum marker rectangle size is **3x3**.

## PR #42 Comment Follow-ups (Applicable)

- **Spotless formatting check** (`src/client/java/com/logistics/client/render/MarkerBlockEntityRenderer.java`)
  - **Note:** CI reported `spotlessCheck` failure on this file in PR #42.
  - **Proposed fix:** Run `./gradlew spotlessApply` and re-run `./gradlew spotlessCheck` to confirm formatting is clean.

- **Interpolation cache cleanup** (`src/client/java/com/logistics/client/render/QuarryRenderState.java:49`)
  - **Issue:** `INTERPOLATION_CACHE` is never cleared.
  - **Proposed fix:** Call `QuarryRenderState.clearInterpolationCache(pos)` on quarry removal and `clearAllInterpolationCaches()` on world unload.

- **Recipe key ingredient format validation** (`src/main/resources/data/logistics/recipe/*.json`)
  - **Issue:** Keys currently use bare string IDs. Depending on the target MC data-pack format, these may need to be ingredient objects (`{ "item": "..." }`).
  - **Proposed fix:** Verify the correct format for the current Minecraft version/mappings. If objects are required, update all `key` entries consistently.

## Architecture Cleanup (Proposed)

- **Centralize block registration in `LogisticsBlocks`**
  - **Issue:** `QuarryBlocks` and `MarkerBlocks` duplicate the registration helpers already present in `LogisticsBlocks`.
  - **Proposed fix:** Move `QUARRY`, `QUARRY_FRAME`, and `MARKER` registrations into `LogisticsBlocks` (add a shared `registerNoItem(...)` helper there). Keep feature packages focused on logic/rendering rather than common registration.

- **Centralize block entity registration in `LogisticsBlockEntities`**
  - **Issue:** `QuarryBlockEntities` and `MarkerBlockEntities` replicate the same registration pattern used in `LogisticsBlockEntities`.
  - **Proposed fix:** Register all block entities in `LogisticsBlockEntities`, and keep feature packages to the entity classes themselves.

- **Consolidate screen handler registration**
  - **Issue:** Screen handlers are split across `PipeScreenHandlers` and `QuarryScreenHandlers`, but both are initialized in `LogisticsMod`.
  - **Proposed fix:** Introduce a common `LogisticsScreenHandlers` (or similar) to register all screen handlers in one place.

- **Reduce no-op `initialize()` helpers**
  - **Issue:** `QuarryBlocks.initialize()` and `MarkerBlocks.initialize()` only log and add churn in `LogisticsMod.onInitialize`.
  - **Proposed fix:** Remove no-op init methods once registrations are centralized.

## Namespace Adjustments (Future)

- **Introduce a stable root namespace:** `com.indemnity83.logistics` (currently `com.logistics`).
  - **Marker system:** `com.indemnity83.logistics.core.marker` (or `com.indemnity83.logistics.core` with marker subpackage).
  - **Quarry system:** `com.indemnity83.logistics.automation.quarry` or `com.indemnity83.logistics.harvesting.quarry`.
  - **Proposed fix:** Plan a staged package move (IDE refactor) once we are ready to update imports, data/resource references, and Fabric entry points.
