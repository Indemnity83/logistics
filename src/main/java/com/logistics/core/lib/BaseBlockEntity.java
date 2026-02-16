package com.logistics.core.lib;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Thin base class for block entities providing common patterns:
 * <ul>
 *   <li>Template methods for custom NBT serialization</li>
 *   <li>Helper for marking dirty and syncing to clients</li>
 *   <li>Proper client chunk sync and update packets</li>
 * </ul>
 *
 * <p>Subclasses override {@link #saveLogisticsData(CompoundTag)} and {@link #loadLogisticsData(CompoundTag)}
 * for normal persistence, using the simple NBT tag-based API.
 */
public abstract class BaseBlockEntity extends BlockEntity {

    private static final String DATA_KEY = "LogisticsData";

    protected BaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // ==================== Disk Persistence ====================

    /**
     * Save logistics block entity data to NBT.
     * Override this instead of {@link #saveAdditional(CompoundTag, net.minecraft.core.HolderLookup.Provider)}.
     *
     * <p>Note: Use {@code level.registryAccess()} to serialize ItemStacks and other registry objects.
     *
     * @param tag the compound tag to write logistics data into
     */
    protected void saveLogisticsData(CompoundTag tag) {
        // Default: no logistics data
    }

    /**
     * Load logistics block entity data from NBT.
     * Override this instead of {@link #loadAdditional(CompoundTag, net.minecraft.core.HolderLookup.Provider)}.
     *
     * <p>Note: Use {@code level.registryAccess()} to deserialize ItemStacks and other registry objects.
     *
     * @param tag the compound tag to read logistics data from
     */
    protected void loadLogisticsData(CompoundTag tag) {
        // Default: no logistics data
    }

    /**
     * Load legacy NBT data from the root level (pre-BaseBlockEntity format).
     * Override this to provide backward compatibility for worlds saved before the BaseBlockEntity migration.
     *
     * <p>This method is called when DATA_KEY tag is not present in the NBT,
     * indicating an old save format where data was stored at the root level.
     *
     * <p>TODO: This method should be removed prior to v1.0 release
     *
     * @param nbt the compound tag to read legacy data from
     */
    protected void loadLegacyData(CompoundTag nbt) {
        // Default: no legacy data migration needed
    }

    @Override
    protected final void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.saveAdditional(nbt, registries);
        CompoundTag logisticsData = new CompoundTag();
        saveLogisticsData(logisticsData);
        if (!logisticsData.isEmpty()) {
            nbt.put(DATA_KEY, logisticsData);
        }
    }

    @Override
    protected final void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.loadAdditional(nbt, registries);
        if (nbt.contains(DATA_KEY)) {
            loadLogisticsData(nbt.getCompound(DATA_KEY));
        } else {
            loadLegacyData(nbt);  // Changed signature: CompoundTag instead of ValueInput
        }
    }

    // ==================== Client Sync ====================

    /**
     * Marks the block entity as dirty and syncs changes to clients.
     * Call this whenever component state changes that needs to be persisted or synced.
     *
     * <p>This triggers:
     * <ul>
     *   <li>Chunk saving (via {@link #setChanged()})</li>
     *   <li>Client block updates (via {@link net.minecraft.world.level.Level#sendBlockUpdated})</li>
     *   <li>Neighbor block updates (comparators, redstone) via {@link Block#UPDATE_ALL}</li>
     * </ul>
     *
     * <p><b>Note on UPDATE_ALL:</b> Uses {@code Block.UPDATE_ALL} (flag 3) which includes
     * both {@code UPDATE_CLIENTS} and {@code UPDATE_NEIGHBORS}. This ensures comparators
     * and redstone circuits update correctly when block entity state changes (e.g., energy
     * levels, inventory contents). If neighbor updates are not needed for performance,
     * subclasses can call {@code level.sendBlockUpdated} directly with {@code UPDATE_CLIENTS}.
     */
    protected final void markDirtyAndSync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    /**
     * Client chunk sync: what the client receives when chunk loads.
     */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    /**
     * Optional "live" update packet when you call {@link #markDirtyAndSync()}.
     */
    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}