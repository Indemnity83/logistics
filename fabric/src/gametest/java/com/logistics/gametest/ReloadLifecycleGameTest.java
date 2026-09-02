package com.logistics.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric wiring; each method delegates to {@link ReloadLifecycleGameTestBody}. */
public class ReloadLifecycleGameTest {

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 140)
    public void kilnCompletesInFlightSmeltAcrossReload(GameTestHelper context) {
        ReloadLifecycleGameTestBody.kilnCompletesInFlightSmeltAcrossReload(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 150)
    public void kilnStartsNewSmeltAfterReload(GameTestHelper context) {
        ReloadLifecycleGameTestBody.kilnStartsNewSmeltAfterReload(context);
    }
}
