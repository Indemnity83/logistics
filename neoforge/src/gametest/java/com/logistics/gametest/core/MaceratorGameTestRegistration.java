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
 * Wires {@link MaceratorGameTestBody}'s methods into MC's data-driven GameTest registries — see
 * {@link GameTestRegistrationSupport}.
 */
@EventBusSubscriber(modid = "logistics_gametest")
public final class MaceratorGameTestRegistration {

    private static final List<GameTestCase> TESTS = List.of(
        new GameTestCase("core/macerator_placement", 100, MaceratorGameTestBody::testPlacementCreatesBlockEntity),
        new GameTestCase("core/macerator_sided_access", 100, MaceratorGameTestBody::testSidedAccess),
        new GameTestCase("core/macerator_capabilities_exposed", 100, MaceratorGameTestBody::testCapabilitiesExposed),
        new GameTestCase(
            "core/macerator_macerates_ore_with_energy", 260, MaceratorGameTestBody::testMaceratesOreWithEnergy),
        new GameTestCase(
            "core/macerator_macerates_via_real_engine_and_hoppers",
            280,
            MaceratorGameTestBody::testMaceratesViaRealEngineAndHoppers));

    private static final Map<String, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> FUNCTIONS =
        GameTestRegistrationSupport.registerFunctions(TESTS);

    private MaceratorGameTestRegistration() {}

    /** Forces this class's static initializer to run — see {@code LogisticsGameTestMod}. */
    public static void bootstrap() {}

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        GameTestRegistrationSupport.registerInstances(event, "core/macerator", TESTS, FUNCTIONS);
    }
}
