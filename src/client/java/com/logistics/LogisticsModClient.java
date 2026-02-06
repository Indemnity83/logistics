package com.logistics;

import com.logistics.core.bootstrap.DomainBootstrap;
import com.logistics.core.bootstrap.DomainBootstraps;
import net.fabricmc.api.ClientModInitializer;

import java.util.Map;
import java.util.function.Supplier;

public class LogisticsModClient implements ClientModInitializer {
    private static final Map<Class<? extends DomainBootstrap>, Supplier<DomainBootstrap>> CLIENT_BOOTSTRAPS = Map.of(
            LogisticsCore.class, LogisticsCoreClient::new,
            LogisticsPipe.class, LogisticsPipeClient::new,
            LogisticsPower.class, LogisticsPowerClient::new,
            LogisticsAutomation.class, LogisticsAutomationClient::new
    );
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
        Supplier<DomainBootstrap> factory = CLIENT_BOOTSTRAPS.get(serverBootstrap.getClass());
        if (factory != null) {
            return factory.get();
        }
        LogisticsMod.LOGGER.debug("No client bootstrap for domain: {}", serverBootstrap.getClass().getSimpleName());
        return null;
    }
}
