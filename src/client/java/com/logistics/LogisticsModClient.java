package com.logistics;

import com.logistics.core.bootstrap.ClientDomainBootstrap;
import com.logistics.core.bootstrap.ClientDomainBootstraps;
import com.logistics.core.bootstrap.DomainBootstrap;
import com.logistics.core.bootstrap.DomainBootstraps;
import net.fabricmc.api.ClientModInitializer;

public class LogisticsModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LogisticsMod.LOGGER.info("Initializing Logistics client");

        for (DomainBootstrap bootstrap : DomainBootstraps.all()) {
            bootstrap.initClient();
        }

        for (ClientDomainBootstrap bootstrap : ClientDomainBootstraps.all()) {
            bootstrap.initClient();
        }
    }
}
