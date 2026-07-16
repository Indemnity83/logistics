package com.logistics.automation.fabricator;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsConfigHost;
import com.logistics.core.machine.MachineBuilder;
import com.logistics.core.machine.MachineContext;
import com.logistics.core.machine.MachineData;
import com.logistics.core.machine.MachineEntity;
import com.logistics.core.machine.component.EnergyStorageComponent;
import com.logistics.core.machine.component.ItemStoreComponent;
import com.logistics.core.machine.component.SlotRole;
import com.logistics.core.lib.resource.ResourceId;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Sequential Fabricator: a 12-slot material pool plus a player-built queue of fabricator recipes it
 * cycles through, ejecting each product out a non-top face. A "beast" — 100k RF buffer, accepts
 * 128 RF/t, works at 80 RF/t.
 */
public class SequentialFabricatorBlockEntity extends MachineEntity {

    public static final int INPUT_SLOTS = 12;

    private EnergyStorageComponent energy;
    private ItemStoreComponent items;
    private FabricatorProcessorComponent processor;
    private ContainerData containerData;

    public SequentialFabricatorBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsAutomation.ENTITY.SEQUENTIAL_FABRICATOR_BLOCK_ENTITY, pos, state);
    }

    @Override
    protected void configure(MachineBuilder machine) {
        energy = machine.energy("energy")
                .capacity(LogisticsConfigHost.get(LogisticsAutomation.CONFIG.FABRICATOR_ENERGY_CAPACITY))
                .maxInput(LogisticsConfigHost.get(LogisticsAutomation.CONFIG.FABRICATOR_MAX_ENERGY_INPUT))
                .build();

        SlotRole[] roles = new SlotRole[INPUT_SLOTS];
        Arrays.fill(roles, SlotRole.INPUT);
        items = machine.items("inventory")
                .slots(roles)
                .topInAccess()
                .build();

        processor = machine.add(new FabricatorProcessorComponent(
                "processor", items, energy,
                LogisticsConfigHost.get(LogisticsAutomation.CONFIG.FABRICATOR_ENERGY_PER_TICK),
                this::setLit, this::setChanged));

        containerData = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case MachineData.PROGRESS -> Math.round(processor.progress() * MachineData.SCALE);
                    case MachineData.ENERGY -> MachineData.energyFraction(energy, energy.capacity());
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                // Server-side source only; the client uses a SimpleContainerData populated over the menu sync.
            }

            @Override
            public int getCount() {
                return MachineData.COUNT;
            }
        };
    }

    private void setLit(MachineContext ctx, boolean lit) {
        BlockState state = ctx.blockState();
        if (state.hasProperty(SequentialFabricatorBlock.LIT) && state.getValue(SequentialFabricatorBlock.LIT) != lit) {
            ctx.setBlockState(state.setValue(SequentialFabricatorBlock.LIT, lit), Block.UPDATE_ALL);
        }
    }

    /** Flip a recipe in the build queue (called from the toggle packet). */
    public void toggleSelection(ResourceId recipeId) {
        processor.toggle(recipeId);
        setChanged();
    }

    /** Currently-craftable outputs with their selection state, for the menu sync. Empty off the server. */
    public List<FabricatorProcessorComponent.Output> currentOutputs() {
        if (level instanceof ServerLevel serverLevel) {
            RecipeManager rm = serverLevel.getServer().getRecipeManager();
            return processor.outputs(rm);
        }
        return List.of();
    }

    @Override
    public MenuProvider createMenuProvider() {
        return new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("container.logistics.sequential_fabricator");
            }

            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
                return new SequentialFabricatorScreenHandler(
                        syncId, playerInventory, SequentialFabricatorBlockEntity.this, containerData);
            }
        };
    }
}
