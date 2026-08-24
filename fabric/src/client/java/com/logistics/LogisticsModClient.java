package com.logistics;

import com.logistics.core.bootstrap.LogisticsClientBootstrap;
import com.logistics.core.lib.platform.ClientNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class LogisticsModClient implements ClientModInitializer {
    private static final LogisticsClientBootstrap CLIENT_BOOTSTRAP = new LogisticsClientBootstrap();

    @Override
    public void onInitializeClient() {
        LogisticsMod.LOGGER.info("Initializing Logistics client");
        ClientNetworking.register(ClientPlayNetworking::send);
        CLIENT_BOOTSTRAP.initialize(); // initialize client domains (block-entity renderers, screens, colors)
    }
}
