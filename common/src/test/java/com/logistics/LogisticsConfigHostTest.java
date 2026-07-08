package com.logistics;

import static org.assertj.core.api.Assertions.assertThat;

import com.indemnity83.configory.Config;
import com.indemnity83.configory.ConfigRegistry;
import com.indemnity83.configory.storage.JsonFileConfigStorage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("LogisticsConfigHost (configory)")
class LogisticsConfigHostTest {

    /** The shared engines child config the {@code Configs} constants are registered on. */
    private static Config engines() {
        return ConfigRegistry.config(LogisticsConfigHost.Configs.REDSTONE_OUTPUT.configId());
    }

    @Test
    @DisplayName("engine keys expose defaults and reject invalid single- and cross-field values")
    void engineKeysDefaultsAndValidation() {
        Config engines = engines();

        assertThat(LogisticsConfigHost.get(LogisticsConfigHost.Configs.REDSTONE_OUTPUT)).isEqualTo(10L);
        assertThat(LogisticsConfigHost.get(LogisticsConfigHost.Configs.STIRLING_MIN_OUTPUT)).isEqualTo(3.0);
        assertThat(LogisticsConfigHost.get(LogisticsConfigHost.Configs.STIRLING_MAX_OUTPUT)).isEqualTo(10.0);

        // Single-field: redstone output must be >= 0 (a rejected trySet leaves the value unchanged).
        assertThat(engines.trySet(LogisticsConfigHost.Configs.REDSTONE_OUTPUT, -1L)).isFalse();
        assertThat(LogisticsConfigHost.get(LogisticsConfigHost.Configs.REDSTONE_OUTPUT)).isEqualTo(10L);

        // Cross-field (recursion fixed in configory): min must not exceed max (default max 10).
        assertThat(engines.trySet(LogisticsConfigHost.Configs.STIRLING_MIN_OUTPUT, 12.0)).isFalse();
        assertThat(LogisticsConfigHost.get(LogisticsConfigHost.Configs.STIRLING_MIN_OUTPUT)).isEqualTo(3.0);
    }

    @Test
    @DisplayName("repairMinMax heals an inverted range even with cross-field validators (#52)")
    void repairMinMaxHealsInvertedRange() {
        Config engines = engines();

        // Raw path set forces an inverted state (bypasses validation).
        engines.set("stirling.min_output", 12.0);
        engines.set("stirling.max_output", 10.0);

        // Before #52 this threw (repairMinMax read through the validating get(), which the inverted state fails).
        engines.repairMinMax(
                LogisticsConfigHost.Configs.STIRLING_MIN_OUTPUT, LogisticsConfigHost.Configs.STIRLING_MAX_OUTPUT);

        double min = LogisticsConfigHost.get(LogisticsConfigHost.Configs.STIRLING_MIN_OUTPUT);
        double max = LogisticsConfigHost.get(LogisticsConfigHost.Configs.STIRLING_MAX_OUTPUT);
        assertThat(min).isLessThanOrEqualTo(max);

        // Restore defaults so the shared registry config doesn't leak into other tests.
        engines.set("stirling.min_output", 3.0);
        engines.set("stirling.max_output", 10.0);
    }

    @Test
    @DisplayName("save writes a JSON file with the defined values")
    void savesFileAsJson(@TempDir Path dir) throws IOException {
        // Storage injection (#41) keeps this isolated on a temp dir instead of the CWD-relative default.
        Config config = ConfigRegistry.getOrCreate("test.engines", new JsonFileConfigStorage(dir));
        config.defineLong("redstone.output", 10L).min(0L).register();

        config.load().save();

        try (Stream<Path> paths = Files.walk(dir)) {
            List<Path> jsonFiles = paths.filter(p -> p.toString().endsWith(".json")).toList();
            assertThat(jsonFiles).isNotEmpty();
            String json = Files.readString(jsonFiles.get(0));
            assertThat(json).contains("redstone").contains("output").contains("10");
        }
    }
}
