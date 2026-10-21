package com.logistics.forestry.farm;

import com.logistics.core.machine.MachineBuilder;
import com.logistics.core.machine.component.EnergyStorageComponent;
import com.logistics.core.machine.component.ItemStoreComponent;
import com.logistics.core.machine.component.SlotRole;
import com.logistics.core.machine.tier.TierComponent;
import java.util.Arrays;
import java.util.function.Supplier;

/**
 * The components every farm is built from. A farm has one generic inventory of
 * {@value #SLOT_COUNT} slots: anything goes in on any face, and any slot comes out on any face.
 *
 * <p>Registration order is load-bearing: the tier loads before the buffer it sizes, so a saved
 * charge is never clamped to a lower tier's capacity, and the energy buffer ticks before the farm
 * spends from it.
 */
public record FarmMachine(
        TierComponent tier,
        EnergyStorageComponent energy,
        ItemStoreComponent items,
        TendFarmComponent farm) {

    /** Slots in a farm's inventory. */
    public static final int SLOT_COUNT = 15;

    /**
     * Builds a farm's components on {@code machine}. The buffer is sized from {@code settings} for the
     * tier now and again whenever the tier changes; the farm reads the rest of it every tick.
     */
    public static FarmMachine assemble(
            MachineBuilder machine,
            TendLogic logic,
            Supplier<FarmSettings> settings,
            TendFarmComponent.WorldFactory worlds) {
        TierComponent tier = machine.tier("tier");
        EnergyStorageComponent energy = energy(machine, tier, settings);
        SlotRole[] roles = new SlotRole[SLOT_COUNT];
        Arrays.fill(roles, SlotRole.GENERIC);
        ItemStoreComponent items = machine.items("inventory").slots(roles).openAccess().build();
        TendFarmComponent farm =
                machine.add(new TendFarmComponent("farm", logic, items, energy, settings, tier::tier, worlds));
        return new FarmMachine(tier, energy, items, farm);
    }

    private static EnergyStorageComponent energy(
            MachineBuilder machine, TierComponent tier, Supplier<FarmSettings> settings) {
        FarmTierProfile initial = settings.get().profile(tier.tier());
        EnergyStorageComponent energy = machine.energy("energy")
                .capacity(initial.energyCapacity())
                .maxInput(initial.maxEnergyInput())
                .build();
        tier.onChange(newTier -> {
            FarmTierProfile profile = settings.get().profile(newTier);
            energy.resize(profile.energyCapacity(), profile.maxEnergyInput());
        });
        return energy;
    }
}
