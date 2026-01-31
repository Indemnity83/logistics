package com.logistics.pipe.modules;

import java.util.List;

import com.logistics.LogisticsMod;
import com.logistics.core.registry.CoreItems;
import com.logistics.pipe.PipeContext;
import com.logistics.pipe.runtime.PipeConfig;
import com.logistics.pipe.runtime.TravelingItem;

import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

public class ExtractionModule implements Module {
    private static final String EXTRACT_FROM = "extract_direction"; // NBT key for save compatibility

    @Override
    public void onTick(PipeContext ctx) {
        Module.super.onTick(ctx);
        if (ctx.world().isClient()) {
            return;
        }

        // TODO: this is 100% functional, but lacks whimsy, all wooden pipes
        //       extract at the same moment, would be better to track the
        //       actual interval
        if (ctx.world().getTime() % PipeConfig.EXTRACTION_INTERVAL != 0) {
            return;
        }

        Direction extractDirection = getExtractionDirection(ctx);
        if (extractDirection != null && ctx.getInventoryConnections().contains(extractDirection)) {
            extractFromDirection(ctx, extractDirection);
            return;
        }

        List<Direction> inventoryFaces = ctx.getInventoryConnections();
        if (inventoryFaces.size() == 1) {
            Direction selected = inventoryFaces.getFirst();
            setExtractionDirection(ctx, selected);
            extractFromDirection(ctx, selected);
        }
    }

    @Override
    public void onConnectionsChanged(PipeContext ctx, List<Direction> options) {
        List<Direction> inventoryFaces = ctx.getInventoryConnections();
        if (inventoryFaces.isEmpty()) {
            setExtractionDirection(ctx, null);
            return;
        }

        Direction current = getExtractionDirection(ctx);
        if (current == null || !inventoryFaces.contains(current)) {
            setExtractionDirection(ctx, inventoryFaces.getFirst());
        }
    }

    @Override
    public ActionResult onUseWithItem(PipeContext ctx, ItemUsageContext usage) {
        if (!CoreItems.isWrench(usage.getStack())) {
            return ActionResult.PASS;
        }

        if (ctx.world().isClient()) {
            return ActionResult.SUCCESS;
        }

        List<Direction> connected = ctx.getInventoryConnections();

        // No valid outputs: clear config.
        if (connected.isEmpty()) {
            setExtractionDirection(ctx, null);
            return ActionResult.SUCCESS;
        }

        Direction current = getExtractionDirection(ctx);
        Direction next = nextInCycle(connected, current);

        setExtractionDirection(ctx, next);
        return ActionResult.SUCCESS;
    }

    private @Nullable Direction getExtractionDirection(PipeContext ctx) {
        NbtCompound state = ctx.moduleState(getStateKey());
        if (!state.contains(EXTRACT_FROM)) {
            return null;
        }
        return state.getString(EXTRACT_FROM).map(Direction::byId).orElse(null);
    }

    private void setExtractionDirection(PipeContext ctx, @Nullable Direction direction) {
        Direction current = getExtractionDirection(ctx);
        if (current == direction) {
            return;
        }

        if (direction == null) {
            ctx.remove(this, EXTRACT_FROM);
        } else {
            ctx.saveString(this, EXTRACT_FROM, direction.getId());
        }

        ctx.markDirtyAndSync();
    }

    private Direction nextInCycle(List<Direction> ordered, @Nullable Direction current) {
        if (ordered.isEmpty()) {
            throw new IllegalArgumentException("ordered directions must not be empty");
        }

        int idx = (current == null) ? -1 : ordered.indexOf(current);
        return (idx < 0) ? ordered.getFirst() : ordered.get((idx + 1) % ordered.size());
    }

    private boolean extractFromDirection(PipeContext ctx, Direction direction) {
        BlockPos targetPos = ctx.pos().offset(direction);
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(ctx.world(), targetPos, direction.getOpposite());
        if (storage == null) {
            return false;
        }

        try (Transaction transaction = Transaction.openOuter()) {
            for (StorageView<ItemVariant> view : storage) {
                ItemVariant variant = view.getResource();
                if (variant.isBlank()) {
                    continue;
                }

                long extracted = view.extract(variant, 1, transaction);
                if (extracted > 0) {
                    ItemStack stack = variant.toStack((int) extracted);
                    TravelingItem item = new TravelingItem(stack, direction.getOpposite(), PipeConfig.ITEM_MIN_SPEED);
                    ctx.blockEntity().forceAddItem(item, direction);
                    transaction.commit();
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public @Nullable Identifier getPipeArm(PipeContext ctx, Direction direction) {
        if (!isExtractionFace(ctx, direction)) {
            return null;
        }
        String suffix = ctx.isInventoryConnection(direction) ? "_feature_extended" : "_feature";
        return Identifier.of(LogisticsMod.MOD_ID, "block/pipe/item_extractor_pipe" + suffix);
    }

    private boolean isExtractionFace(PipeContext ctx, Direction direction) {
        return getExtractionDirection(ctx) == direction;
    }
}
