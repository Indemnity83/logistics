package com.logistics.pipe.modules;

import com.logistics.LogisticsMod;
import com.logistics.block.PipeBlock;
import com.logistics.block.entity.PipeBlockEntity;
import com.logistics.item.LogisticsItems;
import com.logistics.pipe.Pipe;
import com.logistics.pipe.PipeContext;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class DykemModule implements Module {
    public static final String COLOR_KEY = "pipe_color";

    @Nullable
    public DyeColor getStoredColor(PipeContext ctx) {
        String colorId = ctx.getString(this, COLOR_KEY, "");
        if (colorId.isEmpty()) {
            return null;
        }
        for (DyeColor color : DyeColor.values()) {
            if (color.getId().equals(colorId)) {
                return color;
            }
        }
        return null;
    }

    @Override
    public ActionResult onUseWithItem(PipeContext ctx, ItemUsageContext usage) {
        ItemStack stack = usage.getStack();
        PlayerEntity player = usage.getPlayer();
        if (stack.isEmpty() && player != null && player.isInSneakingPose()) {
            if (!ctx.getString(this, COLOR_KEY, "").isEmpty()) {
                if (ctx.world().isClient()) {
                    return ActionResult.SUCCESS;
                }
                ctx.remove(this, COLOR_KEY);
                ctx.markDirtyAndSync();
            }
            return ActionResult.SUCCESS;
        }
        DyeColor color = LogisticsItems.getDykemColor(stack);
        if (color == null) {
            return ActionResult.PASS;
        }

        if (ctx.world().isClient()) {
            return ActionResult.SUCCESS;
        }

        if (player == null) {
            return ActionResult.PASS;
        }

        String colorId = color.getId();
        String current = ctx.getString(this, COLOR_KEY, "");
        if (colorId.equals(current)) {
            return ActionResult.SUCCESS;
        }

        ctx.saveString(this, COLOR_KEY, colorId);
        ctx.markDirtyAndSync();
        stack.damage(1, player, usage.getHand());
        return ActionResult.SUCCESS;
    }

    @Override
    public java.util.List<Pipe.CoreDecoration> getCoreDecorations(PipeContext ctx) {
        DyeColor color = getStoredColor(ctx);
        if (color == null || ctx.pipe() == null) {
            return java.util.List.of();
        }
        Identifier dyedCore = Identifier.of(LogisticsMod.MOD_ID, "block/pipe_core_dye");
        return java.util.List.of(new Pipe.CoreDecoration(dyedCore, color.getEntityColor()));
    }

    @Override
    public boolean allowsConnection(@Nullable PipeContext ctx, Direction direction, Pipe selfPipe, Block neighborBlock) {
        if (ctx == null || !(neighborBlock instanceof PipeBlock neighborPipeBlock)) {
            return true;
        }

        String color = ctx.getString(this, COLOR_KEY, "");
        if (color.isEmpty()) {
            return true;
        }

        Pipe neighborPipe = neighborPipeBlock.getPipe();
        if (neighborPipe == null || neighborPipe.getModule(DykemModule.class) == null) {
            return true;
        }

        BlockPos neighborPos = ctx.pos().offset(direction);
        if (!(ctx.world().getBlockEntity(neighborPos) instanceof PipeBlockEntity neighborEntity)) {
            return true;
        }

        PipeContext neighborContext = new PipeContext(
            ctx.world(),
            neighborPos,
            ctx.world().getBlockState(neighborPos),
            neighborEntity
        );
        String neighborColor = neighborContext.getString(this, COLOR_KEY, "");
        return Objects.equals(neighborColor, "") || color.equals(neighborColor);
    }
}
