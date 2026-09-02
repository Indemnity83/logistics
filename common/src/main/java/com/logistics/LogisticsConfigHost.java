package com.logistics;

import com.indemnity83.configory.Config;
import com.indemnity83.configory.ConfigKey;
import com.indemnity83.configory.ConfigRegistry;
import com.logistics.core.lib.platform.PlatformService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger LOGGER = LoggerFactory.getLogger("logistics/config");

    /** Suffix appended to a config file that could not be read, so the player's edits survive the recovery. */
    private static final String INVALID_SUFFIX = ".invalid";

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
        Path configRoot = PlatformService.INSTANCE.configDir();
        for (Config config : domainConfigs()) {
            loadOrRecover(config, configRoot);
        }
    }

    /**
     * Load one config, treating an unreadable file as recoverable rather than fatal: the bad file is renamed to
     * {@code <name>.json.invalid} and defaults are written in its place, so a single stray comma in one file
     * can't take down mod init. Package-visible for tests.
     */
    static void loadOrRecover(Config config, Path configRoot) {
        try {
            config.load().save();
            return;
        } catch (RuntimeException e) {
            LOGGER.error("Could not read config '{}'; falling back to defaults", config.id(), e);
        }
        if (!setAside(config.id(), configRoot)) {
            return;
        }
        try {
            config.load().save();
        } catch (RuntimeException e) {
            LOGGER.error("Could not write default config for '{}'", config.id(), e);
        }
    }

    /** Rename a config's unreadable file to {@code *.json.invalid} so the next load starts from defaults. */
    private static boolean setAside(String configId, Path configRoot) {
        Path file = fileFor(configId, configRoot);
        if (!Files.exists(file)) {
            return false;
        }
        Path invalid = file.resolveSibling(file.getFileName() + INVALID_SUFFIX);
        try {
            Files.move(file, invalid, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Could not set the unreadable config file {} aside; leaving it in place", file, e);
            return false;
        }
        LOGGER.warn("Renamed the unreadable config to {} — fix its syntax and rename it back to keep those edits",
                invalid.getFileName());
        return true;
    }

    /** Configory's id → file layout: {@code logistics.machines.kiln} → {@code <root>/logistics/machines/kiln.json}. */
    private static Path fileFor(String configId, Path configRoot) {
        String[] segments = configId.split("\\.", -1);
        Path file = configRoot;
        for (int i = 0; i < segments.length; i++) {
            file = file.resolve(i == segments.length - 1 ? segments[i] + ".json" : segments[i]);
        }
        return file;
    }

    /** The registered per-domain configs ({@code config/logistics/<domain>.json}) — for the command + migrator. */
    public static List<Config> domainConfigs() {
        return ConfigRegistry.childConfigs(MOD_ID);
    }
}
