package com.logistics.item;

import java.util.ArrayList;
import java.util.List;

import com.logistics.LogisticsMod;
import com.logistics.block.LogisticsBlocks;
import com.logistics.block.PipeBlock;
import com.logistics.marker.MarkerBlocks;
import com.logistics.pipe.Pipe;
import com.logistics.quarry.QuarryBlocks;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;

public final class LogisticsItemGroups {
    private LogisticsItemGroups() {}

    public static final ItemGroup LOGISTICS_TRANSPORT = Registry.register(
            Registries.ITEM_GROUP,
            Identifier.of(LogisticsMod.MOD_ID, "logistics_transport"),
            FabricItemGroup.builder()
                    .displayName(Text.translatable("itemgroup.logistics.transport"))
                    .icon(() -> new ItemStack(LogisticsBlocks.COPPER_TRANSPORT_PIPE))
                    .entries((displayContext, entries) -> {
                        entries.add(LogisticsItems.WRENCH);
                        // Gears
                        entries.add(LogisticsItems.WOODEN_GEAR);
                        entries.add(LogisticsItems.STONE_GEAR);
                        entries.add(LogisticsItems.COPPER_GEAR);
                        entries.add(LogisticsItems.IRON_GEAR);
                        entries.add(LogisticsItems.GOLD_GEAR);
                        entries.add(LogisticsItems.DIAMOND_GEAR);
                        entries.add(LogisticsItems.NETHERITE_GEAR);
                        // Marking fluids
                        for (DyeColor color : DyeColor.values()) {
                            entries.add(LogisticsItems.getMarkingFluidItem(color));
                        }
                        entries.add(LogisticsBlocks.STONE_TRANSPORT_PIPE);
                        entries.add(LogisticsBlocks.ITEM_PASSTHROUGH_PIPE);
                        entries.add(LogisticsBlocks.COPPER_TRANSPORT_PIPE);
                        addPipeVariants(LogisticsBlocks.COPPER_TRANSPORT_PIPE, entries);
                        entries.add(LogisticsBlocks.ITEM_EXTRACTOR_PIPE);
                        entries.add(LogisticsBlocks.ITEM_MERGER_PIPE);
                        entries.add(LogisticsBlocks.GOLD_TRANSPORT_PIPE);
                        entries.add(LogisticsBlocks.ITEM_FILTER_PIPE);
                        entries.add(LogisticsBlocks.ITEM_INSERTION_PIPE);
                        entries.add(LogisticsBlocks.ITEM_VOID_PIPE);
                        entries.add(QuarryBlocks.QUARRY);
                        entries.add(MarkerBlocks.MARKER);
                    })
                    .build());

    private static void addPipeVariants(net.minecraft.block.Block block, ItemGroup.Entries entries) {
        if (!(block instanceof PipeBlock pipeBlock)) return;

        Pipe pipe = pipeBlock.getPipe();
        if (pipe == null) return;

        ItemStack baseStack = new ItemStack(block);
        List<ItemStack> variants = new ArrayList<>();
        pipe.appendCreativeMenuVariants(variants, baseStack);

        for (ItemStack variant : variants) {
            entries.add(variant);
        }
    }

    public static void initialize() {
        LogisticsMod.LOGGER.info("Registering item groups");
    }
}
