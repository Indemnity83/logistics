package com.logistics;

import com.indemnity83.configory.Config;
import com.indemnity83.configory.ConfigKey;
import com.indemnity83.configory.ConfigRegistry;
import java.util.List;

/**
 * Thin facade over the configory registry for Logistics.
 *
 * <p>Config keys are declared per-domain — each domain's bootstrap has a nested {@code CONFIG} class
 * (`LogisticsPower.CONFIG`, `LogisticsAutomation.CONFIG`, `LogisticsPipe.CONFIG`, `LogisticsCore.CONFIG`) that
 * calls {@code configFor(MOD_ID, "<domain>")} → {@code config/logistics/<domain>.json} and registers its keys,
 * sanitize hooks, and legacy-migration mappings during {@code DomainBootstrap.registerConfig()}. This class only
 * provides the shared read helper and the load / enumeration used by the command surface and migrator.
 */
public final class LogisticsConfigHost {
    public static final String MOD_ID = "logistics";

    private LogisticsConfigHost() {}

    /** Typed read of a config value (resolved against the config that owns the key). */
    public static <T> T get(ConfigKey<T> key) {
        return ConfigRegistry.config(key.configId()).get(key);
    }

    /**
     * Load every per-domain child config from disk (writing defaults on first load and running each config's
     * sanitize hooks). Call once after all domains have run {@code registerConfig()}.
     */
    public static void load() {
        for (Config config : domainConfigs()) {
            config.load().save();
        }
    }

    /** The registered per-domain configs ({@code config/logistics/<domain>.json}) — for the command + migrator. */
    public static List<Config> domainConfigs() {
        return ConfigRegistry.childConfigs(MOD_ID);
    }
}
