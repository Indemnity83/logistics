package com.logistics;

import com.logistics.core.bootstrap.ClientDomainBootstrap;

import static com.logistics.LogisticsMod.LOGGER;

public final class LogisticsCoreClient implements ClientDomainBootstrap {

    @Override
    public void initClient() {
        LOGGER.info("Registering core (client)");
    }

    @Override
    public int order() {
        return -100; // Initialize core first
    }
}
