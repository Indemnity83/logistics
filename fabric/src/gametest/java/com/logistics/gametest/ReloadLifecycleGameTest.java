package com.logistics.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric wiring; each method delegates to {@link ReloadLifecycleGameTestBody}. */
public class ReloadLifecycleGameTest {

    @GameTest(maxTicks = 140)
    public void kilnCompletesInFlightSmeltAcrossReload(GameTestHelper context) {
        ReloadLifecycleGameTestBody.kilnCompletesInFlightSmeltAcrossReload(context);
    }

    @GameTest(maxTicks = 150)
    public void kilnStartsNewSmeltAfterReload(GameTestHelper context) {
        ReloadLifecycleGameTestBody.kilnStartsNewSmeltAfterReload(context);
    }
}
