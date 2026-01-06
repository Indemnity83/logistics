package com.logistics;

import com.logistics.block.LogisticsBlocks;
import com.logistics.block.entity.LogisticsBlockEntities;
import com.logistics.item.LogisticsItemGroups;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogisticsMod implements ModInitializer {
	public static final String MOD_ID = "logistics";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Logistics mod");

		LogisticsBlocks.initialize();
		LogisticsBlockEntities.initialize();
		LogisticsItemGroups.initialize();
	}
}
