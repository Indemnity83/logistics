package com.logistics.ui;

import com.logistics.LogisticsMod;
import com.logistics.pipe.ui.ItemFilterScreenHandler;
import com.logistics.quarry.ui.QuarryScreenHandler;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public final class LogisticsScreenHandlers {
    private LogisticsScreenHandlers() {}

    public static final ScreenHandlerType<ItemFilterScreenHandler> ITEM_FILTER = Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of(LogisticsMod.MOD_ID, "item_filter"),
            new ScreenHandlerType<>(ItemFilterScreenHandler::new, FeatureSet.empty()));

    public static final ScreenHandlerType<QuarryScreenHandler> QUARRY = Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of(LogisticsMod.MOD_ID, "quarry"),
            new ExtendedScreenHandlerType<>(QuarryScreenHandler::new, BlockPos.PACKET_CODEC));

    public static void initialize() {
        LogisticsMod.LOGGER.info("Registering screen handlers");
    }
}
