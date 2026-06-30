package com.logistics.automation.alloysmelter;

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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Alloy Smelter: combines two inputs into a primary output plus an optional chance-based byproduct,
 * driven by {@code logistics:alloy_smelter} recipes.
 *
 * <p>Composed from machine components — an energy buffer, a four-slot inventory (two inputs + two
 * outputs), and an RF-cost recipe processor. The recipe defines the total energy and the byproduct;
 * the processor spends {@link #ENERGY_PER_TICK} RF/tick and rolls the byproduct on completion.
 * Top/sides feed the inputs; the bottom pulls both outputs.
 */
public class AlloySmelterBlockEntity extends MachineEntity {

    public static final int INPUT_A_SLOT = 0;
    public static final int INPUT_B_SLOT = 1;
    public static final int PRIMARY_OUTPUT_SLOT = 2;
    public static final int SECONDARY_OUTPUT_SLOT = 3;
    public static final int TOTAL_SLOTS = 4;

    public static final long ENERGY_CAPACITY = 10_000L;
    static final long MAX_ENERGY_INPUT = 128L;
    static final int ENERGY_PER_TICK = 20;

    static final int DATA_PROGRESS = 0;
    static final int DATA_TOTAL = 1;
    static final int DATA_ENERGY = 2;
    public static final int DATA_COUNT = 3;

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

    public AlloySmelterBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsAutomation.ENTITY.ALLOY_SMELTER_BLOCK_ENTITY, pos, state);
    }

    @Override
    protected void configure(MachineBuilder machine) {
        energy = machine.energy("energy")
                .capacity(ENERGY_CAPACITY)
                .maxInput(MAX_ENERGY_INPUT)
                .build();

        var items = machine.items("inventory")
                .slots(SlotRole.INPUT, SlotRole.INPUT, SlotRole.OUTPUT, SlotRole.OUTPUT)
                .bottomOutAccess(this::isSmeltable)
                .build();

        processor = machine.recipeProcessor("processor")
                .resolver(new AlloySmelterRecipeResolver())
                .rfPerTick(ENERGY_PER_TICK)
                .items(items)
                .energy(energy)
                .lit(this::setLit)
                .build();
    }

    /** Whether {@code stack} can serve as either input of some recipe (gates hopper insertion). */
    private boolean isSmeltable(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return true; // permissive during load / on the client; validated again on the server tick
        }
        // recipeMap().byType() is NeoForge-patched-only, so loader-agnostic common code scans the full
        // recipe set and filters by type.
        for (RecipeHolder<?> holder : serverLevel.getServer().getRecipeManager().getRecipes()) {
            if (holder.value() instanceof AlloySmelterRecipe recipe && recipe.acceptsAsInput(stack)) {
                return true;
            }
        }
        return false;
    }

    private void setLit(MachineContext ctx, boolean lit) {
        BlockState state = ctx.blockState();
        if (state.hasProperty(AlloySmelterBlock.LIT) && state.getValue(AlloySmelterBlock.LIT) != lit) {
            ctx.setBlockState(state.setValue(AlloySmelterBlock.LIT, lit), Block.UPDATE_ALL);
        }
    }

    @Override
    public MenuProvider createMenuProvider() {
        return new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("container.logistics.alloy_smelter");
            }

            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
                return new AlloySmelterScreenHandler(syncId, playerInventory, AlloySmelterBlockEntity.this, containerData);
            }
        };
    }
}
