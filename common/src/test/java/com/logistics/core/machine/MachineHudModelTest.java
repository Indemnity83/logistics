package com.logistics.core.machine;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.test.MinecraftTestEnvironment;
import java.util.List;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.Test;

class MachineHudModelTest extends MinecraftTestEnvironment {

    @Test
    void emptyModelWritesNothing() {
        CompoundTag data = new CompoundTag();
        new MachineHudModel().save(data);

        assertThat(data.isEmpty()).isTrue();
        assertThat(MachineHudModel.entries(data)).isEmpty();
    }

    @Test
    void roundTripsProgressAndFluidInOrder() {
        MachineHudModel model = new MachineHudModel();
        model.progress(0.42f);
        model.fluid(Fluids.LAVA, DataComponentPatch.EMPTY, 500L, 10_000L);

        CompoundTag data = new CompoundTag();
        model.save(data);
        List<MachineHudModel.Entry> entries = MachineHudModel.entries(data);

        assertThat(entries).hasSize(2);
        assertThat(entries.get(0)).isInstanceOf(MachineHudModel.ProgressEntry.class);
        assertThat(((MachineHudModel.ProgressEntry) entries.get(0)).fraction()).isEqualTo(0.42f);

        MachineHudModel.FluidEntry fluid = (MachineHudModel.FluidEntry) entries.get(1);
        assertThat(fluid.fluidId()).isEqualTo(BuiltInRegistries.FLUID.getKey(Fluids.LAVA).toString());
        assertThat(fluid.amountMb()).isEqualTo(500L);
        assertThat(fluid.capacityMb()).isEqualTo(10_000L);
    }
}
