package com.logistics.pipe.registry;

import com.logistics.LogisticsMod;
import com.logistics.core.bootstrap.DomainBootstrap;
import com.logistics.pipe.data.PipeDataComponents;
import com.logistics.util.TimingLog;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;

public final class PipeDomainBootstrap implements DomainBootstrap {
    @Override
    public void initCommon() {
        TimingLog.time(LogisticsMod.LOGGER, "PipeDataComponents.initialize", PipeDataComponents::initialize);
        TimingLog.time(LogisticsMod.LOGGER, "PipeItems.initialize", PipeItems::initialize);
        TimingLog.time(LogisticsMod.LOGGER, "PipeBlocks.initialize", PipeBlocks::initialize);
        TimingLog.time(LogisticsMod.LOGGER, "PipeBlockEntities.initialize", PipeBlockEntities::initialize);
        TimingLog.time(LogisticsMod.LOGGER, "PipeScreenHandlers.initialize", PipeScreenHandlers::initialize);
        TimingLog.time(LogisticsMod.LOGGER, "PipeItemGroups.initialize", PipeItemGroups::initialize);

        PipeItemGroups.registerProviders();

        TimingLog.time(
                LogisticsMod.LOGGER,
                "ItemStorage.SIDED.registerForBlockEntity",
                () -> ItemStorage.SIDED.registerForBlockEntity(
                        (blockEntity, direction) -> blockEntity.getItemStorage(direction),
                        PipeBlockEntities.PIPE_BLOCK_ENTITY));
    }
}
