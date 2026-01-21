package com.logistics.neoforge;

import com.logistics.Logistics;
import com.logistics.block.LogisticsBlocks;
import com.logistics.block.entity.LogisticsBlockEntities;
import com.logistics.item.LogisticsItemGroups;
import com.logistics.item.LogisticsItems;
import com.logistics.neoforge.platform.NeoForgeItemStorageService;
import com.logistics.pipe.ui.PipeScreenHandlers;
import com.logistics.util.TimingLog;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.slf4j.Logger;

@Mod(LogisticsMod.MOD_ID)
public class LogisticsMod {
    public static final String MOD_ID = Logistics.MOD_ID;
    public static final Logger LOGGER = Logistics.LOGGER;

    public LogisticsMod(IEventBus modEventBus) {
        LOGGER.info("Initializing Logistics mod (NeoForge)");

        // Register for common setup
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerCapabilities);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            TimingLog.time(LOGGER, "LogisticsDataComponents.initialize", LogisticsDataComponents::initialize);
            TimingLog.time(LOGGER, "LogisticsItems.initialize", LogisticsItems::initialize);
            TimingLog.time(LOGGER, "LogisticsBlocks.initialize", LogisticsBlocks::initialize);
            TimingLog.time(LOGGER, "LogisticsBlockEntities.initialize", LogisticsBlockEntities::initialize);
            TimingLog.time(LOGGER, "PipeScreenHandlers.initialize", PipeScreenHandlers::initialize);
            TimingLog.time(LOGGER, "LogisticsItemGroups.initialize", LogisticsItemGroups::initialize);
        });
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        NeoForgeItemStorageService.registerPipeCapability(event);
    }
}
