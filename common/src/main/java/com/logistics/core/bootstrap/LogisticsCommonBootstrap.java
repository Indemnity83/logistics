package com.logistics.core.bootstrap;

import com.logistics.core.LogisticsConfig;
import java.util.List;

public final class LogisticsCommonBootstrap {
    public void initialize() {
        List<DomainBootstrap> bootstraps = DomainBootstraps.all();

        // Config lifecycle: every domain declares its entries, registration is frozen, then values
        // are loaded from disk — all before any initCommon() body reads or bakes config values.
        for (DomainBootstrap bootstrap : bootstraps) {
            bootstrap.registerConfig();
        }
        LogisticsConfig.freeze();
        LogisticsConfig.load();

        for (DomainBootstrap bootstrap : bootstraps) {
            bootstrap.initCommon();
        }
    }
}
