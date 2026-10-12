package com.logistics;

import com.logistics.core.bootstrap.ClientDomainBootstrap;

import static com.logistics.LogisticsMod.LOGGER;

public final class LogisticsForestryClient implements ClientDomainBootstrap {

    @Override
    public void initClient() {
        LOGGER.info("Registering forestry (client)");
    }
}
