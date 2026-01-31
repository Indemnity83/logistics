package com.logistics.core.registry;

import com.logistics.LogisticsMod;
import com.logistics.core.item.WrenchItem;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public final class CoreItems {
    private CoreItems() {}

    private static final String DOMAIN = "core/";

    public static final Item WRENCH = registerItem(
            "wrench",
            new WrenchItem(new Item.Settings()
                    .registryKey(RegistryKey.of(RegistryKeys.ITEM, id("wrench")))
                    .maxCount(1)));

    // Gears (tiered crafting components)
    public static final Item WOODEN_GEAR = registerItem(
            "wooden_gear",
            new Item(new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, id("wooden_gear")))));

    public static final Item STONE_GEAR = registerItem(
            "stone_gear",
            new Item(new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, id("stone_gear")))));

    public static final Item COPPER_GEAR = registerItem(
            "copper_gear",
            new Item(new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, id("copper_gear")))));

    public static final Item IRON_GEAR = registerItem(
            "iron_gear", new Item(new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, id("iron_gear")))));

    public static final Item GOLD_GEAR = registerItem(
            "gold_gear", new Item(new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, id("gold_gear")))));

    public static final Item DIAMOND_GEAR = registerItem(
            "diamond_gear",
            new Item(new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, id("diamond_gear")))));

    public static final Item NETHERITE_GEAR = registerItem(
            "netherite_gear",
            new Item(new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, id("netherite_gear")))));

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, id(name), item);
    }

    private static Identifier id(String name) {
        return Identifier.of(LogisticsMod.MOD_ID, DOMAIN + name);
    }

    public static boolean isWrench(ItemStack stack) {
        return stack.getItem() == WRENCH;
    }

    public static void initialize() {
        LogisticsMod.LOGGER.info("Registering core items");
        registerLegacyAliases();
    }

    private static void registerLegacyAliases() {
        // v0.2 => v0.3
        registerAlias("wrench");
        registerAlias("wooden_gear");
        registerAlias("stone_gear");
        registerAlias("copper_gear");
        registerAlias("iron_gear");
        registerAlias("gold_gear");
        registerAlias("diamond_gear");
        registerAlias("netherite_gear");
    }

    private static void registerAlias(String name) {
        Registries.ITEM.addAlias(Identifier.of(LogisticsMod.MOD_ID, name), id(name));
    }
}
