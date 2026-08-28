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
 * Wires {@link FluidLightGameTestBody}'s methods into MC's data-driven GameTest registries — see
 * {@link GameTestRegistrationSupport}.
 */
@EventBusSubscriber(modid = "logistics_gametest")
public final class FluidLightGameTestRegistration {

    // NeoForge's GameTestInstance ticks the environment/structure differently than Fabric's
    // @GameTest shim before handing control to the test body, so timed tests carry ~20 ticks more
    // headroom here than their Fabric @GameTest(maxTicks=...) counterpart for the same callbacks
    // to land within budget.
    private static final List<GameTestCase> TESTS = List.of(
        new GameTestCase("pipe/tank_of_lava_glows", 100, FluidLightGameTestBody::tankOfLavaGlows),
        new GameTestCase(
            "pipe/tank_of_liquid_glowstone_glows", 100, FluidLightGameTestBody::tankOfLiquidGlowstoneGlows),
        new GameTestCase("pipe/tank_of_water_stays_dark", 100, FluidLightGameTestBody::tankOfWaterStaysDark),
        new GameTestCase("pipe/drained_tank_stops_glowing", 100, FluidLightGameTestBody::drainedTankStopsGlowing),
        new GameTestCase("pipe/pipe_of_lava_glows", 100, FluidLightGameTestBody::pipeOfLavaGlows));

    private static final Map<String, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> FUNCTIONS =
        GameTestRegistrationSupport.registerFunctions(TESTS);

    private FluidLightGameTestRegistration() {}

    /** Forces this class's static initializer to run — see {@code LogisticsGameTestMod}. */
    public static void bootstrap() {}

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        GameTestRegistrationSupport.registerInstances(event, "pipe/fluid_light", TESTS, FUNCTIONS);
    }
}
