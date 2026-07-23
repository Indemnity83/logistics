package com.logistics.core.lib.power.component;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.machine.FakeMachineContext;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class ConstantFillComponentTest extends MinecraftTestEnvironment {

    private final HolderLookup.Provider registries = new FakeMachineContext().registries();

    @Test
    void fillsBufferToCapacityWhenPowered() {
        EngineEnergyOutputComponent energy = new EngineEnergyOutputComponent("energy", () -> 1000L, () -> {});
        ConstantFillComponent fill = new ConstantFillComponent("burn", energy, () -> true, null, null);

        fill.serverTick(new FakeMachineContext());
        assertThat(energy.getAmount()).isEqualTo(1000);
    }

    @Test
    void doesNotFillWhenUnpowered() {
        EngineEnergyOutputComponent energy = new EngineEnergyOutputComponent("energy", () -> 1000L, () -> {});
        ConstantFillComponent fill = new ConstantFillComponent("burn", energy, () -> false, null, null);

        fill.serverTick(new FakeMachineContext());
        assertThat(energy.getAmount()).isEqualTo(0);
    }

    @Test
    void persistsOutputLevelIndex() {
        EngineEnergyOutputComponent energy = new EngineEnergyOutputComponent("energy", () -> 1000L, () -> {});
        int[] index = {3};
        ConstantFillComponent fill =
                new ConstantFillComponent("burn", energy, () -> true, () -> index[0], v -> index[0] = v);

        CompoundTag tag = new CompoundTag();
        fill.save(tag, registries);
        assertThat(NbtCompat.getInt(tag, "OutputLevelIndex", -1)).isEqualTo(3);

        index[0] = 0;
        fill.load(tag, registries);
        assertThat(index[0]).isEqualTo(3);
    }
}
