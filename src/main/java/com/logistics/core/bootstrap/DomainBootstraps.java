package com.logistics.core.bootstrap;

import com.logistics.LogisticsMod;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

public final class DomainBootstraps {
    private static final List<DomainBootstrap> BOOTSTRAPS = loadBootstraps();

    private DomainBootstraps() {}

    public static List<DomainBootstrap> all() {
        return BOOTSTRAPS;
    }

    private static List<DomainBootstrap> loadBootstraps() {
        List<DomainBootstrap> bootstraps = new ArrayList<>();
        for (DomainBootstrap bootstrap : ServiceLoader.load(DomainBootstrap.class)) {
            bootstraps.add(bootstrap);
        }

        bootstraps.sort(Comparator.comparingInt(DomainBootstrap::order)
                .thenComparing(bootstrap -> bootstrap.getClass().getName()));

        if (bootstraps.isEmpty()) {
            LogisticsMod.LOGGER.warn("No domain bootstraps discovered. Mod content will not be registered.");
        }

        return List.copyOf(bootstraps);
    }
}
