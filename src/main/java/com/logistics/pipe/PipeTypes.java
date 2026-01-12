package com.logistics.pipe;

import com.logistics.pipe.modules.*;
import com.logistics.pipe.runtime.PipeConfig;

public final class PipeTypes {
    // Cobblestone pipe - does not connect to stone pipes
    public static final Pipe COBBLESTONE_TRANSPORT = new Pipe(
            new BlockConnectionModule(() -> PipeTypes.STONE_TRANSPORT)
    ) {};

    // Stone pipe - does not connect to cobblestone pipes
    public static final Pipe STONE_TRANSPORT = new Pipe(
            new BlockConnectionModule(() -> PipeTypes.COBBLESTONE_TRANSPORT)
    ) {};

    // Void pipe - deletes items at the center with particle effects
    public static final Pipe VOID_TRANSPORT = new Pipe(
            new VoidModule(), new PipeOnlyModule()
    ) {};

    // Gold pipe - accelerates items when powered by redstone
    public static final Pipe GOLD_TRANSPORT = new Pipe(
            new BoostModule(PipeConfig.ACCELERATION_RATE)
    ) {};

    // Copper pipe - distributes items evenly across outputs (round-robin)
    public static final Pipe COPPER_TRANSPORT = new Pipe(
            new SplitterModule()
    ) {};

    // Iron pipe - routes items to a single configured output direction
    public static final Pipe IRON_TRANSPORT = new Pipe(
            new MergerModule()
    ) {};

    // Wooden pipe - extracts items from adjacent inventories
    public static final Pipe WOOD_TRANSPORT = new Pipe(
            new ExtractionModule(),
            new BlockConnectionModule(() -> PipeTypes.WOOD_TRANSPORT)
    ) {};

    // Diamond pipe - routes items based on per-side filters
    public static final Pipe DIAMOND_TRANSPORT = new Pipe(
            new SmartSplitterModule()
    ) {};

    // Quartz pipe - provides redstone comparator output based on item count
    public static final Pipe QUARTZ_TRANSPORT = new Pipe(
            new ComparatorModule()
    ) {};

    private PipeTypes() {}
}
