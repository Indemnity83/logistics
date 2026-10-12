package com.logistics;

import com.indemnity83.configory.ConfigEntries;
import com.logistics.core.bootstrap.DomainBootstrap;
import com.logistics.core.lib.resource.ResourceId;

public final class LogisticsForestry extends LogisticsMod implements DomainBootstrap {
    private static final LogisticsForestry INSTANCE = new LogisticsForestry();

    @Override
    protected String domain() {
        return "forestry";
    }

    public static ResourceId resource(String name) {
        return INSTANCE.domainResource(name);
    }

    public static ResourceId model(String name) {
        return INSTANCE.domainModelResource(name);
    }

    @Override
    public int order() {
        return 30;
    }

    @Override
    public void registerConfig() {
        CONFIG.register();
    }

    @Override
    public void initCommon() {
        LOGGER.info("Registering {}", domain());

        BLOCK.register();
        ENTITY.register();
        MENU.register();
        RECIPE.register();
        CREATIVE.register();
        ALIAS.register();
    }

    public static final class CONFIG extends ConfigEntries {
        private CONFIG() {}

        static void register() {}
    }

    public static final class BLOCK {
        private BLOCK() {}

        static void register() {}
    }

    public static final class ENTITY {
        private ENTITY() {}

        static void register() {}
    }

    public static final class MENU {
        private MENU() {}

        static void register() {}
    }

    public static final class RECIPE {
        private RECIPE() {}

        static void register() {}
    }

    public static final class CREATIVE {
        private CREATIVE() {}

        static void register() {}
    }

    public static final class ALIAS {
        private ALIAS() {}

        static void register() {}
    }
}
