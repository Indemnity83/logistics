package com.logistics.fluid.block.entity;

import com.logistics.LogisticsFluid;
import com.logistics.core.lib.block.BaseBlockEntity;
import com.logistics.core.lib.block.capability.HasFluidStorage;
import com.logistics.core.lib.fluids.FluidTankComponent;
import com.logistics.core.lib.fluids.IFluidKey;
import com.logistics.core.lib.fluids.IFluidStorage;
import com.logistics.core.lib.platform.PlatformService;
import com.logistics.fluid.tank.TankCell;
import com.logistics.fluid.tank.TankColumn;
import com.logistics.fluid.tank.TankColumnStorage;
import com.logistics.fluid.tank.TankTier;
import java.util.ArrayList;
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

    private static final int MAX_COLUMN_HEIGHT = 256;

    private final FluidTankComponent tank;

    public GlassTankBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsFluid.ENTITY.GLASS_TANK_BLOCK_ENTITY, pos, state);
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

    /** Builds a {@link TankColumn} over this tank's vertical stack (bottom-to-top). */
    public TankColumn column() {
        return new TankColumn(columnCells(), key -> PlatformService.INSTANCE.isLighterThanAir(key.getFluid()));
    }

    private List<GlassTankBlockEntity> columnCells() {
        if (level == null) {
            return List.of(this);
        }
        GlassTankBlockEntity bottom = this;
        for (int i = 0; i < MAX_COLUMN_HEIGHT; i++) {
            GlassTankBlockEntity below = tankAt(bottom.getBlockPos().below());
            if (below == null) {
                break;
            }
            bottom = below;
        }
        List<GlassTankBlockEntity> cells = new ArrayList<>();
        GlassTankBlockEntity cur = bottom;
        for (int i = 0; i < MAX_COLUMN_HEIGHT && cur != null; i++) {
            cells.add(cur);
            cur = tankAt(cur.getBlockPos().above());
        }
        return cells;
    }

    @Nullable
    private GlassTankBlockEntity tankAt(BlockPos pos) {
        if (level == null || !level.isLoaded(pos)) {
            return null;
        }
        return level.getBlockEntity(pos) instanceof GlassTankBlockEntity tank ? tank : null;
    }

    private boolean hasTankBelow() {
        return tankAt(getBlockPos().below()) != null;
    }

    /** Whether a connected tank above holds fluid — lets the renderer draw one continuous column. */
    public boolean hasFluidAbove() {
        GlassTankBlockEntity above = tankAt(getBlockPos().above());
        return above != null && above.amount() > 0;
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
        // Only the bottom cell drives the rebalance, so each column settles once per tick.
        if (!be.hasTankBelow()) {
            be.column().rebalance();
        }
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
