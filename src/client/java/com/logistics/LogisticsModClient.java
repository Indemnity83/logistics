package com.logistics;

import net.fabricmc.api.ClientModInitializer;

public class LogisticsModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		LogisticsMod.LOGGER.info("Initializing Logistics client");

		// Register client-side renderers, models, etc. here
	}
}
