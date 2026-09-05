package com.logistics.core.lib.tank;

import com.logistics.core.lib.fluids.IFluidKey;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import net.minecraft.core.component.DataComponents;

/**
 * The tank-column fluid algorithm, owned once here instead of copied into each loader's adapter. A
 * column is a list of {@link TankCell}s ordered bottom-to-top that all share one fluid. Every operation
 * works in native units and re-settles the column via {@link FluidColumn} (liquids to the bottom, gases
 * to the top), keyed on the loader-agnostic {@link IFluidKey} (so no generic {@code FluidKind} seam is needed).
 *
 * <p>Rules, unit-testable without a game:
 * <ul>
 *   <li><b>Mixed columns</b> (two distinct fluids joined together) refuse every operation.</li>
 *   <li><b>Potions</b> are sealed from the fluid path — {@link #insert}/{@link #extract} reject them, so
 *       they move only via {@link #depositBottle}/{@link #extractBottle}.</li>
 *   <li><b>Single fluid + capacity + backpressure</b> fall out of the per-cell math.</li>
 * </ul>
 */
public final class TankColumn {

    private final List<? extends TankCell> cells;
    private final Predicate<IFluidKey> isGas;

    /** @param cells the column ordered bottom-to-top */
    public TankColumn(List<? extends TankCell> cells, Predicate<IFluidKey> isGas) {
        this.cells = cells;
        this.isGas = isGas;
    }

    // ==================== aggregation ====================

    /** Total contents of the column, in native units. */
    public long total() {
        long total = 0;
        for (TankCell cell : cells) {
            total += cell.amount();
        }
        return total;
    }

    /** Total capacity of the column, in native units. */
    public long capacity() {
        long capacity = 0;
        for (TankCell cell : cells) {
            capacity += cell.capacity();
        }
        return capacity;
    }

    /** Free space left in the column, in native units. */
    public long room() {
        return capacity() - total();
    }

    /** The single fluid the column holds, or a blank key if it is empty. */
    public IFluidKey shared() {
        for (TankCell cell : cells) {
            if (!cell.fluid().isBlank()) {
                return cell.fluid();
            }
        }
        return SimpleFluidKey.BLANK;
    }

    /** Whether the column holds two or more distinct fluids (operations refuse to act on it). */
    public boolean mixed() {
        IFluidKey seen = null;
        for (TankCell cell : cells) {
            IFluidKey fluid = cell.fluid();
            if (fluid.isBlank()) {
                continue;
            }
            if (seen == null) {
                seen = fluid;
            } else if (!keysEqual(seen, fluid)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isPotion(IFluidKey fluid) {
        if (fluid.isBlank()) {
            return false;
        }
        return fluid.getComponents().split().added().has(DataComponents.POTION_CONTENTS);
    }

    // ==================== fluid-API operations ====================

    /**
     * Adds up to {@code max} native units of {@code resource} to the column. Returns the amount that was
     * (or, when {@code simulate}, would be) added — 0 if the column is mixed, sealed by a potion, holds a
     * different fluid, or is full.
     */
    public long insert(IFluidKey resource, long max, boolean simulate) {
        if (resource.isBlank() || max <= 0 || mixed()) {
            return 0;
        }
        IFluidKey current = shared();
        if (isPotion(current) || isPotion(resource)) {
            return 0; // sealed: potions are deposited only via depositBottle
        }
        if (!accepts(current, resource)) {
            return 0;
        }
        long take = Math.min(max, room());
        if (take <= 0) {
            return 0;
        }
        if (!simulate) {
            distribute(resource, total() + take);
        }
        return take;
    }

    /**
     * Removes up to {@code max} native units of {@code resource}. Returns the amount that was (or would
     * be) removed — 0 if mixed, sealed, empty, or holding a different fluid.
     */
    public long extract(IFluidKey resource, long max, boolean simulate) {
        if (resource.isBlank() || max <= 0 || mixed()) {
            return 0;
        }
        IFluidKey current = shared();
        if (isPotion(current)) {
            return 0; // sealed: potions are drawn only via extractBottle
        }
        if (!holds(current, resource)) {
            return 0;
        }
        long take = Math.min(max, total());
        if (take <= 0) {
            return 0;
        }
        if (!simulate) {
            distribute(current, total() - take);
        }
        return take;
    }

    /** Deposits {@code bottleAmount} native units of {@code resource}, all-or-nothing. Returns whether it fit. */
    public boolean depositBottle(IFluidKey resource, long bottleAmount) {
        if (resource.isBlank() || bottleAmount <= 0 || mixed()) {
            return false;
        }
        IFluidKey current = shared();
        if (!accepts(current, resource)) {
            return false;
        }
        if (room() < bottleAmount) {
            return false;
        }
        distribute(resource, total() + bottleAmount);
        return true;
    }

    /** Draws {@code bottleAmount} native units of {@code resource} out of the column, all-or-nothing. */
    public boolean extractBottle(IFluidKey resource, long bottleAmount) {
        if (resource.isBlank() || bottleAmount <= 0 || mixed()) {
            return false;
        }
        IFluidKey current = shared();
        if (!holds(current, resource)) {
            return false;
        }
        if (total() < bottleAmount) {
            return false;
        }
        distribute(current, total() - bottleAmount);
        return true;
    }

    // ==================== settling ====================

    /**
     * Re-settles the column's existing contents (liquids down, gases up) and writes each cell's share.
     * Returns the cells whose contents changed (empty if nothing moved, the column is empty, or it holds
     * mixed fluids and is left alone).
     */
    public List<TankCell> rebalance() {
        IFluidKey shared = shared();
        if (shared.isBlank() || mixed()) {
            return List.of();
        }
        long[] settled = settle(shared, total());
        List<TankCell> changed = new ArrayList<>();
        for (int i = 0; i < cells.size(); i++) {
            TankCell cell = cells.get(i);
            long target = settled[i];
            if (cell.amount() != target) {
                cell.setContents(shared, target);
                changed.add(cell);
            }
        }
        return changed;
    }

    private void distribute(IFluidKey fluid, long total) {
        long[] settled = settle(fluid, total);
        for (int i = 0; i < cells.size(); i++) {
            cells.get(i).setContents(fluid, settled[i]);
        }
    }

    private long[] settle(IFluidKey fluid, long total) {
        long[] capacities = new long[cells.size()];
        for (int i = 0; i < cells.size(); i++) {
            capacities[i] = cells.get(i).capacity();
        }
        return FluidColumn.settle(capacities, total, isGas.test(fluid));
    }

    /** Whether {@code resource} may be added to a column currently holding {@code current} (empty or same fluid). */
    private static boolean accepts(IFluidKey current, IFluidKey resource) {
        return current.isBlank() || keysEqual(current, resource);
    }

    /** Whether the column currently holds exactly {@code resource} (non-blank, same fluid). */
    private static boolean holds(IFluidKey current, IFluidKey resource) {
        return !current.isBlank() && keysEqual(current, resource);
    }

    private static boolean keysEqual(IFluidKey a, IFluidKey b) {
        return a.getFluid() == b.getFluid() && Objects.equals(a.getComponents(), b.getComponents());
    }
}
