package com.logistics.test;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsPipe;
import com.logistics.LogisticsPower;

/**
 * Registers every domain's config entries for tests, so the {@code LogisticsConfig} entry constants
 * (e.g. {@code LogisticsPipe.PIPE_MAX_SPEED}) resolve. Idempotent — config registration is never frozen
 * in tests, so re-registering the same keys is safe.
 */
public final class TestConfig {
    private TestConfig() {}

    public static void registerDomains() {
        new LogisticsAutomation().registerConfig();
        new LogisticsPipe().registerConfig();
        new LogisticsPower().registerConfig();
    }
}
