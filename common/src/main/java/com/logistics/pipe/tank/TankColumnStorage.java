package com.logistics.pipe.tank;

import com.logistics.core.lib.fluids.IFluidKey;
import com.logistics.core.lib.fluids.IFluidStorage;
import com.logistics.core.lib.fluids.IFluidView;
import com.logistics.core.lib.tank.TankColumn;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Exposes a tank's whole vertical column as a single {@link IFluidStorage}, so pipes and the loader
 * bucket helper both act on the column rather than one cell. The {@link TankColumn} is rebuilt per call
 * from {@code columnSupplier} so it always reflects the current stack.
 */
public final class TankColumnStorage implements IFluidStorage {

    private final Supplier<TankColumn> columnSupplier;

    public TankColumnStorage(Supplier<TankColumn> columnSupplier) {
        this.columnSupplier = columnSupplier;
    }

    @Override
    public long insert(IFluidKey fluid, long maxAmount, boolean simulate) {
        return columnSupplier.get().insert(fluid, maxAmount, simulate);
    }

    @Override
    public long extract(IFluidKey fluid, long maxAmount, boolean simulate) {
        return columnSupplier.get().extract(fluid, maxAmount, simulate);
    }

    @Override
    public Iterable<IFluidView> contents() {
        TankColumn column = columnSupplier.get();
        if (column.mixed()) {
            return Collections.emptyList(); // refuse to report a single view for a mixed column, like insert/extract
        }
        IFluidKey shared = column.shared();
        long total = column.total();
        if (shared.isBlank() || total <= 0) {
            return Collections.emptyList();
        }
        long amount = total;
        long capacity = column.capacity();
        return List.of(new IFluidView() {
            @Override
            public IFluidKey resource() {
                return shared;
            }

            @Override
            public long amount() {
                return amount;
            }

            @Override
            public long capacity() {
                return capacity;
            }
        });
    }
}
