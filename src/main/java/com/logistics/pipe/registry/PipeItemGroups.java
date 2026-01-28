package com.logistics.pipe.registry;

import com.logistics.core.registry.CoreItemGroups;
import com.logistics.pipe.Pipe;
import com.logistics.pipe.block.PipeBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DyeColor;

public final class PipeItemGroups {
    private PipeItemGroups() {}

    public static void registerProviders() {
        CoreItemGroups.registerEntries(PipeItemGroups::addEntries);
    }

    private static void addEntries(ItemGroup.DisplayContext displayContext, ItemGroup.Entries entries) {
        // Marking fluids
        for (DyeColor color : DyeColor.values()) {
            entries.add(PipeItems.getMarkingFluidItem(color));
        }

        // Pipes
        entries.add(PipeBlocks.STONE_TRANSPORT_PIPE);
        entries.add(PipeBlocks.ITEM_PASSTHROUGH_PIPE);
        entries.add(PipeBlocks.COPPER_TRANSPORT_PIPE);
        addPipeVariants(PipeBlocks.COPPER_TRANSPORT_PIPE, entries);
        entries.add(PipeBlocks.ITEM_EXTRACTOR_PIPE);
        entries.add(PipeBlocks.ITEM_MERGER_PIPE);
        entries.add(PipeBlocks.GOLD_TRANSPORT_PIPE);
        entries.add(PipeBlocks.ITEM_FILTER_PIPE);
        entries.add(PipeBlocks.ITEM_INSERTION_PIPE);
        entries.add(PipeBlocks.ITEM_VOID_PIPE);
    }

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
        // No-op: registration is handled via CoreItemGroups providers.
    }
}
