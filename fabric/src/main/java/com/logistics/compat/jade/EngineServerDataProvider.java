package com.logistics.compat.jade;

import com.logistics.LogisticsMod;
import com.logistics.core.lib.power.EngineEntity;
import com.logistics.power.engine.EngineHudData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation; // raw-id-ok: Jade's IJadeProvider.getUid() returns Identifier
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

/**
 * Server half of the engine Jade integration: captures live engine diagnostics into the tag Jade syncs to
 * the client. Jade requires the data and component providers to be separate classes (since MC 1.21.6); the
 * client half is {@link EngineComponentProvider}. Capture logic is shared in {@link EngineHudData}.
 */
public final class EngineServerDataProvider implements IServerDataProvider<BlockAccessor> {
    public static final EngineServerDataProvider INSTANCE = new EngineServerDataProvider();

    private static final ResourceLocation UID = // raw-id-ok: Jade keys providers by Identifier
            LogisticsMod.modId("engine").toIdentifier();

    private EngineServerDataProvider() {}

    @Override
    public ResourceLocation getUid() { // raw-id-ok: overrides Jade API returning Identifier
        return UID;
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof EngineEntity engine) {
            EngineHudData.write(data, engine);
        }
    }
}
