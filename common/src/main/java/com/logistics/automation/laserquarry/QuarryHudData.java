package com.logistics.automation.laserquarry;

import com.logistics.automation.laserquarry.entity.LaserQuarryBlockEntity;
import net.minecraft.nbt.CompoundTag;

/**
 * Server-side capture of a laser quarry's status into the tag a look-at HUD (Jade) syncs to the client.
 * Loader-agnostic and free of any HUD-mod API; read back by the client-side {@code QuarryHudLines}.
 */
public final class QuarryHudData {

    /** NBT key whose presence marks that the tag carries quarry status. */
    public static final String KEY_PHASE = "phase";

    private QuarryHudData() {}

    public static void write(CompoundTag data, LaserQuarryBlockEntity quarry) {
        data.putString(KEY_PHASE, quarry.getCurrentPhase().name());
        data.putBoolean("finished", quarry.isFinished());
        data.putLong("powerIn", quarry.getEnergyReceivedLastTick());
        data.putFloat("armSpeed", quarry.getSyncedArmSpeed());
    }
}
