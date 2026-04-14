package com.logistics.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
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

    public static final class QuarryConfig {
        public int area = 16;
        public long energyPerBlock = 60L;
        public double energyMultiplier = 1.0;
        public float armSpeed = 0.1f;
        public float armSpeedScaling = 2000f;
        public long armEnergy = 20L;
        public float rainPenalty = 0.7f;
        public int scanRate = 256;

        // Derived values — computed from the fields above.
        public double energyPerBlockMultiplier() { return energyPerBlock * energyMultiplier * 2; }
        public long energyCapacity()          { return (long) (2 * 64 * energyPerBlock * energyMultiplier); }
        public long maxEnergyInput()          { return (long) (1_000L * energyMultiplier); }
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

    // ==================== Config Entry Registry (for commands) ====================

    public static final Map<String, ConfigEntry<?>> ENTRIES;

    static {
        Map<String, ConfigEntry<?>> map = new LinkedHashMap<>();

        // Quarry
        reg(map, "quarry_area", "Quarry mining area side length (NxN blocks)",
                () -> (long) INSTANCE.quarry.area,
                v -> INSTANCE.quarry.area = v.intValue(),
                Long::parseLong);
        reg(map, "quarry_energy_per_block", "RF cost per block mined",
                () -> INSTANCE.quarry.energyPerBlock,
                v -> INSTANCE.quarry.energyPerBlock = v,
                Long::parseLong);
        reg(map, "quarry_energy_multiplier", "Global energy cost multiplier",
                () -> INSTANCE.quarry.energyMultiplier,
                v -> INSTANCE.quarry.energyMultiplier = v,
                Double::parseDouble);
        reg(map, "quarry_arm_speed", "Arm movement speed (blocks/tick)",
                () -> (double) INSTANCE.quarry.armSpeed,
                v -> INSTANCE.quarry.armSpeed = v.floatValue(),
                Double::parseDouble);
        reg(map, "quarry_arm_speed_scaling", "Energy-to-speed scaling constant",
                () -> (double) INSTANCE.quarry.armSpeedScaling,
                v -> INSTANCE.quarry.armSpeedScaling = v.floatValue(),
                Double::parseDouble);
        reg(map, "quarry_arm_energy", "RF/tick cost for arm movement",
                () -> INSTANCE.quarry.armEnergy,
                v -> INSTANCE.quarry.armEnergy = v,
                Long::parseLong);
        reg(map, "quarry_rain_penalty", "Speed multiplier when raining (0.0-1.0)",
                () -> (double) INSTANCE.quarry.rainPenalty,
                v -> INSTANCE.quarry.rainPenalty = v.floatValue(),
                Double::parseDouble);
        reg(map, "quarry_scan_rate", "Max blocks scanned per tick when searching",
                () -> (long) INSTANCE.quarry.scanRate,
                v -> INSTANCE.quarry.scanRate = v.intValue(),
                Long::parseLong);

        // Pipe
        reg(map, "pipe_max_speed", "Item speed ceiling (blocks/tick)",
                () -> (double) INSTANCE.pipe.maxSpeed,
                v -> INSTANCE.pipe.maxSpeed = v.floatValue(),
                Double::parseDouble);
        reg(map, "pipe_min_speed", "Item speed floor (blocks/tick)",
                () -> (double) INSTANCE.pipe.minSpeed,
                v -> INSTANCE.pipe.minSpeed = v.floatValue(),
                Double::parseDouble);
        reg(map, "pipe_acceleration", "Speed gain per tick when accelerating",
                () -> (double) INSTANCE.pipe.acceleration,
                v -> INSTANCE.pipe.acceleration = v.floatValue(),
                Double::parseDouble);
        reg(map, "pipe_drag", "Speed decay fraction per tick",
                () -> (double) INSTANCE.pipe.drag,
                v -> INSTANCE.pipe.drag = v.floatValue(),
                Double::parseDouble);
        reg(map, "pipe_inject_speed", "Item speed when injected by network routing (blocks/tick)",
                () -> (double) INSTANCE.pipe.injectSpeed,
                v -> INSTANCE.pipe.injectSpeed = v.floatValue(),
                Double::parseDouble);

        // Engine
        reg(map, "redstone_engine_output", "RF generated per 16-tick interval",
                () -> INSTANCE.engine.redstoneOutput,
                v -> INSTANCE.engine.redstoneOutput = v,
                Long::parseLong);
        reg(map, "stirling_engine_min_output", "Stirling engine minimum RF/t output",
                () -> INSTANCE.engine.stirlingMinOutput,
                v -> INSTANCE.engine.stirlingMinOutput = v,
                Double::parseDouble);
        reg(map, "stirling_engine_max_output", "Stirling engine maximum RF/t output",
                () -> INSTANCE.engine.stirlingMaxOutput,
                v -> INSTANCE.engine.stirlingMaxOutput = v,
                Double::parseDouble);

        ENTRIES = Collections.unmodifiableMap(map);
    }

    private static <T> void reg(
            Map<String, ConfigEntry<?>> map,
            String key,
            String description,
            Supplier<T> getter,
            Consumer<T> setter,
            Function<String, T> parser) {
        map.put(key, new ConfigEntry<>(key, description, getter, setter, parser));
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
        configPath = FabricLoader.getInstance().getConfigDir().resolve("logistics.json");
        boolean loaded = false;
        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath)) {
                LogisticsConfig parsed = GSON.fromJson(reader, LogisticsConfig.class);
                if (parsed != null) {
                    INSTANCE = parsed;
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
                INSTANCE = loaded;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to reload logistics.json: {}", e.getMessage());
            return;
        }
        LOGGER.info("Reloaded logistics config from {}", configPath);
    }

    // ==================== ConfigEntry ====================

    public record ConfigEntry<T>(
            String key,
            String description,
            Supplier<T> getter,
            Consumer<T> setter,
            Function<String, T> parser) {

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
            ((Consumer<T>) setter).accept(parsed);
        }
    }
}
