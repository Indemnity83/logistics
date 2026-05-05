package com.logistics.core.bootstrap;

public final class LogisticsClientBootstrap {
    public void initialize() {
        for (ClientDomainBootstrap bootstrap : ClientDomainBootstraps.all()) {
            bootstrap.initClient();
        }
    }
}
