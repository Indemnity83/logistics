package com.logistics.power.block;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
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
     * recipe into one that fills the machine's tank from the battery instead.
     *
     * <p>The battery <em>is</em> the thing filled — there is no intermediate frame or core item, so
     * the chain is craft-then-fill, two steps.
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource({"gold,1000,8000", "amethyst,2000,12000", "echo,4000,16000"})
    @DisplayName("filling an empty battery drains liquid redstone and yields the battery")
    void fillingAnEmptyBatteryYieldsTheBattery(String tier, int milliBuckets, int energy) throws IOException {
        JsonObject recipe = loadRecipe("data/logistics/recipe/transposer/fill_" + tier + "_battery.json");

        assertThat(recipe.get("type").getAsString()).isEqualTo("logistics:transposer");
        assertThat(recipe.get("input").getAsString()).isEqualTo("logistics:power/" + tier + "_battery_empty");
        assertThat(recipe.getAsJsonObject("result").get("id").getAsString())
                .isEqualTo("logistics:power/" + tier + "_battery");

        JsonObject fluid = recipe.getAsJsonObject("fluid");
        assertThat(fluid.get("fluid").getAsString()).isEqualTo("logistics:core/liquid_redstone");
        assertThat(fluid.get("amount").getAsInt())
                .as("negative drains the tank; a positive amount would invert the recipe")
                .isEqualTo(-milliBuckets);
        assertThat(recipe.get("energy").getAsInt()).isEqualTo(energy);
    }

    /**
     * The upper tiers must have no hand-crafting recipe at all — the Transposer is the only route.
     * A leftover shaped recipe would let a player skip the fluid gate entirely.
     */
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"gold", "amethyst", "echo"})
    @DisplayName("the upper tiers cannot be crafted directly, only filled")
    void upperTiersHaveNoCraftingRecipe(String tier) {
        assertThat(getClass().getClassLoader()
                        .getResource("data/logistics/recipe/power/" + tier + "_battery.json"))
                .as("%s battery must be reachable only through the Transposer", tier)
                .isNull();
    }

    /** The empty shell uses a quartz-crystal vessel where the lower tiers put solid redstone. */
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"gold", "amethyst", "echo"})
    @DisplayName("an empty battery is built around a quartz crystal")
    void emptyBatteryUsesAQuartzVessel(String tier) throws IOException {
        JsonObject recipe = loadRecipe("data/logistics/recipe/power/" + tier + "_battery_empty.json");

        assertThat(recipe.getAsJsonObject("key").get("C").getAsString())
                .isEqualTo("logistics:core/quartz_crystal");
        assertThat(recipe.getAsJsonObject("result").get("id").getAsString())
                .isEqualTo("logistics:power/" + tier + "_battery_empty");
    }

    /** Tin and Copper pack in solid redstone and are finished at the crafting table. */
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"tin", "copper"})
    @DisplayName("the two lowest tiers are crafted whole from solids")
    void lowestTiersSkipTheFillStep(String tier) throws IOException {
        JsonObject recipe = loadRecipe("data/logistics/recipe/power/" + tier + "_battery.json");

        assertThat(recipe.getAsJsonObject("key").get("R").getAsString())
                .isEqualTo("minecraft:redstone_block");
        assertThat(getClass().getClassLoader()
                        .getResource("data/logistics/recipe/transposer/fill_" + tier + "_battery.json"))
                .as("%s must not have a fill recipe", tier)
                .isNull();
    }

    /**
     * Every tier's body material must be its own namesake, including Copper — a tier named after a
     * metal it does not contain is the kind of thing that only gets noticed after release.
     */
    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
        "tin,logistics:core/tin_ingot",
        "copper,minecraft:copper_ingot",
        "gold,minecraft:gold_ingot",
        "amethyst,minecraft:amethyst_shard",
        "echo,minecraft:echo_shard"
    })
    @DisplayName("each tier is built from its own namesake material")
    void eachTierUsesItsNamesakeMaterial(String tier, String material) throws IOException {
        // The upper tiers carry their material on the empty shell; the filled form comes from it.
        String path = "data/logistics/recipe/power/" + tier + "_battery.json";
        if (getClass().getClassLoader().getResource(path) == null) {
            path = "data/logistics/recipe/power/" + tier + "_battery_empty.json";
        }
        assertThat(loadRecipe(path).getAsJsonObject("key").get("B").getAsString()).isEqualTo(material);
    }
}
