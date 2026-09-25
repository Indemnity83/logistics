package com.logistics.power.block;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Content checks on the shipped battery ladder data: the frame fill chain and the crafting
 * recipes that consume it.
 */
@DisplayName("Battery ladder recipes (spot check)")
class BatteryRecipeSpotCheckTest {

    private static JsonObject loadRecipe(String path) throws IOException {
        try (InputStream stream = BatteryRecipeSpotCheckTest.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(stream).as("recipe resource on the test classpath: %s", path).isNotNull();
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    /**
     * The fill step is the whole point of the ladder: a Transposer recipe whose fluid amount is
     * <em>negative</em> drains the tank (Fill mode). A positive amount would silently invert the
     * recipe into one that fills the machine's tank from the frame instead.
     *
     * <p>There is exactly one fill recipe, on the generic Machine Frame — the tier comes from the
     * battery's body material, not from a per-tier frame.
     */
    @Test
    @DisplayName("filling the Machine Frame drains liquid redstone and yields the filled frame")
    void frameFillDrainsLiquidRedstone() throws IOException {
        JsonObject recipe = loadRecipe("data/logistics/recipe/transposer/fill_machine_frame.json");

        assertThat(recipe.get("type").getAsString()).isEqualTo("logistics:transposer");
        assertThat(recipe.get("input").getAsString()).isEqualTo("logistics:core/machine_core");
        assertThat(recipe.getAsJsonObject("result").get("id").getAsString())
                .isEqualTo("logistics:core/machine_core_filled");

        JsonObject fluid = recipe.getAsJsonObject("fluid");
        assertThat(fluid.get("fluid").getAsString()).isEqualTo("logistics:core/liquid_redstone");
        assertThat(fluid.get("amount").getAsInt())
                .as("negative drains the tank; a positive amount would invert the recipe")
                .isEqualTo(-2000);
        assertThat(recipe.get("energy").getAsInt()).isEqualTo(12000);
    }

    /**
     * Tiers 3-5 must consume the <em>filled</em> frame. Pointing the recipe at the empty frame
     * would skip the Transposer step entirely and make the fluid gate free.
     */
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"gold", "amethyst", "echo"})
    @DisplayName("the upper tiers are built from a filled frame, never an empty one")
    void upperTiersConsumeTheFilledFrame(String tier) throws IOException {
        JsonObject recipe = loadRecipe("data/logistics/recipe/power/" + tier + "_battery.json");

        assertThat(recipe.getAsJsonObject("key").get("R").getAsString())
                .isEqualTo("logistics:core/machine_core_filled");
        assertThat(recipe.getAsJsonObject("result").get("id").getAsString())
                .isEqualTo("logistics:power/" + tier + "_battery");
    }

    /** Copper and Bronze are crafted outright, as TE's two lowest cell frames are. */
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"copper", "bronze"})
    @DisplayName("the two lowest tiers need no frame and no fluid")
    void lowestTiersSkipTheFillStep(String tier) throws IOException {
        JsonObject recipe = loadRecipe("data/logistics/recipe/power/" + tier + "_battery.json");

        assertThat(recipe.getAsJsonObject("key").get("R").getAsString())
                .isEqualTo("minecraft:redstone_block");
        assertThat(recipe.getAsJsonObject("key").get("R").getAsString())
                .as("%s must not need a filled frame", tier)
                .isNotEqualTo("logistics:core/machine_core_filled");
    }

    /**
     * Every tier's body material must be its own namesake, including Copper — a tier named after a
     * metal it does not contain is the kind of thing that only gets noticed after release.
     */
    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
        "copper,minecraft:copper_ingot",
        "bronze,logistics:core/bronze_ingot",
        "gold,minecraft:gold_ingot",
        "amethyst,minecraft:amethyst_shard",
        "echo,minecraft:echo_shard"
    })
    @DisplayName("each tier is built from its own namesake material")
    void eachTierUsesItsNamesakeMaterial(String tier, String material) throws IOException {
        JsonObject recipe = loadRecipe("data/logistics/recipe/power/" + tier + "_battery.json");
        assertThat(recipe.getAsJsonObject("key").get("B").getAsString()).isEqualTo(material);
    }
}
