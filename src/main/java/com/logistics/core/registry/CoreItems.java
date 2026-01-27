package com.logistics.core.registry;

import com.logistics.LogisticsMod;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public final class CoreItems {
    private CoreItems() {}

    public static final Item WRENCH = registerItem(
            "wrench",
            new Item(new Item.Settings()
                    .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(LogisticsMod.MOD_ID, "wrench")))
                    .maxCount(1)));

    // Gears (tiered crafting components)
    public static final Item WOODEN_GEAR = registerItem(
            "wooden_gear",
            new Item(new Item.Settings()
                    .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(LogisticsMod.MOD_ID, "wooden_gear")))));

    public static final Item STONE_GEAR = registerItem(
            "stone_gear",
            new Item(new Item.Settings()
                    .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(LogisticsMod.MOD_ID, "stone_gear")))));

    public static final Item COPPER_GEAR = registerItem(
            "copper_gear",
            new Item(new Item.Settings()
                    .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(LogisticsMod.MOD_ID, "copper_gear")))));

    public static final Item IRON_GEAR = registerItem(
            "iron_gear",
            new Item(new Item.Settings()
                    .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(LogisticsMod.MOD_ID, "iron_gear")))));

    public static final Item GOLD_GEAR = registerItem(
            "gold_gear",
            new Item(new Item.Settings()
                    .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(LogisticsMod.MOD_ID, "gold_gear")))));

    public static final Item DIAMOND_GEAR = registerItem(
            "diamond_gear",
            new Item(new Item.Settings()
                    .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(LogisticsMod.MOD_ID, "diamond_gear")))));

    public static final Item NETHERITE_GEAR = registerItem(
            "netherite_gear",
            new Item(new Item.Settings()
                    .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(LogisticsMod.MOD_ID, "netherite_gear")))));

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(LogisticsMod.MOD_ID, name), item);
    }

    public static boolean isWrench(ItemStack stack) {
        return stack.getItem() == WRENCH;
    }

    public static void initialize() {
        LogisticsMod.LOGGER.info("Registering core items");
    }
}
