package com.logistics.gametest.automation;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the tick-based LaserQuarry mining GameTests. Test logic lives in
 * {@link QuarryMiningGameTestBody} (shared with NeoForge — see {@code common/src/gametest}); these
 * methods only carry the {@code @GameTest} annotation Fabric's reflection-based test discovery
 * requires.
 *
 * <p>Run all in-game: /test runall
 * Run one test:       /test run logistics-gametest.quarrymininggametest.&lt;methodname&gt;
 */
public class QuarryMiningGameTest {





    /** A shulker box keeps its contents in the item it drops; the quarry must not empty it. */
    @GameTest(maxTicks = 30)
    public void testQuarryDoesNotEmptyABrokenShulkerBox(GameTestHelper context) {
        QuarryMiningGameTestBody.testQuarryDoesNotEmptyABrokenShulkerBox(context);
    }

    /** Loose items lying in the quarry's pit are collected, whatever produced them. */
    @GameTest(maxTicks = 30)
    public void testQuarryCollectsLooseItemsInItsPit(GameTestHelper context) {
        QuarryMiningGameTestBody.testQuarryCollectsLooseItemsInItsPit(context);
    }

    /** The pit is the boundary; items outside the frame are left alone. */
    @GameTest(maxTicks = 30)
    public void testQuarryLeavesItemsOutsideItsFrameAlone(GameTestHelper context) {
        QuarryMiningGameTestBody.testQuarryLeavesItemsOutsideItsFrameAlone(context);
    }

    /** With nowhere to put it, a loose item is left where it lies. */
    @GameTest(maxTicks = 30)
    public void testQuarryLeavesItemsItCannotRoute(GameTestHelper context) {
        QuarryMiningGameTestBody.testQuarryLeavesItemsItCannotRoute(context);
    }

    /** A running quarry collects a stack lying in its pit that it never broke. */
    @GameTest(maxTicks = 220)
    public void testRunningQuarryCollectsLooseItemsFromItsPit(GameTestHelper context) {
        QuarryMiningGameTestBody.testRunningQuarryCollectsLooseItemsFromItsPit(context);
    }
}
