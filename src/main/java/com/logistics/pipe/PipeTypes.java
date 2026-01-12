package com.logistics.pipe;

import com.logistics.pipe.modules.*;
import com.logistics.pipe.runtime.PipeConfig;

public final class PipeTypes {
    // -----------------
    // Tier 1 (Basic routing / passive movement)
    // -----------------

    // Base transport pipe (copper in-game) - simple item movement.
    // NOTE: No special connection restrictions; this is the default backbone pipe.
    public static final Pipe COPPER_TRANSPORT_PIPE = new Pipe() {};

    // Accelerator transport - accelerates items when powered by redstone.
    public static final Pipe GOLD_TRANSPORT = new Pipe(
            new BoostModule(PipeConfig.ACCELERATION_RATE)
    ) {};

    // Basic extractor - pulls items from an adjacent inventory
    public static final Pipe BASIC_EXTRACTOR = new Pipe(
            new ExtractionModule(),
            new BlockConnectionModule(() -> PipeTypes.BASIC_EXTRACTOR)
    ) {};

    // Basic merger - combines multiple incoming streams into a single output.
    public static final Pipe BASIC_MERGER = new Pipe(
            new MergerModule()
    ) {};

    // Round-robin splitter - distributes items evenly across outputs.
    public static final Pipe BASIC_SPLITTER = new Pipe(
            new SplitterModule()
    ) {};

    // -----------------
    // Tier 2 (Decision based routing / bulk movement)
    // -----------------

    // Filter gate - routes items based on per-side filters.
    public static final Pipe SMART_SPLITTER = new Pipe(
            new SmartSplitterModule()
    ) {};

    // Void pipe - deletes items at the center with particle effects.
    public static final Pipe VOID = new Pipe(
            new VoidModule(),
            new PipeOnlyModule()
    ) {};

    // -----------------
    // Tier 3 (Network Logistics)
    // -----------------


    // -----------------
    // Special
    // -----------------

    // Quartz pipe - provides redstone comparator output based on item count.
    public static final Pipe COMPARATOR = new Pipe(
        new ComparatorModule()
    ) {};

    private PipeTypes() {}
}
