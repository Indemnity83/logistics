package com.logistics.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the server-data loading GameTests. Test logic lives in
 * {@link ServerDataLoadingGameTestBody} (shared with NeoForge — see {@code common/src/gametest});
 * these methods only carry the {@code @GameTest} annotation Fabric's reflection-based test
 * discovery requires.
 */
public class ServerDataLoadingGameTest {

    @GameTest
    public void allLogisticsLootTablesLoad(GameTestHelper context) {
        ServerDataLoadingGameTestBody.allLogisticsLootTablesLoad(context);
    }

    @GameTest
    public void everyLogisticsBlockLootTableIsLoaded(GameTestHelper context) {
        ServerDataLoadingGameTestBody.everyLogisticsBlockLootTableIsLoaded(context);
    }

    @GameTest
    public void allLogisticsConfiguredFeaturesLoad(GameTestHelper context) {
        ServerDataLoadingGameTestBody.allLogisticsConfiguredFeaturesLoad(context);
    }

    @GameTest
    public void allLogisticsPlacedFeaturesLoad(GameTestHelper context) {
        ServerDataLoadingGameTestBody.allLogisticsPlacedFeaturesLoad(context);
    }
}
