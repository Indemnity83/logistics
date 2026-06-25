package com.logistics.core.machine;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.logistics.core.machine.component.EnergyStorageComponent;
import com.logistics.core.machine.component.ItemStoreComponent;
import com.logistics.core.machine.component.SlotRole;
import com.logistics.test.MinecraftTestEnvironment;
import org.junit.jupiter.api.Test;

class MachineBuilderTest extends MinecraftTestEnvironment {

    private static MachineBuilder builder() {
        return new MachineBuilder(new MachineComponentContainer(), () -> {});
    }

    @Test
    void recipeProcessorRejectsNonPositiveRfPerTick() {
        MachineBuilder machine = builder();
        ItemStoreComponent items =
                machine.items("items").slots(SlotRole.INPUT, SlotRole.OUTPUT).furnaceAccess().build();
        EnergyStorageComponent energy = machine.energy("energy").capacity(1_000).maxInput(1_000).build();

        for (long rfPerTick : new long[] {0, -1}) {
            assertThatThrownBy(() -> machine.recipeProcessor("processor")
                            .resolver((io, ctx) -> null)
                            .items(items)
                            .energy(energy)
                            .rfPerTick(rfPerTick)
                            .build())
                    .isInstanceOf(IllegalStateException.class);
        }
    }
}
