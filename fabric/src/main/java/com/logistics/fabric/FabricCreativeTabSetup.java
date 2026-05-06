package com.logistics.fabric;

import com.logistics.LogisticsCore;
import com.logistics.LogisticsMod;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class FabricCreativeTabSetup {
    private FabricCreativeTabSetup() {}

    public static void register() {
        LogisticsCore.CREATIVE_TAB.LOGISTICS_TRANSPORT = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            LogisticsMod.modId("logistics_transport").toIdentifier(),
            FabricCreativeModeTab.builder()
                .title(Component.literal("Logistics"))
                .icon(() -> new ItemStack(LogisticsCore.ITEM.IRON_GEAR))
                .displayItems((params, entries) -> LogisticsCore.CREATIVE_TAB.populateEntries(entries))
                .build()
        );

        // Add storage blocks to Building Blocks tab
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(entries -> {
            entries.insertAfter(Items.COAL_BLOCK, LogisticsCore.BLOCK.APATITE_BLOCK);
            entries.insertBefore(Items.IRON_BLOCK, LogisticsCore.BLOCK.TIN_BLOCK);
            entries.insertAfter(Items.IRON_BLOCK, LogisticsCore.BLOCK.BRONZE_BLOCK);
        });

        // Add materials to Ingredients tab
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(entries -> {
            // Raw materials
            entries.insertBefore(Items.RAW_IRON, LogisticsCore.ITEM.RAW_TIN);

            // Ingots
            entries.insertBefore(Items.IRON_INGOT, LogisticsCore.ITEM.TIN_INGOT);
            entries.insertAfter(LogisticsCore.ITEM.TIN_INGOT, LogisticsCore.ITEM.BRONZE_INGOT);

            // Nuggets
            entries.insertBefore(Items.IRON_NUGGET, LogisticsCore.ITEM.TIN_NUGGET);
            entries.insertAfter(LogisticsCore.ITEM.TIN_NUGGET, LogisticsCore.ITEM.BRONZE_NUGGET);

            // Apatite
            entries.insertBefore(Items.AMETHYST_SHARD, LogisticsCore.ITEM.APATITE);

            // Intermediate Crafting Items
            entries.insertBefore(Items.HEAVY_CORE, LogisticsCore.ITEM.STURDY_CASING);
            entries.insertAfter(LogisticsCore.ITEM.STURDY_CASING, LogisticsCore.ITEM.MACHINE_CORE);
            entries.insertAfter(LogisticsCore.ITEM.MACHINE_CORE, LogisticsCore.ITEM.WOODEN_GEAR);
            entries.insertAfter(LogisticsCore.ITEM.WOODEN_GEAR, LogisticsCore.ITEM.STONE_GEAR);
            entries.insertAfter(LogisticsCore.ITEM.STONE_GEAR, LogisticsCore.ITEM.COPPER_GEAR);
            entries.insertAfter(LogisticsCore.ITEM.COPPER_GEAR, LogisticsCore.ITEM.TIN_GEAR);
            entries.insertAfter(LogisticsCore.ITEM.TIN_GEAR, LogisticsCore.ITEM.IRON_GEAR);
            entries.insertAfter(LogisticsCore.ITEM.IRON_GEAR, LogisticsCore.ITEM.BRONZE_GEAR);
            entries.insertAfter(LogisticsCore.ITEM.BRONZE_GEAR, LogisticsCore.ITEM.GOLD_GEAR);
            entries.insertAfter(LogisticsCore.ITEM.GOLD_GEAR, LogisticsCore.ITEM.DIAMOND_GEAR);
            entries.insertAfter(LogisticsCore.ITEM.DIAMOND_GEAR, LogisticsCore.ITEM.NETHERITE_GEAR);

            // Valves — after netherite gear
            Item[] valves = {
                LogisticsCore.ITEM.WOODEN_VALVE,
                LogisticsCore.ITEM.COPPER_VALVE, LogisticsCore.ITEM.BRONZE_VALVE,
                LogisticsCore.ITEM.IRON_VALVE, LogisticsCore.ITEM.GOLD_VALVE, LogisticsCore.ITEM.DIAMOND_VALVE,
                LogisticsCore.ITEM.OBSIDIAN_VALVE, LogisticsCore.ITEM.BLAZING_VALVE, LogisticsCore.ITEM.EMERALD_VALVE,
                LogisticsCore.ITEM.APATITE_VALVE, LogisticsCore.ITEM.LAPIS_VALVE, LogisticsCore.ITEM.ENDER_VALVE,
                LogisticsCore.ITEM.NETHERITE_VALVE
            };
            Item prev = LogisticsCore.ITEM.NETHERITE_GEAR;
            for (Item item : valves) {
                entries.insertAfter(prev, item);
                prev = item;
            }
        });

        // Add dusts, chips, cores to Ingredients tab — after bronze_ingot
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(entries -> {
            Item[] intermediates = {
                LogisticsCore.ITEM.APATITE_DUST,
                LogisticsCore.ITEM.IRON_DUST, LogisticsCore.ITEM.COPPER_DUST, LogisticsCore.ITEM.TIN_DUST, LogisticsCore.ITEM.BRONZE_DUST,
                LogisticsCore.ITEM.GOLD_DUST, LogisticsCore.ITEM.LAPIS_DUST, LogisticsCore.ITEM.QUARTZ_DUST, LogisticsCore.ITEM.COAL_DUST,
                LogisticsCore.ITEM.AMETHYST_DUST, LogisticsCore.ITEM.DIAMOND_DUST, LogisticsCore.ITEM.EMERALD_DUST,
                LogisticsCore.ITEM.NETHERITE_DUST, LogisticsCore.ITEM.OBSIDIAN_DUST, LogisticsCore.ITEM.ENDER_DUST,
                LogisticsCore.ITEM.ECHO_DUST, LogisticsCore.ITEM.PRISMARINE_DUST,
                LogisticsCore.ITEM.SILICON_MIX, LogisticsCore.ITEM.SILICON_WAFER, LogisticsCore.ITEM.FLOUR, LogisticsCore.ITEM.WOOD_PULP,
                LogisticsCore.ITEM.CARBON_CHIP, LogisticsCore.ITEM.REDSTONE_CHIP, LogisticsCore.ITEM.AMETHYST_CHIP, LogisticsCore.ITEM.ECHO_CHIP,
                LogisticsCore.ITEM.WOODEN_CORE,
                LogisticsCore.ITEM.COPPER_CORE, LogisticsCore.ITEM.BRONZE_CORE,
                LogisticsCore.ITEM.IRON_CORE, LogisticsCore.ITEM.GOLD_CORE, LogisticsCore.ITEM.LAPIS_CORE,
                LogisticsCore.ITEM.APATITE_CORE, LogisticsCore.ITEM.DIAMOND_CORE, LogisticsCore.ITEM.EMERALD_CORE,
                LogisticsCore.ITEM.BLAZING_CORE, LogisticsCore.ITEM.NETHERITE_CORE,
                LogisticsCore.ITEM.OBSIDIAN_CORE, LogisticsCore.ITEM.ENDER_CORE
            };
            Item anchor = LogisticsCore.ITEM.BRONZE_INGOT;
            for (Item item : intermediates) {
                entries.insertAfter(anchor, item);
                anchor = item;
            }
        });

        // Add ore blocks to Natural Blocks tab
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(entries -> {
            // Tin ores
            entries.insertAfter(Items.DEEPSLATE_COAL_ORE, LogisticsCore.BLOCK.TIN_ORE);
            entries.insertAfter(LogisticsCore.BLOCK.TIN_ORE, LogisticsCore.BLOCK.DEEPSLATE_TIN_ORE);

            // Apatite ore
            entries.insertBefore(Items.AMETHYST_BLOCK, LogisticsCore.BLOCK.APATITE_ORE);

            // Raw tin block
            entries.insertBefore(Items.RAW_IRON_BLOCK, LogisticsCore.BLOCK.RAW_TIN_BLOCK);
        });
    }
}
