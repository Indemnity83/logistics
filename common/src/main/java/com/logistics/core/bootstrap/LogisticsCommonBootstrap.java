package com.logistics.core.bootstrap;

import com.logistics.LogisticsConfigHost;
import com.logistics.core.LogisticsConfigMigrator;

public final class LogisticsCommonBootstrap {
    public void initialize() {
        var domains = DomainBootstraps.all();
        // Phase A: every domain declares its config keys, sanitize hooks, and legacy mappings.
        for (DomainBootstrap bootstrap : domains) {
            bootstrap.registerConfig();
        }
        // Phase B: load all per-domain configs (defaults + hooks), then migrate a legacy logistics.json if present.
        LogisticsConfigHost.load();
        LogisticsConfigMigrator.migrateIfNeeded();
        // Phase C: register blocks/items/etc.; config is loaded and safe to read.
        for (DomainBootstrap bootstrap : domains) {
            bootstrap.initCommon();
        }
    }
}
