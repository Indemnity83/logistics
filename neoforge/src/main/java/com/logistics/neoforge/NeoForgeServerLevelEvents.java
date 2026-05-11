package com.logistics.neoforge;

import com.logistics.automation.laserquarry.entity.LaserQuarryBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.level.LevelEvent;

public final class NeoForgeServerLevelEvents {
    private NeoForgeServerLevelEvents() {}

    public static void register(IEventBus gameBus) {
        gameBus.addListener(NeoForgeServerLevelEvents::onLevelUnload);
    }

    private static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            LaserQuarryBlockEntity.clearActiveQuarries(level);
        }
    }
}
