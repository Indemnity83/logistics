package com.logistics.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.logistics.core.lib.platform.PlatformService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Central config for Logistics. Backed by config/logistics.json (Gson).
 *
 * <p>Lifecycle (driven by {@code LogisticsCommonBootstrap}): every domain's {@code registerConfig()}
 * declares its entries, {@link #freeze()} closes registration, then {@link #load()} reads
 * {@code config/logistics.json} (writing defaults if the file is missing) — all before any
 * {@code initCommon()} body reads config.
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

    private static final Map<String, ConfigEntry<?>> ENTRIES_MAP = new LinkedHashMap<>();
    private static boolean frozen = false;

    /** Read-only view of the registered entries, in registration order. */
    public static final Map<String, ConfigEntry<?>> ENTRIES = Collections.unmodifiableMap(ENTRIES_MAP);

    /** Register a config entry. Domains call this during bootstrap; core registers its own below. */
    public static void register(ConfigEntry<?> entry) {
        if (frozen) {
            throw new IllegalStateException("Config registration is frozen; cannot register " + entry.key());
        }
        ENTRIES_MAP.put(entry.key(), entry);
    }

    /** Close registration. Called once after every domain has registered, before {@link #load()}. */
    public static void freeze() {
        frozen = true;
    }

    /** Strict boolean parse — rejects anything but {@code true}/{@code false} (unlike {@link Boolean#parseBoolean}). */
    public static Boolean parseBooleanStrict(String value) {
        if ("true".equalsIgnoreCase(value)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(value)) {
            return Boolean.FALSE;
        }
        throw new IllegalArgumentException("must be 'true' or 'false'");
    }

    public static <T> void reg(
            String key,
            String description,
            Supplier<T> getter,
            Consumer<T> setter,
            Function<String, T> parser,
            Consumer<T> validator,
            Supplier<T> defaultValue) {
        register(new ConfigEntry<>(key, description, getter, setter, parser, validator, defaultValue));
    }

    public static <T> void regCrossField(
            String key,
            String description,
            Supplier<T> getter,
            Consumer<T> setter,
            Function<String, T> parser,
            Consumer<T> validator,
            Supplier<T> defaultValue,
            Consumer<T> crossFieldValidator) {
        register(new ConfigEntry<>(
                key, description, getter, setter, parser, validator, defaultValue, crossFieldValidator));
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
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                INSTANCE = sanitize(deserialize(json));
                loaded = true;
            } catch (Exception e) {
                LOGGER.error("Failed to load logistics.json, using defaults: {}", e.getMessage());
            }
        }
        // Always write back so new keys are persisted (and any legacy nested file is migrated to flat).
        save();
        if (loaded) {
            LOGGER.info("Loaded logistics config from {}", configPath);
        } else {
            LOGGER.info("Created default logistics config at {}", configPath);
        }
    }

    /** Persist the current config to disk in the flat-key format. */
    public static void save() {
        if (configPath == null) return;
        try (Writer writer = Files.newBufferedWriter(configPath)) {
            GSON.toJson(serialize(), writer);
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
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            INSTANCE = sanitize(deserialize(json));
        } catch (Exception e) {
            LOGGER.error("Failed to reload logistics.json: {}", e.getMessage());
            return;
        }
        LOGGER.info("Reloaded logistics config from {}", configPath);
    }

    /** Serialize the live config to the flat-key format: one key per registry entry, plus crash reporting. */
    static JsonObject serialize() {
        JsonObject root = new JsonObject();
        for (ConfigEntry<?> entry : ENTRIES.values()) {
            root.add(entry.key(), toJson(entry.get()));
        }
        root.add("crash_reporting", GSON.toJsonTree(INSTANCE.crashReporting));
        return root;
    }

    /**
     * Read a config object into a {@link LogisticsConfig} (unsanitized). Accepts both the flat-key format
     * and the legacy nested-group layout (read for one major per the backward-compat policy, then rewritten
     * flat on save).
     */
    static LogisticsConfig deserialize(JsonObject json) {
        if (isLegacyNested(json)) {
            return GSON.fromJson(json, LogisticsConfig.class);
        }
        LogisticsConfig config = new LogisticsConfig();
        LogisticsConfig previous = INSTANCE;
        INSTANCE = config;
        try {
            for (ConfigEntry<?> entry : ENTRIES.values()) {
                JsonElement value = json.get(entry.key());
                if (value == null || !value.isJsonPrimitive()) {
                    continue;
                }
                try {
                    entry.loadFromString(value.getAsString());
                } catch (RuntimeException e) {
                    LOGGER.warn("Ignoring malformed config value {}={}: {}", entry.key(), value, e.getMessage());
                }
            }
        } finally {
            INSTANCE = previous;
        }
        if (json.has("crash_reporting") && json.get("crash_reporting").isJsonObject()) {
            config.crashReporting = GSON.fromJson(json.get("crash_reporting"), CrashReportingConfig.class);
        }
        return config;
    }

    /** The pre-flat format stored each domain as a nested object; the flat format uses flat keys. */
    private static boolean isLegacyNested(JsonObject json) {
        return json.has("quarry") && json.get("quarry").isJsonObject();
    }

    /** Write float-backed values at float precision so they read as {@code 0.16}, not {@code 0.16000000238…}. */
    private static JsonElement toJson(Object value) {
        if (value instanceof Double d) {
            float asFloat = d.floatValue();
            return (double) asFloat == d ? new JsonPrimitive(asFloat) : new JsonPrimitive(d);
        }
        if (value instanceof Number number) {
            return new JsonPrimitive(number);
        }
        if (value instanceof Boolean bool) {
            return new JsonPrimitive(bool);
        }
        return new JsonPrimitive(String.valueOf(value));
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

        // Repair each value against its registry entry (single-field validation only). The registry
        // getters/setters read and write INSTANCE, so point it at the config under repair for the
        // duration (load and reload both run single-threaded, and callers reassign INSTANCE anyway).
        LogisticsConfig previous = INSTANCE;
        INSTANCE = config;
        try {
            for (ConfigEntry<?> entry : ENTRIES.values()) {
                entry.sanitize();
            }
        } finally {
            INSTANCE = previous;
        }

        // Cross-field constraints can't be repaired per-entry; reconcile the min/max ordering here.
        sanitizePipeSpeedRange(config, defaults);
        sanitizeStirlingOutputRange(config, defaults);

        return config;
    }

    static void useForTests(LogisticsConfig config) {
        INSTANCE = sanitize(config);
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

    public static void requireMin(long value, long min, String message) {
        requireCondition(value >= min, message);
    }

    public static void requireRange(long value, long min, long max, String message) {
        requireCondition(value >= min && value <= max, message);
    }

    public static void requireFiniteMin(double value, double min, String message) {
        requireCondition(Double.isFinite(value) && value >= min, message);
    }

    public static void requireFiniteRange(double value, double min, double max, String message) {
        requireCondition(Double.isFinite(value) && value >= min && value <= max, message);
    }

    public static void requireFiniteFloatMin(double value, double min, String message) {
        requireFiniteFloat(value, message);
        requireCondition(value >= min, message);
    }

    public static void requireFiniteFloatGreaterThan(double value, double minExclusive, String message) {
        requireFiniteFloat(value, message);
        requireCondition(value > minExclusive, message);
    }

    private static void requireFiniteFloat(double value, String message) {
        requireCondition(Double.isFinite(value) && Math.abs(value) <= Float.MAX_VALUE, message);
    }

    public static void requireCondition(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    // ==================== ConfigEntry ====================

    /**
     * A single config value. Two flavors: a <em>bridge</em> entry reads/writes an external field via
     * getter/setter (legacy path); a <em>self-storing</em> entry (built via {@link #regInt} etc.) holds
     * its own value. Both expose the same {@link #get()}/{@link #setFromString}/{@link #sanitize} surface.
     */
    public static final class ConfigEntry<T> {
        private final String key;
        private final String description;
        private final Function<String, T> parser;
        private final Consumer<T> validator;
        private final Consumer<T> crossFieldValidator;

        private final boolean selfStoring;
        private final Supplier<T> getter;
        private final Consumer<T> setter;
        private final Supplier<T> defaultSupplier;
        private final T defaultConstant;
        private volatile T value;

        /** Bridge entry backed by an external field. */
        public ConfigEntry(
                String key,
                String description,
                Supplier<T> getter,
                Consumer<T> setter,
                Function<String, T> parser,
                Consumer<T> validator,
                Supplier<T> defaultValue,
                Consumer<T> crossFieldValidator) {
            this.key = key;
            this.description = description;
            this.parser = parser;
            this.validator = validator;
            this.crossFieldValidator = crossFieldValidator;
            this.selfStoring = false;
            this.getter = getter;
            this.setter = setter;
            this.defaultSupplier = defaultValue;
            this.defaultConstant = null;
        }

        /** Bridge entry with no cross-field constraint. */
        public ConfigEntry(
                String key,
                String description,
                Supplier<T> getter,
                Consumer<T> setter,
                Function<String, T> parser,
                Consumer<T> validator,
                Supplier<T> defaultValue) {
            this(key, description, getter, setter, parser, validator, defaultValue, v -> {});
        }

        /** Self-storing entry that holds its own value. */
        ConfigEntry(
                String key,
                String description,
                Function<String, T> parser,
                Consumer<T> validator,
                Consumer<T> crossFieldValidator,
                T defaultValue) {
            this.key = key;
            this.description = description;
            this.parser = parser;
            this.validator = validator;
            this.crossFieldValidator = crossFieldValidator;
            this.selfStoring = true;
            this.getter = null;
            this.setter = null;
            this.defaultSupplier = null;
            this.defaultConstant = defaultValue;
            this.value = defaultValue;
        }

        public String key() {
            return key;
        }

        public String description() {
            return description;
        }

        /** The current value. */
        public T get() {
            return selfStoring ? value : getter.get();
        }

        private void set(T v) {
            if (selfStoring) {
                value = v;
            } else {
                setter.accept(v);
            }
        }

        private T defaultValue() {
            return selfStoring ? defaultConstant : defaultSupplier.get();
        }

        /** Returns the current value as a string. */
        public String getAsString() {
            return String.valueOf(get());
        }

        /** Parse {@code raw} and set it WITHOUT validation. Load path — {@link LogisticsConfig#sanitize} repairs after. */
        public void loadFromString(String raw) {
            set(parser.apply(raw));
        }

        /** Validate the current value; on failure reset it to the default and log a warning. */
        public void sanitize() {
            T current = get();
            try {
                validator.accept(current);
            } catch (IllegalArgumentException e) {
                T replacement = defaultValue();
                set(replacement);
                LOGGER.warn(
                        "Invalid logistics config value {}={}: {}; using default {}",
                        key, current, e.getMessage(), replacement);
            }
        }

        /**
         * Parse {@code value} and apply it. Throws if parsing or validation fails.
         * Callers should call {@link LogisticsConfig#save()} afterward.
         */
        public void setFromString(String value) {
            T parsed = parser.apply(value);
            validator.accept(parsed);
            crossFieldValidator.accept(parsed);
            set(parsed);
        }
    }

    // ==================== Fluent entry builders (self-storing) ====================

    public static IntEntryBuilder regInt(String key, String description) {
        return new IntEntryBuilder(key, description);
    }

    public static LongEntryBuilder regLong(String key, String description) {
        return new LongEntryBuilder(key, description);
    }

    public static FloatEntryBuilder regFloat(String key, String description) {
        return new FloatEntryBuilder(key, description);
    }

    public static DoubleEntryBuilder regDouble(String key, String description) {
        return new DoubleEntryBuilder(key, description);
    }

    public static BoolEntryBuilder regBool(String key, String description) {
        return new BoolEntryBuilder(key, description);
    }

    private static <T> Consumer<T> composeRules(List<Consumer<T>> rules) {
        return value -> rules.forEach(rule -> rule.accept(value));
    }

    public static final class IntEntryBuilder {
        private final String key;
        private final String description;
        private final List<Consumer<Integer>> rules = new ArrayList<>();
        private Consumer<Integer> crossField = v -> {};
        private Integer defaultValue;

        IntEntryBuilder(String key, String description) {
            this.key = key;
            this.description = description;
        }

        public IntEntryBuilder defaultsTo(int value) {
            this.defaultValue = value;
            return this;
        }

        public IntEntryBuilder min(int min) {
            rules.add(v -> requireCondition(v >= min, "must be greater than or equal to " + min));
            return this;
        }

        public IntEntryBuilder max(int max) {
            rules.add(v -> requireCondition(v <= max, "must be less than or equal to " + max));
            return this;
        }

        public ConfigEntry<Integer> register() {
            ConfigEntry<Integer> entry = new ConfigEntry<>(
                    key, description, Integer::parseInt, composeRules(rules), crossField, defaultValue);
            LogisticsConfig.register(entry);
            return entry;
        }
    }

    public static final class LongEntryBuilder {
        private final String key;
        private final String description;
        private final List<Consumer<Long>> rules = new ArrayList<>();
        private Consumer<Long> crossField = v -> {};
        private Long defaultValue;

        LongEntryBuilder(String key, String description) {
            this.key = key;
            this.description = description;
        }

        public LongEntryBuilder defaultsTo(long value) {
            this.defaultValue = value;
            return this;
        }

        public LongEntryBuilder min(long min) {
            rules.add(v -> requireCondition(v >= min, "must be greater than or equal to " + min));
            return this;
        }

        public LongEntryBuilder max(long max) {
            rules.add(v -> requireCondition(v <= max, "must be less than or equal to " + max));
            return this;
        }

        public ConfigEntry<Long> register() {
            ConfigEntry<Long> entry = new ConfigEntry<>(
                    key, description, Long::parseLong, composeRules(rules), crossField, defaultValue);
            LogisticsConfig.register(entry);
            return entry;
        }
    }

    public static final class FloatEntryBuilder {
        private final String key;
        private final String description;
        private final List<Consumer<Float>> rules = new ArrayList<>();
        private Consumer<Float> crossField = v -> {};
        private Float defaultValue;

        FloatEntryBuilder(String key, String description) {
            this.key = key;
            this.description = description;
            rules.add(v -> requireCondition(Float.isFinite(v), "must be a finite number"));
        }

        public FloatEntryBuilder defaultsTo(float value) {
            this.defaultValue = value;
            return this;
        }

        public FloatEntryBuilder min(float min) {
            rules.add(v -> requireCondition(v >= min, "must be greater than or equal to " + min));
            return this;
        }

        public FloatEntryBuilder max(float max) {
            rules.add(v -> requireCondition(v <= max, "must be less than or equal to " + max));
            return this;
        }

        public FloatEntryBuilder greaterThan(float minExclusive) {
            rules.add(v -> requireCondition(v > minExclusive, "must be greater than " + minExclusive));
            return this;
        }

        public FloatEntryBuilder min(Supplier<ConfigEntry<Float>> other) {
            crossField = v -> requireCondition(
                    v >= other.get().get(), "must be greater than or equal to " + other.get().key());
            return this;
        }

        public FloatEntryBuilder max(Supplier<ConfigEntry<Float>> other) {
            crossField = v -> requireCondition(
                    v <= other.get().get(), "must be less than or equal to " + other.get().key());
            return this;
        }

        public ConfigEntry<Float> register() {
            ConfigEntry<Float> entry = new ConfigEntry<>(
                    key, description, Float::parseFloat, composeRules(rules), crossField, defaultValue);
            LogisticsConfig.register(entry);
            return entry;
        }
    }

    public static final class DoubleEntryBuilder {
        private final String key;
        private final String description;
        private final List<Consumer<Double>> rules = new ArrayList<>();
        private Consumer<Double> crossField = v -> {};
        private Double defaultValue;

        DoubleEntryBuilder(String key, String description) {
            this.key = key;
            this.description = description;
            rules.add(v -> requireCondition(Double.isFinite(v), "must be a finite number"));
        }

        public DoubleEntryBuilder defaultsTo(double value) {
            this.defaultValue = value;
            return this;
        }

        public DoubleEntryBuilder min(double min) {
            rules.add(v -> requireCondition(v >= min, "must be greater than or equal to " + min));
            return this;
        }

        public DoubleEntryBuilder max(double max) {
            rules.add(v -> requireCondition(v <= max, "must be less than or equal to " + max));
            return this;
        }

        public DoubleEntryBuilder greaterThan(double minExclusive) {
            rules.add(v -> requireCondition(v > minExclusive, "must be greater than " + minExclusive));
            return this;
        }

        public DoubleEntryBuilder min(Supplier<ConfigEntry<Double>> other) {
            crossField = v -> requireCondition(
                    v >= other.get().get(), "must be greater than or equal to " + other.get().key());
            return this;
        }

        public DoubleEntryBuilder max(Supplier<ConfigEntry<Double>> other) {
            crossField = v -> requireCondition(
                    v <= other.get().get(), "must be less than or equal to " + other.get().key());
            return this;
        }

        public ConfigEntry<Double> register() {
            ConfigEntry<Double> entry = new ConfigEntry<>(
                    key, description, Double::parseDouble, composeRules(rules), crossField, defaultValue);
            LogisticsConfig.register(entry);
            return entry;
        }
    }

    public static final class BoolEntryBuilder {
        private final String key;
        private final String description;
        private Boolean defaultValue;

        BoolEntryBuilder(String key, String description) {
            this.key = key;
            this.description = description;
        }

        public BoolEntryBuilder defaultsTo(boolean value) {
            this.defaultValue = value;
            return this;
        }

        public ConfigEntry<Boolean> register() {
            ConfigEntry<Boolean> entry = new ConfigEntry<>(
                    key, description, LogisticsConfig::parseBooleanStrict, v -> {}, v -> {}, defaultValue);
            LogisticsConfig.register(entry);
            return entry;
        }
    }
}
