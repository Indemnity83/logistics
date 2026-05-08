package com.logistics;

import com.logistics.core.bootstrap.ClientDomainBootstrap;
import com.logistics.core.macerator.MaceratorScreen;
import net.minecraft.client.gui.screens.MenuScreens;

import static com.logistics.LogisticsMod.LOGGER;

public final class LogisticsCoreClient implements ClientDomainBootstrap {

    @Override
    public void initClient() {
        LOGGER.info("Registering core (client)");

        MenuScreens.register(LogisticsCore.MENU.MACERATOR, MaceratorScreen::new);
    }

    @Override
    public int order() {
        return -100; // Initialize core first
    }
}
