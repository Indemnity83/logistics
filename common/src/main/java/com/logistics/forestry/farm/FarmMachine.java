package com.logistics.forestry.farm;

import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.machine.MachineBuilder;
import com.logistics.core.machine.component.EnergyStorageComponent;
import com.logistics.core.machine.component.FluidStoreComponent;
import com.logistics.core.machine.component.ItemStoreComponent;
import com.logistics.core.machine.component.SlotRole;
import java.util.function.Supplier;
import net.minecraft.tags.FluidTags;
import org.jetbrains.annotations.Nullable;

/**
 * The components every farm is built from, and its slot layout. Inserting on any face routes an
 * item by role (germling or soil slots); every face extracts only from the output slots.
 *
 * <p>Registration order is load-bearing: the energy buffer ticks before the farm spends from it.
 */
public record FarmMachine(
        FarmLogic logic,
        EnergyStorageComponent energy,
        @Nullable FluidStoreComponent water,
        ItemStoreComponent items,
        FarmComponent farm) {

    public static final int GERMLING_START = 0;
    public static final int GERMLING_END = 4;
    public static final int SOIL_START = 4;
    public static final int SOIL_END = 8;
    public static final int OUTPUT_START = 8;
    public static final int OUTPUT_END = 17;
    public static final int SLOT_COUNT = 17;

    /**
     * Builds a farm's components on {@code machine}. Buffer sizes are read from {@code settings}
     * once, now; the farm reads the rest of it every tick.
     */
    public static FarmMachine assemble(
            MachineBuilder machine,
            FarmLogic logic,
            Supplier<FarmSettings> settings,
            FarmComponent.WorldFactory worlds) {
        FarmSettings initial = settings.get();
        EnergyStorageComponent energy = machine.energy("energy")
                .capacity(initial.energyCapacity())
                .maxInput(initial.maxEnergyInput())
                .build();
        FluidStoreComponent water = logic.usesWater()
                ? machine.fluids("water")
                        .capacity(FluidUnits.mb(initial.waterCapacityMb()))
                        .inputOnly(fluid -> fluid.getFluid().defaultFluidState().is(FluidTags.WATER))
                        .build()
                : null;
        ItemStoreComponent items = machine.items("inventory")
                .slots(roles())
                .anyFaceAccess()
                .slotFilter(GERMLING_START, GERMLING_END, logic::isGermling)
                .slotFilter(SOIL_START, SOIL_END, stack -> logic.laysSoil() && logic.isSoil(stack))
                .build();
        FarmComponent farm = machine.add(new FarmComponent("farm", logic, items, energy, water, settings, worlds));
        return new FarmMachine(logic, energy, water, items, farm);
    }

    private static SlotRole[] roles() {
        SlotRole[] roles = new SlotRole[SLOT_COUNT];
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            roles[slot] = slot < OUTPUT_START ? SlotRole.INPUT : SlotRole.OUTPUT;
        }
        return roles;
    }
}
