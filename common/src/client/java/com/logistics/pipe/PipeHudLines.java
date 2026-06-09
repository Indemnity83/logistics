package com.logistics.pipe;

import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.core.lib.pipe.PipeHud;
import com.logistics.core.lib.pipe.TravelingItem;
import com.logistics.pipe.block.PipeBlock;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Builds the pipe HUD: items in transit and each module's status. Item displays (chassis modules, filter
 * contents, supplied/requested items, recipes) render as icons at {@link #ICON_SCALE} — tweak there.
 */
public final class PipeHudLines {

    /** Cap on per-item detail lines so a busy pipe can't blow up the tooltip. */
    private static final int MAX_DETAIL_ITEMS = 5;

    private PipeHudLines() {}

    /** Drives each module's {@code appendHud} for this pipe, writing to the given (loader-rendered) sink. */
    public static void appendModuleHud(BlockState state, PipeBlockEntity pipe, PipeHud hud) {
        if (state.getBlock() instanceof PipeBlock pipeBlock && pipeBlock.getPipe() != null) {
            PipeContext ctx = new PipeContext(pipe.getLevel(), pipe.getBlockPos(), state, pipe);
            pipeBlock.getPipe().appendHud(ctx, hud);
        }
    }

    /** The module item stacks installed in a chassis pipe (empty for non-chassis), for icon display. */
    public static List<ItemStack> installedModules(BlockState state, PipeBlockEntity pipe) {
        if (state.getBlock() instanceof PipeBlock pipeBlock && pipeBlock.getPipe() instanceof ChassisPipe chassis) {
            PipeContext ctx = new PipeContext(pipe.getLevel(), pipe.getBlockPos(), state, pipe);
            return chassis.getInstalledModuleStacks(ctx);
        }
        return List.of();
    }

    public static List<Component> build(List<TravelingItem> items, boolean showDetails) {
        List<Component> lines = new ArrayList<>();
        if (items.isEmpty()) {
            return lines;
        }

        lines.add(Component.translatable("jade.logistics.pipe.items")
                .append(Component.literal(": "))
                .append(Component.literal(String.valueOf(items.size())).withStyle(ChatFormatting.WHITE)));

        if (showDetails) {
            int shown = Math.min(items.size(), MAX_DETAIL_ITEMS);
            for (int i = 0; i < shown; i++) {
                TravelingItem item = items.get(i);
                // Append the hover name as a Component so colored/renamed items keep their styling.
                Component line = Component.literal(item.getStack().getCount() + "x ")
                        .append(item.getStack().getHoverName())
                        .append(Component.literal(" → " + item.getDirection().name()))
                        .withStyle(ChatFormatting.AQUA);
                lines.add(line);
            }
            if (items.size() > shown) {
                lines.add(Component.translatable("jade.logistics.pipe.more", items.size() - shown)
                        .withStyle(ChatFormatting.GRAY));
            }
        }

        return lines;
    }
}
