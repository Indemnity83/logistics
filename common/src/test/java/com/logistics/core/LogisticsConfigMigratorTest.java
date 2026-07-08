package com.logistics.core;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;
import com.indemnity83.configory.ConfigKey;
import com.indemnity83.configory.ConfigRegistry;
import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsConfigHost.Configs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LogisticsConfigMigrator")
class LogisticsConfigMigratorTest {

    // Keys this test mutates on the shared registry configs; reset around each test to avoid leakage.
    private static final ConfigKey<?>[] TOUCHED = {
        Configs.QUARRY_AREA, Configs.QUARRY_SCAN_RATE, Configs.PIPE_MIN_SPEED, Configs.PIPE_MAX_SPEED,
        Configs.REDSTONE_OUTPUT, Configs.CRASH_REPORTING_ENABLED, Configs.CRASH_REPORTING_DSN_OVERRIDE,
    };

    @BeforeEach
    @AfterEach
    void resetToDefaults() {
        // Order-independent for the pipe pair: defaults (0.02 <= 0.16) satisfy the cross-field check either way.
        for (ConfigKey<?> key : TOUCHED) {
            reset(key);
        }
    }

    private static <T> void reset(ConfigKey<T> key) {
        ConfigRegistry.config(key.configId()).set(key, key.definition().defaultValue());
    }

    @Test
    @DisplayName("migrates scalars, skips invalid values, repairs an inverted pair, and reads the engine group")
    void seedsFromLegacyJson() {
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
        crash.addProperty("dsnOverride", "http://k@localhost/1");
        root.add("crashReporting", crash);

        LogisticsConfigMigrator.seed(root);

        assertThat(LogisticsConfigHost.get(Configs.QUARRY_AREA)).isEqualTo(24);
        assertThat(LogisticsConfigHost.get(Configs.QUARRY_SCAN_RATE))
                .isEqualTo(Configs.QUARRY_SCAN_RATE.definition().defaultValue()); // invalid skipped
        assertThat(LogisticsConfigHost.get(Configs.PIPE_MIN_SPEED))
                .isLessThanOrEqualTo(LogisticsConfigHost.get(Configs.PIPE_MAX_SPEED)); // repaired
        assertThat(LogisticsConfigHost.get(Configs.REDSTONE_OUTPUT)).isEqualTo(99L);
        assertThat(LogisticsConfigHost.get(Configs.CRASH_REPORTING_ENABLED)).isTrue();
        assertThat(LogisticsConfigHost.get(Configs.CRASH_REPORTING_DSN_OVERRIDE)).isEqualTo("http://k@localhost/1");
    }
}
