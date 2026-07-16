package com.logistics.automation.refinery;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsConfigHost;
import com.logistics.core.lib.fluids.FluidTankComponent;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.IFluidKey;
import com.logistics.core.lib.fluids.IFluidStorage;
import com.logistics.core.lib.fluids.IFluidView;
import com.logistics.core.machine.MachineBuilder;
import com.logistics.core.machine.MachineContext;
import com.logistics.core.machine.MachineData;
import com.logistics.core.machine.MachineEntity;
import com.logistics.core.machine.component.EnergyStorageComponent;
import com.logistics.core.machine.component.FluidStoreComponent;
import com.logistics.core.machine.component.FluidSyncComponent;
import com.logistics.core.machine.component.RecipeProcessorComponent;
import com.logistics.core.machine.component.SlotRole;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Refinery: distills an input fluid into an output fluid using RF energy, with an optional chance-based
 * item byproduct (e.g. crude oil → fuel oil + tar).
 *
 * <p>Composed from machine components — an energy buffer, a single output slot for the byproduct, an
 * input tank and an output tank, and an RF-cost recipe processor whose recipes drain a fluid input and
 * deposit a {@code FluidResult}. Stats match Thermal Expansion's Fractionating Still: 20 RF/tick toward
 * the recipe, a 20,000 RF buffer, a 4,000 mB input tank and a 10,000 mB output tank.
 */
public class RefineryBlockEntity extends MachineEntity {

    static final int OUTPUT_SLOT = 0;

    // Progress + energy sync as 0..MachineData.SCALE fractions (see MachineData); the two tanks add six
    // (id, amount, and capacity each). Capacities are synced from the server so the GUI gauges don't read
    // the client's own config, which can diverge from the server's in multiplayer.
    static final int DATA_IN_FLUID_ID = MachineData.COUNT;
    static final int DATA_IN_FLUID_AMOUNT = MachineData.COUNT + 1;
    static final int DATA_OUT_FLUID_ID = MachineData.COUNT + 2;
    static final int DATA_OUT_FLUID_AMOUNT = MachineData.COUNT + 3;
    static final int DATA_IN_CAPACITY = MachineData.COUNT + 4;
    static final int DATA_OUT_CAPACITY = MachineData.COUNT + 5;
    static final int DATA_COUNT = MachineData.COUNT + 6;

    private EnergyStorageComponent energy;
    private RecipeProcessorComponent processor;
    private FluidStoreComponent inputTank;
    private FluidStoreComponent outputTank;
    private IFluidStorage sidedFluid;

    private final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case MachineData.PROGRESS -> MachineData.progressFraction(processor);
                case MachineData.ENERGY -> MachineData.energyFraction(energy, energy.capacity());
                case DATA_IN_FLUID_ID -> fluidId(inputTank);
                case DATA_IN_FLUID_AMOUNT -> amountMb(inputTank);
                case DATA_OUT_FLUID_ID -> fluidId(outputTank);
                case DATA_OUT_FLUID_AMOUNT -> amountMb(outputTank);
                case DATA_IN_CAPACITY -> capacityMb(inputTank);
                case DATA_OUT_CAPACITY -> capacityMb(outputTank);
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

    public RefineryBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsAutomation.ENTITY.REFINERY_BLOCK_ENTITY, pos, state);
    }

    private static int fluidId(FluidStoreComponent store) {
        return store.tank().isEmpty()
                ? -1
                : BuiltInRegistries.FLUID.getId(store.tank().getFluidKey().getFluid());
    }

    private static int amountMb(FluidStoreComponent store) {
        return (int) Math.min(FluidUnits.toMillibuckets(store.tank().getAmount()), Integer.MAX_VALUE);
    }

    private static int capacityMb(FluidStoreComponent store) {
        return (int) Math.min(FluidUnits.toMillibuckets(store.tank().getCapacity()), Integer.MAX_VALUE);
    }

    @Override
    protected void configure(MachineBuilder machine) {
        energy = machine.energy("energy")
                .capacity(LogisticsConfigHost.get(LogisticsAutomation.CONFIG.REFINERY_ENERGY_CAPACITY))
                .maxInput(LogisticsConfigHost.get(LogisticsAutomation.CONFIG.REFINERY_MAX_ENERGY_INPUT))
                .build();

        var items = machine.items("inventory")
                .slots(SlotRole.OUTPUT)
                .bottomOutAccess()
                .build();

        inputTank = machine.fluids("input")
                .capacity(FluidUnits.mb(LogisticsConfigHost.get(LogisticsAutomation.CONFIG.REFINERY_INPUT_TANK_MB)))
                .build();

        outputTank = machine.fluids("output")
                .capacity(FluidUnits.mb(LogisticsConfigHost.get(LogisticsAutomation.CONFIG.REFINERY_OUTPUT_TANK_MB)))
                .outputOnly()
                .build();

        processor = machine.recipeProcessor("processor")
                .resolver(new RefineryRecipeResolver(inputTank))
                .rfPerTick(LogisticsConfigHost.get(LogisticsAutomation.CONFIG.REFINERY_ENERGY_PER_TICK))
                .items(items)
                .energy(energy)
                .inputFluids(inputTank)
                .fluids(outputTank)
                .lit(this::setLit)
                .build();

        // Sync both tanks to clients on change so the GUI gauges stay current (the menu's ContainerData
        // only reaches a player with the GUI open).
        machine.add(new FluidSyncComponent(inputTank, outputTank));

        // External fluid handlers fill the input tank and drain the output tank.
        sidedFluid = new RefineryFluidView(inputTank, outputTank);
    }

    /** External view: insertion routes to the input tank, extraction to the output tank. */
    @Override
    @Nullable
    public IFluidStorage fluidStorage(@Nullable Direction side) {
        return sidedFluid;
    }

    /** The output tank holder, for the block-entity renderer's face gauge. */
    public FluidTankComponent tank() {
        return outputTank.tank();
    }

    private void setLit(MachineContext ctx, boolean lit) {
        BlockState state = ctx.blockState();
        if (state.hasProperty(RefineryBlock.LIT) && state.getValue(RefineryBlock.LIT) != lit) {
            ctx.setBlockState(state.setValue(RefineryBlock.LIT, lit), Block.UPDATE_ALL);
        }
    }

    @Override
    public MenuProvider createMenuProvider() {
        return new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("container.logistics.refinery");
            }

            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
                return new RefineryScreenHandler(syncId, playerInventory, RefineryBlockEntity.this, containerData);
            }
        };
    }

    /**
     * Combined fluid view exposed to pipes: insertion fills the input tank, extraction drains the output
     * tank. {@link #contents()} reports both tanks (so tooltips like Jade show each), but lists the
     * output tank first: an extractor reads the first listed fluid and then asks {@link #extract} for it,
     * and extract only ever yields the output fluid — so the output must come first or extractors would
     * request the input fluid and pull nothing.
     */
    private record RefineryFluidView(FluidStoreComponent input, FluidStoreComponent output)
            implements IFluidStorage {

        @Override
        public long insert(IFluidKey fluid, long maxAmount, boolean simulate) {
            return input.tank().insert(fluid, maxAmount, simulate);
        }

        @Override
        public long extract(IFluidKey fluid, long maxAmount, boolean simulate) {
            return output.tank().extract(fluid, maxAmount, simulate);
        }

        @Override
        public Iterable<IFluidView> contents() {
            List<IFluidView> views = new ArrayList<>();
            output.tank().contents().forEach(views::add);
            input.tank().contents().forEach(views::add);
            return views;
        }
    }
}
