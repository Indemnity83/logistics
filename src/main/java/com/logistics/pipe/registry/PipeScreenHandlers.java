package com.logistics.pipe.registry;

import com.logistics.LogisticsMod;
import com.logistics.pipe.ui.ItemFilterScreenHandler;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.MenuType;

public final class PipeScreenHandlers {
    private PipeScreenHandlers() {}

    public static final MenuType<ItemFilterScreenHandler> ITEM_FILTER = Registry.register(
            BuiltInRegistries.MENU,
            Identifier.fromNamespaceAndPath(LogisticsMod.MOD_ID, "pipe/item_filter"),
            new MenuType<>(ItemFilterScreenHandler::new, FeatureFlagSet.of()));

    public static void initialize() {
        LogisticsMod.LOGGER.info("Registering pipe screen handlers");
        registerLegacyAliases();
    }

    private static void registerLegacyAliases() {
        // v0.2 => v0.3
        BuiltInRegistries.MENU.addAlias(
                Identifier.fromNamespaceAndPath(LogisticsMod.MOD_ID, "item_filter"),
                BuiltInRegistries.MENU.getKey(ITEM_FILTER));
    }
}
