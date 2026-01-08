package com.logistics;

import com.logistics.block.LogisticsBlocks;
import com.logistics.block.entity.LogisticsBlockEntities;
import com.logistics.item.LogisticsItemGroups;
import com.logistics.item.LogisticsItems;
import com.logistics.pipe.ui.PipeScreenHandlers;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogisticsMod implements ModInitializer {
	public static final String MOD_ID = "logistics";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Logistics mod");

		LogisticsItems.initialize();
		LogisticsBlocks.initialize();
		LogisticsBlockEntities.initialize();
		PipeScreenHandlers.initialize();
		LogisticsItemGroups.initialize();

		ItemStorage.SIDED.registerForBlockEntity(
			(blockEntity, direction) -> blockEntity.getItemStorage(direction),
			LogisticsBlockEntities.PIPE_BLOCK_ENTITY
		);

    }
}
