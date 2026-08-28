package com.logistics.gametest.automation;

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
 * Wires {@link SequentialFabricatorGameTestBody}'s methods into MC's data-driven GameTest
 * registries — see {@link GameTestRegistrationSupport}.
 */
@EventBusSubscriber(modid = "logistics_gametest")
public final class SequentialFabricatorGameTestRegistration {

    private static final List<GameTestCase> TESTS = List.of(
        new GameTestCase("automation/sequential_fabricator_placement", 100, SequentialFabricatorGameTestBody::testPlacement),
        new GameTestCase(
            "automation/sequential_fabricator_builds_selected_chipset",
            160,
            SequentialFabricatorGameTestBody::testBuildsSelectedChipset),
        new GameTestCase(
            "automation/sequential_fabricator_cycles_through_multiple_selected_chipsets",
            380,
            SequentialFabricatorGameTestBody::testCyclesThroughMultipleSelectedChipsets),
        new GameTestCase(
            "automation/sequential_fabricator_builds_chipset_via_real_engine_and_hoppers",
            220,
            SequentialFabricatorGameTestBody::testBuildsChipsetViaRealEngineAndHoppers));

    private static final Map<String, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> FUNCTIONS =
        GameTestRegistrationSupport.registerFunctions(TESTS);

    private SequentialFabricatorGameTestRegistration() {}

    /** Forces this class's static initializer to run — see {@code LogisticsGameTestMod}. */
    public static void bootstrap() {}

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        GameTestRegistrationSupport.registerInstances(event, "automation/sequential_fabricator", TESTS, FUNCTIONS);
    }
}
