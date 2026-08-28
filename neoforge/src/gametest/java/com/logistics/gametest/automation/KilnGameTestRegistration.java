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
 * Wires {@link KilnGameTestBody}'s methods into MC's data-driven GameTest registries — see
 * {@link GameTestRegistrationSupport}.
 */
@EventBusSubscriber(modid = "logistics_gametest")
public final class KilnGameTestRegistration {

    // NeoForge's GameTestInstance ticks the environment/structure differently than Fabric's
    // @GameTest shim before handing control to the test body, so timed tests carry ~20 ticks more
    // headroom here than their Fabric @GameTest(maxTicks=...) counterpart for the same callbacks
    // to land within budget.
    private static final List<GameTestCase> TESTS = List.of(
        new GameTestCase("automation/kiln_placement", 100, KilnGameTestBody::testKilnPlacement),
        new GameTestCase("automation/kiln_inventory_access", 100, KilnGameTestBody::testKilnInventoryAccess),
        new GameTestCase("automation/kiln_input_access", 100, KilnGameTestBody::testKilnInputAccess),
        new GameTestCase("automation/kiln_output_extraction", 100, KilnGameTestBody::testKilnOutputExtraction),
        new GameTestCase("automation/kiln_initial_state", 100, KilnGameTestBody::testKilnInitialState),
        new GameTestCase("automation/kiln_item_storage", 100, KilnGameTestBody::testKilnItemStorage),
        new GameTestCase("automation/kiln_smelts_with_energy", 150, KilnGameTestBody::testKilnSmeltsWithEnergy),
        new GameTestCase("automation/kiln_smelts_continuously", 250, KilnGameTestBody::testKilnSmeltsContinuously),
        new GameTestCase(
            "automation/kiln_smelts_via_real_engine_and_hoppers", 240, KilnGameTestBody::testKilnSmeltsViaRealEngineAndHoppers),
        new GameTestCase("automation/kiln_facing", 100, KilnGameTestBody::testKilnFacing));

    private static final Map<String, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> FUNCTIONS =
        GameTestRegistrationSupport.registerFunctions(TESTS);

    private KilnGameTestRegistration() {}

    /** Forces this class's static initializer to run — see {@code LogisticsGameTestMod}. */
    public static void bootstrap() {}

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        GameTestRegistrationSupport.registerInstances(event, "automation/kiln", TESTS, FUNCTIONS);
    }
}
