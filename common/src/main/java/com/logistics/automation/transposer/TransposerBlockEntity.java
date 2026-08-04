package com.logistics.automation.transposer;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsConfigHost;
import com.logistics.core.lib.fluids.FluidTankComponent;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.machine.MachineBuilder;
import com.logistics.core.machine.MachineEntity;
import com.logistics.core.machine.component.FluidStoreComponent;
import com.logistics.core.machine.component.FluidSyncComponent;
import com.logistics.core.machine.component.SlotRole;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Transposer: a fluid ⇄ bucket station with one internal tank and two item slots (input + output).
 *
 * <p>Composed from machine components — an item inventory (input bucket slot + output slot, furnace-style
 * sided access), a single fluid tank, and a {@link TransposerComponent} that runs the atomic conversion.
 * No RF in v1. The tank is exposed to pipes for filling and draining; only recognized buckets may be
 * inserted into the input slot by automation. On break, {@link com.logistics.core.lib.block.MachineBlock}
 * drops the item slots; the tank fluid is voided (not dropped).
 */
public class TransposerBlockEntity extends MachineEntity {

    static final int INPUT_SLOT = 0;
    static final int OUTPUT_SLOT = 1;

    static final int DATA_FLUID_ID = 0;
    static final int DATA_FLUID_AMOUNT = 1;
    // Capacity is synced from the server so the GUI gauge doesn't read the client's own config, which
    // can diverge from the server's in multiplayer.
    static final int DATA_FLUID_CAPACITY = 2;
    static final int DATA_COUNT = 3;

    private FluidStoreComponent fluidStore;

    private final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_FLUID_ID -> fluidStore.tank().isEmpty()
                        ? -1
                        : BuiltInRegistries.FLUID.getId(fluidStore.tank().getFluidKey().getFluid());
                case DATA_FLUID_AMOUNT -> (int) Math.min(
                        FluidUnits.toMillibuckets(fluidStore.tank().getAmount()), Integer.MAX_VALUE);
                case DATA_FLUID_CAPACITY -> (int) Math.min(
                        FluidUnits.toMillibuckets(fluidStore.tank().getCapacity()), Integer.MAX_VALUE);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Server-side data source only; the client uses a SimpleContainerData populated by sync.
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public TransposerBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsAutomation.ENTITY.TRANSPOSER_BLOCK_ENTITY, pos, state);
    }

    @Override
    protected void configure(MachineBuilder machine) {
        var items = machine.items("inventory")
                .slots(SlotRole.INPUT, SlotRole.OUTPUT)
                .furnaceAccess(TransposerBuckets::isConvertibleInput)
                .build();

        fluidStore = machine.fluids("tank")
                .capacity(FluidUnits.mb(LogisticsConfigHost.get(LogisticsAutomation.CONFIG.TRANSPOSER_TANK_CAPACITY_MB)))
                .build();

        machine.add(new TransposerComponent("transposer", items, fluidStore));

        // Sync the tank to clients on change so the GUI fluid gauge stays current.
        machine.add(new FluidSyncComponent(fluidStore));
    }

    /** The tank holder, for tests and integrations. */
    public FluidTankComponent tank() {
        return fluidStore.tank();
    }

    @Override
    public MenuProvider createMenuProvider() {
        return new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("container.logistics.transposer");
            }

            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
                return new TransposerScreenHandler(syncId, playerInventory, TransposerBlockEntity.this, containerData);
            }
        };
    }
}
