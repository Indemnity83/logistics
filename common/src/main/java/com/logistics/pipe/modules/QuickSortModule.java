package com.logistics.pipe.modules;
import com.logistics.LogisticsPipe;

import com.logistics.pipe.network.NetDbg;
import com.logistics.core.lib.pipe.Module;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.core.lib.pipe.TickingModule;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.core.lib.network.ILogisticsNetwork;
import com.logistics.core.lib.pipe.TravelingItem;
import com.logistics.core.lib.storage.IItemKey;
import com.logistics.core.lib.storage.IItemStorage;
import com.logistics.core.lib.storage.IItemView;
import com.logistics.core.lib.storage.ItemStorageLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * QuickSort module — filterless, autonomous extractor that continuously scans the attached
 * inventory and routes each item stack to the best available filtered sink in the network.
 *
 * <p>No GUI, no configuration. Processes one slot per action cycle.
 * Does not use default-route sinks (no catch-all BasicLogisticsPipe routing).
 *
 * <p>Timing: 6-tick normal cycle, 24-tick stall cycle.
 * Stalls when no routable item is found after a full scan.
 */
public class QuickSortModule implements Module, TickingModule {
    private static final String TICKS_SINCE_ACTION = "ticks_since_action";
    private static final String STALLED = "stalled";

    private static final int NORMAL_DELAY = 6;
    private static final int STALL_DELAY = 24;

    @Override
    public void onTick(PipeContext ctx) {
        boolean stalled = ctx.getInt(this, STALLED, 0) == 1;
        int delay = stalled ? STALL_DELAY : NORMAL_DELAY;

        int ticks = ctx.getInt(this, TICKS_SINCE_ACTION, 0) + 1;
        if (ticks < delay) {
            ctx.saveInt(this, TICKS_SINCE_ACTION, ticks);
            return;
        }
        // Reset timer at start of action (before early returns) so stalls don't shorten next cycle
        ctx.saveInt(this, TICKS_SINCE_ACTION, 0);

        Direction inventoryDir = getInventoryDirection(ctx);
        if (inventoryDir == null) return;

        BlockPos targetPos = ctx.pos().relative(inventoryDir);
        IItemStorage storage =
                ItemStorageLookup.find(ctx.world(), targetPos, inventoryDir.getOpposite());
        if (storage == null) return;

        ILogisticsNetwork network = ctx.network();
        if (network == null) return;

        routeOneItem(ctx, storage, network, inventoryDir);
    }

    /**
     * Scans the attached inventory and routes the first stack that has a filtered sink and fits the
     * pipe, clearing the stall flag on success; enters a stall when nothing routable is found.
     */
    private void routeOneItem(
            PipeContext ctx, IItemStorage storage, ILogisticsNetwork network, Direction inventoryDir) {
        // Iterate contents() fresh each tick — slot indices from contents() are ephemeral
        // (the list only contains non-empty views and changes as items are extracted), so
        // we resolve the candidate by key each tick rather than persisting slot numbers.
        boolean anyNonEmpty = false;
        for (IItemView view : storage.contents()) {
            anyNonEmpty = true;
            IItemKey key = view.resource();
            ItemStack template = key.toStack(1);
            int stackSize = (int) Math.min(view.amount(), template.getItem().getDefaultMaxStackSize());
            ItemStack queryStack = key.toStack(stackSize);

            BlockPos destination = network.findFilteredSinkFor(queryStack);
            if (destination == null) {
                NetDbg.out("[QuickSort @ {}] No network destination for {}", ctx.pos(), template.getItem());
                continue;
            }

            // Check pipe has room for the extracted items
            int occupied = ctx.pipeAccess().getTravelingItems().stream()
                    .mapToInt(ti -> ti.getStack().getCount())
                    .sum();
            if (PipeBlockEntity.VIRTUAL_CAPACITY - occupied < stackSize) continue;

            // Extract and inject into the pipe with destination pre-set
            long extracted = storage.extract(key, stackSize, false);
            if (extracted > 0) {
                ItemStack extractedStack = key.toStack((int) extracted);
                NetDbg.out("[QuickSort @ {}] Extracted {}x{}", ctx.pos(), extracted, template.getItem());
                TravelingItem travelingItem = new TravelingItem(
                        extractedStack, inventoryDir.getOpposite(),
                        LogisticsPipe.PIPE_MIN_SPEED.get(), destination);
                ctx.pipeAccess().forceAddItem(travelingItem, inventoryDir);
                if (ctx.getInt(this, STALLED, 0) == 1) NetDbg.out("[QuickSort @ {}] Exiting stall", ctx.pos());
                ctx.saveInt(this, STALLED, 0);
                return;
            }
        }

        enterStall(ctx, anyNonEmpty ? "no routable items" : "inventory empty");
    }

    /** Sets the stall flag, logging the transition only on the edge into a stall. */
    private void enterStall(PipeContext ctx, String reason) {
        if (ctx.getInt(this, STALLED, 0) == 0) {
            NetDbg.out("[QuickSort @ {}] Entering stall ({})", ctx.pos(), reason);
        }
        ctx.saveInt(this, STALLED, 1);
    }

    @Nullable
    private Direction getInventoryDirection(PipeContext ctx) {
        List<Direction> faces = ctx.getInventoryConnections();
        return faces.isEmpty() ? null : faces.getFirst();
    }
}
