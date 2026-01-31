package com.logistics.automation.registry;

import com.logistics.LogisticsMod;
import com.logistics.automation.quarry.ui.QuarryScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public final class AutomationScreenHandlers {
    private AutomationScreenHandlers() {}

    public static final ScreenHandlerType<QuarryScreenHandler> QUARRY = Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of(LogisticsMod.MOD_ID, "automation/quarry"),
            new ExtendedScreenHandlerType<>(QuarryScreenHandler::new, BlockPos.PACKET_CODEC));

    public static void initialize() {
        LogisticsMod.LOGGER.info("Registering automation screen handlers");
        registerLegacyAliases();
    }

    private static void registerLegacyAliases() {
        // v0.2 => v0.3
        Registries.SCREEN_HANDLER.addAlias(
                Identifier.of(LogisticsMod.MOD_ID, "quarry"), Registries.SCREEN_HANDLER.getId(QUARRY));
    }
}
