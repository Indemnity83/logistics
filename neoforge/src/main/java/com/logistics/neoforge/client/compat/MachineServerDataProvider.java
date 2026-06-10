package com.logistics.neoforge.client.compat;

import com.logistics.LogisticsMod;
import com.logistics.core.machine.MachineHudData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier; // raw-id-ok: Jade's IJadeProvider.getUid() returns Identifier
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

/**
 * Server half of the processing-machine Jade integration (macerator, kiln): captures progress into the
 * synced tag. The client half is {@link MachineComponentProvider}; logic is shared in {@link MachineHudData}.
 */
public final class MachineServerDataProvider implements IServerDataProvider<BlockAccessor> {
    public static final MachineServerDataProvider INSTANCE = new MachineServerDataProvider();

    private static final Identifier UID = // raw-id-ok: Jade keys providers by Identifier
            LogisticsMod.modId("machine").toIdentifier();

    private MachineServerDataProvider() {}

    @Override
    public Identifier getUid() { // raw-id-ok: overrides Jade API returning Identifier
        return UID;
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        MachineHudData.write(data, accessor.getBlockEntity());
    }
}
