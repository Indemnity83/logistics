package com.logistics.item;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import com.logistics.LogisticsMod;
import com.logistics.block.LogisticsBlocks;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public final class LogisticsItems {
    private LogisticsItems() {}

    private static final int MARKING_FLUID_USES = 16;

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


    private static final Map<Item, DyeColor> MARKING_FLUID_ITEM_COLORS = new HashMap<>();
    private static final Map<DyeColor, Item> MARKING_FLUID_ITEMS = registerMarkingFluidItems();

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(LogisticsMod.MOD_ID, name), item);
    }

    public static Item getMarkingFluidItem(DyeColor color) {
        return MARKING_FLUID_ITEMS.get(color);
    }

    /**
     * Resolve the marking fluid dye color from a stack.
     */
    public static @Nullable DyeColor getMarkingFluidColor(ItemStack stack) {
        return MARKING_FLUID_ITEM_COLORS.get(stack.getItem());
    }

    public static boolean isWrench(ItemStack stack) {
        return stack.getItem() == WRENCH;
    }

    public static void initialize() {
        LogisticsMod.LOGGER.info("Registering items");

        registerLegacyAliases();
    }

    private static Map<DyeColor, Item> registerMarkingFluidItems() {
        Map<DyeColor, Item> items = new EnumMap<>(DyeColor.class);
        for (DyeColor color : DyeColor.values()) {
            String name = "marking_fluid_" + color.getId();
            Item item = new Item(new Item.Settings()
                    .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(LogisticsMod.MOD_ID, name)))
                    .maxCount(1)
                    .maxDamage(MARKING_FLUID_USES));
            items.put(color, registerItem(name, item));
            MARKING_FLUID_ITEM_COLORS.put(item, color);
        }
        return Collections.unmodifiableMap(items);
    }

    private static void registerLegacyAliases() {
        // pre-v0.2.0
        Registries.ITEM.addAlias(
                Identifier.of(LogisticsMod.MOD_ID, "item_sensor_pipe"),
                Registries.BLOCK.getId(LogisticsBlocks.COPPER_TRANSPORT_PIPE));
    }
}
