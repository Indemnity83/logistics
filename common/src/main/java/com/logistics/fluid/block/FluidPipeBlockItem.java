package com.logistics.fluid.block;

import com.logistics.core.LogisticsConfig;
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
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay tooltipDisplay,
            Consumer<Component> consumer,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, consumer, flag);

        FluidPipeKind kind = getBlock() instanceof FluidPipeBlock pipe ? pipe.kind() : FluidPipeKind.COPPER;
        LogisticsConfig.FluidPipeConfig cfg = LogisticsConfig.get().fluidPipe;

        String roleKey = kind.isExtractor()
                ? "tooltip.logistics.fluid.fluid_extractor_pipe"
                : "tooltip.logistics.fluid.copper_fluid_pipe";
        consumer.accept(Component.translatable(roleKey).withStyle(ChatFormatting.GRAY));

        int capacity = kind.isExtractor() ? cfg.woodenCapacity : cfg.copperCapacity;
        consumer.accept(Component.translatable("tooltip.logistics.fluid.capacity", capacity)
                .withStyle(ChatFormatting.GRAY));
        if (!kind.isExtractor()) {
            consumer.accept(Component.translatable("tooltip.logistics.fluid.transfer", cfg.copperTransferRate)
                    .withStyle(ChatFormatting.GRAY));
        }
        consumer.accept(Component.translatable("tooltip.logistics.fluid.no_item_pipes")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
