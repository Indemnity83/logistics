package com.logistics.core;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsPipe;
import com.logistics.LogisticsPower;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

@DisplayName("LogisticsConfig")
class LogisticsConfigTest {
    private static final float TOLERANCE = 0.0001f;

    /** Entries now live in their owning domains; register them so {@code ENTRIES} is populated. */
    @BeforeAll
    static void registerDomainConfig() {
        new LogisticsAutomation().registerConfig();
        new LogisticsPipe().registerConfig();
        new LogisticsPower().registerConfig();
    }

    @AfterEach
    void resetConfig() {
        LogisticsConfig.useForTests(new LogisticsConfig());
    }

    @Test
    @DisplayName("should reject invalid command values without changing current config")
    void rejectsInvalidCommandValuesWithoutMutation() {
        LogisticsConfig.useForTests(new LogisticsConfig());
        LogisticsConfig.ConfigEntry<?> entry = LogisticsConfig.ENTRIES.get("pipe_min_speed");
        float original = LogisticsConfig.get().pipe.minSpeed;

        assertThatThrownBy(() -> entry.setFromString("-0.1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than 0");

        assertThat(LogisticsConfig.get().pipe.minSpeed).isCloseTo(original, within(TOLERANCE));
    }

    @Test
    @DisplayName("should reject non-finite floating point command values")
    void rejectsNonFiniteCommandValues() {
        LogisticsConfig.useForTests(new LogisticsConfig());

        assertThatThrownBy(() -> LogisticsConfig.ENTRIES.get("quarry_energy_multiplier").setFromString("NaN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("finite");
        assertThatThrownBy(() -> LogisticsConfig.ENTRIES.get("quarry_arm_speed").setFromString("Infinity"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("finite");
        assertThatThrownBy(() -> LogisticsConfig.ENTRIES.get("pipe_inject_speed").setFromString("1.0E40"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("finite");
    }

    @Test
    @DisplayName("should accept valid boundary command values")
    void acceptsValidBoundaryValues() {
        LogisticsConfig.useForTests(new LogisticsConfig());

        LogisticsConfig.ENTRIES.get("quarry_area").setFromString("3");
        LogisticsConfig.ENTRIES.get("quarry_scan_rate").setFromString("1");
        LogisticsConfig.ENTRIES.get("quarry_rain_penalty").setFromString("0.0");
        LogisticsConfig.ENTRIES.get("pipe_drag").setFromString("1.0");
        LogisticsConfig.ENTRIES.get("redstone_engine_output").setFromString("0");

        assertThat(LogisticsConfig.get().quarry.area).isEqualTo(3);
        assertThat(LogisticsConfig.get().quarry.scanRate).isEqualTo(1);
        assertThat(LogisticsConfig.get().quarry.rainPenalty).isCloseTo(0.0f, within(TOLERANCE));
        assertThat(LogisticsConfig.get().pipe.drag).isCloseTo(1.0f, within(TOLERANCE));
        assertThat(LogisticsPower.REDSTONE_OUTPUT.get()).isEqualTo(0L);
    }

    @Test
    @DisplayName("should clamp the fluid pump search radius so its square fits in an int")
    void clampsPumpSearchRadius() {
        LogisticsConfig.useForTests(new LogisticsConfig());

        // 46340^2 fits in an int; one more overflows it once the pump squares the radius.
        LogisticsConfig.ENTRIES.get("fluid_pump_search_radius").setFromString("46340");
        assertThat(LogisticsConfig.get().fluidPump.searchRadius).isEqualTo(46340);

        assertThatThrownBy(() -> LogisticsConfig.ENTRIES.get("fluid_pump_search_radius").setFromString("46341"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(LogisticsConfig.get().fluidPump.searchRadius).isEqualTo(46340); // rejected, unchanged

        // A hand-edited config that exceeds the cap is reset to the default on load.
        LogisticsConfig parsed = new LogisticsConfig();
        parsed.fluidPump.searchRadius = 46341;
        assertThat(LogisticsConfig.sanitize(parsed).fluidPump.searchRadius)
                .isEqualTo(new LogisticsConfig().fluidPump.searchRadius);
    }

    @Test
    @DisplayName("should reject pipe speed ranges that invert min and max")
    void rejectsInvertedPipeSpeeds() {
        LogisticsConfig.useForTests(new LogisticsConfig());

        assertThatThrownBy(() -> LogisticsConfig.ENTRIES.get("pipe_max_speed").setFromString("0.01"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pipe_min_speed");
        assertThatThrownBy(() -> LogisticsConfig.ENTRIES.get("pipe_min_speed").setFromString("0.2"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pipe_max_speed");

        assertThat(LogisticsConfig.get().pipe.minSpeed).isCloseTo(0.02f, within(TOLERANCE));
        assertThat(LogisticsConfig.get().pipe.maxSpeed).isCloseTo(0.16f, within(TOLERANCE));
    }

    @Test
    @DisplayName("should reject stirling output ranges that invert min and max")
    void rejectsInvertedStirlingOutputs() {
        LogisticsConfig.useForTests(new LogisticsConfig());

        assertThatThrownBy(() -> LogisticsConfig.ENTRIES.get("stirling_engine_max_output").setFromString("2.0"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("stirling_engine_min_output");
        assertThatThrownBy(() -> LogisticsConfig.ENTRIES.get("stirling_engine_min_output").setFromString("12.0"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("stirling_engine_max_output");

        assertThat(LogisticsPower.STIRLING_MIN_OUTPUT.get()).isEqualTo(3.0);
        assertThat(LogisticsPower.STIRLING_MAX_OUTPUT.get()).isEqualTo(10.0);
    }

    @Test
    @DisplayName("should sanitize invalid loaded fields to defaults while preserving valid fields")
    void sanitizesInvalidLoadedFields() {
        LogisticsConfig parsed = new LogisticsConfig();
        parsed.quarry.area = 2;
        parsed.quarry.energyPerBlock = 120L;
        parsed.quarry.energyMultiplier = Double.NaN;
        parsed.quarry.scanRate = 0;
        parsed.pipe.minSpeed = 0.04f;
        parsed.pipe.maxSpeed = 0.03f;
        parsed.pipe.injectSpeed = Float.POSITIVE_INFINITY;

        LogisticsConfig sanitized = LogisticsConfig.sanitize(parsed);

        assertThat(sanitized.quarry.area).isEqualTo(new LogisticsConfig().quarry.area);
        assertThat(sanitized.quarry.energyPerBlock).isEqualTo(120L);
        assertThat(sanitized.quarry.energyMultiplier).isEqualTo(new LogisticsConfig().quarry.energyMultiplier);
        assertThat(sanitized.quarry.scanRate).isEqualTo(new LogisticsConfig().quarry.scanRate);
        assertThat(sanitized.pipe.minSpeed).isCloseTo(0.04f, within(TOLERANCE));
        assertThat(sanitized.pipe.maxSpeed).isCloseTo(new LogisticsConfig().pipe.maxSpeed, within(TOLERANCE));
        assertThat(sanitized.pipe.injectSpeed).isCloseTo(new LogisticsConfig().pipe.injectSpeed, within(TOLERANCE));
    }

    @Test
    @DisplayName("should sanitize invalid engine entries and repair an inverted stirling range")
    void sanitizesInvalidEngineEntries() {
        LogisticsPower.REDSTONE_OUTPUT.loadFromString("-1");
        LogisticsPower.STIRLING_MIN_OUTPUT.loadFromString("12");
        LogisticsPower.STIRLING_MAX_OUTPUT.loadFromString("10");

        LogisticsConfig.sanitize(new LogisticsConfig());

        assertThat(LogisticsPower.REDSTONE_OUTPUT.get()).isEqualTo(10L);
        assertThat(LogisticsPower.STIRLING_MIN_OUTPUT.get()).isEqualTo(3.0);
        assertThat(LogisticsPower.STIRLING_MAX_OUTPUT.get()).isEqualTo(10.0);
    }

    @Test
    @DisplayName("should replace null loaded config groups with defaults")
    void sanitizesNullLoadedGroups() {
        LogisticsConfig parsed = new LogisticsConfig();
        parsed.quarry = null;
        parsed.pipe = null;

        LogisticsConfig sanitized = LogisticsConfig.sanitize(parsed);

        assertThat(sanitized.quarry).isNotNull();
        assertThat(sanitized.pipe).isNotNull();
        assertThat(sanitized.quarry.area).isEqualTo(new LogisticsConfig().quarry.area);
        assertThat(sanitized.pipe.minSpeed).isCloseTo(new LogisticsConfig().pipe.minSpeed, within(TOLERANCE));
    }

    @Test
    @DisplayName("should clamp quarry derived energy values to non-negative")
    void quarryDerivedEnergyValuesAreNonNegative() {
        LogisticsConfig.QuarryConfig quarry = new LogisticsConfig.QuarryConfig();
        quarry.energyPerBlock = 60L;
        quarry.energyMultiplier = -1.0;

        assertThat(quarry.energyPerBlockMultiplier()).isZero();
        assertThat(quarry.energyCapacity()).isZero();
        assertThat(quarry.maxEnergyInput()).isZero();

        quarry.energyMultiplier = Double.NaN;

        assertThat(quarry.energyPerBlockMultiplier()).isZero();
        assertThat(quarry.energyCapacity()).isZero();
        assertThat(quarry.maxEnergyInput()).isZero();

        quarry.energyMultiplier = Double.POSITIVE_INFINITY;

        assertThat(quarry.energyPerBlockMultiplier()).isZero();
        assertThat(quarry.energyCapacity()).isZero();
        assertThat(quarry.maxEnergyInput()).isZero();

        quarry.energyMultiplier = Double.NEGATIVE_INFINITY;

        assertThat(quarry.energyPerBlockMultiplier()).isZero();
        assertThat(quarry.energyCapacity()).isZero();
        assertThat(quarry.maxEnergyInput()).isZero();
    }

    @Test
    @DisplayName("should default crash reporting to opt-in with the join notice on")
    void crashReportingDefaultsToOptIn() {
        LogisticsConfig.CrashReportingConfig cfg = new LogisticsConfig().crashReporting;

        assertThat(cfg.enabled).isFalse();
        assertThat(cfg.notifyOperators).isTrue();
        assertThat(cfg.dsnOverride).isEmpty();
    }

    @Test
    @DisplayName("should recover a null crash reporting group and null dsn override")
    void sanitizesNullCrashReportingGroup() {
        LogisticsConfig parsed = new LogisticsConfig();
        parsed.crashReporting = null;

        LogisticsConfig sanitized = LogisticsConfig.sanitize(parsed);

        assertThat(sanitized.crashReporting).isNotNull();
        assertThat(sanitized.crashReporting.enabled).isFalse();
        assertThat(sanitized.crashReporting.notifyOperators).isTrue();
        assertThat(sanitized.crashReporting.dsnOverride).isEmpty();

        LogisticsConfig withNullDsn = new LogisticsConfig();
        withNullDsn.crashReporting.dsnOverride = null;
        assertThat(LogisticsConfig.sanitize(withNullDsn).crashReporting.dsnOverride).isEmpty();
    }

    @Test
    @DisplayName("should preserve valid crash reporting values through sanitize")
    void preservesValidCrashReportingValues() {
        LogisticsConfig parsed = new LogisticsConfig();
        parsed.crashReporting.enabled = true;
        parsed.crashReporting.notifyOperators = false;
        parsed.crashReporting.dsnOverride = "https://key@example.invalid/1";

        LogisticsConfig sanitized = LogisticsConfig.sanitize(parsed);

        assertThat(sanitized.crashReporting.enabled).isTrue();
        assertThat(sanitized.crashReporting.notifyOperators).isFalse();
        assertThat(sanitized.crashReporting.dsnOverride).isEqualTo("https://key@example.invalid/1");
    }

    @Test
    @DisplayName("should round-trip through the flat-key format")
    void roundTripsFlatFormat() {
        LogisticsConfig.ENTRIES.get("quarry_energy_per_block").setFromString("123");
        LogisticsConfig.ENTRIES.get("pipe_max_speed").setFromString("0.16");
        LogisticsConfig.ENTRIES.get("fluid_pipe_active_extraction").setFromString("false");
        LogisticsConfig.get().crashReporting.enabled = true;

        JsonObject json = LogisticsConfig.serialize();
        assertThat(json.get("quarry_energy_per_block").getAsLong()).isEqualTo(123L);
        // Float-backed values serialize short, not widened (0.16, not 0.16000000238…).
        assertThat(json.get("pipe_max_speed").getAsString()).isEqualTo("0.16");
        assertThat(json.get("fluid_pipe_active_extraction").getAsBoolean()).isFalse();

        LogisticsConfig restored = LogisticsConfig.deserialize(json);
        assertThat(restored.quarry.energyPerBlock).isEqualTo(123L);
        assertThat(restored.pipe.maxSpeed).isCloseTo(0.16f, within(TOLERANCE));
        assertThat(restored.fluidPipe.activeExtraction).isFalse();
        assertThat(restored.crashReporting.enabled).isTrue();
    }

    @Test
    @DisplayName("should read the legacy nested layout and preserve its values")
    void readsLegacyNestedLayout() {
        JsonObject legacy = JsonParser.parseString(
                        "{\"quarry\":{\"energyPerBlock\":77,\"area\":24},"
                                + "\"engine\":{\"redstoneOutput\":42},"
                                + "\"crashReporting\":{\"enabled\":true}}")
                .getAsJsonObject();

        LogisticsConfig loaded = LogisticsConfig.deserialize(legacy);

        assertThat(loaded.quarry.energyPerBlock).isEqualTo(77L);
        assertThat(loaded.quarry.area).isEqualTo(24);
        // engine moved to a self-storing entry; the legacy group is applied there.
        assertThat(LogisticsPower.REDSTONE_OUTPUT.get()).isEqualTo(42L);
        assertThat(loaded.crashReporting.enabled).isTrue();
    }

    @Test
    @DisplayName("self-storing builder entries store, validate, reset, and enforce cross-field rules")
    void selfStoringBuilderEntries() {
        LogisticsConfig.ConfigEntry<Double> max =
                LogisticsConfig.regDouble("test_max", "test ceiling").defaultsTo(10.0).min(0.0).register();
        LogisticsConfig.ConfigEntry<Double> min =
                LogisticsConfig.regDouble("test_min", "test floor").defaultsTo(2.0).min(0.0).max(() -> max).register();

        // Holds its own value.
        min.setFromString("3.0");
        assertThat(min.get()).isEqualTo(3.0);

        // Single-field rule.
        assertThatThrownBy(() -> max.setFromString("-1")).isInstanceOf(IllegalArgumentException.class);

        // Cross-field rule: min must stay <= max.
        assertThatThrownBy(() -> min.setFromString("11"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("test_max");

        // Sanitize resets an out-of-range value to the default.
        min.loadFromString("-5");
        min.sanitize();
        assertThat(min.get()).isEqualTo(2.0);
    }
}
