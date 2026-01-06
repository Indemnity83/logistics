package com.logistics.item;

import com.logistics.LogisticsMod;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class LogisticsItems {
    public static final Item WRENCH = registerItem("wrench",
        new WrenchItem(new Item.Settings().maxCount(1)));

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(LogisticsMod.MOD_ID, name), item);
    }

    public static void initialize() {
        LogisticsMod.LOGGER.info("Registering items");
    }
}
