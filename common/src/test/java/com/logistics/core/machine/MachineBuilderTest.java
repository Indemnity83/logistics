package com.logistics.core.machine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.logistics.core.lib.fluids.InputOnlyFluidStorage;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.core.machine.component.EnergyStorageComponent;
import com.logistics.core.machine.component.FluidStoreComponent;
import com.logistics.core.machine.component.ItemStoreComponent;
import com.logistics.core.machine.component.SlotRole;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.Test;

class MachineBuilderTest extends MinecraftTestEnvironment {

    private static MachineBuilder builder() {
        return new MachineBuilder(new MachineComponentContainer(), () -> {});
    }

    @Test
    void itemsRejectsMissingAccessMode() {
        MachineBuilder machine = builder();

        assertThatThrownBy(() ->
                        machine.items("items").slots(SlotRole.INPUT, SlotRole.OUTPUT).build())
                .isInstanceOf(IllegalStateException.class);
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

    @Test
    void itemsRejectsSlotFilterOutsideItsSlots() {
        for (int[] range : new int[][] {{-1, 1}, {1, 1}, {0, 3}}) {
            MachineBuilder machine = builder();
            assertThatThrownBy(() -> machine.items("items")
                            .slots(SlotRole.INPUT, SlotRole.OUTPUT)
                            .anyFaceAccess()
                            .slotFilter(range[0], range[1], stack -> true)
                            .build())
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void overlappingSlotFiltersMustAllAccept() {
        ItemStoreComponent items = builder().items("items")
                .slots(SlotRole.INPUT, SlotRole.INPUT, SlotRole.OUTPUT)
                .anyFaceAccess()
                .slotFilter(0, 2, stack -> !stack.is(Items.DIRT))
                .slotFilter(1, 2, stack -> stack.is(Items.BONE_MEAL))
                .build();

        assertThat(items.accepts(0, new ItemStack(Items.WHEAT_SEEDS))).isTrue();
        assertThat(items.accepts(0, new ItemStack(Items.DIRT))).isFalse();
        assertThat(items.accepts(1, new ItemStack(Items.BONE_MEAL))).isTrue();
        assertThat(items.accepts(1, new ItemStack(Items.WHEAT_SEEDS))).isFalse();
        assertThat(items.accepts(2, new ItemStack(Items.DIRT))).isTrue();
    }

    @Test
    void fluidsRejectsInputOnlyCombinedWithOutputOnly() {
        MachineBuilder machine = builder();

        assertThatThrownBy(() -> machine.fluids("tank")
                        .capacity(1_000)
                        .outputOnly()
                        .inputOnly(fluid -> true)
                        .build())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void fluidsInputOnlyExposesAFillOnlyView() {
        FluidStoreComponent tank = builder().fluids("tank")
                .capacity(1_000)
                .inputOnly(fluid -> fluid.getFluid() == Fluids.WATER)
                .build();

        assertThat(tank.fluid(null)).isInstanceOf(InputOnlyFluidStorage.class);
        assertThat(tank.fluid(null).insert(SimpleFluidKey.of(Fluids.WATER), 500, false)).isEqualTo(500);
        assertThat(tank.fluid(null).insert(SimpleFluidKey.of(Fluids.LAVA), 500, false)).isZero();
        assertThat(tank.fluid(null).extract(SimpleFluidKey.of(Fluids.WATER), 500, false)).isZero();
    }
}
