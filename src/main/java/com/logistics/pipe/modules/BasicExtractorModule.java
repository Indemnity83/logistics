package com.logistics.pipe.modules;

import com.logistics.LogisticsPipe;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.lib.storage.DirectionSerializer;
import com.logistics.pipe.PipeContext;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.runtime.TravelingItem;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Basic extractor module — pulls items from an adjacent inventory into the pipe on a fixed timer.
 *
 * <p>Parameterised at construction so the same class serves multiple marks:
 * <ul>
 *   <li>MkI  — 1 item every 100 ticks</li>
 *   <li>MkII — 1 item every 20 ticks</li>
 *   <li>MkIII — 64 items every 6 ticks</li>
 * </ul>
 *
 * <p>No GUI, no energy requirement.
 */
public class BasicExtractorModule implements Module {
    private static final String EXTRACT_DIRECTION = "extract_direction";
    private static final String TICKS_SINCE_PULL = "ticks_since_pull";

    private final int itemsPerPull;
    private final int ticksBetweenPulls;

    public BasicExtractorModule(int itemsPerPull, int ticksBetweenPulls) {
        this.itemsPerPull = itemsPerPull;
        this.ticksBetweenPulls = ticksBetweenPulls;
    }

    @Override
    public void onConnectionsChanged(PipeContext ctx, List<Direction> options) {
        List<Direction> inventoryFaces = ctx.getInventoryConnections();
        if (inventoryFaces.isEmpty()) {
            setExtractDirection(ctx, null);
            return;
        }
        Direction current = getExtractDirection(ctx);
        if (current == null || !inventoryFaces.contains(current)) {
            setExtractDirection(ctx, inventoryFaces.getFirst());
        }
    }

    @Override
    public void onTick(PipeContext ctx) {
        if (ctx.world().isClientSide()) return;

        int ticks = ctx.getInt(this, TICKS_SINCE_PULL, 0) + 1;
        if (ticks < ticksBetweenPulls) {
            ctx.saveInt(this, TICKS_SINCE_PULL, ticks);
            return;
        }

        ctx.saveInt(this, TICKS_SINCE_PULL, 0);

        Direction dir = getExtractDirection(ctx);
        if (dir == null) return;

        extract(ctx, dir);
    }

    private void extract(PipeContext ctx, Direction dir) {
        // Check pipe has room
        int occupied = ctx.blockEntity().getTravelingItems().stream()
                .mapToInt(i -> i.getStack().getCount())
                .sum();
        if (PipeBlockEntity.VIRTUAL_CAPACITY - occupied < itemsPerPull) return;

        BlockPos targetPos = ctx.pos().relative(dir);
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(ctx.world(), targetPos, dir.getOpposite());
        if (storage == null) return;

        try (Transaction tx = Transaction.openOuter()) {
            for (StorageView<ItemVariant> view : storage) {
                ItemVariant variant = view.getResource();
                if (variant.isBlank()) continue;

                long extracted = view.extract(variant, itemsPerPull, tx);
                if (extracted > 0) {
                    ItemStack stack = variant.toStack((int) extracted);
                    TravelingItem item = new TravelingItem(stack, dir.getOpposite(), LogisticsPipe.CONFIG.ITEM_MIN_SPEED);
                    ctx.blockEntity().forceAddItem(item, dir);
                    tx.commit();
                    return;
                }
            }
        }
    }

    @Override
    public @Nullable ResourceId getPipeArm(PipeContext ctx, Direction direction) {
        if (getExtractDirection(ctx) != direction) return null;
        String suffix = ctx.isInventoryConnection(direction) ? "_feature_extended" : "_feature";
        return LogisticsPipe.model("item_extractor_pipe" + suffix);
    }

    @Nullable
    private Direction getExtractDirection(PipeContext ctx) {
        return DirectionSerializer.load(ctx, this, EXTRACT_DIRECTION);
    }

    private void setExtractDirection(PipeContext ctx, @Nullable Direction direction) {
        DirectionSerializer.save(ctx, this, EXTRACT_DIRECTION, direction);
    }
}
