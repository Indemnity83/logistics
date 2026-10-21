package com.logistics.core.machine.component;

import java.util.function.Predicate;
import java.util.stream.IntStream;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

/**
 * Static sided-access rules for a machine inventory. Each face exposes a set of slots; input slots
 * are insertable from the faces that expose them (subject to {@code insertFilter}) and output slots
 * are extractable from the faces that expose them.
 *
 * <p>Factories:
 * <ul>
 *   <li>{@link #furnace} — inputs from the top only (sides expose nothing), outputs from the bottom.</li>
 *   <li>{@link #bottomOut} — inputs from the top and all horizontal sides, outputs from the bottom.</li>
 *   <li>{@link #topIn} — inputs from the top only, nothing extractable.</li>
 *   <li>{@link #anyFace} — inputs and outputs on every face.</li>
 *   <li>{@link #open} — every slot insertable and extractable on every face.</li>
 * </ul>
 *
 * <p>A {@link SlotFilter} further restricts which items each slot accepts. It gates face insertion
 * ({@link #canPlace}) and face-independent placement ({@link #accepts}, backing
 * {@code Container#canPlaceItem}), so pipes, hoppers and menus that consult the container agree.
 */
public final class SidedLayout {

    private static final int[] NONE = new int[0];

    private final int[] upSlots;
    private final int[] sideSlots;
    private final int[] downSlots;
    private final int[] inputSlots;
    private final int[] outputSlots;
    private final Predicate<ItemStack> insertFilter;
    private final SlotFilter slotFilter;

    /** Which items a given slot accepts. */
    @FunctionalInterface
    public interface SlotFilter {
        SlotFilter ANY = (slot, stack) -> true;

        boolean test(int slot, ItemStack stack);
    }

    public SidedLayout(
            int[] upSlots,
            int[] sideSlots,
            int[] downSlots,
            int[] inputSlots,
            int[] outputSlots,
            Predicate<ItemStack> insertFilter) {
        this(upSlots, sideSlots, downSlots, inputSlots, outputSlots, insertFilter, SlotFilter.ANY);
    }

    public SidedLayout(
            int[] upSlots,
            int[] sideSlots,
            int[] downSlots,
            int[] inputSlots,
            int[] outputSlots,
            Predicate<ItemStack> insertFilter,
            SlotFilter slotFilter) {
        this.upSlots = upSlots;
        this.sideSlots = sideSlots;
        this.downSlots = downSlots;
        this.inputSlots = inputSlots;
        this.outputSlots = outputSlots;
        this.insertFilter = insertFilter;
        this.slotFilter = slotFilter;
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

    /**
     * Inputs insertable from the top only; nothing extractable. For machines that eject their product
     * directly (the Sequential Fabricator) rather than exposing an output slot pipes can pull from.
     */
    public static SidedLayout topIn(int[] inputSlots, Predicate<ItemStack> insertFilter) {
        return new SidedLayout(inputSlots, NONE, NONE, inputSlots, NONE, insertFilter);
    }

    /**
     * Inputs insertable and outputs extractable from every face. Slots in neither array are
     * unreachable from all faces. Pair with {@link #withSlotFilter} to route items by slot.
     */
    public static SidedLayout anyFace(int[] inputSlots, int[] outputSlots) {
        int[] all = new int[inputSlots.length + outputSlots.length];
        System.arraycopy(inputSlots, 0, all, 0, inputSlots.length);
        System.arraycopy(outputSlots, 0, all, inputSlots.length, outputSlots.length);
        return new SidedLayout(all, all, all, inputSlots, outputSlots, stack -> true);
    }

    /** Every one of {@code slotCount} slots insertable and extractable from every face. */
    public static SidedLayout open(int slotCount) {
        int[] all = IntStream.range(0, slotCount).toArray();
        return new SidedLayout(all, all, all, all, all, stack -> true);
    }

    /** This layout with {@code slotFilter} replacing its per-slot filter. */
    public SidedLayout withSlotFilter(SlotFilter slotFilter) {
        return new SidedLayout(upSlots, sideSlots, downSlots, inputSlots, outputSlots, insertFilter, slotFilter);
    }

    public int[] slotsForFace(Direction side) {
        return switch (side) {
            case UP -> upSlots;
            case DOWN -> downSlots;
            default -> sideSlots;
        };
    }

    public boolean canPlace(int slot, ItemStack stack, Direction dir) {
        return contains(inputSlots, slot)
                && contains(slotsForFace(dir), slot)
                && insertFilter.test(stack)
                && slotFilter.test(slot, stack);
    }

    /** Face-independent: whether the per-slot filter lets {@code stack} into {@code slot}. */
    public boolean accepts(int slot, ItemStack stack) {
        return slotFilter.test(slot, stack);
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
