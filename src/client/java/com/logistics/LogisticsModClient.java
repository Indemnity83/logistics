package com.logistics;

import com.logistics.core.bootstrap.LogisticsClientBootstrap;
import net.fabricmc.api.ClientModInitializer;

public class LogisticsModClient implements ClientModInitializer {
    private static final LogisticsClientBootstrap CLIENT_BOOTSTRAP = LogisticsClientBootstrap.createDefault();

    @Override
    public void onInitializeClient() {
        LogisticsMod.LOGGER.info("Initializing Logistics client");
        CLIENT_BOOTSTRAP.initialize();
    }
}
