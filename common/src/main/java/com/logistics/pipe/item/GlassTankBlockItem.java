package com.logistics.pipe.item;

import com.logistics.pipe.tank.TankTier;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

/** Block item for the Glass Tank that shows its capacity on hover. */
public class GlassTankBlockItem extends BlockItem {

    public GlassTankBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltipComponents,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipComponents, flag);
        tooltipComponents.add(Component.translatable("tooltip.logistics.fluid.tank_capacity", TankTier.GLASS.buckets())
                .withStyle(ChatFormatting.GRAY));
    }
}
