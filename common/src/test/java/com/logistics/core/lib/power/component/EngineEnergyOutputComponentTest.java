package com.logistics.core.lib.power.component;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.core.machine.FakeMachineContext;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

    // ==================== Generation counter ====================

    /** One tick of generation, with the component's own tick (the rollover) before or after it. */
    private static void generateTick(EngineEnergyOutputComponent energy, boolean rolloverFirst, long... amounts) {
        FakeMachineContext ctx = new FakeMachineContext();
        if (rolloverFirst) {
            energy.serverTick(ctx);
        }
        for (long amount : amounts) {
            energy.addEnergy(amount);
        }
        if (!rolloverFirst) {
            energy.serverTick(ctx);
        }
    }

    @ParameterizedTest(name = "rollover first = {0}")
    @ValueSource(booleans = {true, false})
    void generatedLastTickIsOneCompletedTicksTotal(boolean rolloverFirst) {
        EngineEnergyOutputComponent energy = energy();

        generateTick(energy, rolloverFirst, 20, 5);
        generateTick(energy, rolloverFirst, 20, 5);
        if (rolloverFirst) {
            energy.serverTick(new FakeMachineContext());
        }

        assertThat(energy.generatedLastTick()).isEqualTo(25);
    }

    @ParameterizedTest(name = "rollover first = {0}")
    @ValueSource(booleans = {true, false})
    void generatedLastTickReadsZeroOneTickAfterGenerationStops(boolean rolloverFirst) {
        EngineEnergyOutputComponent energy = energy();
        generateTick(energy, rolloverFirst, 25);
        generateTick(energy, rolloverFirst);
        if (rolloverFirst) {
            energy.serverTick(new FakeMachineContext());
        }

        assertThat(energy.generatedLastTick()).isZero();
    }

    @Test
    void generationAtAFullBufferRecordsOnlyWhatFit() {
        EngineEnergyOutputComponent energy = energy();
        energy.setAmount(990);

        energy.addEnergy(40);
        energy.serverTick(new FakeMachineContext());
        assertThat(energy.generatedLastTick()).isEqualTo(10);

        energy.addEnergy(40);
        energy.serverTick(new FakeMachineContext());
        assertThat(energy.getAmount()).isEqualTo(1000);
        assertThat(energy.generatedLastTick()).isZero();
    }

    @Test
    void loadSetAmountAndInsertRecordNoGeneration() {
        EngineEnergyOutputComponent source = energy();
        source.setAmount(640);
        CompoundTag tag = new CompoundTag();
        source.save(tag, registries);

        EngineEnergyOutputComponent energy = energy();
        energy.load(tag, registries);
        energy.setAmount(100); // heat decay / bleed
        energy.setAmount(900);
        energy.energy(null).insert(50, false);
        energy.serverTick(new FakeMachineContext());

        assertThat(energy.generatedLastTick()).isZero();
    }

    @Test
    void pushingOutRecordsNoGeneration() {
        EngineEnergyOutputComponent energy = energy();
        energy.addEnergy(300);
        energy.serverTick(new FakeMachineContext());
        energy.energy(null).extract(200, false);
        energy.serverTick(new FakeMachineContext());

        assertThat(energy.generatedLastTick()).isZero();
        assertThat(energy.getAmount()).isEqualTo(100);
    }
}
