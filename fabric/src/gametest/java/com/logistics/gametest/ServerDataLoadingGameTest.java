package com.logistics.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric wiring; each method delegates to {@link ServerDataLoadingGameTestBody}. */
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
