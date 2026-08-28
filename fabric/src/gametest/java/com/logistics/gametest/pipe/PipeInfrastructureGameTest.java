package com.logistics.gametest.pipe;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the pipe-infrastructure GameTests. Test logic lives in
 * {@link PipeInfrastructureGameTestBody} (shared with NeoForge — see {@code common/src/gametest});
 * these methods only carry the {@code @GameTest} annotation Fabric's reflection-based test discovery
 * requires.
 */
public class PipeInfrastructureGameTest {

    /**
    * Simple test to verify game test infrastructure works.
    */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void verifyGameTestWorks(GameTestHelper context) {
        PipeInfrastructureGameTestBody.verifyGameTestWorks(context);
    }

    /**
    * Test that a pipe block can be placed and creates a block entity.
    */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testPipePlacement(GameTestHelper context) {
        PipeInfrastructureGameTestBody.testPipePlacement(context);
    }

    /**
    * Test that different pipe types can be placed.
    */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testMultiplePipeTypes(GameTestHelper context) {
        PipeInfrastructureGameTestBody.testMultiplePipeTypes(context);
    }

    /**
    * Test that pipes can connect to each other.
    */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testPipeConnections(GameTestHelper context) {
        PipeInfrastructureGameTestBody.testPipeConnections(context);
    }

    /**
    * Test that connection cache is only recalculated when neighbors change.
    * This verifies the performance optimization that avoids per-tick recalculation.
    */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testConnectionCacheOptimization(GameTestHelper context) {
        PipeInfrastructureGameTestBody.testConnectionCacheOptimization(context);
    }
}
