package com.logistics.pipe.registry;

import com.logistics.core.registry.CoreItemGroups;
import com.logistics.pipe.Pipe;
import com.logistics.pipe.block.PipeBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

public final class PipeItemGroups {
    private PipeItemGroups() {}

    public static void registerProviders() {
        CoreItemGroups.registerEntries(PipeItemGroups::addEntries);
    }

    private static void addEntries(CreativeModeTab.ItemDisplayParameters displayContext, CreativeModeTab.Output entries) {
        // Marking fluids
        for (DyeColor color : DyeColor.values()) {
            entries.accept(PipeItems.getMarkingFluidItem(color));
        }

        // Pipes
        entries.accept(PipeBlocks.STONE_TRANSPORT_PIPE);
        entries.accept(PipeBlocks.ITEM_PASSTHROUGH_PIPE);
        entries.accept(PipeBlocks.COPPER_TRANSPORT_PIPE);
        addPipeVariants(PipeBlocks.COPPER_TRANSPORT_PIPE, entries);
        entries.accept(PipeBlocks.ITEM_EXTRACTOR_PIPE);
        entries.accept(PipeBlocks.ITEM_MERGER_PIPE);
        entries.accept(PipeBlocks.GOLD_TRANSPORT_PIPE);
        entries.accept(PipeBlocks.ITEM_FILTER_PIPE);
        entries.accept(PipeBlocks.ITEM_INSERTION_PIPE);
        entries.accept(PipeBlocks.ITEM_VOID_PIPE);
    }

    private static void addPipeVariants(net.minecraft.world.level.block.Block block, CreativeModeTab.Output entries) {
        if (!(block instanceof PipeBlock pipeBlock)) return;

        Pipe pipe = pipeBlock.getPipe();
        if (pipe == null) return;

        ItemStack baseStack = new ItemStack(block);
        List<ItemStack> variants = new ArrayList<>();
        pipe.appendCreativeMenuVariants(variants, baseStack);

        for (ItemStack variant : variants) {
            entries.accept(variant);
        }
    }

    public static void initialize() {
        // No-op: registration is handled via CoreItemGroups providers.
    }
}
