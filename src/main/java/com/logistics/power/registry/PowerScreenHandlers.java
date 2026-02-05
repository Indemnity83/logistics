package com.logistics.power.registry;

import com.logistics.LogisticsMod;
import com.logistics.power.engine.ui.StirlingEngineScreenHandler;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MenuType;

public final class PowerScreenHandlers {
    private PowerScreenHandlers() {}

    public static final MenuType<StirlingEngineScreenHandler> STIRLING_ENGINE = Registry.register(
            BuiltInRegistries.MENU,
            Identifier.fromNamespaceAndPath(LogisticsMod.MOD_ID, "power/stirling_engine"),
            new ExtendedMenuType<>(StirlingEngineScreenHandler::new, BlockPos.STREAM_CODEC));

    public static void initialize() {
        LogisticsMod.LOGGER.info("Registering power screen handlers");
    }
}
