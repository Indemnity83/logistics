package com.logistics.automation.kiln;

import com.logistics.LogisticsAutomation;
import com.logistics.core.machine.MachineBuilder;
import com.logistics.core.machine.MachineContext;
import com.logistics.core.machine.MachineEntity;
import com.logistics.core.machine.component.EnergyStorageComponent;
import com.logistics.core.machine.component.RecipeProcessorComponent;
import com.logistics.core.machine.component.SlotRole;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Electric Kiln: smelts any vanilla smelting recipe using RF energy.
 *
 * <p>Composed from machine components — an energy buffer, a two-slot inventory (input from the top
 * only), and an RF-cost recipe processor backed by the vanilla smelting recipes. The recipe's
 * cooking time is converted to an RF cost so smelting takes the same time as a vanilla furnace.
 */
public class KilnBlockEntity extends MachineEntity {

    static final int INPUT_SLOT = 0;
    static final int OUTPUT_SLOT = 1;

    static final long ENERGY_CAPACITY = 10_000L;
    static final long MAX_ENERGY_INPUT = 128L;

    static final int ENERGY_PER_TICK = 20;

    private static final int DATA_PROGRESS = 0;
    private static final int DATA_TOTAL = 1;
    private static final int DATA_ENERGY = 2;
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

    public KilnBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsAutomation.ENTITY.KILN_BLOCK_ENTITY, pos, state);
    }

    @Override
    protected void configure(MachineBuilder machine) {
        energy = machine.energy("energy")
                .capacity(ENERGY_CAPACITY)
                .maxInput(MAX_ENERGY_INPUT)
                .build();

        var items = machine.items("inventory")
                .slots(SlotRole.INPUT, SlotRole.OUTPUT)
                .furnaceAccess(this::canSmelt)
                .build();

        processor = machine.recipeProcessor("processor")
                .resolver(new SmeltingRecipeResolver())
                .rfPerTick(ENERGY_PER_TICK)
                .items(items)
                .energy(energy)
                .lit(this::setLit)
                .build();
    }

    private boolean canSmelt(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return true; // Allow during world load or client-side; validated again on the server tick.
        }
        return serverLevel.getServer().getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), level)
                .isPresent();
    }

    private void setLit(MachineContext ctx, boolean lit) {
        BlockState state = ctx.blockState();
        if (state.hasProperty(KilnBlock.LIT) && state.getValue(KilnBlock.LIT) != lit) {
            ctx.setBlockState(state.setValue(KilnBlock.LIT, lit), Block.UPDATE_ALL);
        }
    }

    @Override
    public MenuProvider createMenuProvider() {
        return new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("container.logistics.kiln");
            }

            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
                return new KilnScreenHandler(syncId, playerInventory, KilnBlockEntity.this, containerData);
            }
        };
    }
}
