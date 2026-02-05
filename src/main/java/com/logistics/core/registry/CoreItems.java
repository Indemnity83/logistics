package com.logistics.core.registry;

import com.logistics.LogisticsMod;
import com.logistics.core.item.ProbeItem;
import com.logistics.core.item.WrenchItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;

public final class CoreItems {
    private CoreItems() {}

    private static final String DOMAIN = "core/";

    public static final Item WRENCH = registerItem(
            "wrench",
            new WrenchItem(new Item.Properties()
                    .setId(ResourceKey.create(net.minecraft.core.registries.Registries.ITEM, id("wrench")))
                    .stacksTo(1)));

    public static final Item PROBE = registerItem(
            "probe",
            new ProbeItem(new Item.Properties()
                    .setId(ResourceKey.create(net.minecraft.core.registries.Registries.ITEM, id("probe")))
                    .stacksTo(1)));

    // Gears (tiered crafting components)
    public static final Item WOODEN_GEAR = registerItem(
            "wooden_gear",
            new Item(new Item.Properties().setId(ResourceKey.create(net.minecraft.core.registries.Registries.ITEM, id("wooden_gear")))));

    public static final Item STONE_GEAR = registerItem(
            "stone_gear",
            new Item(new Item.Properties().setId(ResourceKey.create(net.minecraft.core.registries.Registries.ITEM, id("stone_gear")))));

    public static final Item COPPER_GEAR = registerItem(
            "copper_gear",
            new Item(new Item.Properties().setId(ResourceKey.create(net.minecraft.core.registries.Registries.ITEM, id("copper_gear")))));

    public static final Item IRON_GEAR = registerItem(
            "iron_gear", new Item(new Item.Properties().setId(ResourceKey.create(net.minecraft.core.registries.Registries.ITEM, id("iron_gear")))));

    public static final Item GOLD_GEAR = registerItem(
            "gold_gear", new Item(new Item.Properties().setId(ResourceKey.create(net.minecraft.core.registries.Registries.ITEM, id("gold_gear")))));

    public static final Item DIAMOND_GEAR = registerItem(
            "diamond_gear",
            new Item(new Item.Properties().setId(ResourceKey.create(net.minecraft.core.registries.Registries.ITEM, id("diamond_gear")))));

    public static final Item NETHERITE_GEAR = registerItem(
            "netherite_gear",
            new Item(new Item.Properties().setId(ResourceKey.create(net.minecraft.core.registries.Registries.ITEM, id("netherite_gear")))));

    private static Item registerItem(String name, Item item) {
        return Registry.register(BuiltInRegistries.ITEM, id(name), item);
    }

    private static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(LogisticsMod.MOD_ID, DOMAIN + name);
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
        BuiltInRegistries.ITEM.addAlias(Identifier.fromNamespaceAndPath(LogisticsMod.MOD_ID, name), id(name));
    }
}
