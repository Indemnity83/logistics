package com.logistics.core.machine.component;

import java.util.function.Predicate;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

/**
<<<<<<< HEAD
<<<<<<< HEAD
 * Static sided-access rules for a machine inventory. Each face exposes a set of slots; input slots
 * are insertable from the faces that expose them (subject to {@code insertFilter}) and output slots
 * are extractable from the faces that expose them.
 *
 * <p>Factories cover the two shapes used today:
 * <ul>
 *   <li>{@link #furnace} — inputs from the top only (sides expose nothing), outputs from the bottom.</li>
 *   <li>{@link #bottomOut} — inputs from the top and all horizontal sides, outputs from the bottom.</li>
=======
 * Static sided-access rules for a machine inventory. Each face exposes a set of slots; the input
 * slot is insertable from the faces that expose it (subject to {@code insertFilter}) and the output
 * slot is extractable from the faces that expose it.
 *
 * <p>Factories cover the two shapes used today:
 * <ul>
<<<<<<< HEAD
 *   <li>{@link #furnace} — input from the top and all horizontal sides, output from the bottom.</li>
 *   <li>{@link #topInput} — input from the top only (sides expose nothing), output from the bottom.</li>
>>>>>>> b9517e5fb (Add component-hosted machine framework)
=======
 *   <li>{@link #furnace} — input from the top only (sides expose nothing), output from the bottom.</li>
 *   <li>{@link #bottomOut} — input from the top and all horizontal sides, output from the bottom.</li>
>>>>>>> 6b00e8500 (Rename sided-access layouts: furnace is top-only, bottomOut is any-side)
=======
 * Static sided-access rules for a machine inventory. Each face exposes a set of slots; input slots
 * are insertable from the faces that expose them (subject to {@code insertFilter}) and output slots
 * are extractable from the faces that expose them.
 *
 * <p>Factories cover the two shapes used today:
 * <ul>
 *   <li>{@link #furnace} — inputs from the top only (sides expose nothing), outputs from the bottom.</li>
 *   <li>{@link #bottomOut} — inputs from the top and all horizontal sides, outputs from the bottom.</li>
>>>>>>> 917a38ec2 (Support byproducts and multi-output in the recipe processor)
 * </ul>
 */
public final class SidedLayout {

    private static final int[] NONE = new int[0];

    private final int[] upSlots;
    private final int[] sideSlots;
    private final int[] downSlots;
<<<<<<< HEAD
<<<<<<< HEAD
    private final int[] inputSlots;
    private final int[] outputSlots;
=======
    private final int inputSlot;
    private final int outputSlot;
>>>>>>> b9517e5fb (Add component-hosted machine framework)
=======
    private final int[] inputSlots;
    private final int[] outputSlots;
>>>>>>> 917a38ec2 (Support byproducts and multi-output in the recipe processor)
    private final Predicate<ItemStack> insertFilter;

    public SidedLayout(
            int[] upSlots,
            int[] sideSlots,
            int[] downSlots,
<<<<<<< HEAD
<<<<<<< HEAD
            int[] inputSlots,
            int[] outputSlots,
=======
            int inputSlot,
            int outputSlot,
>>>>>>> b9517e5fb (Add component-hosted machine framework)
=======
            int[] inputSlots,
            int[] outputSlots,
>>>>>>> 917a38ec2 (Support byproducts and multi-output in the recipe processor)
            Predicate<ItemStack> insertFilter) {
        this.upSlots = upSlots;
        this.sideSlots = sideSlots;
        this.downSlots = downSlots;
<<<<<<< HEAD
<<<<<<< HEAD
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
=======
        this.inputSlot = inputSlot;
        this.outputSlot = outputSlot;
=======
        this.inputSlots = inputSlots;
        this.outputSlots = outputSlots;
>>>>>>> 917a38ec2 (Support byproducts and multi-output in the recipe processor)
        this.insertFilter = insertFilter;
    }

    /**
     * Vanilla-furnace item access: inputs from the top only, outputs from the bottom; horizontal
     * faces expose nothing (a furnace's sides are its fuel slot, which electric machines don't have).
     */
    public static SidedLayout furnace(int[] inputSlots, int[] outputSlots, Predicate<ItemStack> insertFilter) {
        return new SidedLayout(inputSlots, NONE, outputSlots, inputSlots, outputSlots, insertFilter);
    }

<<<<<<< HEAD
    /** Input insertable from the top and all horizontal faces; output extractable from the bottom. */
    public static SidedLayout bottomOut(int inputSlot, int outputSlot, Predicate<ItemStack> insertFilter) {
        return new SidedLayout(
<<<<<<< HEAD
                new int[] {inputSlot}, NONE, new int[] {outputSlot}, inputSlot, outputSlot, insertFilter);
>>>>>>> b9517e5fb (Add component-hosted machine framework)
=======
                new int[] {inputSlot}, new int[] {inputSlot}, new int[] {outputSlot}, inputSlot, outputSlot, insertFilter);
>>>>>>> 6b00e8500 (Rename sided-access layouts: furnace is top-only, bottomOut is any-side)
=======
    /** Inputs insertable from the top and all horizontal faces; outputs extractable from the bottom. */
    public static SidedLayout bottomOut(int[] inputSlots, int[] outputSlots, Predicate<ItemStack> insertFilter) {
        return new SidedLayout(inputSlots, inputSlots, outputSlots, inputSlots, outputSlots, insertFilter);
>>>>>>> 917a38ec2 (Support byproducts and multi-output in the recipe processor)
    }

    public int[] slotsForFace(Direction side) {
        return switch (side) {
            case UP -> upSlots;
            case DOWN -> downSlots;
            default -> sideSlots;
        };
    }

    public boolean canPlace(int slot, ItemStack stack, Direction dir) {
<<<<<<< HEAD
<<<<<<< HEAD
        return contains(inputSlots, slot) && contains(slotsForFace(dir), slot) && insertFilter.test(stack);
    }

    public boolean canTake(int slot, ItemStack stack, Direction dir) {
        return contains(outputSlots, slot) && contains(slotsForFace(dir), slot);
=======
        return slot == inputSlot && contains(slotsForFace(dir), slot) && insertFilter.test(stack);
    }

    public boolean canTake(int slot, ItemStack stack, Direction dir) {
        return slot == outputSlot && contains(slotsForFace(dir), slot);
>>>>>>> b9517e5fb (Add component-hosted machine framework)
=======
        return contains(inputSlots, slot) && contains(slotsForFace(dir), slot) && insertFilter.test(stack);
    }

    public boolean canTake(int slot, ItemStack stack, Direction dir) {
        return contains(outputSlots, slot) && contains(slotsForFace(dir), slot);
>>>>>>> 917a38ec2 (Support byproducts and multi-output in the recipe processor)
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
