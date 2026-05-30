package com.logistics.core.lib.energy;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EnergyComponent")
class EnergyComponentTest {

    @Test
    @DisplayName("should ignore non-positive insert requests")
    void insert_nonPositiveRequest_returnsZeroWithoutMutation() {
        EnergyComponent energy = new EnergyComponent(100, 50, 50, () -> {});
        energy.setAmount(40);

        assertThat(energy.insert(-10, false)).isZero();
        assertThat(energy.insert(0, false)).isZero();
        assertThat(energy.getAmount()).isEqualTo(40);
    }

    @Test
    @DisplayName("should ignore non-positive extract requests")
    void extract_nonPositiveRequest_returnsZeroWithoutMutation() {
        EnergyComponent energy = new EnergyComponent(100, 50, 50, () -> {});
        energy.setAmount(40);

        assertThat(energy.extract(-10, false)).isZero();
        assertThat(energy.extract(0, false)).isZero();
        assertThat(energy.getAmount()).isEqualTo(40);
    }

    @Test
    @DisplayName("should treat negative insert and extract limits as zero")
    void transfer_negativeLimits_returnZero() {
        EnergyComponent noInsert = new EnergyComponent(100, -1, 50, () -> {});
        EnergyComponent noExtract = new EnergyComponent(100, 50, -1, () -> {});
        noExtract.setAmount(40);

        assertThat(noInsert.insert(10, false)).isZero();
        assertThat(noInsert.getAmount()).isZero();
        assertThat(noExtract.extract(10, false)).isZero();
        assertThat(noExtract.getAmount()).isEqualTo(40);
    }

    @Test
    @DisplayName("should treat negative dynamic capacity as zero")
    void capacity_negativeSupplier_isZero() {
        EnergyComponent energy = new EnergyComponent(() -> -100L, 50, 50, () -> {});

        assertThat(energy.getCapacity()).isZero();
        assertThat(energy.insert(10, false)).isZero();

        energy.setAmount(25);
        assertThat(energy.getAmount()).isZero();

        CompoundTag tag = new CompoundTag();
        tag.putLong("Energy", 25L);
        energy.readNbt(tag, "Energy");
        assertThat(energy.getAmount()).isZero();
    }

    @Test
    @DisplayName("should only consume positive amounts and clamp at zero")
    void consume_onlySubtractsPositiveAmounts() {
        EnergyComponent energy = new EnergyComponent(100, 50, 50, () -> {});
        energy.setAmount(40);

        energy.consume(-10);
        energy.consume(0);
        assertThat(energy.getAmount()).isEqualTo(40);

        energy.consume(100);
        assertThat(energy.getAmount()).isZero();
    }
}
