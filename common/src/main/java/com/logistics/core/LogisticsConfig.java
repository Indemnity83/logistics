package com.logistics.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.logistics.core.lib.platform.PlatformService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Central config for Logistics. Backed by config/logistics.json (Gson).
 *
 * <p>{@code LogisticsConfig.load()} is called during LogisticsCore.initCommon() and reads
 * {@code config/logistics.json}, writing defaults if the file is missing.
 *
 * <p>Runtime access: {@code LogisticsConfig.get().quarry.energyPerBlock}
 * <p>Command access: {@code /logistics config list|get|set|reload}
 */
public final class LogisticsConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("logistics/config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static volatile LogisticsConfig INSTANCE = new LogisticsConfig();
    private static Path configPath;

    // ==================== Config Groups ====================

    public QuarryConfig quarry = new QuarryConfig();
    public PipeConfig pipe = new PipeConfig();
    public EngineConfig engine = new EngineConfig();
    public FluidPipeConfig fluidPipe = new FluidPipeConfig();
    public FluidPumpConfig fluidPump = new FluidPumpConfig();
    public CrashReportingConfig crashReporting = new CrashReportingConfig();

    public static final class QuarryConfig {
        public int area = 16;
        public long energyPerBlock = 60L;
        public double energyMultiplier = 1.0;
        public float armSpeed = 0.1f;
        public float armSpeedScaling = 2000f;
        public long armEnergy = 20L;
        public float rainPenalty = 0.7f;
        public int scanRate = 256;
        public boolean loadChunks = false;

        // Derived values — computed from the fields above.
        public double energyPerBlockMultiplier() { return nonNegativeFiniteOrZero(energyPerBlock * energyMultiplier * 2); }
        public long energyCapacity()          { return nonNegativeLongOrZero(128.0 * energyPerBlock * energyMultiplier); }
        public long maxEnergyInput()          { return nonNegativeLongOrZero(1_000L * energyMultiplier); }

        private static double nonNegativeFiniteOrZero(double value) {
            return Double.isFinite(value) ? Math.max(0.0, value) : 0.0;
        }

        private static long nonNegativeLongOrZero(double value) {
            if (!Double.isFinite(value)) return 0L;
            return Math.max(0L, (long) value);
        }
    }

    public static final class PipeConfig {
        public float acceleration = 1.0f / 200.0f;
        public float drag = 0.005f;
        public float minSpeed = 0.02f;
        public float maxSpeed = 0.16f;
        public float injectSpeed = 0.2f;
    }

    public static final class EngineConfig {
        public long redstoneOutput = 10L;
        public double stirlingMinOutput = 3.0;
        public double stirlingMaxOutput = 10.0;
    }

    public static final class FluidPipeConfig {
        /**
         * Base fluid pipe transfer rate, in mB/tick. Each pipe tier scales this by a fixed multiplier
         * (stone/extractor/void 1×, copper/bypass 2×, merger 3×, gold 4×), so this one knob moves them all.
         */
        public int baseTransferRate = 10;
        /** Fluid pipe internal buffer, in mB (shared by every pipe kind). */
        public int baseCapacity = 250;
        /** Whether the Fluid Extractor Pipe requires engine power to extract. */
        public boolean woodenRequiresEngine = true;
        /** Debug toggle: when false, extractors stop pulling fluid into the network. */
        public boolean activeExtraction = true;
    }

    public static final class FluidPumpConfig {
        public int tankCapacityMb = 16_000;
        public long energyCapacity = 1_000L;
        public long maxEnergyInput = 150L;
        public long energyPerSource = 100L;
        public int pushRateMb = 400;
        public int pumpIntervalTicks = 16;
        public int searchRadius = 64;
        public float armSpeed = 0.01f;
        public int infiniteSourceThreshold = 9;
    }

    /**
     * Opt-in sanitized crash reporting (Sentry). Disabled by default; an operator opts in via
     * {@code /logistics diagnostics enable}. Intentionally NOT in the {@link #ENTRIES} registry —
     * the {@code /logistics diagnostics} commands are the single source of truth so the persisted
     * value and the live Sentry client never drift apart (e.g. via a generic {@code config set}).
     */
    public static final class CrashReportingConfig {
        public boolean enabled = false;
        public boolean notifyOperators = true;
        public String dsnOverride = "";
    }

    // ==================== Config Entry Registry (for commands) ====================

    /** Largest pump search radius whose square still fits in an int ({@code 46340^2 < Integer.MAX_VALUE}). */
    private static final long MAX_PUMP_SEARCH_RADIUS = 46_340L;

    public static final Map<String, ConfigEntry<?>> ENTRIES;

    static {
        Map<String, ConfigEntry<?>> map = new LinkedHashMap<>();

        // Quarry
        reg(map, "quarry_area", "Quarry mining area side length (NxN blocks)",
                () -> (long) INSTANCE.quarry.area,
                v -> INSTANCE.quarry.area = v.intValue(),
                Long::parseLong,
                v -> requireRange(v, 3L, (long) Integer.MAX_VALUE, "must be between 3 and " + Integer.MAX_VALUE));
        reg(map, "quarry_energy_per_block", "RF cost per block mined",
                () -> INSTANCE.quarry.energyPerBlock,
                v -> INSTANCE.quarry.energyPerBlock = v,
                Long::parseLong,
                v -> requireMin(v, 0L, "must be greater than or equal to 0"));
        reg(map, "quarry_energy_multiplier", "Global energy cost multiplier",
                () -> INSTANCE.quarry.energyMultiplier,
                v -> INSTANCE.quarry.energyMultiplier = v,
                Double::parseDouble,
                v -> requireFiniteMin(v, 0.0, "must be finite and greater than or equal to 0"));
        reg(map, "quarry_arm_speed", "Arm movement speed (blocks/tick)",
                () -> (double) INSTANCE.quarry.armSpeed,
                v -> INSTANCE.quarry.armSpeed = v.floatValue(),
                Double::parseDouble,
                v -> requireFiniteFloatMin(v, 0.0, "must be finite and greater than or equal to 0"));
        reg(map, "quarry_arm_speed_scaling", "Energy-to-speed scaling constant",
                () -> (double) INSTANCE.quarry.armSpeedScaling,
                v -> INSTANCE.quarry.armSpeedScaling = v.floatValue(),
                Double::parseDouble,
                v -> requireFiniteFloatGreaterThan(v, 0.0, "must be finite and greater than 0"));
        reg(map, "quarry_arm_energy", "RF/tick cost for arm movement",
                () -> INSTANCE.quarry.armEnergy,
                v -> INSTANCE.quarry.armEnergy = v,
                Long::parseLong,
                v -> requireMin(v, 0L, "must be greater than or equal to 0"));
        reg(map, "quarry_rain_penalty", "Speed multiplier when raining (0.0-1.0)",
                () -> (double) INSTANCE.quarry.rainPenalty,
                v -> INSTANCE.quarry.rainPenalty = v.floatValue(),
                Double::parseDouble,
                v -> requireFiniteRange(v, 0.0, 1.0, "must be finite and between 0.0 and 1.0"));
        reg(map, "quarry_scan_rate", "Max blocks scanned per tick when searching",
                () -> (long) INSTANCE.quarry.scanRate,
                v -> INSTANCE.quarry.scanRate = v.intValue(),
                Long::parseLong,
                v -> requireRange(v, 1L, (long) Integer.MAX_VALUE, "must be between 1 and " + Integer.MAX_VALUE));
        reg(map, "quarry_load_chunks", "Keep the quarry and its work area chunk-loaded while running",
                () -> INSTANCE.quarry.loadChunks,
                mode -> INSTANCE.quarry.loadChunks = mode,
                LogisticsConfig::parseBooleanStrict,
                v -> {});

        // Pipe
        reg(map, "pipe_max_speed", "Item speed ceiling (blocks/tick)",
                () -> (double) INSTANCE.pipe.maxSpeed,
                v -> INSTANCE.pipe.maxSpeed = v.floatValue(),
                Double::parseDouble,
                v -> {
                    requireFiniteFloatGreaterThan(v, 0.0, "must be finite and greater than 0");
                    requireCondition(v >= INSTANCE.pipe.minSpeed, "must be greater than or equal to pipe_min_speed");
                });
        reg(map, "pipe_min_speed", "Item speed floor (blocks/tick)",
                () -> (double) INSTANCE.pipe.minSpeed,
                v -> INSTANCE.pipe.minSpeed = v.floatValue(),
                Double::parseDouble,
                v -> {
                    requireFiniteFloatGreaterThan(v, 0.0, "must be finite and greater than 0");
                    requireCondition(v <= INSTANCE.pipe.maxSpeed, "must be less than or equal to pipe_max_speed");
                });
        reg(map, "pipe_acceleration", "Speed gain per tick when accelerating",
                () -> (double) INSTANCE.pipe.acceleration,
                v -> INSTANCE.pipe.acceleration = v.floatValue(),
                Double::parseDouble,
                v -> requireFiniteFloatMin(v, 0.0, "must be finite and greater than or equal to 0"));
        reg(map, "pipe_drag", "Speed decay fraction per tick",
                () -> (double) INSTANCE.pipe.drag,
                v -> INSTANCE.pipe.drag = v.floatValue(),
                Double::parseDouble,
                v -> requireFiniteRange(v, 0.0, 1.0, "must be finite and between 0.0 and 1.0"));
        reg(map, "pipe_inject_speed", "Item speed when injected by network routing (blocks/tick)",
                () -> (double) INSTANCE.pipe.injectSpeed,
                v -> INSTANCE.pipe.injectSpeed = v.floatValue(),
                Double::parseDouble,
                v -> requireFiniteFloatGreaterThan(v, 0.0, "must be finite and greater than 0"));

        // Engine
        reg(map, "redstone_engine_output", "RF generated per 16-tick interval",
                () -> INSTANCE.engine.redstoneOutput,
                v -> INSTANCE.engine.redstoneOutput = v,
                Long::parseLong,
                v -> requireMin(v, 0L, "must be greater than or equal to 0"));
        reg(map, "stirling_engine_min_output", "Stirling engine minimum RF/t output",
                () -> INSTANCE.engine.stirlingMinOutput,
                v -> INSTANCE.engine.stirlingMinOutput = v,
                Double::parseDouble,
                v -> {
                    requireFiniteMin(v, 0.0, "must be finite and greater than or equal to 0");
                    requireCondition(
                            v <= INSTANCE.engine.stirlingMaxOutput,
                            "must be less than or equal to stirling_engine_max_output");
                });
        reg(map, "stirling_engine_max_output", "Stirling engine maximum RF/t output",
                () -> INSTANCE.engine.stirlingMaxOutput,
                v -> INSTANCE.engine.stirlingMaxOutput = v,
                Double::parseDouble,
                v -> {
                    requireFiniteMin(v, 0.0, "must be finite and greater than or equal to 0");
                    requireCondition(
                            v >= INSTANCE.engine.stirlingMinOutput,
                            "must be greater than or equal to stirling_engine_min_output");
                });

        // Fluid pipes
        reg(map, "fluid_pipe_base_transfer_rate", "Base Fluid Pipe transfer rate (mB/tick), scaled per tier",
                () -> (long) INSTANCE.fluidPipe.baseTransferRate,
                v -> INSTANCE.fluidPipe.baseTransferRate = v.intValue(),
                Long::parseLong,
                v -> requireRange(v, 1L, (long) Integer.MAX_VALUE, "must be between 1 and " + Integer.MAX_VALUE));
        reg(map, "fluid_pipe_base_capacity", "Fluid Pipe buffer capacity (mB)",
                () -> (long) INSTANCE.fluidPipe.baseCapacity,
                v -> INSTANCE.fluidPipe.baseCapacity = v.intValue(),
                Long::parseLong,
                v -> requireRange(v, 1L, (long) Integer.MAX_VALUE, "must be between 1 and " + Integer.MAX_VALUE));
        reg(map, "fluid_pipe_wooden_requires_engine", "Fluid Extractor Pipe requires engine power",
                () -> INSTANCE.fluidPipe.woodenRequiresEngine,
                v -> INSTANCE.fluidPipe.woodenRequiresEngine = v,
                LogisticsConfig::parseBooleanStrict,
                v -> {});
        reg(map, "fluid_pipe_active_extraction", "Debug: extractor pulling enabled",
                () -> INSTANCE.fluidPipe.activeExtraction,
                v -> INSTANCE.fluidPipe.activeExtraction = v,
                LogisticsConfig::parseBooleanStrict,
                v -> {});

        // Fluid pump
        reg(map, "fluid_pump_tank_capacity_mb", "Fluid Pump tank capacity (mB)",
                () -> (long) INSTANCE.fluidPump.tankCapacityMb,
                v -> INSTANCE.fluidPump.tankCapacityMb = v.intValue(),
                Long::parseLong,
                v -> requireRange(v, 1L, (long) Integer.MAX_VALUE, "must be between 1 and " + Integer.MAX_VALUE));
        reg(map, "fluid_pump_energy_capacity", "Fluid Pump energy buffer capacity (RF)",
                () -> INSTANCE.fluidPump.energyCapacity,
                v -> INSTANCE.fluidPump.energyCapacity = v,
                Long::parseLong,
                v -> requireMin(v, 1L, "must be greater than or equal to 1"));
        reg(map, "fluid_pump_max_energy_input", "Fluid Pump max energy input (RF/t)",
                () -> INSTANCE.fluidPump.maxEnergyInput,
                v -> INSTANCE.fluidPump.maxEnergyInput = v,
                Long::parseLong,
                v -> requireMin(v, 1L, "must be greater than or equal to 1"));
        reg(map, "fluid_pump_energy_per_source", "RF consumed per source block pumped",
                () -> INSTANCE.fluidPump.energyPerSource,
                v -> INSTANCE.fluidPump.energyPerSource = v,
                Long::parseLong,
                v -> requireMin(v, 0L, "must be greater than or equal to 0"));
        reg(map, "fluid_pump_push_rate_mb", "Fluid Pump output rate (mB/tick)",
                () -> (long) INSTANCE.fluidPump.pushRateMb,
                v -> INSTANCE.fluidPump.pushRateMb = v.intValue(),
                Long::parseLong,
                v -> requireRange(v, 1L, (long) Integer.MAX_VALUE, "must be between 1 and " + Integer.MAX_VALUE));
        reg(map, "fluid_pump_interval_ticks", "Fluid Pump source pickup interval (ticks)",
                () -> (long) INSTANCE.fluidPump.pumpIntervalTicks,
                v -> INSTANCE.fluidPump.pumpIntervalTicks = v.intValue(),
                Long::parseLong,
                v -> requireRange(v, 1L, (long) Integer.MAX_VALUE, "must be between 1 and " + Integer.MAX_VALUE));
        reg(map, "fluid_pump_search_radius", "Fluid Pump connected source search radius",
                () -> (long) INSTANCE.fluidPump.searchRadius,
                v -> INSTANCE.fluidPump.searchRadius = v.intValue(),
                Long::parseLong,
                // Capped so the radius can be squared as an int downstream without overflowing.
                v -> requireRange(v, 1L, MAX_PUMP_SEARCH_RADIUS, "must be between 1 and " + MAX_PUMP_SEARCH_RADIUS));
        reg(map, "fluid_pump_arm_speed", "Fluid Pump arm movement speed (blocks/tick)",
                () -> (double) INSTANCE.fluidPump.armSpeed,
                v -> INSTANCE.fluidPump.armSpeed = v.floatValue(),
                Double::parseDouble,
                v -> requireFiniteFloatGreaterThan(v, 0.0, "must be finite and greater than 0"));
        reg(map, "fluid_pump_infinite_source_threshold",
                "Connected water sources at or above which the pump treats the body as infinite and pumps without consuming blocks (0 = always consume)",
                () -> (long) INSTANCE.fluidPump.infiniteSourceThreshold,
                v -> INSTANCE.fluidPump.infiniteSourceThreshold = v.intValue(),
                Long::parseLong,
                v -> requireMin(v, 0L, "must be greater than or equal to 0"));

        ENTRIES = Collections.unmodifiableMap(map);
    }

    /** Strict boolean parse — rejects anything but {@code true}/{@code false} (unlike {@link Boolean#parseBoolean}). */
    private static Boolean parseBooleanStrict(String value) {
        if ("true".equalsIgnoreCase(value)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(value)) {
            return Boolean.FALSE;
        }
        throw new IllegalArgumentException("must be 'true' or 'false'");
    }

    private static <T> void reg(
            Map<String, ConfigEntry<?>> map,
            String key,
            String description,
            Supplier<T> getter,
            Consumer<T> setter,
            Function<String, T> parser,
            Consumer<T> validator) {
        map.put(key, new ConfigEntry<>(key, description, getter, setter, parser, validator));
    }

    // ==================== API ====================

    public static LogisticsConfig get() {
        return INSTANCE;
    }

    // ==================== Load / Save / Reload ====================

    /**
     * Load config from disk. Creates the file with defaults if missing.
     * Does NOT call refresh listeners — each domain applies config in its own initCommon().
     */
    public static void load() {
        configPath = PlatformService.INSTANCE.configDir().resolve("logistics.json");
        boolean loaded = false;
        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath)) {
                LogisticsConfig parsed = GSON.fromJson(reader, LogisticsConfig.class);
                if (parsed != null) {
                    INSTANCE = sanitize(parsed);
                    loaded = true;
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load logistics.json, using defaults: {}", e.getMessage());
            }
        }
        // Always write back so new fields added in future versions are persisted
        save();
        if (loaded) {
            LOGGER.info("Loaded logistics config from {}", configPath);
        } else {
            LOGGER.info("Created default logistics config at {}", configPath);
        }
    }

    /** Persist the current config to disk. */
    public static void save() {
        if (configPath == null) return;
        try (Writer writer = Files.newBufferedWriter(configPath)) {
            GSON.toJson(INSTANCE, writer);
        } catch (Exception e) {
            LOGGER.error("Failed to save logistics.json: {}", e.getMessage());
        }
    }

    /**
     * Reload config from disk and update in-memory settings.
     * Used by the {@code /logistics config reload} command.
     */
    public static void reload() {
        if (configPath == null) return;
        if (!Files.exists(configPath)) {
            LOGGER.warn("Config file not found at {}, skipping reload", configPath);
            return;
        }
        try (Reader reader = Files.newBufferedReader(configPath)) {
            LogisticsConfig loaded = GSON.fromJson(reader, LogisticsConfig.class);
            if (loaded != null) {
                INSTANCE = sanitize(loaded);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to reload logistics.json: {}", e.getMessage());
            return;
        }
        LOGGER.info("Reloaded logistics config from {}", configPath);
    }

    static LogisticsConfig sanitize(LogisticsConfig config) {
        LogisticsConfig defaults = new LogisticsConfig();
        if (config.quarry == null) {
            LOGGER.warn("Invalid logistics config group quarry: missing; using defaults");
            config.quarry = defaults.quarry;
        }
        if (config.pipe == null) {
            LOGGER.warn("Invalid logistics config group pipe: missing; using defaults");
            config.pipe = defaults.pipe;
        }
        if (config.engine == null) {
            LOGGER.warn("Invalid logistics config group engine: missing; using defaults");
            config.engine = defaults.engine;
        }
        if (config.fluidPipe == null) {
            LOGGER.warn("Invalid logistics config group fluidPipe: missing; using defaults");
            config.fluidPipe = defaults.fluidPipe;
        }
        if (config.fluidPump == null) {
            LOGGER.warn("Invalid logistics config group fluidPump: missing; using defaults");
            config.fluidPump = defaults.fluidPump;
        }
        if (config.crashReporting == null) {
            LOGGER.warn("Invalid logistics config group crashReporting: missing; using defaults");
            config.crashReporting = defaults.crashReporting;
        }
        if (config.crashReporting.dsnOverride == null) {
            config.crashReporting.dsnOverride = defaults.crashReporting.dsnOverride;
        }

        sanitizeInt("quarry_area", () -> (long) config.quarry.area, v -> config.quarry.area = v.intValue(),
                () -> (long) defaults.quarry.area, v -> requireRange(
                        v, 3L, (long) Integer.MAX_VALUE, "must be between 3 and " + Integer.MAX_VALUE));
        sanitizeLong("quarry_energy_per_block", () -> config.quarry.energyPerBlock,
                v -> config.quarry.energyPerBlock = v, () -> defaults.quarry.energyPerBlock,
                v -> requireMin(v, 0L, "must be greater than or equal to 0"));
        sanitizeDouble("quarry_energy_multiplier", () -> config.quarry.energyMultiplier,
                v -> config.quarry.energyMultiplier = v, () -> defaults.quarry.energyMultiplier,
                v -> requireFiniteMin(v, 0.0, "must be finite and greater than or equal to 0"));
        sanitizeFloat("quarry_arm_speed", () -> (double) config.quarry.armSpeed,
                v -> config.quarry.armSpeed = v.floatValue(), () -> (double) defaults.quarry.armSpeed,
                v -> requireFiniteFloatMin(v, 0.0, "must be finite and greater than or equal to 0"));
        sanitizeFloat("quarry_arm_speed_scaling", () -> (double) config.quarry.armSpeedScaling,
                v -> config.quarry.armSpeedScaling = v.floatValue(), () -> (double) defaults.quarry.armSpeedScaling,
                v -> requireFiniteFloatGreaterThan(v, 0.0, "must be finite and greater than 0"));
        sanitizeLong("quarry_arm_energy", () -> config.quarry.armEnergy,
                v -> config.quarry.armEnergy = v, () -> defaults.quarry.armEnergy,
                v -> requireMin(v, 0L, "must be greater than or equal to 0"));
        sanitizeFloat("quarry_rain_penalty", () -> (double) config.quarry.rainPenalty,
                v -> config.quarry.rainPenalty = v.floatValue(), () -> (double) defaults.quarry.rainPenalty,
                v -> requireFiniteRange(v, 0.0, 1.0, "must be finite and between 0.0 and 1.0"));
        sanitizeInt("quarry_scan_rate", () -> (long) config.quarry.scanRate, v -> config.quarry.scanRate = v.intValue(),
                () -> (long) defaults.quarry.scanRate, v -> requireRange(
                        v, 1L, (long) Integer.MAX_VALUE, "must be between 1 and " + Integer.MAX_VALUE));

        sanitizeFloat("pipe_min_speed", () -> (double) config.pipe.minSpeed,
                v -> config.pipe.minSpeed = v.floatValue(), () -> (double) defaults.pipe.minSpeed,
                v -> requireFiniteFloatGreaterThan(v, 0.0, "must be finite and greater than 0"));
        sanitizeFloat("pipe_max_speed", () -> (double) config.pipe.maxSpeed,
                v -> config.pipe.maxSpeed = v.floatValue(), () -> (double) defaults.pipe.maxSpeed,
                v -> requireFiniteFloatGreaterThan(v, 0.0, "must be finite and greater than 0"));
        sanitizePipeSpeedRange(config, defaults);
        sanitizeFloat("pipe_acceleration", () -> (double) config.pipe.acceleration,
                v -> config.pipe.acceleration = v.floatValue(), () -> (double) defaults.pipe.acceleration,
                v -> requireFiniteFloatMin(v, 0.0, "must be finite and greater than or equal to 0"));
        sanitizeFloat("pipe_drag", () -> (double) config.pipe.drag,
                v -> config.pipe.drag = v.floatValue(), () -> (double) defaults.pipe.drag,
                v -> requireFiniteRange(v, 0.0, 1.0, "must be finite and between 0.0 and 1.0"));
        sanitizeFloat("pipe_inject_speed", () -> (double) config.pipe.injectSpeed,
                v -> config.pipe.injectSpeed = v.floatValue(), () -> (double) defaults.pipe.injectSpeed,
                v -> requireFiniteFloatGreaterThan(v, 0.0, "must be finite and greater than 0"));

        sanitizeLong("redstone_engine_output", () -> config.engine.redstoneOutput,
                v -> config.engine.redstoneOutput = v, () -> defaults.engine.redstoneOutput,
                v -> requireMin(v, 0L, "must be greater than or equal to 0"));
        sanitizeDouble("stirling_engine_min_output", () -> config.engine.stirlingMinOutput,
                v -> config.engine.stirlingMinOutput = v, () -> defaults.engine.stirlingMinOutput,
                v -> requireFiniteMin(v, 0.0, "must be finite and greater than or equal to 0"));
        sanitizeDouble("stirling_engine_max_output", () -> config.engine.stirlingMaxOutput,
                v -> config.engine.stirlingMaxOutput = v, () -> defaults.engine.stirlingMaxOutput,
                v -> requireFiniteMin(v, 0.0, "must be finite and greater than or equal to 0"));
        sanitizeStirlingOutputRange(config, defaults);

        sanitizeInt("fluid_pipe_base_transfer_rate", () -> (long) config.fluidPipe.baseTransferRate,
                v -> config.fluidPipe.baseTransferRate = v.intValue(), () -> (long) defaults.fluidPipe.baseTransferRate,
                v -> requireRange(v, 1L, (long) Integer.MAX_VALUE, "must be between 1 and " + Integer.MAX_VALUE));
        sanitizeInt("fluid_pipe_base_capacity", () -> (long) config.fluidPipe.baseCapacity,
                v -> config.fluidPipe.baseCapacity = v.intValue(), () -> (long) defaults.fluidPipe.baseCapacity,
                v -> requireRange(v, 1L, (long) Integer.MAX_VALUE, "must be between 1 and " + Integer.MAX_VALUE));

        sanitizeInt("fluid_pump_tank_capacity_mb", () -> (long) config.fluidPump.tankCapacityMb,
                v -> config.fluidPump.tankCapacityMb = v.intValue(), () -> (long) defaults.fluidPump.tankCapacityMb,
                v -> requireRange(v, 1L, (long) Integer.MAX_VALUE, "must be between 1 and " + Integer.MAX_VALUE));
        sanitizeLong("fluid_pump_energy_capacity", () -> config.fluidPump.energyCapacity,
                v -> config.fluidPump.energyCapacity = v, () -> defaults.fluidPump.energyCapacity,
                v -> requireMin(v, 1L, "must be greater than or equal to 1"));
        sanitizeLong("fluid_pump_max_energy_input", () -> config.fluidPump.maxEnergyInput,
                v -> config.fluidPump.maxEnergyInput = v, () -> defaults.fluidPump.maxEnergyInput,
                v -> requireMin(v, 1L, "must be greater than or equal to 1"));
        sanitizeLong("fluid_pump_energy_per_source", () -> config.fluidPump.energyPerSource,
                v -> config.fluidPump.energyPerSource = v, () -> defaults.fluidPump.energyPerSource,
                v -> requireMin(v, 0L, "must be greater than or equal to 0"));
        sanitizeInt("fluid_pump_push_rate_mb", () -> (long) config.fluidPump.pushRateMb,
                v -> config.fluidPump.pushRateMb = v.intValue(), () -> (long) defaults.fluidPump.pushRateMb,
                v -> requireRange(v, 1L, (long) Integer.MAX_VALUE, "must be between 1 and " + Integer.MAX_VALUE));
        sanitizeInt("fluid_pump_interval_ticks", () -> (long) config.fluidPump.pumpIntervalTicks,
                v -> config.fluidPump.pumpIntervalTicks = v.intValue(), () -> (long) defaults.fluidPump.pumpIntervalTicks,
                v -> requireRange(v, 1L, (long) Integer.MAX_VALUE, "must be between 1 and " + Integer.MAX_VALUE));
        sanitizeInt("fluid_pump_search_radius", () -> (long) config.fluidPump.searchRadius,
                v -> config.fluidPump.searchRadius = v.intValue(), () -> (long) defaults.fluidPump.searchRadius,
                // Same cap as the command path so a hand-edited config can't reach the downstream squaring.
                v -> requireRange(v, 1L, MAX_PUMP_SEARCH_RADIUS, "must be between 1 and " + MAX_PUMP_SEARCH_RADIUS));
        sanitizeFloat("fluid_pump_arm_speed", () -> (double) config.fluidPump.armSpeed,
                v -> config.fluidPump.armSpeed = v.floatValue(), () -> (double) defaults.fluidPump.armSpeed,
                v -> requireFiniteFloatGreaterThan(v, 0.0, "must be finite and greater than 0"));
        sanitizeInt("fluid_pump_infinite_source_threshold", () -> (long) config.fluidPump.infiniteSourceThreshold,
                v -> config.fluidPump.infiniteSourceThreshold = v.intValue(),
                () -> (long) defaults.fluidPump.infiniteSourceThreshold,
                v -> requireMin(v, 0L, "must be greater than or equal to 0"));

        return config;
    }

    static void useForTests(LogisticsConfig config) {
        INSTANCE = sanitize(config);
    }

    private static void sanitizeInt(
            String key,
            Supplier<Long> getter,
            Consumer<Long> setter,
            Supplier<Long> defaultValue,
            Consumer<Long> validator) {
        sanitizeLong(key, getter, setter, defaultValue, validator);
    }

    private static void sanitizeLong(
            String key,
            Supplier<Long> getter,
            Consumer<Long> setter,
            Supplier<Long> defaultValue,
            Consumer<Long> validator) {
        sanitizeValue(key, getter, setter, defaultValue, validator);
    }

    private static void sanitizeFloat(
            String key,
            Supplier<Double> getter,
            Consumer<Double> setter,
            Supplier<Double> defaultValue,
            Consumer<Double> validator) {
        sanitizeValue(key, getter, setter, defaultValue, validator);
    }

    private static void sanitizeDouble(
            String key,
            Supplier<Double> getter,
            Consumer<Double> setter,
            Supplier<Double> defaultValue,
            Consumer<Double> validator) {
        sanitizeValue(key, getter, setter, defaultValue, validator);
    }

    private static <T> void sanitizeValue(
            String key,
            Supplier<T> getter,
            Consumer<T> setter,
            Supplier<T> defaultValue,
            Consumer<T> validator) {
        T value = getter.get();
        try {
            validator.accept(value);
        } catch (IllegalArgumentException e) {
            T replacement = defaultValue.get();
            setter.accept(replacement);
            LOGGER.warn("Invalid logistics config value {}={}: {}; using default {}", key, value, e.getMessage(), replacement);
        }
    }

    private static void sanitizePipeSpeedRange(LogisticsConfig config, LogisticsConfig defaults) {
        if (config.pipe.maxSpeed >= config.pipe.minSpeed) {
            return;
        }
        float originalMax = config.pipe.maxSpeed;
        if (defaults.pipe.maxSpeed >= config.pipe.minSpeed) {
            config.pipe.maxSpeed = defaults.pipe.maxSpeed;
            LOGGER.warn(
                    "Invalid logistics config value pipe_max_speed={}: must be greater than or equal to pipe_min_speed; using default {}",
                    originalMax,
                    config.pipe.maxSpeed);
            return;
        }

        float originalMin = config.pipe.minSpeed;
        config.pipe.minSpeed = defaults.pipe.minSpeed;
        if (config.pipe.maxSpeed < config.pipe.minSpeed) {
            config.pipe.maxSpeed = defaults.pipe.maxSpeed;
        }
        LOGGER.warn(
                "Invalid logistics config value pipe_min_speed={}: must be less than or equal to pipe_max_speed; using default {}",
                originalMin,
                config.pipe.minSpeed);
    }

    private static void sanitizeStirlingOutputRange(LogisticsConfig config, LogisticsConfig defaults) {
        if (config.engine.stirlingMaxOutput >= config.engine.stirlingMinOutput) {
            return;
        }
        double originalMax = config.engine.stirlingMaxOutput;
        if (defaults.engine.stirlingMaxOutput >= config.engine.stirlingMinOutput) {
            config.engine.stirlingMaxOutput = defaults.engine.stirlingMaxOutput;
            LOGGER.warn(
                    "Invalid logistics config value stirling_engine_max_output={}: must be greater than or equal to stirling_engine_min_output; using default {}",
                    originalMax,
                    config.engine.stirlingMaxOutput);
            return;
        }

        double originalMin = config.engine.stirlingMinOutput;
        config.engine.stirlingMinOutput = defaults.engine.stirlingMinOutput;
        if (config.engine.stirlingMaxOutput < config.engine.stirlingMinOutput) {
            config.engine.stirlingMaxOutput = defaults.engine.stirlingMaxOutput;
        }
        LOGGER.warn(
                "Invalid logistics config value stirling_engine_min_output={}: must be less than or equal to stirling_engine_max_output; using default {}",
                originalMin,
                config.engine.stirlingMinOutput);
    }

    private static void requireMin(long value, long min, String message) {
        requireCondition(value >= min, message);
    }

    private static void requireRange(long value, long min, long max, String message) {
        requireCondition(value >= min && value <= max, message);
    }

    private static void requireFiniteMin(double value, double min, String message) {
        requireCondition(Double.isFinite(value) && value >= min, message);
    }

    private static void requireFiniteRange(double value, double min, double max, String message) {
        requireCondition(Double.isFinite(value) && value >= min && value <= max, message);
    }

    private static void requireFiniteFloatMin(double value, double min, String message) {
        requireFiniteFloat(value, message);
        requireCondition(value >= min, message);
    }

    private static void requireFiniteFloatGreaterThan(double value, double minExclusive, String message) {
        requireFiniteFloat(value, message);
        requireCondition(value > minExclusive, message);
    }

    private static void requireFiniteFloat(double value, String message) {
        requireCondition(Double.isFinite(value) && Math.abs(value) <= Float.MAX_VALUE, message);
    }

    private static void requireCondition(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    // ==================== ConfigEntry ====================

    public record ConfigEntry<T>(
            String key,
            String description,
            Supplier<T> getter,
            Consumer<T> setter,
            Function<String, T> parser,
            Consumer<T> validator) {

        /** Returns the current value as a string. */
        public String getAsString() {
            return String.valueOf(getter.get());
        }

        /**
         * Parse {@code value} and apply it. Throws if parsing fails.
         * Callers should call {@link LogisticsConfig#save()} afterward.
         */
        @SuppressWarnings("unchecked")
        public void setFromString(String value) {
            T parsed = parser.apply(value);
            validator.accept(parsed);
            ((Consumer<T>) setter).accept(parsed);
        }
    }
}
