package com.logistics.automation.registry;

import com.logistics.LogisticsMod;
import com.logistics.quarry.ui.QuarryScreenHandler;
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
            Identifier.of(LogisticsMod.MOD_ID, "quarry"),
            new ExtendedScreenHandlerType<>(QuarryScreenHandler::new, BlockPos.PACKET_CODEC));

    public static void initialize() {
        LogisticsMod.LOGGER.info("Registering automation screen handlers");
    }
}
