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
 * Wires {@link BatteryGameTestBody}'s methods into MC's data-driven GameTest registries — see
 * {@link GameTestRegistrationSupport}.
 */
@EventBusSubscriber(modid = "logistics_gametest")
public final class BatteryGameTestRegistration {

    private static final List<GameTestCase> TESTS = List.of(
        new GameTestCase("power/battery_placement", 100, BatteryGameTestBody::testBatteryPlacement),
        new GameTestCase("power/battery_charge_state_tracks_energy", 50, BatteryGameTestBody::testBatteryChargeStateTracksEnergy),
        new GameTestCase(
            "power/network_without_battery_is_unpowered", 60, BatteryGameTestBody::testNetworkWithoutBatteryIsUnpowered),
        new GameTestCase(
            "power/charged_battery_does_not_power_network", 60, BatteryGameTestBody::testChargedBatteryDoesNotPowerNetwork));

    private static final Map<String, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> FUNCTIONS =
        GameTestRegistrationSupport.registerFunctions(TESTS);

    private BatteryGameTestRegistration() {}

    /** Forces this class's static initializer to run — see {@code LogisticsGameTestMod}. */
    public static void bootstrap() {}

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        GameTestRegistrationSupport.registerInstances(event, "power/battery", TESTS, FUNCTIONS);
    }
}
