package com.logistics.item;

import com.logistics.LogisticsMod;
import com.logistics.block.LogisticsBlocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.DyeColor;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class LogisticsItems {
    private static final int DYKEM_USES = 16;

    public static final Item WRENCH = registerItem("wrench",
        new WrenchItem(new Item.Settings()
            .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(LogisticsMod.MOD_ID, "wrench")))
            .maxCount(1)));

    private static final Map<Item, DyeColor> DYKEM_ITEM_COLORS = new HashMap<>();
    private static final Map<DyeColor, Item> DYKEM_ITEMS = registerDykemItems();

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(LogisticsMod.MOD_ID, name), item);
    }

    public static Item getDykemItem(DyeColor color) {
        return DYKEM_ITEMS.get(color);
    }

    public static DyeColor getDykemColor(ItemStack stack) {
        return DYKEM_ITEM_COLORS.get(stack.getItem());
    }

    public static void initialize() {
        LogisticsMod.LOGGER.info("Registering items");

        registerLegacyAliases();
    }

    private static Map<DyeColor, Item> registerDykemItems() {
        Map<DyeColor, Item> items = new EnumMap<>(DyeColor.class);
        for (DyeColor color : DyeColor.values()) {
            String name = "dykem_" + color.getId();
            Item item = new Item(new Item.Settings()
                .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(LogisticsMod.MOD_ID, name)))
                .maxCount(1)
                .maxDamage(DYKEM_USES));
            items.put(color, registerItem(name, item));
            DYKEM_ITEM_COLORS.put(item, color);
        }
        return Collections.unmodifiableMap(items);
    }

    private static void registerLegacyAliases() {
        // pre-v0.2.0
        Registries.ITEM.addAlias(
            Identifier.of(LogisticsMod.MOD_ID, "item_sensor_pipe"),
            Registries.BLOCK.getId(LogisticsBlocks.COPPER_TRANSPORT_PIPE)
        );
    }
}
