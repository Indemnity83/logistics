package com.logistics.pipe.block.entity;

import com.logistics.core.lib.pipe.TravelingItem;
import com.logistics.core.lib.storage.IItemKey;
import com.logistics.core.lib.storage.IItemStorage;
import com.logistics.core.lib.storage.IItemView;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;

/**
 * Exposes a {@link PipeBlockEntity} as an {@link IItemStorage} for incoming item insertion.
 *
 * <p>Pipes do not expose extractable contents ({@link #extract} and {@link #contents} return
 * empty/zero). Only {@link #insert} is meaningful — it routes items into the pipe's traveling
 * item system.
 *
 * <p>Two insert entry points exist:
 * <ul>
 *   <li>{@link #insert(IItemKey, long, boolean)} — used by loader capability adapters when
 *       external inventories push items into the pipe.</li>
 *   <li>{@link #insertTravelingItem(TravelingItem)} — used by {@code PipeRuntime} for
 *       pipe-to-pipe item hand-off, preserving {@link TravelingItem} metadata such as speed
 *       and delivery tracking.</li>
 * </ul>
 */
public class PipeItemStorage implements IItemStorage {

    /** Sentinel: no speed override for externally-inserted items. */
    private static final Float NO_SPEED = null;

    private final PipeBlockEntity pipe;
    private final Direction fromDirection;

    public PipeItemStorage(PipeBlockEntity pipe, Direction fromDirection) {
        this.pipe = pipe;
        this.fromDirection = fromDirection;
    }

    /**
     * Insert items from an external source into this pipe.
     *
     * <p>When {@code simulate=true}, queries whether the pipe would accept the items without
     * modifying any state. When {@code simulate=false}, commits the insertion and schedules
     * a new {@link TravelingItem} into the pipe.
     */
    @Override
    public long insert(IItemKey item, long maxAmount, boolean simulate) {
        if (maxAmount <= 0) return 0;

        ItemStack previewStack = item.toStack(1);
        long accepted = pipe.getInsertableAmount(maxAmount, fromDirection, previewStack);
        if (accepted <= 0) return 0;

        if (!simulate) {
            ItemStack stack = item.toStack((int) Math.min(accepted, Integer.MAX_VALUE));
            pipe.acceptInsertedStack(stack, fromDirection, NO_SPEED);
        }

        return accepted;
    }

    /**
     * Insert a {@link TravelingItem} from an adjacent pipe, preserving its transit metadata.
     *
     * <p>This path is used exclusively by {@code PipeRuntime} for pipe-to-pipe hand-off.
     * It propagates speed, delivery ID, and destination from the source traveling item so the
     * next pipe segment continues the journey correctly.
     *
     * @return the number of items accepted
     */
    public long insertTravelingItem(TravelingItem item) {
        if (item.getStack().isEmpty()) return 0;

        long accepted = pipe.getInsertableAmount(item.getStack().getCount(), fromDirection, item.getStack());
        if (accepted <= 0) return 0;

        ItemStack stack = item.getStack().copy();
        stack.setCount((int) accepted);
        pipe.acceptInsertedStack(stack, fromDirection, item);

        return accepted;
    }

    @Override
    public long extract(IItemKey item, long maxAmount, boolean simulate) {
        return 0; // Pipes do not expose extractable contents
    }

    @Override
    public Iterable<IItemView> contents() {
        return Collections.emptyList(); // Pipes have no storage views
    }
}
