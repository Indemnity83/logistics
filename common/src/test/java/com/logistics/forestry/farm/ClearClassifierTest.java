package com.logistics.forestry.farm;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.logistics.forestry.farm.ClearClassifier.Kind;
import com.logistics.test.MinecraftTestEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Survey classifier")
class ClearClassifierTest extends MinecraftTestEnvironment {

    private static float hardness(BlockState state) {
        return state.getDestroySpeed(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
    }

    static Stream<Arguments> table() {
        BlockState persistentLeaves = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        return Stream.of(
                // state, in natural_clearable, in never_clear, expected
                Arguments.of(Blocks.GRASS_BLOCK.defaultBlockState(), true, false, Kind.NATURAL),
                Arguments.of(Blocks.OAK_LEAVES.defaultBlockState(), true, false, Kind.NATURAL),
                Arguments.of(persistentLeaves, true, false, Kind.FLAGGED),
                Arguments.of(Blocks.OAK_PLANKS.defaultBlockState(), false, false, Kind.FLAGGED),
                Arguments.of(Blocks.OAK_LOG.defaultBlockState(), false, false, Kind.FLAGGED),
                Arguments.of(Blocks.TORCH.defaultBlockState(), false, false, Kind.FLAGGED),
                Arguments.of(Blocks.CHEST.defaultBlockState(), false, false, Kind.PROTECTED),
                Arguments.of(Blocks.BEDROCK.defaultBlockState(), false, false, Kind.PROTECTED),
                Arguments.of(Blocks.STONE.defaultBlockState(), true, true, Kind.PROTECTED),
                Arguments.of(Blocks.GLASS.defaultBlockState(), false, true, Kind.PROTECTED));
    }

    @ParameterizedTest(name = "{0} natural={1} neverClear={2} -> {3}")
    @MethodSource("table")
    @DisplayName("block entities, unbreakable and never-clear blocks are protected; natural blocks are natural; the rest is flagged")
    void classifies(BlockState state, boolean natural, boolean neverClear, Kind expected) {
        assertThat(ClearClassifier.classify(state, hardness(state), natural, neverClear)).isEqualTo(expected);
    }

    @Test
    @DisplayName("a never-clear block blocks a working farm's column too")
    void neverClearBlocksTheColumn() {
        BlockState glass = Blocks.GLASS.defaultBlockState();
        assertThat(ColumnClearer.classify(glass, hardness(glass), false, true)).isEqualTo(ColumnClearer.Kind.PROTECTED);
        assertThat(ColumnClearer.classify(glass, hardness(glass), false, false)).isEqualTo(ColumnClearer.Kind.BREAKABLE);
        assertThat(ColumnClearer.classify(glass, hardness(glass), true, true))
                .as("own growth is harvested, not cleared").isEqualTo(ColumnClearer.Kind.OWN_GROWTH);
    }

    private static List<String> tagValues(String name) throws IOException {
        String path = "/data/logistics/tags/block/forestry/" + name + ".json";
        try (InputStream in = ClearClassifierTest.class.getResourceAsStream(path)) {
            assertThat(in).as(path).isNotNull();
            JsonArray values = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                    .getAsJsonObject()
                    .getAsJsonArray("values");
            List<String> ids = new ArrayList<>();
            for (JsonElement value : values) {
                ids.add(value.isJsonObject() ? value.getAsJsonObject().get("id").getAsString() : value.getAsString());
            }
            return ids;
        }
    }

    @Test
    @DisplayName("natural_clearable holds ground, plants, leaves and fluids but never logs; never_clear ships empty")
    void tags() throws IOException {
        List<String> natural = tagValues("natural_clearable");
        assertThat(natural).contains(
                "#minecraft:dirt", "minecraft:grass_block", "#minecraft:sand", "#minecraft:base_stone_overworld",
                "#minecraft:flowers", "#minecraft:leaves", "minecraft:water", "minecraft:lava",
                "logistics:forestry/humus");
        assertThat(natural).map(id -> id.substring(id.indexOf(':') + 1))
                .noneMatch(path -> path.contains("log") || path.contains("wood") || path.contains("planks"));
        assertThat(tagValues("never_clear")).isEmpty();
    }
}
