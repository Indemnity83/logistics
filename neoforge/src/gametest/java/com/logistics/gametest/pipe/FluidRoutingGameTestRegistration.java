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
 * Wires {@link FluidRoutingGameTestBody}'s methods into MC's data-driven GameTest registries —
 * see {@link GameTestRegistrationSupport}.
 */
@EventBusSubscriber(modid = "logistics_gametest")
public final class FluidRoutingGameTestRegistration {

    private static final List<GameTestCase> TESTS = List.of(
        new GameTestCase(
            "pipe/insertion_pipes_top_off_tanks_in_sequence",
            220,
            FluidRoutingGameTestBody::insertionPipesTopOffTanksInSequence),
        new GameTestCase(
            "pipe/bypass_pipe_runs_past_adjacent_tank",
            220,
            FluidRoutingGameTestBody::bypassPipeRunsPastAdjacentTank),
        new GameTestCase(
            "pipe/merger_pipe_outputs_only_through_wrench_set_face",
            220,
            FluidRoutingGameTestBody::mergerPipeOutputsOnlyThroughWrenchSetFace),
        new GameTestCase(
            "pipe/merger_pipe_refuses_fluid_through_its_output_face",
            220,
            FluidRoutingGameTestBody::mergerPipeRefusesFluidThroughItsOutputFace),
        new GameTestCase(
            "pipe/void_pipe_destroys_fluid_flowing_into_it",
            220,
            FluidRoutingGameTestBody::voidPipeDestroysFluidFlowingIntoIt));

    private static final Map<String, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> FUNCTIONS =
        GameTestRegistrationSupport.registerFunctions(TESTS);

    private FluidRoutingGameTestRegistration() {}

    /** Forces this class's static initializer to run — see {@code LogisticsGameTestMod}. */
    public static void bootstrap() {}

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        GameTestRegistrationSupport.registerInstances(event, "pipe/fluid_routing", TESTS, FUNCTIONS);
    }
}
