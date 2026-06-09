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

    public static final String TYPE_CREATIVE_SINK = "creative_sink";

    private PowerInfraHudData() {}

    public static void write(CompoundTag data, BlockEntity blockEntity) {
        if (blockEntity instanceof CreativeSinkBlockEntity sink) {
            data.putString(KEY_TYPE, TYPE_CREATIVE_SINK);
            data.putLong("drainRate", sink.getDrainRate());
            data.putLong("received", sink.energyReceivedLastTick());
        }
    }
}
