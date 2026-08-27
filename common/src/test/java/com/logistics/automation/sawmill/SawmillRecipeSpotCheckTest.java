package com.logistics.automation.sawmill;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.logistics.core.machine.component.ChanceOutput;
import com.logistics.test.MinecraftTestEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Spot-checks representative shipped Sawmill recipes against the bundled data.
 */
@DisplayName("Sawmill recipes (spot check)")
class SawmillRecipeSpotCheckTest extends MinecraftTestEnvironment {

    private static JsonObject loadRecipe(String fileName) throws IOException {
        String path = "data/logistics/recipe/sawmill/" + fileName;
        try (InputStream stream = SawmillRecipeSpotCheckTest.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(stream).as("recipe resource on the test classpath: %s", path).isNotNull();
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    /** This branch's vanilla Ingredient JSON is always {@code {"item": ...}} or {@code {"tag": ...}}. */
    private static String ingredientId(JsonObject ingredient) {
        return ingredient.has("tag")
                ? "#" + ingredient.get("tag").getAsString()
                : ingredient.get("item").getAsString();
    }

    /**
     * Wiki claim (Recipes § Wood → planks): "Oak Log -> Oak Planks,6 | Byproduct: Sawdust |
     * ByproductChance: 100%" and (Power): "Each recipe carries an RF cost (2,000–3,000 RF)."
     *
     * @see <a href="https://logistics.fandom.com/wiki/Sawmill#Wood_.E2.86.92_planks">wiki/Sawmill.txt § Wood → planks</a>
     */
    @Test
    @DisplayName("oak log saws into 6 planks with a guaranteed sawdust byproduct at 3,000 RF")
    void oakLogMatchesWikiMillingTable() throws IOException {
        JsonObject recipe = loadRecipe("oak.json");

        assertThat(ingredientId(recipe.getAsJsonObject("ingredient"))).isEqualTo("#minecraft:oak_logs");
        assertThat(recipe.get("energy").getAsInt()).isEqualTo(3_000); // within the wiki's 2,000-3,000 RF range

        JsonObject result = recipe.getAsJsonObject("result");
        assertThat(result.get("id").getAsString()).isEqualTo("minecraft:oak_planks");
        assertThat(result.get("count").getAsInt()).isEqualTo(6);

        JsonObject byproduct = recipe.getAsJsonObject("byproduct");
        assertThat(byproduct.get("id").getAsString()).isEqualTo("logistics:core/sawdust");
        assertThat(byproduct.get("chance").getAsDouble()).isEqualTo(1.0);
    }

    /**
     * Wiki claim (Recipes § Wood → planks): "Oak Boat -> Oak Planks,4 | Byproduct: Sawdust |
     * ByproductChance: 125%" — a chance above 100% means a guaranteed drop plus a 25% chance of a
     * second, an edge case worth pinning explicitly.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Sawmill#Wood_.E2.86.92_planks">wiki/Sawmill.txt § Wood → planks</a>
     */
    @Test
    @DisplayName("oak boat saws into 4 planks with a >100% sawdust byproduct chance")
    void oakBoatMatchesWikiMillingTable() throws IOException {
        JsonObject recipe = loadRecipe("oak_boat.json");

        JsonObject result = recipe.getAsJsonObject("result");
        assertThat(result.get("id").getAsString()).isEqualTo("minecraft:oak_planks");
        assertThat(result.get("count").getAsInt()).isEqualTo(4);

        JsonObject byproduct = recipe.getAsJsonObject("byproduct");
        assertThat(byproduct.get("id").getAsString()).isEqualTo("logistics:core/sawdust");
        float chance = byproduct.get("chance").getAsFloat();
        assertThat(chance).isEqualTo(1.25f);

        // A chance > 1.0 means a guaranteed portion plus a fractional bonus roll — pin both halves
        // of that behavior for this recipe's actual chance, not just the raw JSON value.
        ChanceOutput rolled = new ChanceOutput(new ItemStack(Items.GUNPOWDER), chance);
        assertThat(rolled.guaranteedCount()).isEqualTo(1);
        RandomSource random = RandomSource.create(7L);
        int twos = 0;
        for (int i = 0; i < 1000; i++) {
            int rolledCount = rolled.roll(random);
            assertThat(rolledCount).isBetween(1, 2);
            if (rolledCount == 2) {
                twos++;
            }
        }
        // ~25% of rolls should produce the bonus item (loose bounds, deterministic seed).
        assertThat(twos).isBetween(150, 350);
    }

    /**
     * NOTE: wiki/Sawmill.txt § Plants → Pulped Biomass lists Wheat -> Pulped Biomass with no
     * byproduct column at all. The shipped recipe actually grants a 50% chance of a wheat seeds
     * byproduct — a real, undocumented behavior, not just a mislabeled number. See
     * WIKI_DISCREPANCIES.md § Sawmill.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Sawmill#Plants_.E2.86.92_Pulped_Biomass">wiki/Sawmill.txt § Plants → Pulped Biomass</a>
     */
    @Test
    @DisplayName("wheat pulps into biomass with an undocumented wheat-seeds byproduct")
    void wheatPulpingHasUndocumentedByproduct() throws IOException {
        JsonObject recipe = loadRecipe("pulped_biomass_from_wheat.json");

        assertThat(ingredientId(recipe.getAsJsonObject("ingredient"))).isEqualTo("minecraft:wheat");
        assertThat(recipe.get("count").getAsInt()).isEqualTo(4); // matches the wiki's "(×4)"

        JsonObject byproduct = recipe.getAsJsonObject("byproduct");
        assertThat(byproduct.get("id").getAsString()).isEqualTo("minecraft:wheat_seeds");
        assertThat(byproduct.get("chance").getAsDouble()).isEqualTo(0.5);
    }

    /**
     * NOTE: same wiki omission as wheat above — the shipped recipe grants a guaranteed 2x sugar
     * byproduct (chance 2.0 -> always 1, 100% chance of a 2nd) that the wiki doesn't mention at all.
     * See WIKI_DISCREPANCIES.md § Sawmill.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Sawmill#Plants_.E2.86.92_Pulped_Biomass">wiki/Sawmill.txt § Plants → Pulped Biomass</a>
     */
    @Test
    @DisplayName("sugar cane pulps into biomass with an undocumented guaranteed-2x sugar byproduct")
    void sugarCanePulpingHasUndocumentedByproduct() throws IOException {
        JsonObject recipe = loadRecipe("pulped_biomass_from_sugar_cane.json");

        assertThat(ingredientId(recipe.getAsJsonObject("ingredient"))).isEqualTo("minecraft:sugar_cane");
        assertThat(recipe.get("count").getAsInt()).isEqualTo(6); // matches the wiki's "(×6)"

        JsonObject byproduct = recipe.getAsJsonObject("byproduct");
        assertThat(byproduct.get("id").getAsString()).isEqualTo("minecraft:sugar");
        assertThat(byproduct.get("chance").getAsDouble()).isEqualTo(2.0);
    }

    /**
     * Wiki claim (Recipes § Plants → Pulped Biomass): "Oak Leaves -> Pulped Biomass" with no
     * byproduct listed — confirmed correct here, unlike the wheat/sugar-cane recipes above.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Sawmill#Plants_.E2.86.92_Pulped_Biomass">wiki/Sawmill.txt § Plants → Pulped Biomass</a>
     */
    @Test
    @DisplayName("leaves pulp into biomass with genuinely no byproduct")
    void leavesPulpingHasNoByproduct() throws IOException {
        JsonObject recipe = loadRecipe("pulped_biomass_from_leaves.json");

        assertThat(ingredientId(recipe.getAsJsonObject("ingredient"))).isEqualTo("#minecraft:leaves");
        assertThat(recipe.get("count").getAsInt()).isEqualTo(8); // matches the wiki's "(×8)"
        assertThat(recipe.has("byproduct")).isFalse();
    }
}
