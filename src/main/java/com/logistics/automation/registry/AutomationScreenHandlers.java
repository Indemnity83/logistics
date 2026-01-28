package com.logistics.automation.registry;

import com.logistics.LogisticsMod;
import com.logistics.automation.quarry.ui.QuarryScreenHandler;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MenuType;

public final class AutomationScreenHandlers {
    private AutomationScreenHandlers() {}

    public static final MenuType<QuarryScreenHandler> QUARRY = Registry.register(
            BuiltInRegistries.MENU,
            Identifier.fromNamespaceAndPath(LogisticsMod.MOD_ID, "automation/quarry"),
            new ExtendedMenuType<>(QuarryScreenHandler::new, BlockPos.STREAM_CODEC));

    public static void initialize() {
        LogisticsMod.LOGGER.info("Registering automation screen handlers");
        registerLegacyAliases();
    }

    private static void registerLegacyAliases() {
        // v0.2 => v0.3
        BuiltInRegistries.MENU.addAlias(
                Identifier.fromNamespaceAndPath(LogisticsMod.MOD_ID, "quarry"), BuiltInRegistries.MENU.getKey(QUARRY));
    }
}
