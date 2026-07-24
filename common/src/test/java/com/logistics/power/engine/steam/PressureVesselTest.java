package com.logistics.power.engine.steam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class PressureVesselTest {

    @Test
    void addAndDrawTrackStoredPressure() {
        PressureVessel v = new PressureVessel();
        v.add(100);
        assertThat(v.pressure()).isEqualTo(100.0, within(1e-9));

        double drawn = v.draw(30);
        assertThat(drawn).isEqualTo(30.0, within(1e-9));
        assertThat(v.pressure()).isEqualTo(70.0, within(1e-9));
    }

    @Test
    void drawNeverGoesNegative() {
        PressureVessel v = new PressureVessel();
        v.add(10);
        double drawn = v.draw(25);
        assertThat(drawn).isEqualTo(10.0, within(1e-9)); // only what was stored
        assertThat(v.pressure()).isZero();
    }

    @Test
    void leakFloorsAtZero() {
        PressureVessel v = new PressureVessel();
        v.add(0.03);
        v.leak(0.05);
        assertThat(v.pressure()).isZero();
    }

    @Test
    void clampToBoundsPressure() {
        PressureVessel v = new PressureVessel();
        v.add(1500);
        v.clampTo(1000);
        assertThat(v.pressure()).isEqualTo(1000.0, within(1e-9));
    }

    @Test
    void fractionIsPressureOverMax() {
        PressureVessel v = new PressureVessel();
        v.add(250);
        assertThat(v.fraction(1000)).isEqualTo(0.25, within(1e-9));
        assertThat(v.fraction(0)).isZero(); // guard against divide-by-zero
    }

    @Test
    void roundTripsThroughNbt() {
        PressureVessel writer = new PressureVessel();
        writer.add(432.5);
        CompoundTag tag = new CompoundTag();
        writer.save(tag, "Pressure");

        PressureVessel reader = new PressureVessel();
        reader.load(tag, "Pressure");
        assertThat(reader.pressure()).isEqualTo(432.5, within(1e-9));
    }
}
