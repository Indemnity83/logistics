package com.logistics;

import com.indemnity83.configory.Config;
import com.indemnity83.configory.ConfigEntries;
import com.indemnity83.configory.ConfigHost;
import com.indemnity83.configory.ConfigKey;
import com.indemnity83.configory.ConfigRegistry;
import com.logistics.core.crash.CrashReporting;

/**
 * Configory-backed configuration for Logistics.
 *
 * <p>Config is split into per-domain child configs — {@code configFor(MOD_ID, "<domain>")} → {@code
 * config/logistics/<domain>.json}. Each domain is a command group, so keys are short within their file
 * ({@code max_speed} under {@code pipes}, {@code pump_search_radius} under {@code fluids}) and the generated
 * surface reads {@code /logistics config <domain> <key>}. The main {@code logistics} config declares no keys,
 * so bootstrap writes no {@code config/logistics.json} (that name belongs to the legacy config and its
 * {@code .bak}).
 */
public final class LogisticsConfigHost implements ConfigHost {
    public static final String MOD_ID = "logistics";

    private static final LogisticsConfigHost INSTANCE = new LogisticsConfigHost();

    private LogisticsConfigHost() {}

    /**
     * Register keys, install sanitize hooks, and load every child config from disk. Call once during
     * startup. {@code bootstrapConfig} loads the main config and every child and writes defaults on
     * first load — no explicit save needed.
     */
    public static void bootstrap() {
        INSTANCE.bootstrapConfig(MOD_ID);
    }

    /** Typed read of a config value (resolved against the config that owns the key). */
    public static <T> T get(ConfigKey<T> key) {
        return ConfigRegistry.config(key.configId()).get(key);
    }

    // Per-domain config accessors — configory builds the flat /logistics config command from these.
    public static Config engines() {
        return ConfigRegistry.config(Configs.REDSTONE_OUTPUT.configId());
    }

    public static Config machines() {
        return ConfigRegistry.config(Configs.QUARRY_AREA.configId());
    }

    public static Config pipes() {
        return ConfigRegistry.config(Configs.PIPE_MAX_SPEED.configId());
    }

    public static Config fluids() {
        return ConfigRegistry.config(Configs.FLUID_PIPE_BASE_TRANSFER_RATE.configId());
    }

    public static Config reporting() {
        return ConfigRegistry.config(Configs.CRASH_REPORTING_ENABLED.configId());
    }

    public static final class Configs extends ConfigEntries {
        // Child configs → config/logistics/<name>.json. No configFor(MOD_ID), so the main config stays
        // keyless and bootstrap never writes config/logistics.json (owned by the legacy config + its .bak).
        private static final Config engines = configFor(MOD_ID, "engines");
        private static final Config machines = configFor(MOD_ID, "machines");
        private static final Config pipes = configFor(MOD_ID, "pipes");
        private static final Config fluids = configFor(MOD_ID, "fluids");
        private static final Config reporting = configFor(MOD_ID, "reporting");

        private Configs() {}

        // ==================== engines ====================

        public static final ConfigKey<Long> REDSTONE_OUTPUT = engines.defineLong("redstone_output", 10L)
                .min(0L)
                .describe("RF generated per 16-tick interval.")
                .register();

        public static final ConfigKey<Double> STIRLING_MIN_OUTPUT = engines.defineDouble("stirling_min_output", 3.0)
                .min(0.0)
                .maxValueOf(() -> Configs.STIRLING_MAX_OUTPUT)
                .describe("Stirling engine minimum RF/t output.")
                .register();

        public static final ConfigKey<Double> STIRLING_MAX_OUTPUT = engines.defineDouble("stirling_max_output", 10.0)
                .min(0.0)
                .minValueOf(() -> Configs.STIRLING_MIN_OUTPUT)
                .describe("Stirling engine maximum RF/t output.")
                .register();

        // ==================== machines (quarry) ====================

        public static final ConfigKey<Integer> QUARRY_AREA = machines.defineInt("quarry_area", 16)
                .min(3)
                .describe("Quarry mining area side length (NxN blocks)")
                .register();

        public static final ConfigKey<Long> QUARRY_ENERGY_PER_BLOCK = machines.defineLong("quarry_energy_per_block", 60L)
                .min(0L)
                .describe("RF cost per block mined")
                .register();

        public static final ConfigKey<Double> QUARRY_ENERGY_MULTIPLIER = machines.defineDouble("quarry_energy_multiplier", 1.0)
                .finite()
                .min(0.0)
                .describe("Global energy cost multiplier")
                .register();

        public static final ConfigKey<Float> QUARRY_ARM_SPEED = machines.defineFloat("quarry_arm_speed", 0.1f)
                .finite()
                .min(0.0f)
                .describe("Arm movement speed (blocks/tick)")
                .register();

        public static final ConfigKey<Float> QUARRY_ARM_SPEED_SCALING = machines.defineFloat("quarry_arm_speed_scaling", 2000f)
                .finite()
                .greaterThan(0.0f)
                .describe("Energy-to-speed scaling constant")
                .register();

        public static final ConfigKey<Long> QUARRY_ARM_ENERGY = machines.defineLong("quarry_arm_energy", 20L)
                .min(0L)
                .describe("RF/tick cost for arm movement")
                .register();

        public static final ConfigKey<Float> QUARRY_RAIN_PENALTY = machines.defineFloat("quarry_rain_penalty", 0.7f)
                .finite()
                .range(0.0f, 1.0f)
                .describe("Speed multiplier when raining (0.0-1.0)")
                .register();

        public static final ConfigKey<Integer> QUARRY_SCAN_RATE = machines.defineInt("quarry_scan_rate", 256)
                .min(1)
                .describe("Max blocks scanned per tick when searching")
                .register();

        public static final ConfigKey<Boolean> QUARRY_LOAD_CHUNKS = machines.defineBoolean("quarry_load_chunks", false)
                .describe("Keep the quarry and its work area chunk-loaded while running")
                .register();

        // ==================== pipes (item transport) ====================

        public static final ConfigKey<Float> PIPE_MAX_SPEED = pipes.defineFloat("max_speed", 0.16f)
                .finite()
                .greaterThan(0.0f)
                .minValueOf(() -> Configs.PIPE_MIN_SPEED)
                .describe("Item speed ceiling (blocks/tick)")
                .register();

        public static final ConfigKey<Float> PIPE_MIN_SPEED = pipes.defineFloat("min_speed", 0.02f)
                .finite()
                .greaterThan(0.0f)
                .maxValueOf(() -> Configs.PIPE_MAX_SPEED)
                .describe("Item speed floor (blocks/tick)")
                .register();

        public static final ConfigKey<Float> PIPE_ACCELERATION = pipes.defineFloat("acceleration", 1.0f / 200.0f)
                .finite()
                .min(0.0f)
                .describe("Speed gain per tick when accelerating")
                .register();

        public static final ConfigKey<Float> PIPE_DRAG = pipes.defineFloat("drag", 0.005f)
                .finite()
                .range(0.0f, 1.0f)
                .describe("Speed decay fraction per tick")
                .register();

        public static final ConfigKey<Float> PIPE_INJECT_SPEED = pipes.defineFloat("inject_speed", 0.2f)
                .finite()
                .greaterThan(0.0f)
                .describe("Item speed when injected by network routing (blocks/tick)")
                .register();

        // ==================== fluids (fluid pipe + fluid pump) ====================

        public static final ConfigKey<Integer> FLUID_PIPE_BASE_TRANSFER_RATE =
                fluids.defineInt("pipe_base_transfer_rate", 10)
                        .min(1)
                        .describe("Base Fluid Pipe transfer rate (mB/tick), scaled per tier")
                        .register();

        public static final ConfigKey<Integer> FLUID_PIPE_BASE_CAPACITY = fluids.defineInt("pipe_base_capacity", 250)
                .min(1)
                .describe("Fluid Pipe buffer capacity (mB)")
                .register();

        public static final ConfigKey<Boolean> FLUID_PIPE_WOODEN_REQUIRES_ENGINE =
                fluids.defineBoolean("pipe_wooden_requires_engine", true)
                        .describe("Fluid Extractor Pipe requires engine power")
                        .register();

        public static final ConfigKey<Boolean> FLUID_PIPE_ACTIVE_EXTRACTION =
                fluids.defineBoolean("pipe_active_extraction", true)
                        .describe("Debug: extractor pulling enabled")
                        .register();

        public static final ConfigKey<Integer> FLUID_PUMP_TANK_CAPACITY_MB =
                fluids.defineInt("pump_tank_capacity_mb", 16_000)
                        .min(1)
                        .describe("Fluid Pump tank capacity (mB)")
                        .register();

        public static final ConfigKey<Long> FLUID_PUMP_ENERGY_CAPACITY = fluids.defineLong("pump_energy_capacity", 1_000L)
                .min(1L)
                .describe("Fluid Pump energy buffer capacity (RF)")
                .register();

        public static final ConfigKey<Long> FLUID_PUMP_MAX_ENERGY_INPUT = fluids.defineLong("pump_max_energy_input", 150L)
                .min(1L)
                .describe("Fluid Pump max energy input (RF/t)")
                .register();

        public static final ConfigKey<Long> FLUID_PUMP_ENERGY_PER_SOURCE = fluids.defineLong("pump_energy_per_source", 100L)
                .min(0L)
                .describe("RF consumed per source block pumped")
                .register();

        public static final ConfigKey<Integer> FLUID_PUMP_PUSH_RATE_MB = fluids.defineInt("pump_push_rate_mb", 400)
                .min(1)
                .describe("Fluid Pump output rate (mB/tick)")
                .register();

        public static final ConfigKey<Integer> FLUID_PUMP_INTERVAL_TICKS = fluids.defineInt("pump_interval_ticks", 16)
                .min(1)
                .describe("Fluid Pump source pickup interval (ticks)")
                .register();

        // Capped at 46340 so the radius can be squared as an int downstream without overflowing.
        public static final ConfigKey<Integer> FLUID_PUMP_SEARCH_RADIUS = fluids.defineInt("pump_search_radius", 64)
                .range(1, 46_340)
                .describe("Fluid Pump connected source search radius")
                .register();

        public static final ConfigKey<Float> FLUID_PUMP_ARM_SPEED = fluids.defineFloat("pump_arm_speed", 0.01f)
                .finite()
                .greaterThan(0.0f)
                .describe("Fluid Pump arm movement speed (blocks/tick)")
                .register();

        public static final ConfigKey<Integer> FLUID_PUMP_INFINITE_SOURCE_THRESHOLD =
                fluids.defineInt("pump_infinite_source_threshold", 9)
                        .min(0)
                        .describe("Connected water sources at or above which the pump treats the body as infinite and"
                                + " pumps without consuming blocks (0 = always consume)")
                        .register();

        // ==================== reporting (crash reporting) ====================

        public static final ConfigKey<Boolean> CRASH_REPORTING_ENABLED = reporting.defineBoolean("enabled", false)
                .describe("Opt-in sanitized crash reporting via Sentry")
                .register();

        public static final ConfigKey<Boolean> CRASH_REPORTING_SHOW_NOTIFICATION =
                reporting.defineBoolean("show_notification", true)
                        .describe("Notify operators on join while crash reporting is active")
                        .register();

        public static final ConfigKey<String> CRASH_REPORTING_DSN_OVERRIDE =
                reporting.defineString("dsn_override", "")
                        .describe("Override the Sentry DSN (blank = built-in default)")
                        .register();

        /** Runs after every load (startup + reload-configs): heal inverted ranges, reconcile Sentry. */
        public static void bootstrap(Config config) {
            engines.registerSanitizeHook(() -> engines.repairMinMax(STIRLING_MIN_OUTPUT, STIRLING_MAX_OUTPUT));
            pipes.registerSanitizeHook(() -> pipes.repairMinMax(PIPE_MIN_SPEED, PIPE_MAX_SPEED));
            reporting.registerSanitizeHook(CrashReporting::reconcile);
        }
    }
}
