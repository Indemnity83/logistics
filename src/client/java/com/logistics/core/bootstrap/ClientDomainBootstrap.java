package com.logistics.core.bootstrap;

public interface ClientDomainBootstrap {
    void initClient();

    default int order() {
        return 0;
    }
}
