package com.logistics;

import com.logistics.core.bootstrap.DomainBootstrap;
import com.logistics.core.bootstrap.DomainBootstraps;
import net.fabricmc.api.ClientModInitializer;

public class LogisticsModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LogisticsMod.LOGGER.info("Initializing Logistics client");

        // Initialize client bootstraps in server domain order
        for (DomainBootstrap bootstrap : DomainBootstraps.all()) {
            DomainBootstrap clientBootstrap = createClientBootstrap(bootstrap);
            if (clientBootstrap != null) {
                clientBootstrap.initClient();
            }
        }
    }

    private static DomainBootstrap createClientBootstrap(DomainBootstrap serverBootstrap) {
        if (serverBootstrap instanceof LogisticsCore) {
            return new LogisticsCoreClient();
        } else if (serverBootstrap instanceof LogisticsPipe) {
            return new LogisticsPipeClient();
        } else if (serverBootstrap instanceof LogisticsPower) {
            return new LogisticsPowerClient();
        } else if (serverBootstrap instanceof LogisticsAutomation) {
            return new LogisticsAutomationClient();
        }
        LogisticsMod.LOGGER.debug("No client bootstrap for domain: {}", serverBootstrap.getClass().getSimpleName());
        return null;
    }
}
