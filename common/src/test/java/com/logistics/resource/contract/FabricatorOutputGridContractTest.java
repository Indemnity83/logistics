package com.logistics.resource.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.automation.fabricator.FabricatorProcessorComponent;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The Sequential Fabricator's output grid is a fixed set of buttons with no scrolling or pagination,
 * so it can only show {@link FabricatorProcessorComponent#OUTPUT_GRID_CAPACITY} recipes. Shipping more
 * fabricator recipes than that drops the surplus silently: no button means no way to queue the recipe,
 * and no way to cancel it once queued. Fail the build here rather than hide a recipe in-game.
 */
@DisplayName("Fabricator output grid contract")
class FabricatorOutputGridContractTest {

    private static final String RECIPE_DIR = ResourceFiles.NAMESPACE + "/recipe";
    private static final String FABRICATOR_TYPE = ResourceFiles.NAMESPACE + ":fabricator";

    @Test
    @DisplayName("shipped fabricator recipes all fit the output grid")
    void shippedFabricatorRecipesFitTheOutputGrid() {
        assertThat(fabricatorRecipes())
            .as(
                "fabricator recipes; the GUI shows the first %d and silently drops the rest, and a "
                    + "dropped recipe can neither be queued nor cancelled. Add a row to the output "
                    + "grid (and its texture), or paginate it, before shipping another recipe",
                FabricatorProcessorComponent.OUTPUT_GRID_CAPACITY)
            .hasSizeLessThanOrEqualTo(FabricatorProcessorComponent.OUTPUT_GRID_CAPACITY);
    }

    /** Every shipped {@code logistics:fabricator} recipe, wherever it lives in the data tree. */
    private static List<String> fabricatorRecipes() {
        List<String> found = ResourceFiles.dataJsonFiles(RECIPE_DIR).stream()
            .filter(file -> FABRICATOR_TYPE.equals(ResourceFiles.parse(file).get("type").getAsString()))
            .map(ResourceFiles::describe)
            .sorted()
            .toList();

        assertThat(found).as("shipped fabricator recipes; an empty list would pass the capacity check").isNotEmpty();
        return found;
    }
}
