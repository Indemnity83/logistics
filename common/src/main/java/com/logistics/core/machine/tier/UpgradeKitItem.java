package com.logistics.core.machine.tier;

import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Raises a {@link TieredMachine} one step, to {@link #target()}, when used on it. The kit fits only
 * a machine one tier below its target and is consumed only once the tier has changed.
 */
public class UpgradeKitItem extends Item {

    public static final String TOOLTIP_UPGRADES = "tooltip.logistics.upgrade_kit.upgrades";
    public static final String TOOLTIP_FARM = "tooltip.logistics.upgrade_kit.farm";
    public static final String MESSAGE_UPGRADED = "message.logistics.upgrade_kit.upgraded";
    public static final String MESSAGE_NEEDS_PREVIOUS = "message.logistics.upgrade_kit.needs_previous";
    public static final String MESSAGE_ALREADY = "message.logistics.upgrade_kit.already";

    /** What using a kit on a machine of a given tier does. */
    public enum Outcome {
        /** The machine is one tier below the kit's target: upgrade it. */
        APPLY,
        /** The machine is more than one tier below the target: it needs the previous kit first. */
        NEEDS_PREVIOUS,
        /** The machine is already at or above the target. */
        ALREADY
    }

    private final MachineTier target;
    private final MachineTier required;

    public UpgradeKitItem(MachineTier target, Properties properties) {
        super(properties);
        this.target = Objects.requireNonNull(target);
        this.required = target.previous()
                .orElseThrow(() -> new IllegalArgumentException("No kit can upgrade to " + target.id()));
    }

    public MachineTier target() {
        return target;
    }

    /** The outcome of using a kit for {@code target} on a machine at {@code current}. */
    public static Outcome decide(MachineTier current, MachineTier target) {
        if (current.rank() >= target.rank()) {
            return Outcome.ALREADY;
        }
        return current.next().orElseThrow() == target ? Outcome.APPLY : Outcome.NEEDS_PREVIOUS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level.getBlockEntity(context.getClickedPos()) instanceof TieredMachine machine)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        Player player = context.getPlayer();
        Outcome outcome = decide(machine.tier(), target);
        if (outcome == Outcome.APPLY && machine.tierComponent().setTier(target)) {
            context.getItemInHand().consume(1, player);
            level.playSound(null, context.getClickedPos(), SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            notify(player, machine.upgradeFeedback(target)
                    .orElseGet(() -> Component.translatable(MESSAGE_UPGRADED, tierName(target))));
            return InteractionResult.SUCCESS_SERVER;
        }
        notify(player, outcome == Outcome.NEEDS_PREVIOUS
                ? Component.translatable(MESSAGE_NEEDS_PREVIOUS, tierName(required))
                : Component.translatable(MESSAGE_ALREADY, tierName(target)));
        return InteractionResult.FAIL;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay tooltipDisplay,
            Consumer<Component> consumer,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, consumer, flag);
        consumer.accept(Component.translatable(TOOLTIP_UPGRADES, tierName(required), tierName(target))
                .withStyle(ChatFormatting.GRAY));
        consumer.accept(Component.translatable(TOOLTIP_FARM).withStyle(ChatFormatting.GRAY));
    }

    private static Component tierName(MachineTier tier) {
        return Component.translatable(tier.translationKey());
    }

    private static void notify(@Nullable Player player, Component message) {
        if (player != null) {
            player.sendOverlayMessage(message);
        }
    }
}
