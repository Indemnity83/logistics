package com.logistics.core.item;

import com.logistics.core.lib.block.Probeable;
import com.logistics.core.lib.support.ProbeResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.ChatFormatting;

/**
 * Probe tool for inspecting blocks and displaying diagnostic information.
 * Delegates probe actions to blocks implementing {@link Probeable}.
 */
public class ProbeItem extends Item {

    private static final ChatFormatting TITLE_COLOR = ChatFormatting.GOLD;
    private static final ChatFormatting KEY_COLOR = ChatFormatting.WHITE;
    private static final ChatFormatting DEFAULT_VALUE_COLOR = ChatFormatting.GRAY;
    private static final ChatFormatting WARNING_COLOR = ChatFormatting.RED;

    public ProbeItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() == null) {
            return InteractionResult.PASS;
        }

        var world = context.getLevel();
        var pos = context.getClickedPos();
        var player = context.getPlayer();
        var block = world.getBlockState(pos).getBlock();

        if (block instanceof Probeable probeable) {
            if (world.isClientSide()) {
                return InteractionResult.SUCCESS;
            }

            ProbeResult result = probeable.onProbe(world, pos, player);
            if (result != null) {
                displayResult(player, result);
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }

    private void displayResult(Player player, ProbeResult result) {
        // Display title
        player.displayClientMessage(Component.literal("=== " + result.title() + " ===").withStyle(TITLE_COLOR), false);

        // Display entries
        for (ProbeResult.Entry entry : result.entries()) {
            switch (entry) {
                case ProbeResult.Entry.KeyValue kv -> {
                    ChatFormatting valueColor = kv.color() != null ? kv.color() : DEFAULT_VALUE_COLOR;
                    player.displayClientMessage(
                            Component.literal(kv.key() + ": ")
                                    .withStyle(KEY_COLOR)
                                    .append(Component.literal(kv.value()).withStyle(valueColor)),
                            false);
                }
                case ProbeResult.Entry.Warning warning -> {
                    player.displayClientMessage(
                            Component.literal("WARNING: " + warning.message()).withStyle(WARNING_COLOR, ChatFormatting.BOLD),
                            false);
                }
                case ProbeResult.Entry.Separator ignored -> {
                    player.displayClientMessage(Component.literal("---").withStyle(ChatFormatting.DARK_GRAY), false);
                }
            }
        }
    }
}
