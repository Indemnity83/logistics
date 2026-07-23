package com.logistics.core.lib.power.component;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.core.machine.FakeMachineContext;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class EngineEnergyOutputComponentTest extends MinecraftTestEnvironment {

    private final HolderLookup.Provider registries = new FakeMachineContext().registries();

    private EngineEnergyOutputComponent energy() {
        return new EngineEnergyOutputComponent("energy", () -> 1000L, () -> {});
    }

    @Test
    void addEnergy_capsAtCapacity() {
        EngineEnergyOutputComponent energy = energy();
        energy.addEnergy(1500);
        assertThat(energy.getAmount()).isEqualTo(1000);
    }

    @Test
    void addEnergy_ignoresNonPositive() {
        EngineEnergyOutputComponent energy = energy();
        energy.setAmount(100);
        energy.addEnergy(-10);
        assertThat(energy.getAmount()).isEqualTo(100);
    }

    @Test
    void neverAcceptsExternalInsert() {
        EngineEnergyOutputComponent energy = energy();
        long accepted = energy.energy(null).insert(500, false);
        assertThat(accepted).isEqualTo(0);
        assertThat(energy.getAmount()).isEqualTo(0);
    }

    @Test
    void saveAndLoad_roundTripsStoredEnergy() {
        EngineEnergyOutputComponent energy = energy();
        energy.setAmount(640);

        CompoundTag tag = new CompoundTag();
        energy.save(tag, registries);

        EngineEnergyOutputComponent restored = energy();
        restored.load(tag, registries);
        assertThat(restored.getAmount()).isEqualTo(640);
    }

    @Test
    void loadLegacy_readsRootStoredEnergy() {
        CompoundTag root = new CompoundTag();
        root.putLong("StoredEnergy", 321);

        EngineEnergyOutputComponent restored = energy();
        restored.loadLegacy(root, registries);
        assertThat(restored.getAmount()).isEqualTo(321);
    }
}
