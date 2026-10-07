package com.logistics.core;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;
import com.indemnity83.configory.ConfigKey;
import com.indemnity83.configory.ConfigRegistry;
import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsCore;
import com.logistics.LogisticsPipe;
import com.logistics.LogisticsPower;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LogisticsConfigMigrator")
class LogisticsConfigMigratorTest {

    // Keys this test mutates on the shared registry configs; reset around each test to avoid leakage.
    private static final ConfigKey<?>[] TOUCHED = {
        LogisticsAutomation.CONFIG.QUARRY_AREA, LogisticsAutomation.CONFIG.QUARRY_SCAN_RATE,
        LogisticsPipe.CONFIG.PIPE_MIN_SPEED, LogisticsPipe.CONFIG.PIPE_MAX_SPEED,
        LogisticsCore.CONFIG.REDSTONE_OUTPUT, LogisticsCore.CONFIG.CRASH_REPORTING_ENABLED,
        LogisticsPower.CONFIG.BATTERY_BASIC_CAPACITY, LogisticsPower.CONFIG.BATTERY_BASIC_MAX_IO,
        LogisticsPower.CONFIG.BATTERY_BASIC_OUTPUT_PER_SIDE,
    };

    @BeforeEach
    @AfterEach
    void reset() {
        LogisticsConfigMigrator.clearMappingsForTest();
        for (ConfigKey<?> key : TOUCHED) {
            resetKey(key);
        }
    }

    private static <T> void resetKey(ConfigKey<T> key) {
        ConfigRegistry.config(key.configId()).set(key, key.definition().defaultValue());
    }

    @Test
    @DisplayName("applies registered mappings: scalars, invalid-skip, inverted-pair repair, engine group")
    void appliesRegisteredMappings() {
        // Domains register these during registerConfig(); register the ones under test directly.
        LogisticsConfigMigrator.mapLegacy("quarry", "area", LogisticsAutomation.CONFIG.QUARRY_AREA);
        LogisticsConfigMigrator.mapLegacy("quarry", "scanRate", LogisticsAutomation.CONFIG.QUARRY_SCAN_RATE);
        LogisticsConfigMigrator.mapLegacyPair(
                "pipe", "minSpeed", "maxSpeed", LogisticsPipe.CONFIG.PIPE_MIN_SPEED, LogisticsPipe.CONFIG.PIPE_MAX_SPEED);
        LogisticsConfigMigrator.mapLegacy("engine", "redstoneOutput", LogisticsCore.CONFIG.REDSTONE_OUTPUT);
        LogisticsConfigMigrator.mapLegacy("crashReporting", "enabled", LogisticsCore.CONFIG.CRASH_REPORTING_ENABLED);

        JsonObject root = new JsonObject();
        JsonObject quarry = new JsonObject();
        quarry.addProperty("area", 24); // valid → migrated
        quarry.addProperty("scanRate", 0); // invalid (min 1) → skipped, keeps default
        root.add("quarry", quarry);
        JsonObject pipe = new JsonObject();
        pipe.addProperty("minSpeed", 0.5); // inverted vs max → repaired
        pipe.addProperty("maxSpeed", 0.1);
        root.add("pipe", pipe);
        JsonObject engine = new JsonObject(); // 0.8.x upgraders carry this group
        engine.addProperty("redstoneOutput", 99);
        root.add("engine", engine);
        JsonObject crash = new JsonObject();
        crash.addProperty("enabled", true);
        root.add("crashReporting", crash);

        LogisticsConfigMigrator.apply(root);

        assertThat(LogisticsConfigHost.get(LogisticsAutomation.CONFIG.QUARRY_AREA)).isEqualTo(24);
        assertThat(LogisticsConfigHost.get(LogisticsAutomation.CONFIG.QUARRY_SCAN_RATE))
                .isEqualTo(LogisticsAutomation.CONFIG.QUARRY_SCAN_RATE.definition().defaultValue()); // skipped
        assertThat(LogisticsConfigHost.get(LogisticsPipe.CONFIG.PIPE_MIN_SPEED))
                .isLessThanOrEqualTo(LogisticsConfigHost.get(LogisticsPipe.CONFIG.PIPE_MAX_SPEED)); // repaired
        assertThat(LogisticsConfigHost.get(LogisticsCore.CONFIG.REDSTONE_OUTPUT)).isEqualTo(99L);
        assertThat(LogisticsConfigHost.get(LogisticsCore.CONFIG.CRASH_REPORTING_ENABLED)).isTrue();
    }

    /**
     * The pre-tier {@code power/battery.json} folds into the Basic tier.
     *
     * <p>Capacity matters most: {@code EnergyComponent} clamps a saved charge to the configured
     * capacity on load, so a player who raised it and then updated would lose the excess RF in every
     * battery they own, not merely their setting.
     */
    @Test
    @DisplayName("the pre-tier battery section migrates into the Basic tier")
    void migratesBatterySectionIntoBasic() {
        registerBatterySplit();

        JsonObject legacy = new JsonObject();
        legacy.addProperty("capacity", 500_000);
        legacy.addProperty("max_io", 4_000);
        legacy.addProperty("output_per_side", 900);
        LogisticsConfigMigrator.applySplit("power/battery.json", legacy);

        assertThat(valueOf(LogisticsPower.CONFIG.BATTERY_BASIC_CAPACITY)).isEqualTo(500_000L);
        assertThat(valueOf(LogisticsPower.CONFIG.BATTERY_BASIC_MAX_IO)).isEqualTo(4_000L);
        assertThat(valueOf(LogisticsPower.CONFIG.BATTERY_BASIC_OUTPUT_PER_SIDE)).isEqualTo(900L);
    }

    /** A value the player already set on the new section wins; migration never overwrites it. */
    @Test
    @DisplayName("an already-customised Basic setting is left alone")
    void doesNotOverwriteCustomisedBasicSettings() {
        registerBatterySplit();
        ConfigRegistry.config(LogisticsPower.CONFIG.BATTERY_BASIC_CAPACITY.configId())
                .set(LogisticsPower.CONFIG.BATTERY_BASIC_CAPACITY, 250_000L);

        JsonObject legacy = new JsonObject();
        legacy.addProperty("capacity", 500_000);
        LogisticsConfigMigrator.applySplit("power/battery.json", legacy);

        assertThat(valueOf(LogisticsPower.CONFIG.BATTERY_BASIC_CAPACITY)).isEqualTo(250_000L);
    }

    /** Only the Basic tier inherits the old numbers — the tiers above it keep their own defaults. */
    @Test
    @DisplayName("migration does not touch the tiers above Basic")
    void leavesHigherTiersAtTheirDefaults() {
        registerBatterySplit();

        JsonObject legacy = new JsonObject();
        legacy.addProperty("capacity", 500_000);
        LogisticsConfigMigrator.applySplit("power/battery.json", legacy);

        assertThat(valueOf(LogisticsPower.CONFIG.BATTERY_CONDUCTIVE_CAPACITY))
                .isEqualTo(LogisticsPower.CONFIG.BATTERY_CONDUCTIVE_CAPACITY.definition().defaultValue());
    }

    private static void registerBatterySplit() {
        LogisticsConfigMigrator.mapSplitField(
                "power/battery.json", "capacity", LogisticsPower.CONFIG.BATTERY_BASIC_CAPACITY);
        LogisticsConfigMigrator.mapSplitPair("power/battery.json", "output_per_side", "max_io",
                LogisticsPower.CONFIG.BATTERY_BASIC_OUTPUT_PER_SIDE, LogisticsPower.CONFIG.BATTERY_BASIC_MAX_IO);
    }

    private static <T> T valueOf(ConfigKey<T> key) {
        return ConfigRegistry.config(key.configId()).get(key);
    }
}
