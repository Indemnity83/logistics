package com.logistics.gametest.core;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the Upgrade Kit GameTests. Test logic lives in
 * {@link UpgradeKitGameTestBody} (shared with NeoForge — see {@code common/src/gametest}).
 */
public class UpgradeKitGameTest {

    @GameTest(maxTicks = 20)
    public void testIndustrialKitUpgradesBasicFarm(GameTestHelper context) {
        UpgradeKitGameTestBody.testIndustrialKitUpgradesBasicFarm(context);
    }

    @GameTest(maxTicks = 20)
    public void testPrecisionKitRefusedOnBasicFarm(GameTestHelper context) {
        UpgradeKitGameTestBody.testPrecisionKitRefusedOnBasicFarm(context);
    }

    @GameTest(maxTicks = 20)
    public void testUpgradedFarmDropsItsTier(GameTestHelper context) {
        UpgradeKitGameTestBody.testUpgradedFarmDropsItsTier(context);
    }
}
