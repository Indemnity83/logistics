package com.logistics.core.bootstrap;

public interface DomainBootstrap {
    /**
     * Register this domain's config entries. Runs for every domain before the config is frozen and
     * loaded, so entries are declared where the domain lives rather than centralized in core. Must
     * be pure registration — no block/item registration and no reading of config values.
     */
    default void registerConfig() {}

    void initCommon();

    default void initClient() {}

    default int order() {
        return 0;
    }
}
