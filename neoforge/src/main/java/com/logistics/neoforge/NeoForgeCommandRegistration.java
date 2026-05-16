package com.logistics.neoforge;

import com.logistics.core.LogisticsCommandTree;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class NeoForgeCommandRegistration {
    private NeoForgeCommandRegistration() {}

    public static void register(IEventBus gameBus) {
        gameBus.addListener(NeoForgeCommandRegistration::onRegisterCommands);
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(LogisticsCommandTree.build());
    }
}
