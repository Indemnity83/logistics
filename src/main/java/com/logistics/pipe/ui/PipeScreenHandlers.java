package com.logistics.pipe.ui;

import com.logistics.LogisticsMod;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public final class PipeScreenHandlers {
    public static final ScreenHandlerType<ItemFilterScreenHandler> ITEM_FILTER = Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of(LogisticsMod.MOD_ID, "item_filter"),
            new ScreenHandlerType<>(ItemFilterScreenHandler::new, FeatureSet.empty()));

    private PipeScreenHandlers() {}

    public static void initialize() {}
}
