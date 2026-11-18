package com.logistics.forestry.farm.orchard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.logistics.DomainRegistrations;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.test.MinecraftTestEnvironment;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Orchard fruit definitions")
class OrchardFruitTest extends MinecraftTestEnvironment {

    private static final ResourceId ID = ResourceId.in("logistics", "apple");

    @BeforeAll
    static void registerDomains() {
        DomainRegistrations.ensureRegistered();
    }

    private static JsonElement json(String text) {
        return JsonParser.parseString(text);
    }

    @Test
    void parsesTheShippedAppleDefinition() throws Exception {
        String path = "/data/logistics/" + OrchardFruit.DIRECTORY + "/apple.json";
        try (InputStream in = OrchardFruitTest.class.getResourceAsStream(path)) {
            assertThat(in).as(path).isNotNull();
            OrchardFruit apple =
                    OrchardFruit.parse(ID, JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)));

            assertThat(apple.leaves()).isEqualTo("#logistics:forestry/orchard/apple_leaves");
            assertThat(apple.fruit()).isEqualTo(Items.APPLE);
            assertThat(apple.chance()).isEqualTo(0.02);
            assertThat(apple.stack().is(Items.APPLE)).isTrue();
        }
    }

    @Test
    void namesLeavesByBlockIdAsWellAsByTag() {
        OrchardFruit fruit =
                OrchardFruit.parse(ID, json("{\"leaves\": \"minecraft:oak_leaves\", \"fruit\": \"minecraft:apple\", \"chance\": 1}"));

        assertThat(fruit.bears(Blocks.OAK_LEAVES.defaultBlockState())).isTrue();
        assertThat(fruit.bears(Blocks.BIRCH_LEAVES.defaultBlockState())).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "[]",
        "{\"fruit\": \"minecraft:apple\", \"chance\": 0.02}",
        "{\"leaves\": \"minecraft:oak_leaves\", \"chance\": 0.02}",
        "{\"leaves\": \"minecraft:oak_leaves\", \"fruit\": \"minecraft:apple\"}",
        "{\"leaves\": \"minecraft:oak_leaves\", \"fruit\": \"minecraft:apple\", \"chance\": \"often\"}",
        "{\"leaves\": \"minecraft:oak_leaves\", \"fruit\": \"minecraft:apple\", \"chance\": 0}",
        "{\"leaves\": \"minecraft:oak_leaves\", \"fruit\": \"minecraft:apple\", \"chance\": 1.5}",
        "{\"leaves\": \"minecraft:oak_leaves\", \"fruit\": \"minecraft:no_such_fruit\", \"chance\": 0.02}",
        "{\"leaves\": \"minecraft:no_such_leaves\", \"fruit\": \"minecraft:apple\", \"chance\": 0.02}",
        "{\"leaves\": \"#Not A Tag\", \"fruit\": \"minecraft:apple\", \"chance\": 0.02}"
    })
    void rejectsAnInvalidDefinition(String text) {
        assertThatThrownBy(() -> OrchardFruit.parse(ID, json(text))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void loadsInAStableOrderAndSkipsDefinitionsThatFail() {
        Map<ResourceId, JsonElement> files = new LinkedHashMap<>();
        files.put(ResourceId.in("zeta", "apple"),
                json("{\"leaves\": \"minecraft:oak_leaves\", \"fruit\": \"minecraft:apple\", \"chance\": 0.5}"));
        files.put(ResourceId.in("alpha", "pear"),
                json("{\"leaves\": \"minecraft:birch_leaves\", \"fruit\": \"minecraft:apple\", \"chance\": 0.1}"));
        files.put(ResourceId.in("alpha", "broken"), json("{\"leaves\": \"minecraft:oak_leaves\"}"));
        files.put(ResourceId.in("alpha", "golden"),
                json("{\"leaves\": \"minecraft:oak_leaves\", \"fruit\": \"minecraft:golden_apple\", \"chance\": 0.01}"));

        List<OrchardFruit> loaded = OrchardFruits.parse(files);

        assertThat(loaded).extracting(fruit -> fruit.id().toString())
                .containsExactly("alpha:golden", "alpha:pear", "zeta:apple");
    }
}
