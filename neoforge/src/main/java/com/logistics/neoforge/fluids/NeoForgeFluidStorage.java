package com.logistics.neoforge.fluids;

import com.logistics.core.lib.fluids.IFluidKey;
import com.logistics.core.lib.fluids.IFluidStorage;
import com.logistics.core.lib.fluids.IFluidView;
import java.util.ArrayList;
import java.util.List;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import org.jetbrains.annotations.Nullable;

/**
 * Bridges the NeoForge 21.1 {@link IFluidHandler} to the common {@link IFluidStorage}
 * abstraction and vice versa.
 *
 * <p>NeoForge 21.1 uses {@link FluidAction#SIMULATE} / {@link FluidAction#EXECUTE}
 * for simulation, which maps cleanly to the common interface's simulate boolean.
 */
public final class NeoForgeFluidStorage implements IFluidStorage {
    private final IFluidHandler handler;

    private NeoForgeFluidStorage(IFluidHandler handler) {
        this.handler = handler;
    }

    /**
     * Wrap a NeoForge {@link IFluidHandler} as the common {@link IFluidStorage}.
     *
     * @param handler the NeoForge handler to wrap; may be {@code null}
     * @return the wrapped storage, or {@code null} if handler is {@code null}
     */
    @Nullable
    public static IFluidStorage wrap(@Nullable IFluidHandler handler) {
        return handler == null ? null : new NeoForgeFluidStorage(handler);
    }

    /**
     * Adapt a common {@link IFluidStorage} to a NeoForge {@link IFluidHandler}.
     *
     * <p>The resulting handler reports a single tank that reflects the first non-empty
     * fluid in the common storage.
     *
     * @param storage the common storage to adapt; may be {@code null}
     * @return the NeoForge adapter, or {@code null} if storage is {@code null}
     */
    @Nullable
    public static IFluidHandler asNeoForge(@Nullable IFluidStorage storage) {
        return storage == null ? null : new CommonFluidHandler(storage);
    }

    @Override
    public long insert(IFluidKey fluid, long maxAmount, boolean simulate) {
        if (fluid.isBlank() || maxAmount <= 0) {
            return 0;
        }
        FluidStack resource = toStack(fluid, clampToInt(maxAmount));
        FluidAction action = simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE;
        return handler.fill(resource, action);
    }

    @Override
    public long extract(IFluidKey fluid, long maxAmount, boolean simulate) {
        if (fluid.isBlank() || maxAmount <= 0) {
            return 0;
        }
        FluidStack resource = toStack(fluid, clampToInt(maxAmount));
        FluidAction action = simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE;
        FluidStack drained = handler.drain(resource, action);
        return drained.isEmpty() ? 0 : drained.getAmount();
    }

    @Override
    public Iterable<IFluidView> contents() {
        List<IFluidView> views = new ArrayList<>();
        for (int i = 0; i < handler.getTanks(); i++) {
            FluidStack stack = handler.getFluidInTank(i);
            if (stack.isEmpty()) {
                continue;
            }
            final NeoForgeFluidKey key = NeoForgeFluidKey.of(stack);
            final long amount = stack.getAmount();
            views.add(new IFluidView() {
                @Override public IFluidKey resource() { return key; }
                @Override public long amount() { return amount; }
            });
        }
        return views;
    }

    private static FluidStack toStack(IFluidKey key, int amount) {
        if (key instanceof NeoForgeFluidKey nfKey) {
            return nfKey.toStack(amount);
        }
        return new FluidStack(key.getFluid(), amount);
    }

    private static int clampToInt(long amount) {
        return (int) Math.max(0, Math.min(amount, Integer.MAX_VALUE));
    }

    /**
     * Adapts a common {@link IFluidStorage} to NeoForge's tank-based {@link IFluidHandler}.
     *
     * <p>Reports a single tank that reflects the first non-empty fluid in the common storage.
     */
    private static final class CommonFluidHandler implements IFluidHandler {
        private final IFluidStorage storage;

        private CommonFluidHandler(IFluidStorage storage) {
            this.storage = storage;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            checkIndex(tank);
            for (IFluidView view : storage.contents()) {
                if (view.amount() > 0) {
                    return toStack(view.resource(), clampToInt(view.amount()));
                }
            }
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            checkIndex(tank);
            // Report a sensible maximum; use the first fluid type to probe capacity
            for (IFluidView view : storage.contents()) {
                if (view.amount() > 0) {
                    long capacity = storage.insert(view.resource(), Integer.MAX_VALUE, true) + view.amount();
                    return clampToInt(capacity);
                }
            }
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            checkIndex(tank);
            if (stack.isEmpty()) {
                return false;
            }
            return storage.insert(NeoForgeFluidKey.of(stack), 1, true) > 0;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return 0;
            }
            IFluidKey key = NeoForgeFluidKey.of(resource);
            return clampToInt(storage.insert(key, resource.getAmount(), action.simulate()));
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return FluidStack.EMPTY;
            }
            IFluidKey key = NeoForgeFluidKey.of(resource);
            long extracted = storage.extract(key, resource.getAmount(), action.simulate());
            return extracted <= 0 ? FluidStack.EMPTY : toStack(key, clampToInt(extracted));
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0) {
                return FluidStack.EMPTY;
            }
            // Drain whatever fluid type is present first
            for (IFluidView view : storage.contents()) {
                if (view.amount() > 0) {
                    IFluidKey key = view.resource();
                    long extracted = storage.extract(key, maxDrain, action.simulate());
                    if (extracted > 0) {
                        return toStack(key, clampToInt(extracted));
                    }
                }
            }
            return FluidStack.EMPTY;
        }

        private static void checkIndex(int index) {
            if (index != 0) {
                throw new IndexOutOfBoundsException(index);
            }
        }
    }
}
