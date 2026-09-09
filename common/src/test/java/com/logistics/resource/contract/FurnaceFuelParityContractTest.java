package com.logistics.resource.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.logistics.core.lib.power.FurnaceFuels;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The two loaders register furnace fuel by different mechanisms — Fabric reads
 * {@link FurnaceFuels#BURN_TIMES} at runtime, NeoForge ships a {@code neoforge:furnace_fuels} data
 * map that cannot. This pins the data map to the same values, which is the only way the two can be
 * checked against each other.
 */
@DisplayName("Furnace fuel parity across loaders")
class FurnaceFuelParityContractTest {

    private static Map<String, Integer> neoForgeDataMap() throws IOException {
        // loaderAssetRoot ends at <module>/src/main/resources/assets/logistics; the data map is a
        // sibling of assets/, so climb back to the resources root.
        Path json = ResourceFiles.loaderAssetRoot("neoforge")
                .getParent()
                .getParent()
                .resolve("data/neoforge/data_maps/item/furnace_fuels.json");
        assertThat(json).as("the NeoForge furnace fuel data map").exists();

        JsonObject values = JsonParser.parseString(Files.readString(json))
                .getAsJsonObject()
                .getAsJsonObject("values");

        Map<String, Integer> burnTimes = new TreeMap<>();
        values.entrySet().forEach(entry -> burnTimes.put(
                entry.getKey().replaceFirst("^logistics:", ""),
                entry.getValue().getAsJsonObject().get("burn_time").getAsInt()));
        return burnTimes;
    }

    @Test
    @DisplayName("NeoForge's data map carries exactly the shared burn times")
    void neoForgeDataMapMatchesTheSharedBurnTimes() throws IOException {
        assertThat(neoForgeDataMap())
                .as("a fuel added on one loader but not the other is invisible until someone plays that loader")
                .containsExactlyInAnyOrderEntriesOf(new TreeMap<>(FurnaceFuels.BURN_TIMES));
    }

    @Test
    @DisplayName("every shared burn time names a real item path")
    void sharedBurnTimesNameRealItems() {
        assertThat(FurnaceFuels.BURN_TIMES).isNotEmpty();
        FurnaceFuels.BURN_TIMES.forEach((path, burnTime) -> {
            assertThat(path).as("item path").doesNotStartWith("logistics:").doesNotStartWith("/");
            assertThat(burnTime).as("burn time for %s", path).isPositive();
        });
    }
}
