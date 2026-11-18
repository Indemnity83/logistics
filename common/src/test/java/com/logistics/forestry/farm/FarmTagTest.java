package com.logistics.forestry.farm;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.logistics.DomainRegistrations;
import com.logistics.LogisticsForestry;
import com.logistics.test.MinecraftTestEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Farm block tag")
class FarmTagTest extends MinecraftTestEnvironment {

    private static final String FARMS_TAG = "/data/logistics/tags/block/forestry/farms.json";

    @Test
    @DisplayName("#logistics:forestry/farms lists exactly the registered farms")
    void tagListsEveryFarm() throws IOException {
        DomainRegistrations.ensureRegistered();
        List<String> farms = new ArrayList<>();
        for (Block farm : LogisticsForestry.BLOCK.farms()) {
            farms.add(String.valueOf(BuiltInRegistries.BLOCK.getKey(farm)));
        }

        assertThat(farms).as("registered farms").isNotEmpty();
        assertThat(tagValues())
                .as("the tool tags reach the farms only through this tag, so a farm missing from it "
                        + "is mined at bare-hand speed and drops nothing")
                .containsExactlyInAnyOrderElementsOf(farms);
    }

    private static List<String> tagValues() throws IOException {
        try (InputStream in = FarmTagTest.class.getResourceAsStream(FARMS_TAG)) {
            assertThat(in).as(FARMS_TAG + " on the test classpath").isNotNull();
            List<String> values = new ArrayList<>();
            for (JsonElement value : JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                    .getAsJsonObject()
                    .getAsJsonArray("values")) {
                values.add(value.getAsString());
            }
            return values;
        }
    }
}
