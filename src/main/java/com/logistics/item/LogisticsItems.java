package com.logistics.item;

import com.logistics.LogisticsMod;
import com.logistics.block.LogisticsBlocks;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class LogisticsItems {
    public static final Item WRENCH = registerItem("wrench",
        new WrenchItem(new Item.Settings()
            .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(LogisticsMod.MOD_ID, "wrench")))
            .maxCount(1)));

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(LogisticsMod.MOD_ID, name), item);
    }

    public static void initialize() {
        LogisticsMod.LOGGER.info("Registering items");

        Registries.ITEM.addAlias(
            Identifier.of(LogisticsMod.MOD_ID, "item_sensor_pipe"),
            Registries.BLOCK.getId(LogisticsBlocks.COPPER_TRANSPORT_PIPE)
        );
    }
}
