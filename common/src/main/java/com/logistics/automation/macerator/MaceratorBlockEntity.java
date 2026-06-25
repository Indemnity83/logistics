package com.logistics.automation.macerator;

import com.logistics.LogisticsAutomation;
import com.logistics.core.machine.MachineBuilder;
import com.logistics.core.machine.MachineContext;
import com.logistics.core.machine.MachineEntity;
import com.logistics.core.machine.component.EnergyStorageComponent;
import com.logistics.core.machine.component.RecipeProcessorComponent;
import com.logistics.core.machine.component.SlotRole;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Iron Macerator: grinds input items into dust using RF energy.
 *
 * <p>Composed from machine components — an energy buffer, a two-slot furnace-style inventory, and
 * an RF-cost recipe processor. The recipe defines the total energy required; the processor spends
 * {@link #ENERGY_PER_TICK} RF/tick toward it.
 */
public class MaceratorBlockEntity extends MachineEntity {

    static final int INPUT_SLOT = 0;
    static final int OUTPUT_SLOT = 1;

    static final long ENERGY_CAPACITY = 10_000L;
    static final long MAX_ENERGY_INPUT = 128L;

    // Tuned so 1 coal in a Stirling Engine (16,000 RF) macerates 8 items (8 × 2,000 RF each).
    static final int ENERGY_PER_TICK = 10;

    // ContainerData indices for GUI sync. Progress/total are RF-denominated under the RF-cost model.
    // Package-visible so MaceratorScreenHandler reads the same layout instead of raw literals.
    static final int DATA_PROGRESS = 0;
    static final int DATA_TOTAL = 1;
    static final int DATA_ENERGY = 2;
    static final int DATA_COUNT = 3;

    private EnergyStorageComponent energy;
    private RecipeProcessorComponent processor;

    private final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> (int) Math.min(processor.energySpent(), Integer.MAX_VALUE);
                case DATA_TOTAL -> (int) Math.min(processor.energyRequired(), Integer.MAX_VALUE);
                case DATA_ENERGY -> (int) Math.min(energy.amount(), Integer.MAX_VALUE);
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

    public MaceratorBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsAutomation.ENTITY.MACERATOR_BLOCK_ENTITY, pos, state);
    }

    @Override
    protected void configure(MachineBuilder machine) {
        energy = machine.energy("energy")
                .capacity(ENERGY_CAPACITY)
                .maxInput(MAX_ENERGY_INPUT)
                .providesDemand()
                .build();

        var items = machine.items("inventory")
                .slots(SlotRole.INPUT, SlotRole.OUTPUT)
                .bottomOutAccess()
                .build();

        processor = machine.recipeProcessor("processor")
                .resolver(new MaceratorRecipeResolver())
                .rfPerTick(ENERGY_PER_TICK)
                .items(items)
                .energy(energy)
                .lit(this::setLit)
                .build();
    }

    private void setLit(MachineContext ctx, boolean lit) {
        BlockState state = ctx.blockState();
        if (state.hasProperty(MaceratorBlock.LIT) && state.getValue(MaceratorBlock.LIT) != lit) {
            ctx.setBlockState(state.setValue(MaceratorBlock.LIT, lit), Block.UPDATE_ALL);
        }
    }

    @Override
    public MenuProvider createMenuProvider() {
        return new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("container.logistics.macerator");
            }

            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
                return new MaceratorScreenHandler(syncId, playerInventory, MaceratorBlockEntity.this, containerData);
            }
        };
    }
}
