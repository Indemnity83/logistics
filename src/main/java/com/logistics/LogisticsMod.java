package com.logistics;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogisticsMod implements ModInitializer {
	public static final String MOD_ID = "logistics";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Logistics mod");

		// Register blocks, items, block entities, etc. here
	}
}
