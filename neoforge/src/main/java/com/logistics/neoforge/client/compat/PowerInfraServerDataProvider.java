package com.logistics.neoforge.client.compat;

import com.logistics.LogisticsMod;
import com.logistics.power.PowerInfraHudData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation; // raw-id-ok: Jade's IJadeProvider.getUid() returns Identifier
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

/**
 * Server half of the power-infrastructure Jade integration (currently the creative sink's drain/throughput).
 * The client half is {@link PowerInfraComponentProvider}; capture logic is shared in {@link PowerInfraHudData}.
 */
public final class PowerInfraServerDataProvider implements IServerDataProvider<BlockAccessor> {
    public static final PowerInfraServerDataProvider INSTANCE = new PowerInfraServerDataProvider();

    private static final ResourceLocation UID = // raw-id-ok: Jade keys providers by Identifier
            LogisticsMod.modId("power_infra").toIdentifier();

    private PowerInfraServerDataProvider() {}

    @Override
    public ResourceLocation getUid() { // raw-id-ok: overrides Jade API returning Identifier
        return UID;
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        PowerInfraHudData.write(data, accessor.getBlockEntity());
    }
}
