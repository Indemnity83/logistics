package com.logistics.gametest.power;

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
 * Wires {@link CableGameTestBody}'s methods into MC's data-driven GameTest registries — see
 * {@link GameTestRegistrationSupport}.
 */
@EventBusSubscriber(modid = "logistics_gametest")
public final class CableGameTestRegistration {

    private static final List<GameTestCase> TESTS = List.of(
        new GameTestCase(
            "power/cable_creative_engine_powers_network",
            60,
            CableGameTestBody::testCreativeEnginePowersCableNetwork),
        new GameTestCase(
            "power/cable_connection_updates_when_neighbor_output_rotates",
            100,
            CableGameTestBody::testCableConnectionUpdatesWhenNeighborOutputRotates),
        new GameTestCase(
            "power/cable_connects_to_bufferless_engine_outputs",
            100,
            CableGameTestBody::testCableConnectsToBufferlessEngineOutputs),
        new GameTestCase(
            "power/cable_does_not_connect_to_redstone_engine",
            100,
            CableGameTestBody::testCableDoesNotConnectToRedstoneEngine),
        new GameTestCase(
            "power/cable_redstone_engine_not_pulled_by_network",
            60,
            CableGameTestBody::testRedstoneEngineIsNotPulledByCableNetwork));

    private static final Map<String, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> FUNCTIONS =
        GameTestRegistrationSupport.registerFunctions(TESTS);

    private CableGameTestRegistration() {}

    /** Forces this class's static initializer to run — see {@code LogisticsGameTestMod}. */
    public static void bootstrap() {}

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        GameTestRegistrationSupport.registerInstances(event, "power/cable", TESTS, FUNCTIONS);
    }
}
