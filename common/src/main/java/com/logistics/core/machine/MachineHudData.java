package com.logistics.core.machine;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Server-side capture of a machine's look-at HUD into the tag a HUD mod (Jade) syncs to the client.
 * Delegates to each machine's {@link MachineComponent.HudContributor} components via
 * {@link MachineEntity#contributeHud}, so progress, tank contents, and any future contribution show up
 * without machine-specific plumbing. Read back by the client-side {@code MachineHudLines} (text) and the
 * loader-specific Jade provider (graphical elements).
 */
public final class MachineHudData {

    private MachineHudData() {}

    public static void write(CompoundTag data, BlockEntity blockEntity) {
        if (blockEntity instanceof MachineEntity machine) {
            MachineHudModel hud = new MachineHudModel();
            machine.contributeHud(hud);
            hud.save(data);
        }
    }
}
