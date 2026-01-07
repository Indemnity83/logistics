package com.logistics.item;

import com.logistics.LogisticsMod;
import com.logistics.block.LogisticsBlocks;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class LogisticsItemGroups {
    public static final ItemGroup LOGISTICS_TRANSPORT = Registry.register(
        Registries.ITEM_GROUP,
        Identifier.of(LogisticsMod.MOD_ID, "logistics_transport"),
        FabricItemGroup.builder()
            .displayName(Text.translatable("itemgroup.logistics.transport"))
            .icon(() -> new ItemStack(LogisticsBlocks.STONE_PIPE))
            .entries((displayContext, entries) -> {
                entries.add(LogisticsItems.WRENCH);
                entries.add(LogisticsBlocks.COBBLESTONE_PIPE);
                entries.add(LogisticsBlocks.STONE_PIPE);
                entries.add(LogisticsBlocks.WOOD_PIPE);
                entries.add(LogisticsBlocks.IRON_PIPE);
                entries.add(LogisticsBlocks.GOLD_PIPE);
                entries.add(LogisticsBlocks.DIAMOND_PIPE);
                entries.add(LogisticsBlocks.COPPER_PIPE);
                entries.add(LogisticsBlocks.VOID_PIPE);
            })
            .build()
    );

    public static void initialize() {
        LogisticsMod.LOGGER.info("Registering item groups");
    }
}
