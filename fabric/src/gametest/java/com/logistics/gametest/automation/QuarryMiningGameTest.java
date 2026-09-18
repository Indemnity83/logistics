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

    /** Loose items lying around the laser head are collected, whatever produced them. */
    @GameTest(maxTicks = 30)
    public void testQuarryCollectsLooseItemsAroundTheArm(GameTestHelper context) {
        QuarryMiningGameTestBody.testQuarryCollectsLooseItemsAroundTheArm(context);
    }

    /** Items well away from the laser head are left alone. */
    @GameTest(maxTicks = 30)
    public void testQuarryLeavesItemsAwayFromTheArmAlone(GameTestHelper context) {
        QuarryMiningGameTestBody.testQuarryLeavesItemsAwayFromTheArmAlone(context);
    }


    /** A running quarry collects a stack lying by its arm that it never broke. */
    @GameTest(maxTicks = 220)
    public void testRunningQuarryCollectsLooseItemsAroundItsArm(GameTestHelper context) {
        QuarryMiningGameTestBody.testRunningQuarryCollectsLooseItemsAroundItsArm(context);
    }
}
