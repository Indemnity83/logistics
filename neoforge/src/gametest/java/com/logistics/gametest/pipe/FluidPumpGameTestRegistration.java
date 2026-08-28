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
 * Wires {@link FluidPumpGameTestBody}'s methods into MC's data-driven GameTest registries — see
 * {@link GameTestRegistrationSupport}.
 */
@EventBusSubscriber(modid = "logistics_gametest")
public final class FluidPumpGameTestRegistration {

    // NeoForge's GameTestInstance ticks the environment/structure differently than Fabric's
    // @GameTest shim before handing control to the test body, so timed tests carry ~20 ticks more
    // headroom here than their Fabric @GameTest(maxTicks=...) counterpart for the same callbacks
    // to land within budget.
    private static final List<GameTestCase> TESTS = List.of(
        new GameTestCase("pipe/fluid_pump_placement", 100, FluidPumpGameTestBody::testFluidPumpPlacement),
        new GameTestCase(
            "pipe/fluid_pump_energy_and_tank_accessible_from_top_and_sides",
            100,
            FluidPumpGameTestBody::testFluidPumpEnergyAndTankAccessibleFromTopAndSides),
        new GameTestCase(
            "pipe/fluid_pump_tube_descends_without_energy",
            60,
            FluidPumpGameTestBody::testFluidPumpTubeDescendsWithoutEnergy),
        new GameTestCase(
            "pipe/fluid_pump_removes_source_and_fills_tank",
            60,
            FluidPumpGameTestBody::testFluidPumpRemovesSourceAndFillsTank),
        new GameTestCase(
            "pipe/fluid_pump_does_not_drain_without_energy",
            80,
            FluidPumpGameTestBody::testFluidPumpDoesNotDrainWithoutEnergy),
        new GameTestCase(
            "pipe/fluid_pump_does_not_drain_waterlogged_blocks",
            60,
            FluidPumpGameTestBody::testFluidPumpDoesNotDrainWaterloggedBlocks),
        new GameTestCase(
            "pipe/fluid_pump_finds_connected_source_in_radius",
            140,
            FluidPumpGameTestBody::testFluidPumpFindsConnectedSourceInRadius),
        new GameTestCase(
            "pipe/fluid_pump_drains_finite_pool", 180, FluidPumpGameTestBody::testFluidPumpDrainsFinitePool),
        new GameTestCase(
            "pipe/fluid_pump_treats_large_body_as_infinite",
            80,
            FluidPumpGameTestBody::testFluidPumpTreatsLargeBodyAsInfinite),
        new GameTestCase(
            "pipe/fluid_pump_outputs_to_pipe_above", 80, FluidPumpGameTestBody::testFluidPumpOutputsToPipeAbove),
        new GameTestCase(
            "pipe/fluid_pump_outputs_to_pipe_on_side", 80, FluidPumpGameTestBody::testFluidPumpOutputsToPipeOnSide),
        new GameTestCase(
            "pipe/fluid_pump_push_rate_is_four_hundred_not_sixty_two_point_five",
            40,
            FluidPumpGameTestBody::testFluidPumpPushRateIsFourHundredNotSixtyTwoPointFive),
        new GameTestCase(
            "pipe/fluid_pump_drains_connected_lava_sources",
            100,
            FluidPumpGameTestBody::testFluidPumpDrainsConnectedLavaSources),
        new GameTestCase(
            "pipe/fluid_pump_crosses_flowing_to_reach_sources",
            100,
            FluidPumpGameTestBody::testFluidPumpCrossesFlowingToReachSources),
        new GameTestCase(
            "pipe/fluid_pump_drains_open_lava_pool", 220, FluidPumpGameTestBody::testFluidPumpDrainsOpenLavaPool),
        new GameTestCase(
            "pipe/fluid_pump_finishes_layer_with_output_tank",
            100,
            FluidPumpGameTestBody::testFluidPumpFinishesLayerWithOutputTank),
        new GameTestCase(
            "pipe/fluid_pump_stalls_above_solid_floor",
            80,
            FluidPumpGameTestBody::testFluidPumpStallsAboveSolidFloor),
        new GameTestCase(
            "pipe/fluid_pump_drains_furthest_first", 60, FluidPumpGameTestBody::testFluidPumpDrainsFurthestFirst),
        new GameTestCase(
            "pipe/fluid_pump_drains_and_outputs_via_real_engine",
            120,
            FluidPumpGameTestBody::testFluidPumpDrainsAndOutputsViaRealEngine));

    private static final Map<String, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> FUNCTIONS =
        GameTestRegistrationSupport.registerFunctions(TESTS);

    private FluidPumpGameTestRegistration() {}

    /** Forces this class's static initializer to run — see {@code LogisticsGameTestMod}. */
    public static void bootstrap() {}

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        GameTestRegistrationSupport.registerInstances(event, "pipe/fluid_pump", TESTS, FUNCTIONS);
    }
}
