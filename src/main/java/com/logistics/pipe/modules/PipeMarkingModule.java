package com.logistics.pipe.modules;

import com.logistics.LogisticsMod;
import com.logistics.pipe.Pipe;
import com.logistics.pipe.PipeContext;
import com.logistics.pipe.block.PipeBlock;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.LogisticsPipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.DyeColor;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

public class PipeMarkingModule implements Module {
    public static final String COLOR_KEY = "pipe_color";

    /**
     * Read the stored marking color for this pipe, if any.
     */
    @Nullable public DyeColor getStoredColor(PipeContext ctx) {
        String colorId = ctx.getString(this, COLOR_KEY, "");
        if (colorId.isEmpty()) {
            return null;
        }
        for (DyeColor color : DyeColor.values()) {
            if (color.getName().equals(colorId)) {
                return color;
            }
        }
        return null;
    }

    /**
     * Apply pipe markings with marking fluid bottles.
     */
    public InteractionResult onUseWithItem(PipeContext ctx, UseOnContext usage) {
        ItemStack stack = usage.getItemInHand();
        Player player = usage.getPlayer();

        DyeColor color = LogisticsPipe.getMarkingFluidColor(stack);
        if (color == null) {
            return InteractionResult.PASS;
        }

        if (ctx.world().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (player == null) {
            return InteractionResult.PASS;
        }

        String colorId = color.getName();
        String current = ctx.getString(this, COLOR_KEY, "");
        if (colorId.equals(current)) {
            return InteractionResult.SUCCESS;
        }

        ctx.saveString(this, COLOR_KEY, colorId);
        ctx.markDirtyAndSync();
        EquipmentSlot slot = usage.getHand() == InteractionHand.MAIN_HAND
                ? EquipmentSlot.MAINHAND
                : EquipmentSlot.OFFHAND;
        stack.hurtAndBreak(1, player, slot);
        return InteractionResult.SUCCESS;
    }

    /**
     * Clear pipe markings with shift+empty hand.
     */
    public InteractionResult onUseWithoutItem(PipeContext ctx, UseOnContext usage) {
        Player player = usage.getPlayer();
        if (player != null && player.isShiftKeyDown()) {
            if (!ctx.getString(this, COLOR_KEY, "").isEmpty()) {
                if (ctx.world().isClientSide()) {
                    return InteractionResult.SUCCESS;
                }
                ctx.remove(this, COLOR_KEY);
                ctx.markDirtyAndSync();
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    /**
     * Provide the marking overlay for the pipe core when marked.
     */
    public java.util.List<Pipe.CoreDecoration> getCoreDecorations(PipeContext ctx) {
        DyeColor color = getStoredColor(ctx);
        if (color == null || ctx.pipe() == null) {
            return java.util.List.of();
        }
        Identifier pipeMarkings = Identifier.fromNamespaceAndPath(LogisticsMod.MOD_ID, "block/pipe/pipe_markings");
        return java.util.List.of(new Pipe.CoreDecoration(pipeMarkings, color.getFireworkColor()));
    }

    /**
     * Prevent pipe connections between different markings.
     */
    public boolean allowsConnection(
            @Nullable PipeContext ctx, Direction direction, Pipe selfPipe, Block neighborBlock) {
        if (ctx == null || !(neighborBlock instanceof PipeBlock neighborPipeBlock)) {
            return true;
        }

        String color = ctx.getString(this, COLOR_KEY, "");
        if (color.isEmpty()) {
            return true;
        }

        Pipe neighborPipe = neighborPipeBlock.getPipe();
        if (neighborPipe == null || neighborPipe.getModule(PipeMarkingModule.class) == null) {
            return true;
        }

        BlockPos neighborPos = ctx.pos().relative(direction);
        if (!(ctx.world().getBlockEntity(neighborPos) instanceof PipeBlockEntity neighborEntity)) {
            return true;
        }

        PipeContext neighborContext =
                new PipeContext(ctx.world(), neighborPos, ctx.world().getBlockState(neighborPos), neighborEntity);
        String neighborColor = neighborContext.getString(this, COLOR_KEY, "");
        if (neighborColor.isEmpty()) {
            return true;
        }
        return color.equals(neighborColor);
    }
}
