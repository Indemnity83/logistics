package com.logistics.automation.sawmill;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsConfigHost;
import com.logistics.core.machine.MachineBuilder;
import com.logistics.core.machine.MachineContext;
import com.logistics.core.machine.MachineData;
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
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Sawmill: saws wood into a primary product plus a chance-based Sawdust byproduct, driven by
 * {@code logistics:sawmill} recipes.
 *
 * <p>Composed from machine components — an energy buffer, a three-slot inventory (input + two
 * outputs), and an RF-cost recipe processor. The recipe defines the total energy and the byproduct;
 * the processor spends a configurable RF/tick and rolls the byproduct on completion.
 * Top/sides feed the input; the bottom pulls both outputs.
 */
public class SawmillBlockEntity extends MachineEntity {

    public static final int INPUT_SLOT = 0;
    public static final int PRIMARY_OUTPUT_SLOT = 1;
    public static final int SECONDARY_OUTPUT_SLOT = 2;
    public static final int TOTAL_SLOTS = 3;

    private EnergyStorageComponent energy;
    private RecipeProcessorComponent processor;
    private ContainerData containerData;

    public SawmillBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsAutomation.ENTITY.SAWMILL_BLOCK_ENTITY, pos, state);
    }

    @Override
    protected void configure(MachineBuilder machine) {
        long capacity = LogisticsConfigHost.get(LogisticsAutomation.CONFIG.SAWMILL_ENERGY_CAPACITY);
        energy = machine.energy("energy")
                .capacity(capacity)
                .maxInput(LogisticsConfigHost.get(LogisticsAutomation.CONFIG.SAWMILL_MAX_ENERGY_INPUT))
                .build();

        var items = machine.items("inventory")
                .slots(SlotRole.INPUT, SlotRole.OUTPUT, SlotRole.OUTPUT)
                .bottomOutAccess(this::isSawable)
                .build();

        processor = machine.recipeProcessor("processor")
                .resolver(new SawmillRecipeResolver())
                .rfPerTick(LogisticsConfigHost.get(LogisticsAutomation.CONFIG.SAWMILL_ENERGY_PER_TICK))
                .items(items)
                .energy(energy)
                .lit(this::setLit)
                .build();

        containerData = MachineData.source(processor, energy, capacity);
    }

    private boolean isSawable(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return true; // permissive during load / on the client; validated again on the server tick
        }
        return serverLevel.getServer().getRecipeManager()
                .getRecipeFor(LogisticsAutomation.RECIPE.SAWMILL_RECIPE_TYPE, new SingleRecipeInput(stack), level)
                .isPresent();
    }

    private void setLit(MachineContext ctx, boolean lit) {
        BlockState state = ctx.blockState();
        if (state.hasProperty(SawmillBlock.LIT) && state.getValue(SawmillBlock.LIT) != lit) {
            ctx.setBlockState(state.setValue(SawmillBlock.LIT, lit), Block.UPDATE_ALL);
        }
    }

    @Override
    public MenuProvider createMenuProvider() {
        return new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("container.logistics.sawmill");
            }

            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
                return new SawmillScreenHandler(syncId, playerInventory, SawmillBlockEntity.this, containerData);
            }
        };
    }
}
