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
 * <p>Runtime access: via typed entry constants, e.g. {@code LogisticsAutomation.QUARRY_ENERGY_PER_BLOCK.get()}
 * <p>Command access: {@code /logistics config list|get|set|reload}
 */
public final class LogisticsConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("logistics/config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static volatile LogisticsConfig INSTANCE = new LogisticsConfig();
    private static Path configPath;

    // ==================== Config Groups ====================

    public CrashReportingConfig crashReporting = new CrashReportingConfig();

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

    private static final List<Runnable> SANITIZE_HOOKS = new ArrayList<>();

    /** Register a cross-field repair to run after per-entry sanitize (e.g. {@link #repairMinMax}). */
    public static void registerSanitizeHook(Runnable hook) {
        SANITIZE_HOOKS.add(hook);
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
            return deserializeLegacy(json);
        }
        LogisticsConfig config = new LogisticsConfig();
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
        if (json.has("crash_reporting") && json.get("crash_reporting").isJsonObject()) {
            config.crashReporting = GSON.fromJson(json.get("crash_reporting"), CrashReportingConfig.class);
        }
        return config;
    }

    /**
     * Read the legacy nested layout. Domains still backed by structs deserialize straight into the POJO;
     * domains that have moved to self-storing entries pull their values from their old nested group.
     */
    private static LogisticsConfig deserializeLegacy(JsonObject json) {
        LogisticsConfig config = GSON.fromJson(json, LogisticsConfig.class);
        applyLegacyGroup(json, "engine",
                "redstoneOutput", "redstone_engine_output",
                "stirlingMinOutput", "stirling_engine_min_output",
                "stirlingMaxOutput", "stirling_engine_max_output");
        applyLegacyGroup(json, "quarry",
                "area", "quarry_area",
                "energyPerBlock", "quarry_energy_per_block",
                "energyMultiplier", "quarry_energy_multiplier",
                "armSpeed", "quarry_arm_speed",
                "armSpeedScaling", "quarry_arm_speed_scaling",
                "armEnergy", "quarry_arm_energy",
                "rainPenalty", "quarry_rain_penalty",
                "scanRate", "quarry_scan_rate",
                "loadChunks", "quarry_load_chunks");
        applyLegacyGroup(json, "pipe",
                "maxSpeed", "pipe_max_speed",
                "minSpeed", "pipe_min_speed",
                "acceleration", "pipe_acceleration",
                "drag", "pipe_drag",
                "injectSpeed", "pipe_inject_speed");
        applyLegacyGroup(json, "fluidPipe",
                "baseTransferRate", "fluid_pipe_base_transfer_rate",
                "baseCapacity", "fluid_pipe_base_capacity",
                "woodenRequiresEngine", "fluid_pipe_wooden_requires_engine",
                "activeExtraction", "fluid_pipe_active_extraction");
        applyLegacyGroup(json, "fluidPump",
                "tankCapacityMb", "fluid_pump_tank_capacity_mb",
                "energyCapacity", "fluid_pump_energy_capacity",
                "maxEnergyInput", "fluid_pump_max_energy_input",
                "energyPerSource", "fluid_pump_energy_per_source",
                "pushRateMb", "fluid_pump_push_rate_mb",
                "pumpIntervalTicks", "fluid_pump_interval_ticks",
                "searchRadius", "fluid_pump_search_radius",
                "armSpeed", "fluid_pump_arm_speed",
                "infiniteSourceThreshold", "fluid_pump_infinite_source_threshold");
        return config;
    }

    /** Map a legacy nested group's fields ({@code field, flatKey} pairs) onto their self-storing entries. */
    private static void applyLegacyGroup(JsonObject json, String group, String... fieldKeyPairs) {
        if (!json.has(group) || !json.get(group).isJsonObject()) {
            return;
        }
        JsonObject obj = json.getAsJsonObject(group);
        for (int i = 0; i < fieldKeyPairs.length; i += 2) {
            ConfigEntry<?> entry = ENTRIES.get(fieldKeyPairs[i + 1]);
            JsonElement value = obj.get(fieldKeyPairs[i]);
            if (entry == null || value == null || !value.isJsonPrimitive()) {
                continue;
            }
            try {
                entry.loadFromString(value.getAsString());
            } catch (RuntimeException e) {
                LOGGER.warn("Ignoring malformed legacy config {}.{}: {}", group, fieldKeyPairs[i], e.getMessage());
            }
        }
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
        if (config.crashReporting == null) {
            LOGGER.warn("Invalid logistics config group crashReporting: missing; using defaults");
            config.crashReporting = defaults.crashReporting;
        }
        if (config.crashReporting.dsnOverride == null) {
            config.crashReporting.dsnOverride = defaults.crashReporting.dsnOverride;
        }

        // Repair each self-storing entry against its own validator (single-field only).
        for (ConfigEntry<?> entry : ENTRIES.values()) {
            entry.sanitize();
        }

        // Cross-field constraints can't be repaired per-entry; domains that own a min/max pair register
        // a hook (see repairMinMax).
        SANITIZE_HOOKS.forEach(Runnable::run);

        return config;
    }

    static void useForTests(LogisticsConfig config) {
        INSTANCE = sanitize(config);
        // Self-storing entries live outside the struct-based `config`; reset them so tests start clean.
        for (ConfigEntry<?> entry : ENTRIES.values()) {
            entry.resetToDefault();
        }
    }

    /**
     * Reconcile a min/max entry pair after per-entry sanitize: if {@code max < min}, reset whichever is
     * needed to its default, preferring to keep the other. Domains with a cross-field pair register a
     * {@link #registerSanitizeHook hook} that calls this.
     */
    public static <T extends Number> void repairMinMax(ConfigEntry<T> minEntry, ConfigEntry<T> maxEntry) {
        double min = minEntry.get().doubleValue();
        double max = maxEntry.get().doubleValue();
        if (max >= min) {
            return;
        }
        T defaultMax = maxEntry.defaultValue();
        if (defaultMax.doubleValue() >= min) {
            maxEntry.set(defaultMax);
            LOGGER.warn(
                    "Invalid logistics config value {}={}: must be greater than or equal to {}; using default {}",
                    maxEntry.key(), max, minEntry.key(), defaultMax);
            return;
        }

        T defaultMin = minEntry.defaultValue();
        minEntry.set(defaultMin);
        if (maxEntry.get().doubleValue() < defaultMin.doubleValue()) {
            maxEntry.set(defaultMax);
        }
        LOGGER.warn(
                "Invalid logistics config value {}={}: must be less than or equal to {}; using default {}",
                minEntry.key(), min, maxEntry.key(), defaultMin);
    }

    private static void requireCondition(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    // ==================== ConfigEntry ====================

    /**
     * A single config value that holds its own state. Built via {@link #regInt} etc.; read with {@link #get()}.
     */
    public static final class ConfigEntry<T> {
        private final String key;
        private final String description;
        private final Function<String, T> parser;
        private final Consumer<T> validator;
        private final Consumer<T> crossFieldValidator;
        private final T defaultConstant;
        private volatile T value;

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
            return value;
        }

        /** Set the value directly, without parsing or validation (used by cross-field repair). */
        public void set(T v) {
            value = v;
        }

        /** The configured default. */
        public T defaultValue() {
            return defaultConstant;
        }

        /** Reset to the default. */
        void resetToDefault() {
            value = defaultConstant;
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
