package com.logistics.forestry.farm;

import com.logistics.core.machine.MachineBuilder;
import com.logistics.core.machine.component.EnergyStorageComponent;
import com.logistics.core.machine.component.ItemStoreComponent;
import com.logistics.core.machine.component.SlotRole;
import com.logistics.core.machine.tier.TierComponent;
import java.util.Arrays;
import java.util.function.Supplier;
import org.jetbrains.annotations.Nullable;

/**
 * The components every farm is built from, and its slot layout.
 *
 * <p>A ring farm ({@link #assemble}) routes an item inserted on any face by role (germling or soil
 * slots), and every face extracts only from the output slots. A tend farm ({@link #assembleTend})
 * has one generic inventory of {@value #TEND_SLOT_COUNT} slots: anything goes in on any face, and
 * any slot comes out on any face.
 *
 * <p>Registration order is load-bearing: the tier loads before the buffers it sizes, so a saved
 * charge is never clamped to a lower tier's capacity, and the energy buffer ticks before the farm
 * spends from it.
 *
 * @param logic the ring farm's logic; {@code null} for a tend farm
 */
public record FarmMachine(
        @Nullable FarmLogic logic,
        TierComponent tier,
        EnergyStorageComponent energy,
        ItemStoreComponent items,
        FarmEngine farm) {

    public static final int GERMLING_START = 0;
    public static final int GERMLING_END = 4;
    public static final int SOIL_START = 4;
    public static final int SOIL_END = 8;
    public static final int OUTPUT_START = 8;
    public static final int OUTPUT_END = 17;
    public static final int SLOT_COUNT = 17;

    /** Slots in a tend farm's inventory. */
    public static final int TEND_SLOT_COUNT = 15;

    /**
     * Builds a ring farm's components on {@code machine}. Buffer sizes are read from {@code settings}
     * for the tier now and again whenever the tier changes; the farm reads the rest of it every tick.
     */
    public static FarmMachine assemble(
            MachineBuilder machine,
            FarmLogic logic,
            Supplier<FarmSettings> settings,
            FarmComponent.WorldFactory worlds) {
        TierComponent tier = machine.tier("tier");
        EnergyStorageComponent energy = energy(machine, tier, settings);
        ItemStoreComponent items = machine.items("inventory")
                .slots(roles())
                .anyFaceAccess()
                .slotFilter(GERMLING_START, GERMLING_END, logic::isGermling)
                .slotFilter(SOIL_START, SOIL_END, stack -> logic.laysSoil() && logic.isSoil(stack))
                .build();
        FarmComponent farm =
                machine.add(new FarmComponent("farm", logic, items, energy, settings, tier::tier, worlds));
        return new FarmMachine(logic, tier, energy, items, farm);
    }

    /** Builds a tend farm's components on {@code machine}; buffers are sized as {@link #assemble} sizes them. */
    public static FarmMachine assembleTend(
            MachineBuilder machine,
            TendLogic logic,
            Supplier<FarmSettings> settings,
            FarmComponent.WorldFactory worlds) {
        TierComponent tier = machine.tier("tier");
        EnergyStorageComponent energy = energy(machine, tier, settings);
        SlotRole[] roles = new SlotRole[TEND_SLOT_COUNT];
        Arrays.fill(roles, SlotRole.GENERIC);
        ItemStoreComponent items = machine.items("inventory").slots(roles).openAccess().build();
        TendFarmComponent farm =
                machine.add(new TendFarmComponent("farm", logic, items, energy, settings, tier::tier, worlds));
        return new FarmMachine(null, tier, energy, items, farm);
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

    /** Whether this farm runs the tend engine. */
    public boolean tends() {
        return logic == null;
    }

    private static SlotRole[] roles() {
        SlotRole[] roles = new SlotRole[SLOT_COUNT];
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            roles[slot] = slot < OUTPUT_START ? SlotRole.INPUT : SlotRole.OUTPUT;
        }
        return roles;
    }
}
