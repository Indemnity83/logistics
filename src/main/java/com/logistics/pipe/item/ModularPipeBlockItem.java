package com.logistics.pipe.item;

import com.logistics.pipe.Pipe;
import com.logistics.pipe.block.PipeBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * A BlockItem that uses module hooks for display names.
 * Allows modules to provide custom name suffixes based on item component state.
 */
public class ModularPipeBlockItem extends BlockItem {

    public ModularPipeBlockItem(Block block, Properties settings) {
        super(block, settings);
    }

    @Override
    public Component getName(ItemStack stack) {
        Block block = getBlock();
        if (!(block instanceof PipeBlock pipeBlock)) {
            return super.getName(stack);
        }

        Pipe pipe = pipeBlock.getPipe();
        if (pipe == null) {
            return super.getName(stack);
        }

        String suffix = pipe.getItemNameSuffixFromComponents(stack);
        if (suffix.isEmpty()) {
            return super.getName(stack);
        }

        return Component.translatable(block.getDescriptionId() + suffix);
    }
}
