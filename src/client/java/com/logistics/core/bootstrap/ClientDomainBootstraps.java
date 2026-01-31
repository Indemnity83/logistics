package com.logistics.core.bootstrap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

import com.logistics.LogisticsMod;

public final class ClientDomainBootstraps {
    private static final List<ClientDomainBootstrap> BOOTSTRAPS = loadBootstraps();

    private ClientDomainBootstraps() {}

    public static List<ClientDomainBootstrap> all() {
        return BOOTSTRAPS;
    }

    private static List<ClientDomainBootstrap> loadBootstraps() {
        List<ClientDomainBootstrap> bootstraps = new ArrayList<>();
        for (ClientDomainBootstrap bootstrap : ServiceLoader.load(ClientDomainBootstrap.class)) {
            bootstraps.add(bootstrap);
        }

        bootstraps.sort(Comparator.comparingInt(DomainBootstrap::order)
                .thenComparing(bootstrap -> bootstrap.getClass().getName()));

        if (bootstraps.isEmpty()) {
            LogisticsMod.LOGGER.warn("No client domain bootstraps discovered. Client content will not be registered.");
        }

        return List.copyOf(bootstraps);
    }
}
