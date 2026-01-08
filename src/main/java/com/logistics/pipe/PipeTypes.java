package com.logistics.pipe;

import com.logistics.pipe.modules.*;
import com.logistics.pipe.runtime.PipeConfig;

public final class PipeTypes {
    // Basic transport pipe with no special behavior (just moves items)
    public static final Pipe BASIC_TRANSPORT = new Pipe() {};

    // Void pipe - deletes items at the center with particle effects
    public static final Pipe VOID_TRANSPORT = new Pipe(new VoidModule()) {};

    // Gold pipe - accelerates items when powered by redstone
    public static final Pipe GOLD_TRANSPORT = new Pipe(new BoostModule(PipeConfig.ACCELERATION_RATE)) {};

    // Copper pipe - distributes items evenly across outputs (round-robin)
    public static final Pipe COPPER_TRANSPORT = new Pipe(new SplitterModule()) {};

    // Iron pipe - routes items to a single configured output direction
    public static final Pipe IRON_TRANSPORT = new Pipe(new MergerModule()) {};

    // Wooden pipe - extracts items from adjacent inventories
    public static final Pipe WOOD_TRANSPORT = new Pipe(new ExtractionModule()) {};

    // Quartz pipe - provides redstone comparator output based on item count
    public static final Pipe QUARTZ_TRANSPORT = new Pipe(new ComparatorModule()) {};

    private PipeTypes() {}
}
