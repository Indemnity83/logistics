package com.logistics.automation.crucible;

import com.logistics.LogisticsAutomation;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.machine.MachineBuilder;
import com.logistics.core.machine.MachineContext;
import com.logistics.core.machine.MachineEntity;
import com.logistics.core.machine.component.EnergyStorageComponent;
import com.logistics.core.machine.component.FluidStoreComponent;
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
 * Magma Crucible: melts an input item into a fluid using RF energy.
 *
 * <p>Composed from machine components — an energy buffer, a single input slot, a fluid tank, and an
 * RF-cost recipe processor whose recipes output a {@code FluidResult} into the tank. The recipe defines
 * the total energy required; the processor spends {@link #ENERGY_PER_TICK} RF/tick toward it.
 */
public class MagmaCrucibleBlockEntity extends MachineEntity {

    static final int INPUT_SLOT = 0;

    static final long ENERGY_CAPACITY = 10_000L;
    static final long MAX_ENERGY_INPUT = 128L;
    static final int ENERGY_PER_TICK = 20;

    static final long TANK_CAPACITY_MB = 4_000L;

    static final int DATA_PROGRESS = 0;
    static final int DATA_TOTAL = 1;
    static final int DATA_ENERGY = 2;
    static final int DATA_FLUID_ID = 3;
    static final int DATA_FLUID_AMOUNT = 4;
    static final int DATA_COUNT = 5;

    private EnergyStorageComponent energy;
    private RecipeProcessorComponent processor;
    private FluidStoreComponent fluidStore;

    private final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> (int) Math.min(processor.energySpent(), Integer.MAX_VALUE);
                case DATA_TOTAL -> (int) Math.min(processor.energyRequired(), Integer.MAX_VALUE);
                case DATA_ENERGY -> (int) Math.min(energy.amount(), Integer.MAX_VALUE);
                case DATA_FLUID_ID -> fluidStore.tank().isEmpty()
                        ? -1
                        : BuiltInRegistries.FLUID.getId(fluidStore.tank().getFluidKey().getFluid());
                case DATA_FLUID_AMOUNT -> (int) Math.min(
                        FluidUnits.toMillibuckets(fluidStore.tank().getAmount()), Integer.MAX_VALUE);
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

    public MagmaCrucibleBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsAutomation.ENTITY.MAGMA_CRUCIBLE_BLOCK_ENTITY, pos, state);
    }

    @Override
    protected void configure(MachineBuilder machine) {
        energy = machine.energy("energy")
                .capacity(ENERGY_CAPACITY)
                .maxInput(MAX_ENERGY_INPUT)
                .build();

        var items = machine.items("inventory")
                .slots(SlotRole.INPUT)
                .furnaceAccess()
                .build();

        fluidStore = machine.fluids("tank")
                .capacity(FluidUnits.mb(TANK_CAPACITY_MB))
                .build();

        processor = machine.recipeProcessor("processor")
                .resolver(new MagmaCrucibleRecipeResolver())
                .rfPerTick(ENERGY_PER_TICK)
                .items(items)
                .energy(energy)
                .fluids(fluidStore)
                .lit(this::setLit)
                .build();
    }

    private void setLit(MachineContext ctx, boolean lit) {
        BlockState state = ctx.blockState();
        if (state.hasProperty(MagmaCrucibleBlock.LIT) && state.getValue(MagmaCrucibleBlock.LIT) != lit) {
            ctx.setBlockState(state.setValue(MagmaCrucibleBlock.LIT, lit), Block.UPDATE_ALL);
        }
    }

    @Override
    public MenuProvider createMenuProvider() {
        return new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("container.logistics.magma_crucible");
            }

            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
                return new MagmaCrucibleScreenHandler(syncId, playerInventory, MagmaCrucibleBlockEntity.this, containerData);
            }
        };
    }
}
