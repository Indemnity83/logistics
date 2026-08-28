package com.logistics.gametest.core;

import com.logistics.gametest.GameTestCase;
import com.logistics.gametest.GameTestRegistrationSupport;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Wires {@link OreGenerationGameTestBody}'s methods into MC's data-driven GameTest registries —
 * see {@link GameTestRegistrationSupport}.
 */
@EventBusSubscriber(modid = "logistics_gametest")
public final class OreGenerationGameTestRegistration {

    private static final List<GameTestCase> TESTS = List.of(
        new GameTestCase(
            "core/tin_ore_stone_feature_registered", 100, OreGenerationGameTestBody::testTinOreStoneFeatureRegistered),
        new GameTestCase(
            "core/tin_ore_deepslate_feature_registered",
            100,
            OreGenerationGameTestBody::testTinOreDeepslateFeatureRegistered),
        new GameTestCase(
            "core/apatite_ore_feature_registered", 100, OreGenerationGameTestBody::testApatiteOreFeatureRegistered),
        new GameTestCase(
            "core/tin_ore_configured_feature_registered",
            100,
            OreGenerationGameTestBody::testTinOreConfiguredFeatureRegistered),
        new GameTestCase(
            "core/apatite_ore_configured_feature_registered",
            100,
            OreGenerationGameTestBody::testApatiteOreConfiguredFeatureRegistered),
        new GameTestCase("core/tin_ore_blocks_placeable", 100, OreGenerationGameTestBody::testTinOreBlocksPlaceable),
        new GameTestCase(
            "core/apatite_ore_block_placeable", 100, OreGenerationGameTestBody::testApatiteOreBlockPlaceable),
        new GameTestCase(
            "core/tin_ore_can_replace_stone", 100, OreGenerationGameTestBody::testTinOreCanReplaceStone),
        new GameTestCase(
            "core/tin_ore_can_replace_deepslate", 100, OreGenerationGameTestBody::testTinOreCanReplaceDeepslate),
        new GameTestCase(
            "core/apatite_ore_can_replace_stone", 100, OreGenerationGameTestBody::testApatiteOreCanReplaceStone));

    private static final Map<String, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> FUNCTIONS =
        GameTestRegistrationSupport.registerFunctions(TESTS);

    private OreGenerationGameTestRegistration() {}

    /** Forces this class's static initializer to run — see {@code LogisticsGameTestMod}. */
    public static void bootstrap() {}

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        GameTestRegistrationSupport.registerInstances(event, "core/ore_generation", TESTS, FUNCTIONS);
    }
}
