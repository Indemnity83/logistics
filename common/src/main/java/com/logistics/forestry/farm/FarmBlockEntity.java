package com.logistics.forestry.farm;

import com.logistics.LogisticsForestry;
import com.logistics.core.machine.MachineBuilder;
import com.logistics.core.machine.MachineEntity;
import com.logistics.core.machine.control.MachineControls;
import com.logistics.core.machine.control.RedstoneMode;
import com.logistics.core.machine.tier.MachineTier;
import com.logistics.core.machine.tier.TierComponent;
import com.logistics.core.machine.tier.TieredMachine;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
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
 * is comes from its block: a {@link FarmMachineBlock#tendLogic()} runs the tend engine
 * ({@link FarmMachine#assembleTend}), otherwise its {@link FarmMachineBlock#logic()} runs the ring
 * engine ({@link FarmMachine#assemble}).
 */
public class FarmBlockEntity extends MachineEntity implements TieredMachine, FarmMenu.Buttons {

    public static final String MESSAGE_EXPANDED = "message.logistics.farm.expanded";

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
    private final FarmEnergyRate usage = new FarmEnergyRate();

    public FarmBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsForestry.ENTITY.FARM, pos, state);
    }

    @Override
    protected void configure(MachineBuilder machine) {
        FarmMachineBlock block = getBlockState().getBlock() instanceof FarmMachineBlock farm ? farm : null;
        TendLogic tendLogic = block != null ? block.tendLogic() : null;
        if (tendLogic != null) {
            parts = FarmMachine.assembleTend(machine, tendLogic, FarmSettings::fromConfig, FarmComponent.LEVEL);
        } else {
            FarmLogic logic = block != null ? block.logic() : UNKNOWN_FARM;
            parts = FarmMachine.assemble(machine, logic, FarmSettings::fromConfig, FarmComponent.LEVEL);
        }
        machine.redstoneControl("redstone").defaultMode(RedstoneMode.IGNORED).build();
        containerData = FarmMenu.dataSource(parts, this::previewShown);
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
                return new FarmMenu(syncId, playerInventory, FarmBlockEntity.this, containerData, FarmBlockEntity.this,
                        MachineControls.server(FarmBlockEntity.this));
            }
        };
    }

    /** The ring farm's logic; {@code null} for a tend farm. */
    @Nullable
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

    public FarmState state() {
        return parts.farm().state();
    }

    /** What the farm's survey found, or what Show area or the last upgrade found once it works. */
    public FarmSurvey survey() {
        return parts.farm().survey();
    }

    /** Whether the area preview shows now; the server's view of what {@link #preview()} carries. */
    public boolean previewShown() {
        return level != null && parts.farm().previewShown(level.getGameTime());
    }

    /** The area preview as the last update tag carried it; what the client draws. */
    public FarmPreview preview() {
        return parts.farm().syncedPreview();
    }

    /** Starts a surveying farm; false, changing nothing, once it works. Also the tests' way to start a farm. */
    @Override
    public boolean start() {
        if (!parts.farm().start()) {
            return false;
        }
        sync();
        return true;
    }

    /** Shows the area preview until toggled off, or hides it. */
    @Override
    public boolean toggleArea() {
        if (level == null || !parts.farm().toggleArea(this, level.getGameTime())) {
            return false;
        }
        sync();
        return true;
    }

    /** Shows the widened ring and what working its new lanes will clear, which starts straight away. */
    @Override
    public Optional<Component> upgradeFeedback(MachineTier newTier) {
        if (level == null) {
            return Optional.empty();
        }
        FarmSurvey lanes = parts.farm().expanded(this, level.getGameTime());
        sync();
        return Optional.of(Component.translatable(MESSAGE_EXPANDED, areaSize(), lanes.toClear(), lanes.flagged()));
    }

    /** The update tag also carries the area preview, which is never saved. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        long now = level != null ? level.getGameTime() : 0;
        return parts.farm().withPreview(this, now, () -> super.getUpdateTag(registries));
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

    /** Test seam: works at {@code checksPerSecond} and {@code actionsPerSecond} for this farm only; 0 for both restores the tier's. */
    public void setRateOverride(int checksPerSecond, int actionsPerSecond) {
        parts.farm().setRateOverride(checksPerSecond, actionsPerSecond);
    }
}
