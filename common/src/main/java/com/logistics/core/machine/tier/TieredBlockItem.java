package com.logistics.core.machine.tier;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

/** Block item of a {@link TieredMachine}; shows the tier its stack carries, if any. */
public class TieredBlockItem extends BlockItem {

    public static final String TOOLTIP_TIER = "tooltip.logistics.machine.tier";

    public TieredBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltipComponents,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipComponents, flag);
        // Hidden while nothing can change a tier.
        // MachineTier tier = stack.get(LogisticsCore.DATA.MACHINE_TIER);
        // if (tier != null) {
        //     tooltipComponents.add(Component.translatable(TOOLTIP_TIER, Component.translatable(tier.translationKey()))
        //             .withStyle(ChatFormatting.GRAY));
        // }
    }
}
