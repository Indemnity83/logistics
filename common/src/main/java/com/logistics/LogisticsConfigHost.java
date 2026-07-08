package com.logistics;

import com.indemnity83.configory.Config;
import com.indemnity83.configory.ConfigDefinition;
import com.indemnity83.configory.ConfigEntries;
import com.indemnity83.configory.ConfigHost;
import com.indemnity83.configory.ConfigKey;
import com.indemnity83.configory.ConfigRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Configory-backed configuration for Logistics.
 *
 * <p>Proof-of-concept scope: only the power/engine domain lives here; the remaining domains still use
 * the legacy {@link com.logistics.core.LogisticsConfig} until they are migrated. To avoid colliding with
 * the legacy {@code config/logistics.json}, the engine keys live on a child config ({@code configFor(MOD_ID,
 * "engines")} → {@code config/logistics/engines.json}); the main {@code logistics} config declares no keys,
 * so bootstrap writes no {@code config/logistics.json}.
 */
public final class LogisticsConfigHost implements ConfigHost {
    public static final String MOD_ID = "logistics";

    private static final LogisticsConfigHost INSTANCE = new LogisticsConfigHost();

    private LogisticsConfigHost() {}

    /**
     * Register keys, install cross-field repair, and load from disk. Call once during startup.
     * {@code bootstrapConfig} loads the main config and every child config (here, {@code engines}) and
     * writes defaults to disk on first load — no explicit save needed.
     */
    public static void bootstrap() {
        INSTANCE.bootstrapConfig(MOD_ID);
    }

    /** Typed read of a config value (resolved against the config that owns the key). */
    public static <T> T get(ConfigKey<T> key) {
        return ConfigRegistry.config(key.configId()).get(key);
    }

    // ==================== Command bridge (/logistics config) ====================

    /** The dotted keys of every configory-backed setting (across the mod's configs), in registration order. */
    public static List<String> keys() {
        return allConfigs().stream()
                .flatMap(config -> config.definitions().stream())
                .map(def -> def.path().fullPath())
                .toList();
    }

    /** Whether {@code key} names a configory-backed setting. */
    public static boolean has(String key) {
        return definition(key).isPresent();
    }

    /** The human description for {@code key} (empty if unknown or undescribed). */
    public static String describe(String key) {
        return definition(key).map(ConfigDefinition::description).orElse("");
    }

    /** The current value of {@code key} as a display string (empty if unknown). */
    public static String valueString(String key) {
        return owner(key).map(config -> config.get(key).asDisplayString()).orElse("");
    }

    /**
     * Parse {@code value} for {@code key}, validate, set, and persist. Returns {@code false} (no change)
     * if the key is unknown, the value doesn't parse, or validation rejects it.
     */
    public static boolean trySet(String key, String value) {
        ConfigDefinition<?> def = definition(key).orElse(null);
        Config config = owner(key).orElse(null);
        if (def == null || config == null) {
            return false;
        }
        Object typed;
        try {
            typed = switch (def.type()) {
                case BOOLEAN -> Boolean.parseBoolean(value);
                case STRING -> value;
                case INT -> Integer.parseInt(value);
                case LONG -> Long.parseLong(value);
                case FLOAT -> Float.parseFloat(value);
                case DOUBLE -> Double.parseDouble(value);
                case ENUM -> value;
            };
        } catch (NumberFormatException e) {
            return false;
        }
        if (config.trySet(key, typed)) {
            config.save();
            return true;
        }
        return false;
    }

    /** Reload every configory-backed config from disk (discarding unsaved in-memory changes). */
    public static void reload() {
        allConfigs().forEach(Config::discardAndReload);
    }

    /** The mod's configs: the main config plus every child (e.g. {@code logistics.engines}). */
    private static List<Config> allConfigs() {
        List<Config> configs = new ArrayList<>();
        configs.add(INSTANCE.config());
        configs.addAll(ConfigRegistry.childConfigs(MOD_ID));
        return configs;
    }

    private static Optional<Config> owner(String key) {
        return allConfigs().stream()
                .filter(config -> config.definitions().stream().anyMatch(def -> def.path().fullPath().equals(key)))
                .findFirst();
    }

    private static Optional<ConfigDefinition<?>> definition(String key) {
        return allConfigs().stream()
                .flatMap(config -> config.definitions().stream())
                .filter(def -> def.path().fullPath().equals(key))
                .findFirst();
    }

    public static final class Configs extends ConfigEntries {
        // Child config → config/logistics/engines.json. No configFor(MOD_ID) here, so the main config stays
        // keyless and bootstrap never writes config/logistics.json (owned by the legacy config for now).
        private static final Config engines = configFor(MOD_ID, "engines");

        private Configs() {}

        public static final ConfigKey<Long> REDSTONE_OUTPUT = engines.defineLong("redstone.output", 10L)
                .min(0L)
                .describe("RF generated per 16-tick interval.")
                .register();

        public static final ConfigKey<Double> STIRLING_MIN_OUTPUT = engines.defineDouble("stirling.min_output", 3.0)
                .min(0.0)
                .maxValueOf(() -> Configs.STIRLING_MAX_OUTPUT)
                .describe("Stirling engine minimum RF/t output.")
                .register();

        public static final ConfigKey<Double> STIRLING_MAX_OUTPUT = engines.defineDouble("stirling.max_output", 10.0)
                .min(0.0)
                .minValueOf(() -> Configs.STIRLING_MIN_OUTPUT)
                .describe("Stirling engine maximum RF/t output.")
                .register();

        // Cross-field is enforced by minValueOf/maxValueOf above: a bad `/config set` is rejected, and a
        // hand-edited inverted file self-heals to defaults on load. (A repairMinMax sanitize hook can't be
        // combined with these — it reads the pair through the validating get(), which throws on the very
        // inverted state it's meant to fix.)
    }
}
