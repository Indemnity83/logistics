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
        String domainName = serverBootstrap.getClass().getSimpleName();
        return switch (domainName) {
            case "LogisticsCore" -> new LogisticsCoreClient();
            case "LogisticsPipe" -> new LogisticsPipeClient();
            case "LogisticsPower" -> new LogisticsPowerClient();
            case "LogisticAutomation" -> new LogisticsAutomationClient();
            default -> {
                LogisticsMod.LOGGER.debug("No client bootstrap for domain: {}", domainName);
                yield null;
            }
        };
    }
}
