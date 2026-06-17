package com.logistics.pipe.item;

import com.logistics.core.LogisticsConfig;
import com.logistics.pipe.FluidPipe;
import com.logistics.pipe.block.FluidPipeBlock;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

/** Block item for fluid pipes that explains role, capacity, and transfer rate on hover. */
public class FluidPipeBlockItem extends BlockItem {

    public FluidPipeBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        Block block = getBlock();
        if (!(block instanceof FluidPipeBlock pipe) || pipe.fluidPipe() == null) {
            return super.getName(stack);
        }
        String suffix = pipe.fluidPipe().getItemNameSuffixFromComponents(stack);
        if (suffix.isEmpty()) {
            return super.getName(stack);
        }
        return Component.translatable(block.getDescriptionId() + suffix);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltipComponents,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipComponents, flag);

        if (!(getBlock() instanceof FluidPipeBlock block) || block.fluidPipe() == null) {
            return;
        }
        FluidPipe def = block.fluidPipe();
        LogisticsConfig.FluidPipeConfig cfg = LogisticsConfig.get().fluidPipe;

        tooltipComponents.add(Component.translatable("tooltip.logistics.fluid." + def.modelBase())
                .withStyle(ChatFormatting.GRAY));

        tooltipComponents.add(Component.translatable("tooltip.logistics.fluid.capacity", (int) def.capacity(cfg))
                .withStyle(ChatFormatting.GRAY));
        // Extractors are paced by engine power, not a fixed rate; void destroys rather than transfers.
        if (!def.isExtractor() && !def.isVoid()) {
            tooltipComponents.add(Component.translatable("tooltip.logistics.fluid.transfer", (int) def.transferRate(cfg))
                    .withStyle(ChatFormatting.GRAY));
        }
        tooltipComponents.add(Component.translatable("tooltip.logistics.fluid.no_item_pipes")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
