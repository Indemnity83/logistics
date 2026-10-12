package com.logistics.core.machine.upgrade;

import com.logistics.core.machine.MachineComponent;
import com.logistics.core.machine.MachineContext;
import com.logistics.core.machine.component.ItemStoreComponent;
import com.logistics.core.machine.component.SlotRole;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * Reads a machine's installed upgrades and exposes the resulting {@link MachineModifiers}.
 *
 * <p>The upgrades live in the {@link SlotRole#UPGRADE} slots of the machine's own
 * {@link ItemStoreComponent}, so menus, persistence and block-break drops see them like any other
 * slot. An unbound component (no upgrade slots) reports {@link MachineModifiers#identity()}.
 *
 * <p>{@link #modifiers()} is one object for the component's lifetime that re-reads the slots on
 * every call, so a reference captured at construction stays current. Upgrades change only
 * {@link MachineModifiers#range}; cost and speed stay 1.0 and capacity/receive-rate stay identity.
 *
 * <p>Each server tick the component compares the slots with what it last saw; on a change it syncs
 * the machine to clients and runs the {@link #onChange} listeners. Register it before the
 * components that react, so they see the change the same tick.
 */
public final class UpgradeComponent implements MachineComponent {

    private final String id;
    private final Container container;
    private final int[] slots;
    private final MachineModifiers modifiers;
    private final ItemStack[] observed;
    private final List<Runnable> listeners = new ArrayList<>();

    /** An upgrade component with no upgrade slots; its modifiers are identity. */
    public UpgradeComponent(String id) {
        this(id, null, new int[0]);
    }

    /** Binds to every {@link SlotRole#UPGRADE} slot of {@code items}. */
    public UpgradeComponent(String id, ItemStoreComponent items) {
        this(id, items.container(), requireUpgradeSlots(id, items));
    }

    private UpgradeComponent(String id, Container container, int[] slots) {
        this.id = id;
        this.container = container;
        this.slots = slots;
        this.modifiers = slots.length == 0 ? MachineModifiers.identity() : new SlotModifiers();
        this.observed = new ItemStack[slots.length];
        Arrays.fill(observed, ItemStack.EMPTY);
    }

    private static int[] requireUpgradeSlots(String id, ItemStoreComponent items) {
        int[] slots = items.upgradeSlots();
        if (slots.length == 0) {
            throw new IllegalStateException(
                    "upgrades(" + id + ") binds to items(" + items.id() + "), which has no UPGRADE slots");
        }
        return slots;
    }

    @Override
    public String id() {
        return id;
    }

    /** The modifiers contributed by the installed upgrades; the same object on every call. */
    public MachineModifiers modifiers() {
        return modifiers;
    }

    /** Runs {@code listener} on the server tick after the installed upgrades change. */
    public void onChange(Runnable listener) {
        listeners.add(Objects.requireNonNull(listener));
    }

    @Override
    public void serverTick(MachineContext ctx) {
        if (slots.length == 0 || !captureChanges()) {
            return;
        }
        ctx.sync();
        for (Runnable listener : listeners) {
            listener.run();
        }
    }

    /** Records the current slot contents; true when they differ from the last observation. */
    private boolean captureChanges() {
        boolean changed = false;
        for (int i = 0; i < slots.length; i++) {
            ItemStack current = container.getItem(slots[i]);
            if (!ItemStack.matches(current, observed[i])) {
                observed[i] = current.copy();
                changed = true;
            }
        }
        return changed;
    }

    /** Installed copies of {@code stack}'s upgrade that count, capped at the per-slot maximum. */
    private static int installed(ItemStack stack, UpgradeItem upgrade) {
        return Math.max(0, Math.min(stack.getCount(), upgrade.maxPerSlot()));
    }

    private final class SlotModifiers implements MachineModifiers {
        @Override
        public double cost(OperationKey op) {
            return 1.0;
        }

        @Override
        public double speed(OperationKey op) {
            return 1.0;
        }

        @Override
        public int range(OperationKey op) {
            int total = 0;
            for (int slot : slots) {
                ItemStack stack = container.getItem(slot);
                if (stack.getItem() instanceof UpgradeItem upgrade) {
                    total += upgrade.rangeBonus(op) * installed(stack, upgrade);
                }
            }
            return total;
        }

        @Override
        public long capacity(long base) {
            return base;
        }

        @Override
        public long receiveRate(long base) {
            return base;
        }
    }
}
