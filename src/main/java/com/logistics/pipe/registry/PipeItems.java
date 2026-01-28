package com.logistics.pipe.registry;

import com.logistics.LogisticsMod;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class PipeItems {
    private PipeItems() {}

    private static final String DOMAIN = "pipe/";
    private static final int MARKING_FLUID_USES = 16;

    private static final Map<Item, DyeColor> MARKING_FLUID_ITEM_COLORS = new HashMap<>();
    private static final Map<DyeColor, Item> MARKING_FLUID_ITEMS = registerMarkingFluidItems();

    private static Item registerItem(String name, Item item) {
        return Registry.register(BuiltInRegistries.ITEM, id(name), item);
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

    public static void initialize() {
        LogisticsMod.LOGGER.info("Registering pipe items");

        registerLegacyAliases();
    }

    private static Map<DyeColor, Item> registerMarkingFluidItems() {
        Map<DyeColor, Item> items = new EnumMap<>(DyeColor.class);
        for (DyeColor color : DyeColor.values()) {
            String name = "marking_fluid_" + color.getSerializedName();
            Item item = new Item(new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, id(name)))
                    .stacksTo(1)
                    .durability(MARKING_FLUID_USES));
            items.put(color, registerItem(name, item));
            MARKING_FLUID_ITEM_COLORS.put(item, color);
        }
        return Collections.unmodifiableMap(items);
    }

    private static void registerLegacyAliases() {
        // v0.2 => v0.3
        for (DyeColor color : DyeColor.values()) {
            String name = "marking_fluid_" + color.getSerializedName();
            BuiltInRegistries.ITEM.addAlias(Identifier.fromNamespaceAndPath(LogisticsMod.MOD_ID, name), id(name));
        }
    }

    private static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(LogisticsMod.MOD_ID, DOMAIN + name);
    }
}
