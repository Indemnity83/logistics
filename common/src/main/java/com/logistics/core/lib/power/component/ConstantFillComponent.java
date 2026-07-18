package com.logistics.core.lib.power.component;

import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.machine.MachineComponent;
import com.logistics.core.machine.MachineContext;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

/**
 * Infinite generation for the Creative Engine: while powered, fills the buffer to capacity every
 * tick. Optionally persists an output-level index (the sneak-wrench-cycled rate).
 */
public final class ConstantFillComponent implements MachineComponent {

    private static final String LEVEL_KEY = "OutputLevelIndex";

    private final String id;
    private final EngineEnergyOutputComponent energy;
    private final BooleanSupplier powered;
    @Nullable
    private final IntSupplier levelIndexGetter;
    @Nullable
    private final IntConsumer levelIndexSetter;

    public ConstantFillComponent(
            String id,
            EngineEnergyOutputComponent energy,
            BooleanSupplier powered,
            @Nullable IntSupplier levelIndexGetter,
            @Nullable IntConsumer levelIndexSetter) {
        this.id = id;
        this.energy = energy;
        this.powered = powered;
        this.levelIndexGetter = levelIndexGetter;
        this.levelIndexSetter = levelIndexSetter;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public void serverTick(MachineContext ctx) {
        if (powered.getAsBoolean()) {
            energy.setAmount(energy.getCapacity());
        }
    }

    @Override
    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        if (levelIndexGetter != null) {
            tag.putInt(LEVEL_KEY, levelIndexGetter.getAsInt());
        }
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        readState(tag);
    }

    @Override
    public void loadLegacy(CompoundTag root, HolderLookup.Provider registries) {
        readState(root);
    }

    private void readState(CompoundTag tag) {
        if (levelIndexSetter != null) {
            levelIndexSetter.accept(NbtCompat.getInt(tag, LEVEL_KEY, 0));
        }
    }
}
