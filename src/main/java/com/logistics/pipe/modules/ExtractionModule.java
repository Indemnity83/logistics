package com.logistics.pipe.modules;

import com.logistics.LogisticsMod;
import com.logistics.core.registry.CoreItems;
import com.logistics.pipe.PipeContext;
import com.logistics.pipe.runtime.PipeConfig;
import com.logistics.pipe.runtime.TravelingItem;
import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.jetbrains.annotations.Nullable;

public class ExtractionModule implements Module {
    private static final String EXTRACT_FROM = "extract_direction"; // NBT key for save compatibility

    @Override
    public void onTick(PipeContext ctx) {
        Module.super.onTick(ctx);
        if (ctx.world().isClientSide()) {
            return;
        }

        // TODO: this is 100% functional, but lacks whimsy, all wooden pipes
        //       extract at the same moment, would be better to track the
        //       actual interval
        if (ctx.world().getGameTime() % PipeConfig.EXTRACTION_INTERVAL != 0) {
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
    public InteractionResult onUseWithItem(PipeContext ctx, UseOnContext usage) {
        if (!CoreItems.isWrench(usage.getItemInHand())) {
            return InteractionResult.PASS;
        }

        if (ctx.world().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        List<Direction> connected = ctx.getInventoryConnections();

        // No valid outputs: clear config.
        if (connected.isEmpty()) {
            setExtractionDirection(ctx, null);
            return InteractionResult.SUCCESS;
        }

        Direction current = getExtractionDirection(ctx);
        Direction next = nextInCycle(connected, current);

        setExtractionDirection(ctx, next);
        return InteractionResult.SUCCESS;
    }

    private @Nullable Direction getExtractionDirection(PipeContext ctx) {
        CompoundTag state = ctx.moduleState(getStateKey());
        if (!state.contains(EXTRACT_FROM)) {
            return null;
        }
        return state.getString(EXTRACT_FROM).map(Direction::byName).orElse(null);
    }

    private void setExtractionDirection(PipeContext ctx, @Nullable Direction direction) {
        Direction current = getExtractionDirection(ctx);
        if (current == direction) {
            return;
        }

        if (direction == null) {
            ctx.remove(this, EXTRACT_FROM);
        } else {
            ctx.saveString(this, EXTRACT_FROM, direction.getName());
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
        BlockPos targetPos = ctx.pos().relative(direction);
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
        return Identifier.fromNamespaceAndPath(LogisticsMod.MOD_ID, "block/pipe/item_extractor_pipe" + suffix);
    }

    private boolean isExtractionFace(PipeContext ctx, Direction direction) {
        return getExtractionDirection(ctx) == direction;
    }
}
