package com.logistics;

import static org.assertj.core.api.Assertions.assertThat;

import com.indemnity83.configory.Config;
import com.indemnity83.configory.ConfigKey;
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

    /** The per-engine child configs the power {@code CONFIG} keys are registered on. */
    private static Config redstone() {
        return ConfigRegistry.config(LogisticsCore.CONFIG.REDSTONE_OUTPUT.configId());
    }

    private static Config stirling() {
        return ConfigRegistry.config(LogisticsPower.CONFIG.STIRLING_MIN_OUTPUT.configId());
    }

    @Test
    @DisplayName("engine keys expose defaults and reject invalid single- and cross-field values")
    void engineKeysDefaultsAndValidation() {
        Config redstone = redstone();
        Config stirling = stirling();

        assertThat(LogisticsConfigHost.get(LogisticsCore.CONFIG.REDSTONE_OUTPUT)).isEqualTo(10L);
        assertThat(LogisticsConfigHost.get(LogisticsPower.CONFIG.STIRLING_MIN_OUTPUT)).isEqualTo(3.0);
        assertThat(LogisticsConfigHost.get(LogisticsPower.CONFIG.STIRLING_MAX_OUTPUT)).isEqualTo(10.0);

        // Single-field: redstone output must be >= 0 (a rejected trySet leaves the value unchanged).
        assertThat(redstone.trySet(LogisticsCore.CONFIG.REDSTONE_OUTPUT, -1L)).isFalse();
        assertThat(LogisticsConfigHost.get(LogisticsCore.CONFIG.REDSTONE_OUTPUT)).isEqualTo(10L);

        // Cross-field: min must not exceed max (default max 10).
        assertThat(stirling.trySet(LogisticsPower.CONFIG.STIRLING_MIN_OUTPUT, 12.0)).isFalse();
        assertThat(LogisticsConfigHost.get(LogisticsPower.CONFIG.STIRLING_MIN_OUTPUT)).isEqualTo(3.0);
    }

    @Test
    @DisplayName("fluid packet keys expose defaults and reject values below their minimums")
    void fluidPacketKeysDefaultsAndValidation() {
        Config fluidLogistics = ConfigRegistry.config(LogisticsPipe.CONFIG.FLUID_PACKET_MAX_MB.configId());

        assertThat(LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PACKET_MAX_MB)).isEqualTo(5000L);
        assertThat(LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_ENDPOINT_RF_PER_PACKET)).isEqualTo(10L);

        try {
            assertThat(fluidLogistics.trySet(LogisticsPipe.CONFIG.FLUID_PACKET_MAX_MB, 1L)).isTrue();
            assertThat(fluidLogistics.trySet(LogisticsPipe.CONFIG.FLUID_PACKET_MAX_MB, 0L)).isFalse();

            assertThat(fluidLogistics.trySet(LogisticsPipe.CONFIG.FLUID_ENDPOINT_RF_PER_PACKET, 0L)).isTrue();
            assertThat(fluidLogistics.trySet(LogisticsPipe.CONFIG.FLUID_ENDPOINT_RF_PER_PACKET, -1L)).isFalse();
        } finally {
            fluidLogistics.set(LogisticsPipe.CONFIG.FLUID_PACKET_MAX_MB, 5000L);
            fluidLogistics.set(LogisticsPipe.CONFIG.FLUID_ENDPOINT_RF_PER_PACKET, 10L);
        }
    }

    @Test
    @DisplayName("repairMinMax heals an inverted range even with cross-field validators")
    void repairMinMaxHealsInvertedRange() {
        Config stirling = stirling();

        // Raw path set forces an inverted state (bypasses validation).
        stirling.set("min_output", 12.0);
        stirling.set("max_output", 10.0);

        // repairMinMax bypasses the validating get(), so it can heal a state the validators reject.
        stirling.repairMinMax(
                LogisticsPower.CONFIG.STIRLING_MIN_OUTPUT, LogisticsPower.CONFIG.STIRLING_MAX_OUTPUT);

        double min = LogisticsConfigHost.get(LogisticsPower.CONFIG.STIRLING_MIN_OUTPUT);
        double max = LogisticsConfigHost.get(LogisticsPower.CONFIG.STIRLING_MAX_OUTPUT);
        assertThat(min).isLessThanOrEqualTo(max);

        // Restore defaults so the shared registry config doesn't leak into other tests.
        stirling.set(LogisticsPower.CONFIG.STIRLING_MIN_OUTPUT, 3.0);
        stirling.set(LogisticsPower.CONFIG.STIRLING_MAX_OUTPUT, 10.0);
    }

    @Test
    @DisplayName("fluid pump search radius is capped so radius squared fits in an int")
    void pumpSearchRadiusCap() {
        Config fluids = ConfigRegistry.config(LogisticsPipe.CONFIG.FLUID_PUMP_SEARCH_RADIUS.configId());

        assertThat(fluids.trySet(LogisticsPipe.CONFIG.FLUID_PUMP_SEARCH_RADIUS, 46_340)).isTrue();
        assertThat(fluids.trySet(LogisticsPipe.CONFIG.FLUID_PUMP_SEARCH_RADIUS, 46_341)).isFalse();

        fluids.set(LogisticsPipe.CONFIG.FLUID_PUMP_SEARCH_RADIUS, 64); // restore default
    }

    @Test
    @DisplayName("each domain's CONFIG registers its child config under logistics")
    void domainConfigsEnumeratesEveryDomain() {
        // Touch a key from every domain so each CONFIG class initializes (registers its configFor child).
        List<ConfigKey<?>> ignored = List.of(
                LogisticsCore.CONFIG.REDSTONE_OUTPUT,
                LogisticsAutomation.CONFIG.QUARRY_AREA,
                LogisticsPipe.CONFIG.PIPE_MAX_SPEED,
                LogisticsPipe.CONFIG.FLUID_PUMP_SEARCH_RADIUS,
                LogisticsPower.CONFIG.FUEL_MIN_OUTPUT,
                LogisticsCore.CONFIG.CRASH_REPORTING_ENABLED);
        assertThat(ignored).isNotEmpty();

        List<String> ids = LogisticsConfigHost.domainConfigs().stream().map(Config::id).toList();
        // The complete set of registered child configs: one per engine/machine/power unit plus the flat
        // pipe/fluid/reporting files. Asserted exhaustively so a missing registration can't slip through.
        assertThat(ids).containsExactlyInAnyOrder(
                "logistics.engines.redstone",
                "logistics.engines.stirling",
                "logistics.engines.reaction",
                "logistics.engines.steam",
                "logistics.engines.creative",
                "logistics.engines.magmatic",
                "logistics.engines.fuel",
                "logistics.power.battery",
                "logistics.power.cables",
                "logistics.machines.macerator",
                "logistics.machines.kiln",
                "logistics.machines.sawmill",
                "logistics.machines.crucible",
                "logistics.machines.alloy_smelter",
                "logistics.machines.quarry",
                "logistics.machines.refinery",
                "logistics.machines.fabricator",
                "logistics.machines.transposer",
                "logistics.pipes",
                "logistics.fluids",
                "logistics.fluid_logistics",
                "logistics.reporting");
    }

    /**
     * A config standing in for one of the real per-domain files. Deliberately created off-registry with an
     * id that is <em>not</em> under {@code logistics.} — {@link ConfigRegistry} has no removal API, so a
     * registered test config would leak into {@link LogisticsConfigHost#domainConfigs()} forever.
     */
    private static Config strayConfig(Path dir, String id) {
        return Config.create(id, new JsonFileConfigStorage(dir));
    }

    @Test
    @DisplayName("a config file with a syntax error is set aside and replaced with defaults")
    void malformedConfigIsQuarantinedAndDefaulted(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("configtest/machines/kiln.json");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "{ \"speed\": 10"); // unbalanced brace
        Config config = strayConfig(dir, "configtest.machines.kiln");
        ConfigKey<Long> speed = config.defineLong("speed", 7L).min(0L).register();

        LogisticsConfigHost.loadOrRecover(config, dir);

        // Defaults in memory and on disk, with the player's broken file preserved beside it.
        assertThat(config.get(speed)).isEqualTo(7L);
        assertThat(Files.readString(file)).contains("speed").contains("7");
        assertThat(Files.readString(dir.resolve("configtest/machines/kiln.json.invalid")))
                .isEqualTo("{ \"speed\": 10");
    }

    @Test
    @DisplayName("a config file whose root is not a JSON object is recovered too")
    void nonObjectRootIsQuarantinedAndDefaulted(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("configtest2.json");
        Files.writeString(file, "[1, 2, 3]"); // parses, but casting the root to JsonObject throws
        Config config = strayConfig(dir, "configtest2");
        ConfigKey<Long> speed = config.defineLong("speed", 7L).min(0L).register();

        LogisticsConfigHost.loadOrRecover(config, dir);

        assertThat(config.get(speed)).isEqualTo(7L);
        assertThat(Files.readString(dir.resolve("configtest2.json.invalid"))).isEqualTo("[1, 2, 3]");
    }

    @Test
    @DisplayName("a readable config file is loaded normally and never set aside")
    void validConfigIsLeftAlone(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("configtest3.json");
        Files.writeString(file, "{\n  \"speed\": 42\n}");
        Config config = strayConfig(dir, "configtest3");
        ConfigKey<Long> speed = config.defineLong("speed", 7L).min(0L).register();

        LogisticsConfigHost.loadOrRecover(config, dir);

        assertThat(config.get(speed)).isEqualTo(42L);
        assertThat(Files.exists(dir.resolve("configtest3.json.invalid"))).isFalse();
    }

    @Test
    @DisplayName("save writes a JSON file with the defined values")
    void savesFileAsJson(@TempDir Path dir) throws IOException {
        // Inject temp storage so the test stays isolated on a temp dir instead of the CWD-relative default.
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
