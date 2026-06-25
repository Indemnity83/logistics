package com.logistics.core.machine;

import com.logistics.core.lib.energy.IEnergyStorage;
import com.logistics.core.lib.fluids.IFluidStorage;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A reusable unit of machine behavior hosted by a {@link MachineEntity}.
 *
 * <p>The host owns a set of components, ticks them in registration order, and dispatches
 * save/load to each under its {@link #id()}. Components expose capabilities to the world by
 * implementing the nested access interfaces, which the host queries generically.
 */
public interface MachineComponent {

    /** Stable, unique key used for this component's NBT sub-tag. */
    String id();

    /** Called once on the server after deserialization, before the first tick. */
    default void onLoad(MachineContext ctx) {}

    /** Per-tick behavior; only invoked server-side. */
    default void serverTick(MachineContext ctx) {}

    default void save(CompoundTag tag, HolderLookup.Provider registries) {}

    default void load(CompoundTag tag, HolderLookup.Provider registries) {}

    /**
     * Read pre-component-format save data from the {@code LogisticsData} root, for worlds saved
     * before this component existed. Called instead of {@link #load} when no {@code components}
     * sub-tag is present.
     */
    default void loadLegacy(CompoundTag root, HolderLookup.Provider registries) {}

    // ==================== Capability access interfaces ====================

    /** Exposes sided energy storage. */
    interface EnergyAccess {
        @Nullable
        IEnergyStorage energy(@Nullable Direction side);
    }

    /** Exposes sided fluid storage. */
    interface FluidAccess {
        @Nullable
        IFluidStorage fluid(@Nullable Direction side);
    }

    /** Reports per-tick energy demand to the logistics network. */
    interface Demand {
        long networkDemandPerTick();
    }

    /** Provides a vanilla {@link Container} with sided access rules for {@code WorldlyContainer} delegation. */
    interface SidedItems {
        Container container();

        int[] slotsForFace(Direction side);

        boolean canPlace(int slot, ItemStack stack, Direction dir);

        boolean canTake(int slot, ItemStack stack, Direction dir);
    }

    /** Reports recipe-processing state for HUD/integration code. */
    interface ProcessState {
        float progress();

        boolean isProcessing();
    }
}
