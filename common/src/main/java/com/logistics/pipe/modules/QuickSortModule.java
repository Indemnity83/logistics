package com.logistics.pipe.modules;

import com.logistics.core.LogisticsConfig;
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

import java.util.ArrayList;
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
    private static final String LAST_SLOT_SCANNED = "last_slot_scanned";
    private static final String LAST_SUCCESS_SLOT = "last_success_slot";
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

        // Build indexed slot list for stable position tracking
        List<IItemView> slots = new ArrayList<>();
        for (IItemView view : storage.contents()) {
            slots.add(view);
        }
        int size = slots.size();
        if (size == 0) {
            if (ctx.getInt(this, STALLED, 0) == 0) NetDbg.out("[QuickSort @ {}] Entering stall (inventory has no slots)", ctx.pos());
            ctx.saveInt(this, STALLED, 1);
            return;
        }

        // Bounds-check position counters against current inventory size
        int lastScanned = ctx.getInt(this, LAST_SLOT_SCANNED, 0);
        int lastSuccess = ctx.getInt(this, LAST_SUCCESS_SLOT, 0);
        if (lastScanned >= size) lastScanned = 0;
        if (lastSuccess >= size) lastSuccess = 0;

        // Advance forward from lastScanned, skipping empty slots.
        // Stall if we wrap all the way back to lastSuccess without finding a non-empty slot.
        int candidate = -1;
        for (int i = 1; i <= size; i++) {
            int idx = (lastScanned + i) % size;
            IItemView view = slots.get(idx);
            if (view.amount() > 0) {
                candidate = idx;
                break;
            }
            if (idx == lastSuccess) {
                // Scanned back to last-success position with nothing found — stall
                if (ctx.getInt(this, STALLED, 0) == 0) NetDbg.out("[QuickSort @ {}] Entering stall (all slots empty)", ctx.pos());
                ctx.saveInt(this, STALLED, 1);
                return;
            }
        }
        if (candidate == -1) {
            if (ctx.getInt(this, STALLED, 0) == 0) NetDbg.out("[QuickSort @ {}] Entering stall (all slots empty)", ctx.pos());
            ctx.saveInt(this, STALLED, 1);
            return;
        }

        // Commit the new scan position
        ctx.saveInt(this, LAST_SLOT_SCANNED, candidate);

        // Query network for a filtered destination (no default-route sinks)
        IItemView candidateView = slots.get(candidate);
        IItemKey key = candidateView.resource();
        int stackSize = (int) Math.min(candidateView.amount(), key.toStack(1).getItem().getDefaultMaxStackSize());
        ItemStack queryStack = key.toStack(stackSize);

        ILogisticsNetwork network = ctx.network();
        if (network == null) return;

        BlockPos destination = network.findFilteredSinkFor(queryStack);
        if (destination == null) {
            NetDbg.out("[QuickSort @ {}] No network destination for {} in slot {}", ctx.pos(), key.toStack(1).getItem(), candidate);
            // No destination — stall if full circle, otherwise just wait for next cycle
            if (candidate == lastSuccess) {
                if (ctx.getInt(this, STALLED, 0) == 0) NetDbg.out("[QuickSort @ {}] Entering stall (no network destination for {})", ctx.pos(), key.toStack(1).getItem());
                ctx.saveInt(this, STALLED, 1);
            }
            return;
        }

        // Check pipe has room for the extracted items
        int occupied = ctx.blockEntity().getTravelingItems().stream()
                .mapToInt(ti -> ti.getStack().getCount())
                .sum();
        if (PipeBlockEntity.VIRTUAL_CAPACITY - occupied < stackSize) return;

        // Extract and inject into the pipe with destination pre-set
        long extracted = storage.extract(key, stackSize, false);
        if (extracted > 0) {
            ItemStack extractedStack = key.toStack((int) extracted);
            NetDbg.out("[QuickSort @ {}] Extracted {}x{} from slot {}", ctx.pos(), extracted, key.toStack(1).getItem(), candidate);
            TravelingItem travelingItem = new TravelingItem(
                    extractedStack, inventoryDir.getOpposite(),
                    LogisticsConfig.get().pipe.minSpeed, destination);
            ctx.blockEntity().forceAddItem(travelingItem, inventoryDir);
            // Success: clear stall, record last successful slot
            if (ctx.getInt(this, STALLED, 0) == 1) NetDbg.out("[QuickSort @ {}] Exiting stall", ctx.pos());
            ctx.saveInt(this, STALLED, 0);
            ctx.saveInt(this, LAST_SUCCESS_SLOT, candidate);
        } else {
            // Slot appeared non-empty but extraction failed (temporarily locked) —
            // advance past it and back off to the stall delay
            ctx.saveInt(this, LAST_SLOT_SCANNED, (candidate + 1) % size);
            if (ctx.getInt(this, STALLED, 0) == 0) NetDbg.out("[QuickSort @ {}] Entering stall (extraction locked for {} in slot {})", ctx.pos(), key.toStack(1).getItem(), candidate);
            ctx.saveInt(this, STALLED, 1);
        }
    }

    @Nullable
    private Direction getInventoryDirection(PipeContext ctx) {
        List<Direction> faces = ctx.getInventoryConnections();
        return faces.isEmpty() ? null : faces.getFirst();
    }
}
