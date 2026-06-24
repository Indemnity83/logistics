package com.logistics.core.machine.component;

import java.util.function.Predicate;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

/**
 * Static sided-access rules for a machine inventory. Each face exposes a set of slots; the input
 * slot is insertable from the faces that expose it (subject to {@code insertFilter}) and the output
 * slot is extractable from the faces that expose it.
 *
 * <p>Factories cover the two shapes used today:
 * <ul>
 *   <li>{@link #furnace} — input from the top only (sides expose nothing), output from the bottom.</li>
 *   <li>{@link #bottomOut} — input from the top and all horizontal sides, output from the bottom.</li>
 * </ul>
 */
public final class SidedLayout {

    private static final int[] NONE = new int[0];

    private final int[] upSlots;
    private final int[] sideSlots;
    private final int[] downSlots;
    private final int inputSlot;
    private final int outputSlot;
    private final Predicate<ItemStack> insertFilter;

    public SidedLayout(
            int[] upSlots,
            int[] sideSlots,
            int[] downSlots,
            int inputSlot,
            int outputSlot,
            Predicate<ItemStack> insertFilter) {
        this.upSlots = upSlots;
        this.sideSlots = sideSlots;
        this.downSlots = downSlots;
        this.inputSlot = inputSlot;
        this.outputSlot = outputSlot;
        this.insertFilter = insertFilter;
    }

    /**
     * Vanilla-furnace item access: input from the top only, output from the bottom; horizontal faces
     * expose nothing (a furnace's sides are its fuel slot, which electric machines don't have).
     */
    public static SidedLayout furnace(int inputSlot, int outputSlot, Predicate<ItemStack> insertFilter) {
        return new SidedLayout(
                new int[] {inputSlot}, NONE, new int[] {outputSlot}, inputSlot, outputSlot, insertFilter);
    }

    /** Input insertable from the top and all horizontal faces; output extractable from the bottom. */
    public static SidedLayout bottomOut(int inputSlot, int outputSlot, Predicate<ItemStack> insertFilter) {
        return new SidedLayout(
                new int[] {inputSlot}, new int[] {inputSlot}, new int[] {outputSlot}, inputSlot, outputSlot, insertFilter);
    }

    public int[] slotsForFace(Direction side) {
        return switch (side) {
            case UP -> upSlots;
            case DOWN -> downSlots;
            default -> sideSlots;
        };
    }

    public boolean canPlace(int slot, ItemStack stack, Direction dir) {
        return slot == inputSlot && contains(slotsForFace(dir), slot) && insertFilter.test(stack);
    }

    public boolean canTake(int slot, ItemStack stack, Direction dir) {
        return slot == outputSlot && contains(slotsForFace(dir), slot);
    }

    private static boolean contains(int[] slots, int slot) {
        for (int s : slots) {
            if (s == slot) {
                return true;
            }
        }
        return false;
    }
}
