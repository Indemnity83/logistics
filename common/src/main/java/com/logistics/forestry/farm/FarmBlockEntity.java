package com.logistics.forestry.farm;

import com.logistics.LogisticsForestry;
import com.logistics.core.machine.MachineBuilder;
import com.logistics.core.machine.MachineEntity;
import com.logistics.core.machine.control.MachineControls;
import com.logistics.core.machine.control.RedstoneMode;
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
 * is comes from its block's {@link FarmMachineBlock#logic()}.
 */
public class FarmBlockEntity extends MachineEntity implements TieredMachine {

    /** Logic for a block entity found on a block that is not a farm: nothing is ever ripe. */
    private static final TendLogic UNKNOWN_FARM = new TendLogic() {
        @Override
        @Nullable
        public Harvest ripe(FarmWorld world, BlockPos pos) {
            return null;
        }

        @Override
        public FarmCost harvestCost(BlockState state) {
            return FarmCost.HARVEST_CROP;
        }

        @Override
        @Nullable
        public BlockState plotGround(BlockState ground) {
            return null;
        }

        @Override
        @Nullable
        public BlockState plantState(ItemStack germling, BlockPos plot, FarmWorld world) {
            return null;
        }
    };

    // Assigned in configure(), which the super constructor calls: no initializers, they would run after it.
    private FarmMachine parts;
    private ContainerData containerData;
    private final FarmEnergyRate usage = new FarmEnergyRate();

    public FarmBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsForestry.ENTITY.FARM, pos, state);
    }

    @Override
    protected void configure(MachineBuilder machine) {
        TendLogic logic = getBlockState().getBlock() instanceof FarmMachineBlock farm ? farm.logic() : UNKNOWN_FARM;
        parts = FarmMachine.assemble(machine, logic, FarmSettings::fromConfig, TendFarmComponent.LEVEL);
        machine.redstoneControl("redstone").defaultMode(RedstoneMode.IGNORED).build();
        containerData = FarmMenu.dataSource(parts);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FarmBlockEntity farm) {
        MachineEntity.tick(level, pos, state, farm);
        if (!level.isClientSide()) {
            farm.usage.record(farm.parts.energy().consumedLastTick());
        }
    }

    /** RF/t spent on farm work, averaged over the last {@link FarmEnergyRate#WINDOW} ticks. */
    @Override
    public long energyRate() {
        return usage.perTick();
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
                return new FarmMenu(syncId, playerInventory, FarmBlockEntity.this, containerData,
                        MachineControls.server(FarmBlockEntity.this));
            }
        };
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

    /** Test seam: works at {@code checksPerSecond} and {@code actionsPerSecond} for this farm only; 0 for both restores the tier's. */
    public void setRateOverride(int checksPerSecond, int actionsPerSecond) {
        parts.farm().setRateOverride(checksPerSecond, actionsPerSecond);
    }
}
