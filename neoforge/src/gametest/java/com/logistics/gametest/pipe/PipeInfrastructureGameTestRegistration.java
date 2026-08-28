package com.logistics.gametest.pipe;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wires {@link PipeInfrastructureGameTestBody}'s methods into NeoForge's GameTest discovery.
 *
 * <p>MC 1.21.1 predates the data-driven {@code TEST_FUNCTION} registry, so registration
 * here is the legacy reflection model: {@link GameTestHolder} makes NeoForge scan this
 * type, and {@link PrefixGameTestTemplate}(false) keeps the class name out of the
 * template id so every test can share {@code logistics_gametest:empty}.
 */
@GameTestHolder("logistics_gametest")
@PrefixGameTestTemplate(false)
public final class PipeInfrastructureGameTestRegistration {

    private PipeInfrastructureGameTestRegistration() {}

    /**
    * Simple test to verify game test infrastructure works.
    */
    @GameTest(template = "empty", batch = "pipeinfrastructure")
    public static void verifyGameTestWorks(GameTestHelper context) {
        PipeInfrastructureGameTestBody.verifyGameTestWorks(context);
    }

    /**
    * Test that a pipe block can be placed and creates a block entity.
    */
    @GameTest(template = "empty", batch = "pipeinfrastructure")
    public static void testPipePlacement(GameTestHelper context) {
        PipeInfrastructureGameTestBody.testPipePlacement(context);
    }

    /**
    * Test that different pipe types can be placed.
    */
    @GameTest(template = "empty", batch = "pipeinfrastructure")
    public static void testMultiplePipeTypes(GameTestHelper context) {
        PipeInfrastructureGameTestBody.testMultiplePipeTypes(context);
    }

    /**
    * Test that pipes can connect to each other.
    */
    @GameTest(template = "empty", batch = "pipeinfrastructure")
    public static void testPipeConnections(GameTestHelper context) {
        PipeInfrastructureGameTestBody.testPipeConnections(context);
    }

    /**
    * Test that connection cache is only recalculated when neighbors change.
    * This verifies the performance optimization that avoids per-tick recalculation.
    */
    @GameTest(template = "empty", batch = "pipeinfrastructure")
    public static void testConnectionCacheOptimization(GameTestHelper context) {
        PipeInfrastructureGameTestBody.testConnectionCacheOptimization(context);
    }
}
