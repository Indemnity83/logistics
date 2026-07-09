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
        LogisticsPower.CONFIG.REDSTONE_OUTPUT, LogisticsCore.CONFIG.CRASH_REPORTING_ENABLED,
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
        LogisticsConfigMigrator.mapLegacy("engine", "redstoneOutput", LogisticsPower.CONFIG.REDSTONE_OUTPUT);
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
        assertThat(LogisticsConfigHost.get(LogisticsPower.CONFIG.REDSTONE_OUTPUT)).isEqualTo(99L);
        assertThat(LogisticsConfigHost.get(LogisticsCore.CONFIG.CRASH_REPORTING_ENABLED)).isTrue();
    }
}
