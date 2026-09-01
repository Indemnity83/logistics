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
 * Wires {@link CauldronFluidGameTestBody}'s methods into MC's data-driven GameTest registries —
 * see {@link GameTestRegistrationSupport}.
 */
@EventBusSubscriber(modid = "logistics_gametest")
public final class CauldronFluidGameTestRegistration {

    private static final List<GameTestCase> TESTS = List.of(
        new GameTestCase(
            "pipe/extractor_drains_lava_cauldron",
            200,
            CauldronFluidGameTestBody::extractorDrainsLavaCauldron),
        new GameTestCase(
            "pipe/extractor_drains_water_cauldron",
            200,
            CauldronFluidGameTestBody::extractorDrainsWaterCauldron),
        new GameTestCase(
            "pipe/insertion_pipe_fills_cauldron_with_water",
            200,
            CauldronFluidGameTestBody::insertionPipeFillsCauldronWithWater),
        new GameTestCase(
            "pipe/insertion_pipe_fills_cauldron_with_lava",
            200,
            CauldronFluidGameTestBody::insertionPipeFillsCauldronWithLava));

    private static final Map<String, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> FUNCTIONS =
        GameTestRegistrationSupport.registerFunctions(TESTS);

    private CauldronFluidGameTestRegistration() {}

    /** Forces this class's static initializer to run — see {@code LogisticsGameTestMod}. */
    public static void bootstrap() {}

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        GameTestRegistrationSupport.registerInstances(event, "pipe/cauldron_fluid", TESTS, FUNCTIONS);
    }
}
