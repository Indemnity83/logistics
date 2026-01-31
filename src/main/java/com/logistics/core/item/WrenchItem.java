package com.logistics.core.item;

import com.logistics.power.engine.block.entity.CreativeEngineBlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;

/**
 * Wrench tool for rotating blocks and special interactions.
 *
 * <p>When sneaking, the wrench bypasses normal block interaction and
 * performs special actions on supported blocks (e.g., cycling Creative Engine output).
 */
public class WrenchItem extends Item {

    public WrenchItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        // Only handle sneak+use - non-sneak is handled by block's onUseWithItem
        if (context.getPlayer() == null || !context.getPlayer().isSneaking()) {
            return ActionResult.PASS;
        }

        var world = context.getWorld();
        var pos = context.getBlockPos();
        var player = context.getPlayer();

        // Creative Engine: sneak+wrench cycles output level
        if (world.getBlockEntity(pos) instanceof CreativeEngineBlockEntity engine) {
            if (!world.isClient()) {
                long newRate = engine.cycleOutputLevel();
                player.sendMessage(Text.literal("Output: " + newRate + " RF/t").formatted(Formatting.AQUA), true);
            }
            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }
}
