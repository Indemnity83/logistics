package com.logistics;

import static org.assertj.core.api.Assertions.assertThat;

import com.indemnity83.configory.Config;
import com.indemnity83.configory.ConfigKey;
import com.indemnity83.configory.ConfigRegistry;
import com.indemnity83.configory.storage.JsonFileConfigStorage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("LogisticsConfigHost (configory)")
class LogisticsConfigHostTest {

    @Test
    @DisplayName("engine keys expose defaults and reject invalid single-field values")
    void engineKeysDefaultsAndValidation() {
        // Referencing the Configs constants forces registration. get() returns the definition default
        // when nothing is loaded, and trySet validates without touching disk.
        Config config = ConfigRegistry.config(LogisticsConfigHost.MOD_ID);

        assertThat(config.get(LogisticsConfigHost.Configs.REDSTONE_OUTPUT)).isEqualTo(10L);
        assertThat(config.get(LogisticsConfigHost.Configs.STIRLING_MIN_OUTPUT)).isEqualTo(3.0);
        assertThat(config.get(LogisticsConfigHost.Configs.STIRLING_MAX_OUTPUT)).isEqualTo(10.0);

        // Single-field: redstone output must be >= 0 (a rejected trySet leaves the value unchanged).
        assertThat(config.trySet(LogisticsConfigHost.Configs.REDSTONE_OUTPUT, -1L)).isFalse();
        assertThat(config.get(LogisticsConfigHost.Configs.REDSTONE_OUTPUT)).isEqualTo(10L);
    }

    @Test
    @DisplayName("repairMinMax heals an inverted stirling range")
    void repairMinMaxHealsInvertedRange() {
        Config config = ConfigRegistry.config(LogisticsConfigHost.MOD_ID);

        // Drive the pair into an inverted state (each value is individually valid: both >= 0).
        config.set(LogisticsConfigHost.Configs.STIRLING_MIN_OUTPUT, 12.0);
        config.set(LogisticsConfigHost.Configs.STIRLING_MAX_OUTPUT, 10.0);

        config.repairMinMax(LogisticsConfigHost.Configs.STIRLING_MIN_OUTPUT, LogisticsConfigHost.Configs.STIRLING_MAX_OUTPUT);

        double min = config.get(LogisticsConfigHost.Configs.STIRLING_MIN_OUTPUT);
        double max = config.get(LogisticsConfigHost.Configs.STIRLING_MAX_OUTPUT);
        assertThat(min).isLessThanOrEqualTo(max);

        // Restore defaults so the shared registry config doesn't leak into other tests.
        config.set(LogisticsConfigHost.Configs.STIRLING_MIN_OUTPUT, 3.0);
        config.set(LogisticsConfigHost.Configs.STIRLING_MAX_OUTPUT, 10.0);
    }

    @Test
    @DisplayName("save writes the engines file as JSON grouped by path prefix")
    void savesEnginesFileAsJson(@TempDir Path dir) throws IOException {
        // A standalone config on a temp dir (not the shared CWD-relative registry) to keep the test isolated.
        Config config = Config.create("logistics-test", new JsonFileConfigStorage(dir));
        ConfigKey<Long> redstone = config.defineLong("engines.redstone.output", 10L)
                .min(0L)
                .register();

        config.load(); // applies defaults in memory
        config.save(); // persists to disk

        Path enginesFile = dir.resolve("engines.json"); // grouped by the "engines" path prefix
        assertThat(enginesFile).exists();
        String json = Files.readString(enginesFile);
        assertThat(json).contains("redstone").contains("output").contains("10");
        assertThat(config.get(redstone)).isEqualTo(10L);
    }
}
