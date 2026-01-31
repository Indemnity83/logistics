package com.logistics.core.lib.support;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the result of probing a block.
 * Contains a title and a list of key-value entries to display to the player.
 *
 * <p>Usage:
 * <pre>{@code
 * ProbeResult.builder("Engine Stats")
 *     .entry("Stage", stage.name(), Formatting.GREEN)
 *     .entry("Temperature", String.format("%.0f°C", temp))
 *     .warning("OVERHEATED!")
 *     .build();
 * }</pre>
 */
public final class ProbeResult {
    private final String title;
    private final List<Entry> entries;

    private ProbeResult(String title, List<Entry> entries) {
        this.title = title;
        this.entries = List.copyOf(entries);
    }

    public String title() {
        return title;
    }

    public List<Entry> entries() {
        return entries;
    }

    public static Builder builder(String title) {
        return new Builder(title);
    }

    /**
     * A single entry in the probe result.
     */
    public sealed interface Entry {
        record KeyValue(String key, String value, @Nullable Formatting color) implements Entry {}

        record Warning(String message) implements Entry {}

        record Separator() implements Entry {}
    }

    /**
     * Builder for creating ProbeResult instances.
     */
    public static final class Builder {
        private final String title;
        private final List<Entry> entries = new ArrayList<>();

        private Builder(String title) {
            this.title = title;
        }

        /**
         * Adds a key-value entry with optional color formatting.
         */
        public Builder entry(String key, String value, @Nullable Formatting color) {
            entries.add(new Entry.KeyValue(key, value, color));
            return this;
        }

        /**
         * Adds a key-value entry with default formatting.
         */
        public Builder entry(String key, String value) {
            return entry(key, value, null);
        }

        /**
         * Adds a warning message (displayed in red/bold).
         */
        public Builder warning(String message) {
            entries.add(new Entry.Warning(message));
            return this;
        }

        /**
         * Adds a visual separator between sections.
         */
        public Builder separator() {
            entries.add(new Entry.Separator());
            return this;
        }

        public ProbeResult build() {
            return new ProbeResult(title, entries);
        }
    }
}
