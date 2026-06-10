package com.logistics.core.machine;

import com.logistics.core.lib.block.ProcessingMachine;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Server-side capture of a processing machine's progress into the tag a look-at HUD (Jade) syncs to the
 * client. Works off the {@link ProcessingMachine} contract, so it covers the macerator, kiln, and any
 * future machine without depending on a specific domain. Read back by the client-side {@code MachineHudLines}.
 */
public final class MachineHudData {

    /** NBT key whose presence marks that the tag carries machine processing state. */
    public static final String KEY_PROCESSING = "processing";

    private MachineHudData() {}

    public static void write(CompoundTag data, BlockEntity blockEntity) {
        if (blockEntity instanceof ProcessingMachine machine) {
            data.putBoolean(KEY_PROCESSING, machine.isProcessing());
            data.putFloat("progress", machine.processProgress());
        }
    }
}
