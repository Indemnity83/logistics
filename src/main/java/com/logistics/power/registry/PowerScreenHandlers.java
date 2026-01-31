package com.logistics.power.registry;

import com.logistics.LogisticsMod;
import com.logistics.power.engine.ui.StirlingEngineScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public final class PowerScreenHandlers {
    private PowerScreenHandlers() {}

    public static final ScreenHandlerType<StirlingEngineScreenHandler> STIRLING_ENGINE = Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of(LogisticsMod.MOD_ID, "power/stirling_engine"),
            new ExtendedScreenHandlerType<>(StirlingEngineScreenHandler::new, BlockPos.PACKET_CODEC));

    public static void initialize() {
        LogisticsMod.LOGGER.info("Registering power screen handlers");
    }
}
