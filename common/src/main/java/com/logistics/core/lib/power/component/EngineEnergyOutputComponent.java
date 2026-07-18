package com.logistics.core.lib.power.component;

import com.logistics.core.lib.energy.EnergyComponent;
import com.logistics.core.lib.energy.IEnergyStorage;
import com.logistics.core.lib.power.EngineEnergyPusher;
import com.logistics.core.machine.MachineComponent;
import java.util.function.LongSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * An engine's output energy buffer. Unlike a machine's {@code EnergyStorageComponent} (a sink that
 * reports demand and pulls from the network), this is a <em>source</em>: it never accepts energy
 * ({@code maxInsert = 0}) and is exposed to the world only on the output face (gated by the host).
 */
public final class EngineEnergyOutputComponent implements MachineComponent, MachineComponent.EnergyAccess {

    private static final String ENERGY_KEY = "StoredEnergy";

    private final String id;
    private final EnergyComponent buffer;
    private final Runnable onChanged;

    public EngineEnergyOutputComponent(String id, LongSupplier capacity, Runnable onChanged) {
        this.id = id;
        this.onChanged = onChanged;
        this.buffer = new EnergyComponent(capacity, /* maxInsert */ 0L, /* maxExtract */ Long.MAX_VALUE, onChanged);
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    @Nullable
    public IEnergyStorage energy(@Nullable Direction side) {
        return buffer;
    }

    public long getAmount() {
        return buffer.getAmount();
    }

    public long getCapacity() {
        return buffer.getCapacity();
    }

    /** Adds energy, capped at capacity, and marks dirty. */
    public void addEnergy(long amount) {
        if (amount <= 0) {
            return;
        }
        buffer.setAmount(buffer.getAmount() + amount);
        onChanged.run();
    }

    /** Sets the stored amount directly (clamped to capacity) and marks dirty. */
    public void setAmount(long amount) {
        buffer.setAmount(amount);
        onChanged.run();
    }

    /**
     * Push up to {@code outputPower} RF into the neighbor at {@code outputFace}, debiting the buffer.
     *
     * @return the amount sent ({@code >= 0})
     */
    public long push(@Nullable Level level, BlockPos pos, Direction outputFace, long outputPower) {
        long sent = EngineEnergyPusher.push(level, pos, outputFace, buffer, outputPower);
        if (sent > 0) {
            onChanged.run();
        }
        return sent;
    }

    @Override
    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        buffer.writeNbt(tag, ENERGY_KEY);
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        buffer.readNbt(tag, ENERGY_KEY);
    }

    // Pre-component (0.7.x) flat-root save format; prune when the 0.9 major opens.
    @Override
    public void loadLegacy(CompoundTag root, HolderLookup.Provider registries) {
        buffer.readNbt(root, ENERGY_KEY);
    }
}
