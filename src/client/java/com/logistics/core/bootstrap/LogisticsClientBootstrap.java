package com.logistics.core.bootstrap;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsAutomationClient;
import com.logistics.LogisticsCore;
import com.logistics.LogisticsCoreClient;
import com.logistics.LogisticsMod;
import com.logistics.LogisticsPipe;
import com.logistics.LogisticsPipeClient;
import com.logistics.LogisticsPower;
import com.logistics.LogisticsPowerClient;

import java.util.Map;
import java.util.function.Supplier;

public final class LogisticsClientBootstrap {
    private final Map<Class<? extends DomainBootstrap>, Supplier<DomainBootstrap>> clientBootstraps;

    private LogisticsClientBootstrap(Map<Class<? extends DomainBootstrap>, Supplier<DomainBootstrap>> clientBootstraps) {
        this.clientBootstraps = clientBootstraps;
    }

    public static LogisticsClientBootstrap createDefault() {
        return new LogisticsClientBootstrap(Map.of(
                LogisticsCore.class, LogisticsCoreClient::new,
                LogisticsPipe.class, LogisticsPipeClient::new,
                LogisticsPower.class, LogisticsPowerClient::new,
                LogisticsAutomation.class, LogisticsAutomationClient::new
        ));
    }

    public void initialize() {
        for (DomainBootstrap bootstrap : DomainBootstraps.all()) {
            DomainBootstrap clientBootstrap = createClientBootstrap(bootstrap);
            if (clientBootstrap != null) {
                clientBootstrap.initClient();
            }
        }
    }

    private DomainBootstrap createClientBootstrap(DomainBootstrap serverBootstrap) {
        Supplier<DomainBootstrap> factory = clientBootstraps.get(serverBootstrap.getClass());
        if (factory != null) {
            return factory.get();
        }

        LogisticsMod.LOGGER.debug("No client bootstrap for domain: {}", serverBootstrap.getClass().getSimpleName());
        return null;
    }
}
