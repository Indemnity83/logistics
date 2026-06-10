package com.logistics.neoforge.client.compat;

import com.logistics.LogisticsMod;
import com.logistics.automation.laserquarry.QuarryHudData;
import com.logistics.automation.laserquarry.entity.LaserQuarryBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation; // raw-id-ok: Jade's IJadeProvider.getUid() returns Identifier
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

/**
 * Server half of the laser-quarry Jade integration: captures quarry status into the synced tag. The client
 * half is {@link QuarryComponentProvider}; capture logic is shared in {@link QuarryHudData}.
 */
public final class QuarryServerDataProvider implements IServerDataProvider<BlockAccessor> {
    public static final QuarryServerDataProvider INSTANCE = new QuarryServerDataProvider();

    private static final ResourceLocation UID = // raw-id-ok: Jade keys providers by Identifier
            LogisticsMod.modId("quarry").toIdentifier();

    private QuarryServerDataProvider() {}

    @Override
    public ResourceLocation getUid() { // raw-id-ok: overrides Jade API returning Identifier
        return UID;
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof LaserQuarryBlockEntity quarry) {
            QuarryHudData.write(data, quarry);
        }
    }
}
