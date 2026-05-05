package com.logistics.core.bootstrap;

public final class LogisticsCommonBootstrap {
    public void initialize() {
        for (DomainBootstrap bootstrap : DomainBootstraps.all()) {
            bootstrap.initCommon();
        }
    }
}
