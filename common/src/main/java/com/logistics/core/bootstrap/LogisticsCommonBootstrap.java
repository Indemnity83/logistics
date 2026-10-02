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
        // Phase B: load all per-domain configs (defaults + hooks), then migrate a legacy logistics.json and
        // any config section that has since split into subsections. Both run before blocks exist, so a
        // migrated capacity is in place before the first block entity reads it.
        LogisticsConfigHost.load();
        LogisticsConfigMigrator.migrateIfNeeded();
        LogisticsConfigMigrator.migrateSplitSections();
        // Phase C: register blocks/items/etc.; config is loaded and safe to read.
        for (DomainBootstrap bootstrap : domains) {
            bootstrap.initCommon();
        }
    }
}
