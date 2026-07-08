package com.logistics.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.indemnity83.configory.Config;
import com.indemnity83.configory.ConfigKey;
import com.indemnity83.configory.ConfigRegistry;
import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsConfigHost.Configs;
import com.logistics.core.crash.CrashReporting;
import com.logistics.core.lib.platform.PlatformService;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One-time migrator from the pre-configory god-config ({@code config/logistics.json}) to the per-domain
 * configory files ({@code config/logistics/*.json}).
 *
 * <p>Runs after {@link LogisticsConfigHost#bootstrap()}. If the legacy file exists, each nested value is
 * copied to the matching flat configory key, all configs are saved, and the legacy file is renamed to
 * {@code logistics.json.bak} — so it never re-triggers and can't clobber later edits. Reads the legacy file
 * as raw JSON (no dependency on the removed {@code LogisticsConfig} class). A failure is logged and swallowed;
 * migration is best-effort and must never abort startup (defaults are already loaded).
 */
public final class LogisticsConfigMigrator {
    private static final Logger LOGGER = LoggerFactory.getLogger("logistics/config");

    private LogisticsConfigMigrator() {}

    /** Migrate {@code config/logistics.json} into the per-domain configory files, if it still exists. */
    public static void migrateIfNeeded() {
        Path legacy = PlatformService.INSTANCE.configDir().resolve("logistics.json");
        if (!Files.exists(legacy)) {
            return;
        }
        try {
            JsonObject root;
            try (Reader reader = Files.newBufferedReader(legacy)) {
                JsonElement parsed = JsonParser.parseReader(reader);
                root = parsed != null && parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
            }
            seed(root);
            LogisticsConfigHost.engines().save();
            LogisticsConfigHost.machines().save();
            LogisticsConfigHost.pipes().save();
            LogisticsConfigHost.fluids().save();
            LogisticsConfigHost.reporting().save();
            // A migrated crash_reporting_enabled=true should take effect this session, not just next launch.
            CrashReporting.reconcile();
            Files.move(legacy, legacy.resolveSibling("logistics.json.bak"), StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("Migrated legacy logistics.json into per-domain config; renamed it to logistics.json.bak");
        } catch (Exception e) {
            LOGGER.error("Failed to migrate legacy logistics.json; continuing with current config", e);
        }
    }

    /** Copy every legacy nested value into its flat configory key (in memory). Package-visible for tests. */
    static void seed(JsonObject root) {
        // Quarry → machines
        migrate(root, "quarry", "area", Configs.QUARRY_AREA);
        migrate(root, "quarry", "energyPerBlock", Configs.QUARRY_ENERGY_PER_BLOCK);
        migrate(root, "quarry", "energyMultiplier", Configs.QUARRY_ENERGY_MULTIPLIER);
        migrate(root, "quarry", "armSpeed", Configs.QUARRY_ARM_SPEED);
        migrate(root, "quarry", "armSpeedScaling", Configs.QUARRY_ARM_SPEED_SCALING);
        migrate(root, "quarry", "armEnergy", Configs.QUARRY_ARM_ENERGY);
        migrate(root, "quarry", "rainPenalty", Configs.QUARRY_RAIN_PENALTY);
        migrate(root, "quarry", "scanRate", Configs.QUARRY_SCAN_RATE);
        migrate(root, "quarry", "loadChunks", Configs.QUARRY_LOAD_CHUNKS);

        // Item pipe → pipes (min/max as a repaired pair; the rest scalar)
        migrate(root, "pipe", "acceleration", Configs.PIPE_ACCELERATION);
        migrate(root, "pipe", "drag", Configs.PIPE_DRAG);
        migrate(root, "pipe", "injectSpeed", Configs.PIPE_INJECT_SPEED);
        migratePair(root, "pipe", "minSpeed", "maxSpeed", Configs.PIPE_MIN_SPEED, Configs.PIPE_MAX_SPEED);

        // Fluids → fluids
        migrate(root, "fluidPipe", "baseTransferRate", Configs.FLUID_PIPE_BASE_TRANSFER_RATE);
        migrate(root, "fluidPipe", "baseCapacity", Configs.FLUID_PIPE_BASE_CAPACITY);
        migrate(root, "fluidPipe", "woodenRequiresEngine", Configs.FLUID_PIPE_WOODEN_REQUIRES_ENGINE);
        migrate(root, "fluidPipe", "activeExtraction", Configs.FLUID_PIPE_ACTIVE_EXTRACTION);
        migrate(root, "fluidPump", "tankCapacityMb", Configs.FLUID_PUMP_TANK_CAPACITY_MB);
        migrate(root, "fluidPump", "energyCapacity", Configs.FLUID_PUMP_ENERGY_CAPACITY);
        migrate(root, "fluidPump", "maxEnergyInput", Configs.FLUID_PUMP_MAX_ENERGY_INPUT);
        migrate(root, "fluidPump", "energyPerSource", Configs.FLUID_PUMP_ENERGY_PER_SOURCE);
        migrate(root, "fluidPump", "pushRateMb", Configs.FLUID_PUMP_PUSH_RATE_MB);
        migrate(root, "fluidPump", "pumpIntervalTicks", Configs.FLUID_PUMP_INTERVAL_TICKS);
        migrate(root, "fluidPump", "searchRadius", Configs.FLUID_PUMP_SEARCH_RADIUS);
        migrate(root, "fluidPump", "armSpeed", Configs.FLUID_PUMP_ARM_SPEED);
        migrate(root, "fluidPump", "infiniteSourceThreshold", Configs.FLUID_PUMP_INFINITE_SOURCE_THRESHOLD);

        // Crash reporting → reporting
        migrate(root, "crashReporting", "enabled", Configs.CRASH_REPORTING_ENABLED);
        migrate(root, "crashReporting", "notifyOperators", Configs.CRASH_REPORTING_NOTIFY_OPERATORS);
        migrate(root, "crashReporting", "dsnOverride", Configs.CRASH_REPORTING_DSN_OVERRIDE);

        // Engines → engines. Released 0.8.x files carry an `engine` group; POC-era files don't.
        migrate(root, "engine", "redstoneOutput", Configs.REDSTONE_OUTPUT);
        migratePair(root, "engine", "stirlingMinOutput", "stirlingMaxOutput",
                Configs.STIRLING_MIN_OUTPUT, Configs.STIRLING_MAX_OUTPUT);
    }

    /** Copy one legacy {@code group.field} to {@code key} via validating trySet; skip (keep default) if invalid. */
    private static void migrate(JsonObject root, String group, String field, ConfigKey<?> key) {
        JsonElement value = valueAt(root, group, field);
        if (value == null) {
            return;
        }
        Config config = ConfigRegistry.config(key.configId());
        if (!config.trySet(key.path().fullPath(), coerce(key, value))) {
            LOGGER.warn("Skipped invalid legacy config value {}.{} during migration", group, field);
        }
    }

    /**
     * Copy a legacy min/max pair. Uses raw {@code set} (no per-key validation) for both members then
     * {@link Config#repairMinMax}, so a widened value isn't rejected against the still-default sibling and an
     * inverted on-disk pair is healed rather than dropped.
     */
    private static <T extends Number & Comparable<T>> void migratePair(
            JsonObject root, String group, String minField, String maxField, ConfigKey<T> minKey, ConfigKey<T> maxKey) {
        JsonElement minValue = valueAt(root, group, minField);
        JsonElement maxValue = valueAt(root, group, maxField);
        if (minValue == null && maxValue == null) {
            return;
        }
        Config config = ConfigRegistry.config(minKey.configId());
        if (minValue != null) {
            config.set(minKey.path().fullPath(), coerce(minKey, minValue));
        }
        if (maxValue != null) {
            config.set(maxKey.path().fullPath(), coerce(maxKey, maxValue));
        }
        config.repairMinMax(minKey, maxKey);
    }

    private static JsonElement valueAt(JsonObject root, String group, String field) {
        if (!root.has(group) || !root.get(group).isJsonObject()) {
            return null;
        }
        JsonObject groupObject = root.getAsJsonObject(group);
        if (!groupObject.has(field) || groupObject.get(field).isJsonNull()) {
            return null;
        }
        return groupObject.get(field);
    }

    private static Object coerce(ConfigKey<?> key, JsonElement value) {
        return switch (key.definition().type()) {
            case BOOLEAN -> value.getAsBoolean();
            case STRING, ENUM -> value.getAsString();
            case INT -> value.getAsInt();
            case LONG -> value.getAsLong();
            case FLOAT -> value.getAsFloat();
            case DOUBLE -> value.getAsDouble();
        };
    }
}
