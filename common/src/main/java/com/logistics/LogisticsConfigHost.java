package com.logistics;

import com.indemnity83.configory.Config;
import com.indemnity83.configory.ConfigEntries;
import com.indemnity83.configory.ConfigHost;
import com.indemnity83.configory.ConfigKey;

/**
 * Configory-backed configuration for Logistics.
 *
 * <p>Proof-of-concept scope: only the power/engine domain lives here; the remaining domains still use
 * the legacy {@link com.logistics.core.LogisticsConfig} until they are migrated. Keys live in the nested
 * {@link Configs} class (found by {@code bootstrapConfig} convention) and are grouped on disk by their
 * path prefix — {@code engines.*} lands in {@code config/logistics/engines.json}.
 */
public final class LogisticsConfigHost implements ConfigHost {
    public static final String MOD_ID = "logistics";

    private static final LogisticsConfigHost INSTANCE = new LogisticsConfigHost();

    private LogisticsConfigHost() {}

    /** Register keys, install cross-field repair, and load from disk. Call once during startup. */
    public static void bootstrap() {
        INSTANCE.bootstrapConfig(MOD_ID);
        // configory's load() applies defaults in memory but does not persist them, so a fresh install has
        // no file to edit and newly-added keys aren't written back. Save so the on-disk file always exists.
        INSTANCE.saveConfig();
    }

    /** Typed read of a config value. */
    public static <T> T get(ConfigKey<T> key) {
        return INSTANCE.getConfig(key);
    }

    public static final class Configs extends ConfigEntries {
        private static final Config config = configFor(MOD_ID);

        private Configs() {}

        public static final ConfigKey<Long> REDSTONE_OUTPUT = config.defineLong("engines.redstone.output", 10L)
                .min(0L)
                .describe("RF generated per 16-tick interval.")
                .register();

        // NOTE: the min/max relationship is enforced by the repairMinMax sanitize hook below, NOT by
        // configory's minValueOf/maxValueOf. Those build validators that call the *validating* get() on
        // the sibling key, so a mutual pair recurses infinitely (get -> validate -> get -> …) — see the
        // configory findings. The sanitize hook is the working cross-field mechanism.
        public static final ConfigKey<Double> STIRLING_MIN_OUTPUT = config.defineDouble(
                        "engines.stirling.min_output", 3.0)
                .min(0.0)
                .describe("Stirling engine minimum RF/t output.")
                .register();

        public static final ConfigKey<Double> STIRLING_MAX_OUTPUT = config.defineDouble(
                        "engines.stirling.max_output", 10.0)
                .min(0.0)
                .describe("Stirling engine maximum RF/t output.")
                .register();

        /** Runs after all keys are registered: reconcile the stirling min/max pair on every load. */
        public static void bootstrap(Config config) {
            config.registerSanitizeHook(() -> config.repairMinMax(STIRLING_MIN_OUTPUT, STIRLING_MAX_OUTPUT));
        }
    }
}
