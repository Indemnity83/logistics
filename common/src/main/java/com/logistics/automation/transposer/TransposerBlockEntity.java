package com.logistics.automation.transposer;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsConfigHost;
import com.logistics.core.lib.fluids.FluidTankComponent;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.machine.MachineBuilder;
import com.logistics.core.machine.MachineContext;
import com.logistics.core.machine.MachineData;
import com.logistics.core.machine.MachineEntity;
import com.logistics.core.machine.component.EnergyStorageComponent;
import com.logistics.core.machine.component.FluidStoreComponent;
import com.logistics.core.machine.component.FluidSyncComponent;
import com.logistics.core.machine.component.RecipeProcessorComponent;
import com.logistics.core.machine.component.SlotRole;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Transposer: a fluid ⇄ item station with one internal tank and two item slots (input + output), RF
 * energy, and a data-driven recipe system ({@link TransposerRecipe}).
 *
 * <p>Composed from machine components — an energy buffer, an item inventory (input slot + output slot,
 * furnace-style sided access), a single fluid tank, and an RF-cost recipe processor whose recipes drain
 * or fill that same tank depending on the recipe's signed fluid amount. The tank is exposed to pipes for
 * filling and draining. On break, {@link com.logistics.core.lib.block.MachineBlock} drops the item
 * slots; the tank fluid is voided (not dropped).
 */
public class TransposerBlockEntity extends MachineEntity {

    static final int INPUT_SLOT = 0;
    static final int OUTPUT_SLOT = 1;

    // Progress + energy sync as 0..MachineData.SCALE fractions (see MachineData); the tank adds three
    // (id, amount, and capacity), plus one for the active recipe's fluid direction. Capacity is synced
    // from the server so the GUI gauge doesn't read the client's own config, which can diverge from the
    // server's in multiplayer.
    static final int DATA_FLUID_ID = MachineData.COUNT;
    static final int DATA_FLUID_AMOUNT = MachineData.COUNT + 1;
    static final int DATA_FLUID_CAPACITY = MachineData.COUNT + 2;
    // 1 while the active recipe drains the tank (Fill mode) so the GUI mirrors its gauge; 0 otherwise.
    static final int DATA_FILL_MODE = MachineData.COUNT + 3;
    static final int DATA_COUNT = MachineData.COUNT + 4;

    private EnergyStorageComponent energy;
    private RecipeProcessorComponent processor;
    private FluidStoreComponent fluidStore;

    private final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case MachineData.PROGRESS -> MachineData.progressFraction(processor);
                case MachineData.ENERGY -> MachineData.energyFraction(energy, energy.capacity());
                case DATA_FLUID_ID -> fluidStore.tank().isEmpty()
                        ? -1
                        : BuiltInRegistries.FLUID.getId(fluidStore.tank().getFluidKey().getFluid());
                case DATA_FLUID_AMOUNT -> (int) Math.min(
                        FluidUnits.toMillibuckets(fluidStore.tank().getAmount()), Integer.MAX_VALUE);
                case DATA_FLUID_CAPACITY -> (int) Math.min(
                        FluidUnits.toMillibuckets(fluidStore.tank().getCapacity()), Integer.MAX_VALUE);
                case DATA_FILL_MODE -> processor.activeRecipeHasFluidInput() ? 1 : 0;
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
        energy = machine.energy("energy")
                .capacity(LogisticsConfigHost.get(LogisticsAutomation.CONFIG.TRANSPOSER_ENERGY_CAPACITY))
                .maxInput(LogisticsConfigHost.get(LogisticsAutomation.CONFIG.TRANSPOSER_MAX_ENERGY_INPUT))
                .build();

        var items = machine.items("inventory")
                .slots(SlotRole.INPUT, SlotRole.OUTPUT)
                .furnaceAccess()
                .build();

        fluidStore = machine.fluids("tank")
                .capacity(FluidUnits.mb(LogisticsConfigHost.get(LogisticsAutomation.CONFIG.TRANSPOSER_TANK_CAPACITY_MB)))
                .build();

        processor = machine.recipeProcessor("processor")
                .resolver(new TransposerRecipeResolver(fluidStore))
                .rfPerTick(LogisticsConfigHost.get(LogisticsAutomation.CONFIG.TRANSPOSER_ENERGY_PER_TICK))
                .items(items)
                .energy(energy)
                .inputFluids(fluidStore) // same tank used for both directions
                .fluids(fluidStore)
                .lit(this::setLit)
                .build();

        // Sync the tank to clients on change so the GUI fluid gauge stays current.
        machine.add(new FluidSyncComponent(fluidStore));
    }

    /** The tank holder, for tests and integrations. */
    public FluidTankComponent tank() {
        return fluidStore.tank();
    }

    private void setLit(MachineContext ctx, boolean lit) {
        BlockState state = ctx.blockState();
        if (state.hasProperty(TransposerBlock.LIT) && state.getValue(TransposerBlock.LIT) != lit) {
            ctx.setBlockState(state.setValue(TransposerBlock.LIT, lit), Block.UPDATE_ALL);
        }
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
