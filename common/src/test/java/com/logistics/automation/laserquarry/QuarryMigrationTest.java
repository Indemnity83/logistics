package com.logistics.automation.laserquarry;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.automation.laserquarry.entity.QuarryComponent;
import com.logistics.core.machine.MachineComponentContainer;
import com.logistics.core.machine.component.EnergyStorageComponent;
import com.logistics.core.machine.upgrade.MachineModifiers;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Laser quarry save migration")
class QuarryMigrationTest extends MinecraftTestEnvironment {

    private final HolderLookup.Provider registries =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    private static EnergyStorageComponent energyComponent() {
        return new EnergyStorageComponent("energy", 10_000L, 128L, 0L, () -> {});
    }

    @Test
    @DisplayName("migrates stored energy and custom bounds from the pre-component root format")
    void migratesLegacyRoot() {
        // A world saved before the component format: energy under the root "Energy" key, quarry state
        // written at the root (no "components" tag).
        CompoundTag root = new CompoundTag();
        EnergyStorageComponent sourceEnergy = energyComponent();
        sourceEnergy.setAmount(5_000L);
        sourceEnergy.saveLegacy(root);

        QuarryComponent sourceQuarry =
                new QuarryComponent("quarry", BlockPos.ZERO, sourceEnergy, MachineModifiers.identity());
        sourceQuarry.setCustomBounds(1, 2, 3, 4);
        sourceQuarry.save(root, registries);

        // No "components" tag -> the container migrates via each component's loadLegacy.
        MachineComponentContainer components = new MachineComponentContainer();
        EnergyStorageComponent energy = components.add(energyComponent());
        QuarryComponent quarry =
                components.add(new QuarryComponent("quarry", BlockPos.ZERO, energy, MachineModifiers.identity()));
        components.load(root, registries);

        assertThat(energy.amount()).isEqualTo(5_000L);
        assertThat(quarry.hasCustomBounds()).isTrue();
        assertThat(quarry.customMinX()).isEqualTo(1);
        assertThat(quarry.customMinZ()).isEqualTo(2);
        assertThat(quarry.customMaxX()).isEqualTo(3);
        assertThat(quarry.customMaxZ()).isEqualTo(4);
    }

    @Test
    @DisplayName("round-trips through the component (new) format")
    void roundTripsComponentFormat() {
        CompoundTag root = new CompoundTag();
        MachineComponentContainer writer = new MachineComponentContainer();
        EnergyStorageComponent writeEnergy = writer.add(energyComponent());
        QuarryComponent writeQuarry =
                writer.add(new QuarryComponent("quarry", BlockPos.ZERO, writeEnergy, MachineModifiers.identity()));
        writeEnergy.setAmount(2_500L);
        writeQuarry.setCustomBounds(5, 6, 7, 8);
        writer.save(root, registries);

        MachineComponentContainer reader = new MachineComponentContainer();
        EnergyStorageComponent energy = reader.add(energyComponent());
        QuarryComponent quarry =
                reader.add(new QuarryComponent("quarry", BlockPos.ZERO, energy, MachineModifiers.identity()));
        reader.load(root, registries);

        assertThat(energy.amount()).isEqualTo(2_500L);
        assertThat(quarry.hasCustomBounds()).isTrue();
        assertThat(quarry.customMaxX()).isEqualTo(7);
    }
}
