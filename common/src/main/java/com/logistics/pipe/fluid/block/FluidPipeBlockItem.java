package com.logistics.pipe.fluid.block;

import com.logistics.core.LogisticsConfig;
import com.logistics.pipe.fluid.FluidPipe;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
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
            TooltipDisplay tooltipDisplay,
            Consumer<Component> consumer,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, consumer, flag);

        if (!(getBlock() instanceof FluidPipeBlock block) || block.fluidPipe() == null) {
            return;
        }
        FluidPipe def = block.fluidPipe();
        LogisticsConfig.FluidPipeConfig cfg = LogisticsConfig.get().fluidPipe;

        consumer.accept(Component.translatable("tooltip.logistics.fluid." + def.modelBase())
                .withStyle(ChatFormatting.GRAY));

        consumer.accept(Component.translatable("tooltip.logistics.fluid.capacity", (int) def.capacity(cfg))
                .withStyle(ChatFormatting.GRAY));
        // Extractors are paced by engine power, not a fixed rate; void destroys rather than transfers.
        if (!def.isExtractor() && !def.isVoid()) {
            consumer.accept(Component.translatable("tooltip.logistics.fluid.transfer", (int) def.transferRate(cfg))
                    .withStyle(ChatFormatting.GRAY));
        }
        consumer.accept(Component.translatable("tooltip.logistics.fluid.no_item_pipes")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
