package com.logistics.power.cable;

import com.logistics.LogisticsPower;
import com.logistics.core.lib.block.BaseBlockEntity;
import com.logistics.core.lib.block.capability.HasEnergyStorage;
import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.lib.energy.IEnergyStorage;
import com.logistics.core.lib.block.behavior.ProbeResult;
import com.logistics.core.lib.power.AcceptsLowTierEnergy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Block entity for cables. Cables do not store energy; they expose
 * an insert-only conduit endpoint that forwards accepted energy through the
 * connected cable network.
 *
 * <p>Any energy that cannot be delivered to a connected consumer is rejected,
 * allowing the source to keep it instead of leaving power buffered in cables.
 */
public class CableBlockEntity extends BaseBlockEntity
        implements HasEnergyStorage, AcceptsLowTierEnergy {

    private static final String KEY_CONNECTIONS = "connections";

    private final CableBlock.ConnectionType[] connectionCache = new CableBlock.ConnectionType[6];
    private boolean connectionCacheDirty = true;
    private boolean registeredInNetwork = false;
    private int lastConnectionMask = -1;
    private int renderConnectionMask = 0;

    public CableBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsPower.ENTITY.CABLE_BLOCK_ENTITY, pos, state);
        for (int i = 0; i < 6; i++) {
            connectionCache[i] = CableBlock.ConnectionType.NONE;
        }
    }

    // ==================== HasEnergyStorage ====================

    @Override
    public IEnergyStorage energyStorage(@Nullable Direction side) {
        return new CableEnergyStorage(side);
    }

    // ==================== Lifecycle ====================

    /**
     * Called from CableBlock.onRemove when the cable is broken or replaced.
     */
    public void onCableRemoved() {
        if (level != null && !level.isClientSide()) {
            CableNetworkManager.get(level).removeCable(worldPosition);
        }
    }

    // ==================== Connection Cache ====================

    public void invalidateConnectionCache() {
        connectionCacheDirty = true;
    }

    public CableBlock.ConnectionType getCachedConnectionType(Direction direction) {
        if (connectionCacheDirty) {
            rebuildConnectionCache();
        }
        return connectionCache[direction.get3DDataValue()];
    }

    public int getRenderConnectionMask() {
        if (connectionCacheDirty && (level == null || !level.isClientSide())) {
            rebuildConnectionCache();
        }
        return renderConnectionMask;
    }

    private void rebuildConnectionCache() {
        if (level == null) return;
        if (!(getBlockState().getBlock() instanceof CableBlock cableBlock)) return;

        for (Direction dir : Direction.values()) {
            connectionCache[dir.get3DDataValue()] = cableBlock.getDynamicConnectionType(level, worldPosition, dir);
        }
        renderConnectionMask = computeConnectionMask();
        connectionCacheDirty = false;
    }

    private int computeConnectionMask() {
        int mask = 0;
        for (Direction dir : Direction.values()) {
            CableBlock.ConnectionType type = connectionCache[dir.get3DDataValue()];
            if (type != CableBlock.ConnectionType.NONE) {
                mask |= (type.ordinal() << (dir.get3DDataValue() * 2));
            }
        }
        return mask;
    }

    private void updateConnections() {
        if (!connectionCacheDirty) return;
        rebuildConnectionCache();

        int mask = computeConnectionMask();
        if (mask != lastConnectionMask) {
            lastConnectionMask = mask;
            renderConnectionMask = mask;
            if (level != null && !level.isClientSide()) {
                markDirtyAndSync();
            }
        }
    }

    private void applyConnectionMask(int mask) {
        renderConnectionMask = mask;
        lastConnectionMask = mask;
        for (Direction dir : Direction.values()) {
            int ordinal = (mask >> (dir.get3DDataValue() * 2)) & 0b11;
            CableBlock.ConnectionType[] values = CableBlock.ConnectionType.values();
            connectionCache[dir.get3DDataValue()] = ordinal < values.length
                    ? values[ordinal]
                    : CableBlock.ConnectionType.NONE;
        }
        connectionCacheDirty = false;
    }

    @Override
    protected void saveLogisticsData(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveLogisticsData(tag, registries);
        tag.putInt(KEY_CONNECTIONS, renderConnectionMask);
    }

    @Override
    protected void loadLogisticsData(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadLogisticsData(tag, registries);
        applyConnectionMask(NbtCompat.getInt(tag, KEY_CONNECTIONS, 0));
        if (level != null && level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
        }
    }

    // ==================== Transfer Access ====================

    public long getTransferRate() { return tier().transferRate(); }

    // ==================== Tick ====================

    public static void tick(Level world, BlockPos pos, BlockState state, CableBlockEntity cable) {
        if (!cable.registeredInNetwork) {
            CableNetworkManager.get(world).addCable(pos);
            cable.registeredInNetwork = true;
        }
        cable.updateConnections();
    }

    private CableTier tier() {
        return getBlockState().getBlock() instanceof CableBlock cableBlock ? cableBlock.tier() : CableTier.COPPER;
    }

    private final class CableEnergyStorage implements IEnergyStorage {
        @Nullable
        private final Direction side;

        private CableEnergyStorage(@Nullable Direction side) {
            this.side = side;
        }

        @Override
        public long insert(long maxAmount, boolean simulate) {
            if (maxAmount <= 0 || level == null || level.isClientSide()) return 0;
            return CableNetworkManager.get(level).insert(
                    level, worldPosition, side, Math.min(maxAmount, getTransferRate()), simulate);
        }

        @Override
        public long extract(long maxAmount, boolean simulate) { return 0; }

        @Override
        public long getAmount() { return 0; }

        @Override
        public long getCapacity() { return 0; }

        @Override
        public boolean canInsert() { return true; }

        @Override
        public boolean canExtract() { return false; }
    }
}
