package com.logistics.core.bootstrap;

public interface DomainBootstrap {
    /**
     * Declare this domain's config keys, sanitize hooks, and legacy-migration mappings. Runs for every domain
     * before any config is loaded or any {@link #initCommon()} runs. Must only <em>declare</em> — no config reads.
     */
    default void registerConfig() {}

    void initCommon();

    default void initClient() {}

    default int order() {
        return 0;
    }
}
