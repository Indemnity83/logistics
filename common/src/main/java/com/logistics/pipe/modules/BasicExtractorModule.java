package com.logistics.pipe.modules;

import com.logistics.LogisticsPipe;
import com.logistics.core.LogisticsConfig;
import com.logistics.pipe.network.NetDbg;
import com.logistics.core.lib.pipe.Module;
import com.logistics.core.lib.pipe.TickingModule;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.lib.serialization.DirectionSerializer;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.core.lib.storage.IItemKey;
import com.logistics.core.lib.storage.IItemStorage;
import com.logistics.core.lib.storage.IItemView;
import com.logistics.core.lib.storage.ItemStorageLookup;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.core.lib.pipe.TravelingItem;
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
 *   <li>MkIII — 64 items every tick</li>
 * </ul>
 *
 * <p>No GUI, no energy requirement.
 */
public class BasicExtractorModule implements Module, TickingModule {
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
        int occupied = ctx.pipeAccess().getTravelingItems().stream()
                .mapToInt(i -> i.getStack().getCount())
                .sum();
        int remaining = PipeBlockEntity.VIRTUAL_CAPACITY - occupied;
        if (remaining <= 0) return;

        BlockPos targetPos = ctx.pos().relative(dir);
        IItemStorage storage = ItemStorageLookup.find(ctx.world(), targetPos, dir.getOpposite());
        if (storage == null) {
            NetDbg.out("[BasicExtractor @ {}] No storage on {}, skipping", ctx.pos(), dir);
            return;
        }

        int allowed = Math.min(itemsPerPull, remaining);
        boolean anyExtracted = false;
        for (IItemView view : storage.contents()) {
            if (allowed <= 0 || remaining <= 0) break;
            IItemKey key = view.resource();

            int toExtract = Math.min(allowed, remaining);
            long extracted = storage.extract(key, toExtract, false);
            if (extracted > 0) {
                ItemStack stack = key.toStack((int) extracted);
                NetDbg.out("[BasicExtractor @ {}] Extracted {}x{} via {}", ctx.pos(), extracted, stack.getItem(), dir);
                TravelingItem item = new TravelingItem(stack, dir.getOpposite(), LogisticsConfig.get().pipe.minSpeed);
                ctx.pipeAccess().forceAddItem(item, dir);
                remaining -= (int) extracted;
                allowed -= (int) extracted;
                anyExtracted = true;
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
