package com.logistics.pipe.modules;

import com.logistics.pipe.runtime.TravelingItem;
import com.logistics.pipe.runtime.PipeConfig;
import com.logistics.pipe.PipeContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ExtractionModule implements Module {
    private static final String EXTRACT_FROM = "extract_direction"; // NBT key for save compatibility

    @Override
    public void onTick(PipeContext ctx) {
        Module.super.onTick(ctx);
        if (ctx.world().isClient) {
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
    public void onWrenchUse(PipeContext ctx, ItemUsageContext usage) {
        List<Direction> connected = ctx.getInventoryConnections();

        // No valid outputs: clear config.
        if (connected.isEmpty()) {
            setExtractionDirection(ctx, null);
            return;
        }

        Direction current = getExtractionDirection(ctx);
        Direction next = nextInCycle(connected, current);

        setExtractionDirection(ctx, next);
    }

    @Override
    public boolean canAcceptFrom(PipeContext ctx, Direction from, ItemStack stack) {
        return false;
    }

    private @Nullable Direction getExtractionDirection(PipeContext ctx) {
        return Direction.byId(ctx.getString(this, EXTRACT_FROM, Direction.NORTH.getId()));
    }

    private void setExtractionDirection(PipeContext ctx, @Nullable Direction direction) {
        if (direction == null) {
            ctx.remove(this, EXTRACT_FROM);
            ctx.setFeatureFace(null);
        } else {
            ctx.saveString(this, EXTRACT_FROM, direction.getId());
            ctx.setFeatureFace(direction);
        }

        ctx.blockEntity().markDirty();
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
                    TravelingItem item = new TravelingItem(stack, direction.getOpposite(), PipeConfig.EXTRACTED_ITEM_SPEED);
                    ctx.blockEntity().forceAddItem(item, direction);
                    transaction.commit();
                    return true;
                }
            }
        }

        return false;
    }

}
