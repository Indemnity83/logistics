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
 * Wires {@link PipeInfrastructureGameTestBody}'s methods into MC's data-driven GameTest registries —
 * see {@link GameTestRegistrationSupport}.
 */
@EventBusSubscriber(modid = "logistics_gametest")
public final class PipeInfrastructureGameTestRegistration {

    private static final List<GameTestCase> TESTS = List.of(
        new GameTestCase("pipe/verify_game_test_works", 100, PipeInfrastructureGameTestBody::verifyGameTestWorks),
        new GameTestCase("pipe/pipe_placement", 100, PipeInfrastructureGameTestBody::testPipePlacement),
        new GameTestCase("pipe/multiple_pipe_types", 100, PipeInfrastructureGameTestBody::testMultiplePipeTypes),
        new GameTestCase("pipe/pipe_connections", 100, PipeInfrastructureGameTestBody::testPipeConnections),
        new GameTestCase(
            "pipe/connection_cache_optimization", 100, PipeInfrastructureGameTestBody::testConnectionCacheOptimization));

    private static final Map<String, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> FUNCTIONS =
        GameTestRegistrationSupport.registerFunctions(TESTS);

    private PipeInfrastructureGameTestRegistration() {}

    /** Forces this class's static initializer to run — see {@code LogisticsGameTestMod}. */
    public static void bootstrap() {}

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        GameTestRegistrationSupport.registerInstances(event, "pipe/pipe_infrastructure", TESTS, FUNCTIONS);
    }
}
