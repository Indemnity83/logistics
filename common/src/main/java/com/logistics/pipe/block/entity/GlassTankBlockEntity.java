package com.logistics.pipe.block.entity;

import com.logistics.LogisticsCore;
import com.logistics.LogisticsPipe;
import com.logistics.core.lib.block.BaseBlockEntity;
import com.logistics.core.lib.block.capability.HasFluidStorage;
import com.logistics.core.lib.fluids.FluidLight;
import com.logistics.core.lib.fluids.FluidTankComponent;
import com.logistics.core.lib.fluids.IFluidKey;
import com.logistics.core.lib.fluids.IFluidStorage;
import com.logistics.core.lib.tank.TankCell;
import com.logistics.core.lib.tank.TankCellLookup;
import com.logistics.core.lib.tank.TankColumn;
import com.logistics.core.lib.tank.TankColumns;
import com.logistics.pipe.block.GlassTankBlock;
import com.logistics.pipe.tank.TankColumnStorage;
import com.logistics.pipe.tank.TankTier;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Block entity for the Glass Tank. Holds one {@link FluidTankComponent} cell and participates in a
 * vertical column: the tank exposes the whole column as its fluid storage (so pipes and buckets fill
 * the column, settling bottom-up), and the bottom cell re-settles the column each tick.
 */
public class GlassTankBlockEntity extends BaseBlockEntity implements HasFluidStorage, TankCell {

    private final FluidTankComponent tank;

    public GlassTankBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsPipe.ENTITY.GLASS_TANK_BLOCK_ENTITY, pos, state);
        this.tank = new FluidTankComponent(TankTier.GLASS.capacityNative(), this::markDirtyAndSync);
    }

    public FluidTankComponent tank() {
        return tank;
    }

    // ==================== TankCell ====================

    @Override
    public IFluidKey fluid() {
        return tank.getFluidKey();
    }

    @Override
    public long amount() {
        return tank.getAmount();
    }

    @Override
    public long capacity() {
        return tank.getCapacity();
    }

    @Override
    public void setContents(IFluidKey fluid, long amount) {
        tank.setContents(fluid, amount);
    }

    // ==================== Column ====================

    /**
     * Builds a {@link TankColumn} over this tank's vertical stack (bottom-to-top), via the shared
     * {@link TankColumns} walker so a column can include another mod's tanks registered with
     * {@link TankCellLookup}.
     */
    public TankColumn column() {
        if (level == null) {
            return new TankColumn(List.of(this), TankCellLookup::isGas);
        }
        return TankColumns.columnAt(level, getBlockPos());
    }

    /** Whether a connected glass tank above holds fluid — lets the renderer draw one continuous column. */
    public boolean hasFluidAbove() {
        if (level == null) {
            return false;
        }
        return level.getBlockEntity(getBlockPos().above()) instanceof GlassTankBlockEntity above && above.amount() > 0;
    }

    /** The same, downward — a gas settles against the ceiling, so its free surface is the bottom one. */
    public boolean hasFluidBelow() {
        if (level == null) {
            return false;
        }
        return level.getBlockEntity(getBlockPos().below()) instanceof GlassTankBlockEntity below && below.amount() > 0;
    }

    // ==================== Capability ====================

    @Override
    @Nullable
    public IFluidStorage fluidStorage(@Nullable Direction side) {
        return new TankColumnStorage(this::column);
    }

    // ==================== Tick ====================

    public static void tick(Level level, BlockPos pos, BlockState state, GlassTankBlockEntity be) {
        if (level.isClientSide()) {
            return;
        }
        // Only the bottom cell drives the rebalance, so each (possibly cross-mod) column settles once per tick.
        if (TankColumns.isColumnBottom(level, pos)) {
            TankColumns.columnAt(level, pos).rebalance();
        }
        be.updateLightLevel(level);
    }

    /** Emit the contained fluid's light through the block state so a glowing fluid lights the tank. */
    private void updateLightLevel(Level level) {
        FluidLight.update(
                level, getBlockPos(), getBlockState(), GlassTankBlock.LIGHT_LEVEL,
                fluid().getFluid(), amount(), LogisticsCore::fluidLuminance);
    }

    // ==================== Persistence ====================

    @Override
    protected void saveLogisticsData(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveLogisticsData(tag, registries);
        tank.writeNbt(tag, "Tank");
    }

    @Override
    protected void loadLogisticsData(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadLogisticsData(tag, registries);
        tank.readNbt(tag, "Tank");
    }
}
