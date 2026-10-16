package com.logistics.forestry.farm;

import com.logistics.LogisticsForestry;
import com.logistics.core.machine.MachineBuilder;
import com.logistics.core.machine.MachineEntity;
import com.logistics.core.machine.tier.TierComponent;
import com.logistics.core.machine.tier.TieredMachine;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The block entity every farm block shares ({@link LogisticsForestry.ENTITY#FARM}). Which farm it
 * is comes from its block's {@link FarmMachineBlock#logic()}; the components are built by
 * {@link FarmMachine#assemble}.
 */
public class FarmBlockEntity extends MachineEntity implements TieredMachine {

    /** Logic for a block entity found on a block that is not a farm; accepts and does nothing. */
    private static final FarmLogic UNKNOWN_FARM = new FarmLogic() {
        @Override
        public boolean isGermling(ItemStack stack) {
            return false;
        }

        @Override
        public PlotAction decide(FarmWorld world, BlockPos plot, FarmInputs inputs) {
            return PlotAction.NONE;
        }
    };

    // Assigned in configure(), which the super constructor calls: no initializers, they would run after it.
    private FarmMachine parts;
    private ContainerData containerData;

    public FarmBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsForestry.ENTITY.FARM, pos, state);
    }

    @Override
    protected void configure(MachineBuilder machine) {
        FarmLogic logic = getBlockState().getBlock() instanceof FarmMachineBlock farm ? farm.logic() : UNKNOWN_FARM;
        parts = FarmMachine.assemble(machine, logic, FarmSettings::fromConfig, FarmComponent.LEVEL);
        containerData = FarmMenu.dataSource(parts);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FarmBlockEntity farm) {
        MachineEntity.tick(level, pos, state, farm);
    }

    @Override
    public MenuProvider createMenuProvider() {
        return new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return getBlockState().getBlock().getName();
            }

            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
                return new FarmMenu(syncId, playerInventory, FarmBlockEntity.this, containerData);
            }
        };
    }

    public FarmLogic logic() {
        return parts.logic();
    }

    @Override
    public TierComponent tierComponent() {
        return parts.tier();
    }

    /** Side of the square the farm works out to. */
    public int areaSize() {
        return parts.farm().areaSize();
    }

    public FarmStatus status() {
        return parts.farm().status();
    }

    @Nullable
    public UUID owner() {
        return parts.farm().owner();
    }

    void setOwner(UUID owner) {
        parts.farm().setOwner(owner);
        setChanged();
    }

    /**
     * Test seam: works the ring between squares of {@code inner} and {@code outer} for this farm
     * only, instead of the courtyard and the tier's ring; 0 for both restores them.
     */
    public void setAreaOverride(int inner, int outer) {
        parts.farm().setAreaOverride(inner, outer);
    }

    /** Test seam: keeps {@code height} blocks clear above each plot for this farm only. */
    public void setClearanceHeightOverride(int height) {
        parts.farm().setClearanceHeightOverride(height);
    }
}
