package com.logistics.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the datapack-reload lifecycle GameTests. Test logic lives in
 * {@link ReloadLifecycleGameTestBody} (shared with NeoForge — see {@code common/src/gametest});
 * these methods only carry the {@code @GameTest} annotation Fabric's reflection-based test
 * discovery requires.
 */
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
