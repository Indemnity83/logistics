package com.logistics.core.lib.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Loader-agnostic service for pushing energy from a source buffer into an adjacent block.
 *
 * <p>Set once during loader-specific initialization (Fabric or NeoForge bootstrap) via
 * {@link #set}. Used by energy producers (engines) and buffers (batteries) to send energy
 * to neighboring blocks without importing platform-specific energy APIs.
 */
@FunctionalInterface
public interface EnergyPushService {
    /**
     * Push up to {@code maxAmount} energy into the block at {@code targetPos}, approached from
     * {@code fromDirection}.
     *
     * <p>Implementations must <em>not</em> mutate {@code source}; only return the amount
     * transferred. The caller is the single authority that debits the source buffer
     * (e.g. via {@link EnergyComponent#consume(long)}).
     *
     * @return the amount actually transferred (must be &ge; 0 and &le; maxAmount)
     */
    long push(Level level, BlockPos targetPos, Direction fromDirection, IEnergyStorage source, long maxAmount);

    /** Registers the loader-specific implementation. Called once during bootstrap. */
    static void set(EnergyPushService service) {
        if (service == null) throw new IllegalArgumentException("EnergyPushService must not be null");
        Holder.instance = service;
    }

    /** Returns the registered implementation, or {@code null} if none has been set yet. */
    @Nullable
    static EnergyPushService get() {
        return Holder.instance;
    }

    final class Holder {
        private static EnergyPushService instance;

        private Holder() {}
    }
}
