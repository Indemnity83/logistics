package com.logistics.pipe.item;

import com.logistics.pipe.block.PipeBlock;
import com.logistics.pipe.Pipe;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

/**
 * A BlockItem that uses module hooks for display names.
 * Allows modules to provide custom name suffixes based on item component state.
 */
public class ModularPipeBlockItem extends BlockItem {

    public ModularPipeBlockItem(Block block, Settings settings) {
        super(block, settings);
    }

    @Override
    public Text getName(ItemStack stack) {
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

        return Text.translatable(block.getTranslationKey() + suffix);
    }
}
