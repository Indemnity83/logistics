package com.logistics.core.item;

import com.logistics.core.machine.upgrade.MachineOperations;
import com.logistics.core.machine.upgrade.OperationKey;
import com.logistics.core.machine.upgrade.UpgradeItem;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** Widens a machine's work area by {@link #RANGE_BONUS} blocks on every side per installed copy. */
public class RangeUpgradeItem extends Item implements UpgradeItem {

    /** Blocks added to each side of the work area per copy: 9x9 -> 13x13 -> 17x17. */
    public static final int RANGE_BONUS = 2;

    /** Copies one upgrade slot holds. */
    public static final int MAX_PER_SLOT = 2;

    public RangeUpgradeItem(Properties properties) {
        super(properties);
    }

    @Override
    public int rangeBonus(OperationKey op) {
        return op == MachineOperations.WORK_AREA ? RANGE_BONUS : 0;
    }

    @Override
    public int maxPerSlot() {
        return MAX_PER_SLOT;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay tooltipDisplay,
            Consumer<Component> consumer,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, consumer, flag);
        consumer.accept(Component.translatable("tooltip.logistics.range_upgrade").withStyle(ChatFormatting.GRAY));
    }
}
