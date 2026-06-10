package com.logistics.power;

import com.logistics.power.block.entity.CreativeSinkBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Server-side capture of power-infrastructure diagnostics into the tag a look-at HUD (Jade) syncs to the
 * client. Only the creative sink needs synced state (its drain rate and last-tick throughput); cables
 * derive everything from their tier client-side, so they are not handled here. Read back by
 * {@code PowerInfraHudLines}.
 */
public final class PowerInfraHudData {

    /** NBT key naming which power-infra block the tag describes. */
    public static final String KEY_TYPE = "type";

    /** NBT key for the creative sink's configured drain rate. Shared so writer and reader can't drift. */
    public static final String KEY_DRAIN_RATE = "drainRate";

    /** NBT key for the energy the creative sink discarded last tick. */
    public static final String KEY_RECEIVED = "received";

    public static final String TYPE_CREATIVE_SINK = "creative_sink";

    private PowerInfraHudData() {}

    public static void write(CompoundTag data, BlockEntity blockEntity) {
        if (blockEntity instanceof CreativeSinkBlockEntity sink) {
            data.putString(KEY_TYPE, TYPE_CREATIVE_SINK);
            data.putLong(KEY_DRAIN_RATE, sink.getDrainRate());
            data.putLong(KEY_RECEIVED, sink.energyReceivedLastTick());
        }
    }
}
