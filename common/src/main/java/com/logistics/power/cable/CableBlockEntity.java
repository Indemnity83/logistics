package com.logistics.power.cable;

import com.logistics.LogisticsPower;
import com.logistics.core.lib.BaseBlockEntity;
import com.logistics.core.lib.block.capability.HasEnergyStorage;
import com.logistics.core.lib.power.AcceptsLowTierEnergy;
import com.logistics.core.lib.power.EnergyDemandProvider;
import com.logistics.core.lib.support.ProbeResult;
import com.logistics.core.lib.storage.NbtCompat;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Locale;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;

/**
 * Block entity for cables. Cables do not store energy; they expose
 * an insert-only conduit endpoint that forwards accepted energy through the
 * connected cable network during the same transaction.
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
    public EnergyStorage energyStorage(@Nullable Direction side) {
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

    // ==================== Probe ====================

    public ProbeResult getProbeResult() {
        ProbeResult.Builder builder = ProbeResult.builder(tier().displayName())
                .entry("Transfer", String.format("%d RF/t", getTransferRate()), ChatFormatting.AQUA);

        addConnectedMachineEntries(builder);
        return builder.build();
    }

    private CableTier tier() {
        return getBlockState().getBlock() instanceof CableBlock cableBlock ? cableBlock.tier() : CableTier.COPPER;
    }

    private void addConnectedMachineEntries(ProbeResult.Builder builder) {
        if (level == null) return;

        boolean hasMachineEntry = false;
        for (Direction direction : Direction.values()) {
            if (getCachedConnectionType(direction) != CableBlock.ConnectionType.DEVICE) continue;

            BlockPos neighborPos = worldPosition.relative(direction);
            if (!level.isLoaded(neighborPos)) continue;

            EnergyStorage storage = EnergyStorage.SIDED.find(level, neighborPos, direction.getOpposite());
            if (storage == null || !storage.supportsInsertion()) continue;

            if (!hasMachineEntry) {
                builder.separator();
                hasMachineEntry = true;
            }

            BlockEntity blockEntity = level.getBlockEntity(neighborPos);
            BlockState neighborState = level.getBlockState(neighborPos);
            long demand = connectedDemand(blockEntity, storage);
            builder.entry(
                    direction.getSerializedName().toUpperCase(Locale.ROOT),
                    String.format("%s: %s", neighborState.getBlock().getName().getString(), formatRate(demand)),
                    demand > 0 ? ChatFormatting.GREEN : ChatFormatting.GRAY);
        }
    }

    private long connectedDemand(@Nullable BlockEntity blockEntity, EnergyStorage storage) {
        long demand = blockEntity instanceof EnergyDemandProvider provider
                ? provider.networkDemandPerTick()
                : storageRoom(storage);
        return Math.min(Math.max(0, demand), getTransferRate());
    }

    private static long storageRoom(EnergyStorage storage) {
        long capacity = storage.getCapacity();
        if (capacity == Long.MAX_VALUE) return Long.MAX_VALUE;
        return Math.max(0, capacity - storage.getAmount());
    }

    private static String formatRate(long rate) {
        return rate > 0 ? String.format("%,d RF/t demand", rate) : "idle";
    }

    private final class CableEnergyStorage implements EnergyStorage {
        @Nullable
        private final Direction side;

        private CableEnergyStorage(@Nullable Direction side) {
            this.side = side;
        }

        @Override
        public boolean supportsInsertion() { return true; }

        @Override
        public long insert(long maxAmount, TransactionContext transaction) {
            if (maxAmount <= 0 || level == null || level.isClientSide()) {
                return 0;
            }
            return CableNetworkManager.get(level).insert(
                    level, worldPosition, side, Math.min(maxAmount, getTransferRate()), transaction);
        }

        @Override
        public boolean supportsExtraction() { return false; }

        @Override
        public long extract(long maxAmount, TransactionContext transaction) { return 0; }

        @Override
        public long getAmount() { return 0; }

        @Override
        public long getCapacity() { return 0; }
    }
}
