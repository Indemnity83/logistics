package com.logistics.resource.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonElement;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Our fuels carry the {@code c:} fluid tags other mods read, so their generators, engines and
 * refineries accept them. Neither loader defines oil or fuel tags; these are the names the major
 * tech mods use.
 */
@DisplayName("Fluid tag contract")
class FluidTagContractTest {

    private static final Map<String, List<String>> TAGS_BY_FLUID = Map.of(
        "crude_oil", List.of("crude_oil", "oil"),
        "seed_oil", List.of("plantoil", "plant_oil"),
        "bio_fuel", List.of("biofuel", "ethanol", "bioethanol"),
        "fuel_oil", List.of("fuel"));

    @Test
    @DisplayName("each fuel is in every fluid tag other mods read for it, source and flowing")
    void fuelsCarryTheirFluidTags() {
        Path root = ResourceFiles.dataRoot().resolve("c/tags/fluid");
        List<String> failures = new ArrayList<>();

        TAGS_BY_FLUID.forEach((fluid, tags) -> {
            for (String tag : tags) {
                List<String> values = values(root.resolve(tag + ".json"));
                for (String id : List.of("logistics:core/" + fluid, "logistics:core/flowing_" + fluid)) {
                    if (!values.contains(id)) {
                        failures.add("c:" + tag + " is missing " + id);
                    }
                }
            }
        });

        assertThat(failures).as("fuels missing their fluid tags").isEmpty();
    }

    private static List<String> values(Path tagFile) {
        List<String> values = new ArrayList<>();
        if (!Files.isRegularFile(tagFile)) {
            return values;
        }
        for (JsonElement entry : ResourceFiles.parse(tagFile).getAsJsonArray("values")) {
            values.add(entry.isJsonObject() ? entry.getAsJsonObject().get("id").getAsString() : entry.getAsString());
        }
        return values;
    }
}
