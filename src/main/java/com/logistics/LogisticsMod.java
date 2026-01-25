package com.logistics;

import com.logistics.block.LogisticsBlocks;
import com.logistics.block.entity.LogisticsBlockEntities;
import com.logistics.item.LogisticsItemGroups;
import com.logistics.item.LogisticsItems;
import com.logistics.marker.MarkerBlockEntities;
import com.logistics.marker.MarkerBlocks;
import com.logistics.pipe.ui.PipeScreenHandlers;
import com.logistics.quarry.QuarryBlockEntities;
import com.logistics.quarry.QuarryBlocks;
import com.logistics.quarry.ui.QuarryScreenHandlers;
import com.logistics.util.TimingLog;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogisticsMod implements ModInitializer {
    public static final String MOD_ID = "logistics";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Logistics mod");

        TimingLog.time(LOGGER, "LogisticsDataComponents.initialize", LogisticsDataComponents::initialize);
        TimingLog.time(LOGGER, "LogisticsItems.initialize", LogisticsItems::initialize);
        TimingLog.time(LOGGER, "LogisticsBlocks.initialize", LogisticsBlocks::initialize);
        TimingLog.time(LOGGER, "LogisticsBlockEntities.initialize", LogisticsBlockEntities::initialize);
        TimingLog.time(LOGGER, "PipeScreenHandlers.initialize", PipeScreenHandlers::initialize);
        TimingLog.time(LOGGER, "QuarryBlocks.initialize", QuarryBlocks::initialize);
        TimingLog.time(LOGGER, "QuarryBlockEntities.initialize", QuarryBlockEntities::initialize);
        TimingLog.time(LOGGER, "QuarryScreenHandlers.initialize", QuarryScreenHandlers::initialize);
        TimingLog.time(LOGGER, "MarkerBlocks.initialize", MarkerBlocks::initialize);
        TimingLog.time(LOGGER, "MarkerBlockEntities.initialize", MarkerBlockEntities::initialize);
        TimingLog.time(LOGGER, "LogisticsItemGroups.initialize", LogisticsItemGroups::initialize);

        TimingLog.time(
                LOGGER,
                "ItemStorage.SIDED.registerForBlockEntity",
                () -> ItemStorage.SIDED.registerForBlockEntity(
                        (blockEntity, direction) -> blockEntity.getItemStorage(direction),
                        LogisticsBlockEntities.PIPE_BLOCK_ENTITY));

        ServerLifecycleEvents.SERVER_STARTING.register(server -> TimingLog.start("server_starting"));
        ServerLifecycleEvents.SERVER_STARTED.register(
                server -> TimingLog.logSince(LOGGER, "server_starting", "Server starting"));
        ServerWorldEvents.LOAD.register((server, world) -> {
            long start = TimingLog.getStart("server_starting");
            if (start > 0L) {
                TimingLog.log(LOGGER, "World load " + world.getRegistryKey().getValue(), start);
            }
        });
    }
}
