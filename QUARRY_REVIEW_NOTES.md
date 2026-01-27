# Quarry Feature Review Notes

## PR #42 Comment Follow-ups (Applicable)

- **Spotless formatting check** (`src/client/java/com/logistics/client/render/MarkerBlockEntityRenderer.java`)
  - **Note:** CI reported `spotlessCheck` failure on this file in PR #42.
  - **Proposed fix:** Run `./gradlew spotlessApply` and re-run `./gradlew spotlessCheck` to confirm formatting is clean.

- **Recipe key ingredient format validation** (`src/main/resources/data/logistics/recipe/*.json`)
  - **Issue:** Keys currently use bare string IDs. Depending on the target MC data-pack format, these may need to be ingredient objects (`{ "item": "..." }`).
  - **Proposed fix:** Verify the correct format for the current Minecraft version/mappings. If objects are required, update all `key` entries consistently.

## Namespace Adjustments (Future)

- **Introduce a stable root namespace:** `com.indemnity83.logistics` (currently `com.logistics`).
  - **Marker system:** `com.indemnity83.logistics.core.marker` (or `com.indemnity83.logistics.core` with marker subpackage).
  - **Quarry system:** `com.indemnity83.logistics.automation.quarry` or `com.indemnity83.logistics.harvesting.quarry`.
  - **Proposed fix:** Plan a staged package move (IDE refactor) once we are ready to update imports, data/resource references, and Fabric entry points.
