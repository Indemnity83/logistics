package com.logistics.core.machine.component;

import java.util.function.Predicate;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

/**
 * Static sided-access rules for a machine inventory. Each face exposes a set of slots; input slots
 * are insertable from the faces that expose them (subject to {@code insertFilter}) and output slots
 * are extractable from the faces that expose them.
 *
 * <p>Factories cover the two shapes used today:
 * <ul>
 *   <li>{@link #furnace} — inputs from the top only (sides expose nothing), outputs from the bottom.</li>
 *   <li>{@link #bottomOut} — inputs from the top and all horizontal sides, outputs from the bottom.</li>
 * </ul>
 */
public final class SidedLayout {

    private static final int[] NONE = new int[0];

    private final int[] upSlots;
    private final int[] sideSlots;
    private final int[] downSlots;
    private final int[] inputSlots;
    private final int[] outputSlots;
    private final Predicate<ItemStack> insertFilter;

    public SidedLayout(
            int[] upSlots,
            int[] sideSlots,
            int[] downSlots,
            int[] inputSlots,
            int[] outputSlots,
            Predicate<ItemStack> insertFilter) {
        this.upSlots = upSlots;
        this.sideSlots = sideSlots;
        this.downSlots = downSlots;
        this.inputSlots = inputSlots;
        this.outputSlots = outputSlots;
        this.insertFilter = insertFilter;
    }

    /**
     * Vanilla-furnace item access: inputs from the top only, outputs from the bottom; horizontal
     * faces expose nothing (a furnace's sides are its fuel slot, which electric machines don't have).
     */
    public static SidedLayout furnace(int[] inputSlots, int[] outputSlots, Predicate<ItemStack> insertFilter) {
        return new SidedLayout(inputSlots, NONE, outputSlots, inputSlots, outputSlots, insertFilter);
    }

    /** Inputs insertable from the top and all horizontal faces; outputs extractable from the bottom. */
    public static SidedLayout bottomOut(int[] inputSlots, int[] outputSlots, Predicate<ItemStack> insertFilter) {
        return new SidedLayout(inputSlots, inputSlots, outputSlots, inputSlots, outputSlots, insertFilter);
    }

    public int[] slotsForFace(Direction side) {
        return switch (side) {
            case UP -> upSlots;
            case DOWN -> downSlots;
            default -> sideSlots;
        };
    }

    public boolean canPlace(int slot, ItemStack stack, Direction dir) {
        return contains(inputSlots, slot) && contains(slotsForFace(dir), slot) && insertFilter.test(stack);
    }

    public boolean canTake(int slot, ItemStack stack, Direction dir) {
        return contains(outputSlots, slot) && contains(slotsForFace(dir), slot);
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
