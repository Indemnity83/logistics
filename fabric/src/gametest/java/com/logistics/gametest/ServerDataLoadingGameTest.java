package com.logistics.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric wiring; each method delegates to {@link ServerDataLoadingGameTestBody}. */
public class ServerDataLoadingGameTest {

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 100)
    public void allLogisticsLootTablesLoad(GameTestHelper context) {
        ServerDataLoadingGameTestBody.allLogisticsLootTablesLoad(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 100)
    public void everyLogisticsBlockLootTableIsLoaded(GameTestHelper context) {
        ServerDataLoadingGameTestBody.everyLogisticsBlockLootTableIsLoaded(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 100)
    public void allLogisticsConfiguredFeaturesLoad(GameTestHelper context) {
        ServerDataLoadingGameTestBody.allLogisticsConfiguredFeaturesLoad(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 100)
    public void allLogisticsPlacedFeaturesLoad(GameTestHelper context) {
        ServerDataLoadingGameTestBody.allLogisticsPlacedFeaturesLoad(context);
    }
}
