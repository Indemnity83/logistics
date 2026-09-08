package com.logistics.pipe.modules;

/**
 * Throughput ladder for the three extractor module tiers.
 *
 * <p>Single source of truth for the tuning: {@code LogisticsPipe} constructs each extractor module
 * from a tier rather than from a literal pair, so the registration and the module javadocs cannot
 * drift apart.
 *
 * <p>Each tier must be strictly faster than the one below it in {@link #itemsPerTick()} — an
 * upgrade that pulls no faster than the module it is crafted from is a bug, not a sidegrade.
 * {@code BASIC} and {@code MKII} run on {@link BasicExtractorModule}; {@code MKIII} runs on
 * {@link AdvancedExtractorModule}, which adds an include/exclude filter on top of the extra
 * throughput.
 */
public enum ExtractorTier {

    /** Extractor Module — 8 items every 80 ticks (0.1 items/tick). */
    BASIC(8, 80),

    /** Extractor MkII Module — 8 items every 20 ticks (0.4 items/tick). */
    MKII(8, 20),

    /** Extractor MkIII Module — 32 items every 10 ticks (3.2 items/tick), plus a filter. */
    MKIII(32, 10);

    private final int itemsPerPull;
    private final int ticksBetweenPulls;

    ExtractorTier(int itemsPerPull, int ticksBetweenPulls) {
        this.itemsPerPull = itemsPerPull;
        this.ticksBetweenPulls = ticksBetweenPulls;
    }

    /** Maximum items moved into the pipe by a single pull. */
    public int itemsPerPull() {
        return itemsPerPull;
    }

    /** Ticks between consecutive pulls. */
    public int ticksBetweenPulls() {
        return ticksBetweenPulls;
    }

    /** Sustained throughput, used to compare tiers against each other. */
    public double itemsPerTick() {
        return (double) itemsPerPull / ticksBetweenPulls;
    }
}
