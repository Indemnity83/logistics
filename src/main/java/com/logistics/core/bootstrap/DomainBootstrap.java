package com.logistics.core.bootstrap;

public interface DomainBootstrap {
    void initCommon();

    default void initClient() {}

    default int order() {
        return 0;
    }
}
