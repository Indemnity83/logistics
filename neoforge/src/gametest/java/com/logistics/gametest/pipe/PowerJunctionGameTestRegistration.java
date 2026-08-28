package com.logistics.gametest.pipe;

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
 * Wires {@link PowerJunctionGameTestBody}'s methods into MC's data-driven GameTest registries —
 * see {@link GameTestRegistrationSupport}.
 */
@EventBusSubscriber(modid = "logistics_gametest")
public final class PowerJunctionGameTestRegistration {

    private static final List<GameTestCase> TESTS = List.of(
        new GameTestCase("pipe/junction_powers_network", 60, PowerJunctionGameTestBody::testJunctionPowersNetwork),
        new GameTestCase(
            "pipe/empty_junction_does_not_power_network",
            60,
            PowerJunctionGameTestBody::testEmptyJunctionDoesNotPowerNetwork),
        new GameTestCase(
            "pipe/pipe_forms_power_connection_to_junction",
            60,
            PowerJunctionGameTestBody::testPipeFormsPowerConnectionToJunction),
        new GameTestCase(
            "pipe/logistics_arms_powered_when_junction_charged",
            60,
            PowerJunctionGameTestBody::testLogisticsArmsPoweredWhenJunctionCharged),
        new GameTestCase(
            "pipe/quarry_arm_is_not_treated_as_power",
            60,
            PowerJunctionGameTestBody::testQuarryArmIsNotTreatedAsPower));

    private static final Map<String, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> FUNCTIONS =
        GameTestRegistrationSupport.registerFunctions(TESTS);

    private PowerJunctionGameTestRegistration() {}

    /** Forces this class's static initializer to run — see {@code LogisticsGameTestMod}. */
    public static void bootstrap() {}

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        GameTestRegistrationSupport.registerInstances(event, "pipe/power_junction", TESTS, FUNCTIONS);
    }
}
