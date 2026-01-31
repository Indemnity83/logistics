package com.logistics.core.item;

import com.logistics.core.lib.block.Probeable;
import com.logistics.core.lib.support.ProbeResult;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;

/**
 * Probe tool for inspecting blocks and displaying diagnostic information.
 * Delegates probe actions to blocks implementing {@link Probeable}.
 */
public class ProbeItem extends Item {

    private static final Formatting TITLE_COLOR = Formatting.GOLD;
    private static final Formatting KEY_COLOR = Formatting.WHITE;
    private static final Formatting DEFAULT_VALUE_COLOR = Formatting.GRAY;
    private static final Formatting WARNING_COLOR = Formatting.RED;

    public ProbeItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getPlayer() == null) {
            return ActionResult.PASS;
        }

        var world = context.getWorld();
        var pos = context.getBlockPos();
        var player = context.getPlayer();
        var block = world.getBlockState(pos).getBlock();

        if (block instanceof Probeable probeable) {
            if (world.isClient()) {
                return ActionResult.SUCCESS;
            }

            ProbeResult result = probeable.onProbe(world, pos, player);
            if (result != null) {
                displayResult(player, result);
                return ActionResult.SUCCESS;
            }
        }

        return ActionResult.PASS;
    }

    private void displayResult(PlayerEntity player, ProbeResult result) {
        // Display title
        player.sendMessage(Text.literal("=== " + result.title() + " ===").formatted(TITLE_COLOR), false);

        // Display entries
        for (ProbeResult.Entry entry : result.entries()) {
            switch (entry) {
                case ProbeResult.Entry.KeyValue kv -> {
                    Formatting valueColor = kv.color() != null ? kv.color() : DEFAULT_VALUE_COLOR;
                    player.sendMessage(
                            Text.literal(kv.key() + ": ")
                                    .formatted(KEY_COLOR)
                                    .append(Text.literal(kv.value()).formatted(valueColor)),
                            false);
                }
                case ProbeResult.Entry.Warning warning -> {
                    player.sendMessage(
                            Text.literal("WARNING: " + warning.message()).formatted(WARNING_COLOR, Formatting.BOLD),
                            false);
                }
                case ProbeResult.Entry.Separator ignored -> {
                    player.sendMessage(Text.literal("---").formatted(Formatting.DARK_GRAY), false);
                }
            }
        }
    }
}
