package com.logistics.pipe;

import com.logistics.pipe.modules.BlockConnectionModule;
import com.logistics.pipe.modules.BoostModule;
import com.logistics.pipe.modules.WeatheringModule;
import com.logistics.pipe.modules.ExtractionModule;
import com.logistics.pipe.modules.InsertionModule;
import com.logistics.pipe.modules.ItemFilterModule;
import com.logistics.pipe.modules.MergerModule;
import com.logistics.pipe.modules.PipeMarkingModule;
import com.logistics.pipe.modules.PipeOnlyModule;
import com.logistics.pipe.modules.TransportModule;
import com.logistics.pipe.modules.VoidModule;
import com.logistics.pipe.runtime.PipeConfig;

public final class PipeTypes {
    // -----------------
    // Tier 1 (Basic routing / passive movement)
    // -----------------

    // Early transport pipe - slow item movement.
    public static final Pipe STONE_TRANSPORT_PIPE =
            new Pipe(new TransportModule(PipeConfig.ITEM_MIN_SPEED, PipeConfig.DRAG_COEFFICIENT)) {};

    // Base transport pipe - simple item movement.
    // NOTE: No special connection restrictions; this is the default backbone pipe.
    public static final Pipe COPPER_TRANSPORT_PIPE =
            new Pipe(new WeatheringModule(), new PipeMarkingModule()) {};

    // Accelerator transport - accelerates items when powered by redstone.
    public static final Pipe GOLD_TRANSPORT = new Pipe(new BoostModule(PipeConfig.ACCELERATION_RATE)) {};

    // Item extractor - pulls items from an adjacent inventory
    public static final Pipe ITEM_EXTRACTOR =
            new Pipe(new ExtractionModule(), new BlockConnectionModule(() -> PipeTypes.ITEM_EXTRACTOR)) {};

    // Item merger - combines multiple incoming streams into a single output.
    public static final Pipe ITEM_MERGER = new Pipe(new MergerModule()) {};

    // Item insertion pipe - decorative pipe that only connects to other pipes.
    public static final Pipe ITEM_PASSTHROUGH_PIPE = new Pipe(new PipeOnlyModule()) {};

    // -----------------
    // Tier 2 (Decision based routing / bulk movement)
    // -----------------

    // Item filter - routes items based on per-side filters.
    public static final Pipe ITEM_FILTER = new Pipe(new ItemFilterModule()) {};

    // Item void - deletes items at the center with particle effects.
    public static final Pipe ITEM_VOID = new Pipe(new VoidModule(), new PipeOnlyModule()) {};

    // -----------------
    // Tier 3 (Network Logistics)
    // -----------------

    // -----------------
    // Special
    // -----------------

    // Item insertion - prefers inventories with space, otherwise routes to pipes.
    public static final Pipe ITEM_INSERTION = new Pipe(new InsertionModule()) {};

    private PipeTypes() {}
}
