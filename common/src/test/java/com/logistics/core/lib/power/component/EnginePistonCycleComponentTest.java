package com.logistics.core.lib.power.component;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.core.machine.FakeMachineContext;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class EnginePistonCycleComponentTest extends MinecraftTestEnvironment {

    private final HolderLookup.Provider registries = new FakeMachineContext().registries();

    private EnginePistonCycleComponent cycle(boolean powered, double pistonSpeed) {
        EngineEnergyOutputComponent energy = new EngineEnergyOutputComponent("energy", () -> 1000L, () -> {});
        EngineHeatComponent heat = new EngineHeatComponent(
                "heat", energy, 20, 250, true, 10, null, () -> false, () -> false, () -> {});
        return new EnginePistonCycleComponent(
                "cycle", energy, heat, () -> powered, () -> 10L, () -> Direction.NORTH, () -> pistonSpeed, false, () -> {});
    }

    @Test
    void idleUntilPowered() {
        EnginePistonCycleComponent cycle = cycle(/* powered */ false, 0.08);
        cycle.serverTick(new FakeMachineContext());
        assertThat(cycle.progress()).isEqualTo(0f);
        assertThat(cycle.isCompression()).isFalse();
    }

    @Test
    void advancesToCompressionOverTheStroke() {
        EnginePistonCycleComponent cycle = cycle(/* powered */ true, 0.08);
        FakeMachineContext ctx = new FakeMachineContext();

        boolean reachedCompression = false;
        for (int i = 0; i < 8; i++) {
            cycle.serverTick(ctx);
            reachedCompression |= cycle.isCompression();
        }
        assertThat(reachedCompression).isTrue();
        assertThat(cycle.progress()).isGreaterThan(0f);
    }

    @Test
    void saveAndLoad_roundTripsProgressAndPhase() {
        EnginePistonCycleComponent cycle = cycle(true, 0.08);
        FakeMachineContext ctx = new FakeMachineContext();
        for (int i = 0; i < 4; i++) {
            cycle.serverTick(ctx);
        }

        CompoundTag tag = new CompoundTag();
        cycle.save(tag, registries);

        EnginePistonCycleComponent restored = cycle(true, 0.08);
        restored.load(tag, registries);
        assertThat(restored.progress()).isEqualTo(cycle.progress());
    }
}
