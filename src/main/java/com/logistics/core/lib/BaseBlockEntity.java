package com.logistics.core.lib;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * Thin base class for block entities providing common patterns:
 * <ul>
 *   <li>Template methods for custom NBT serialization</li>
 *   <li>Helper for marking dirty and syncing to clients</li>
 *   <li>Proper client chunk sync and update packets</li>
 * </ul>
 *
 * <p>Subclasses override {@link #saveLogisticsData(CompoundTag, HolderLookup.Provider)} and
 * {@link #loadLogisticsData(CompoundTag, HolderLookup.Provider)} for normal persistence,
 * using the simple NBT tag-based API.
 */
public abstract class BaseBlockEntity extends BlockEntity {

    private static final String DATA_KEY = "LogisticsData";

    protected BaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // ==================== Disk Persistence ====================

    /**
     * Save logistics block entity data to NBT.
     * Override this instead of {@link #saveAdditional(ValueOutput)}.
     *
     * @param tag the compound tag to write logistics data into
     * @param registries registry access for serializing ItemStacks and other registry objects
     */
    protected void saveLogisticsData(CompoundTag tag, HolderLookup.Provider registries) {
        // Default: no logistics data
    }

    /**
     * Load logistics block entity data from NBT.
     * Override this instead of {@link #loadAdditional(ValueInput)}.
     *
     * <p>Use {@code registries} (not {@code level.registryAccess()}) to deserialize ItemStacks —
     * {@code level} is null at load time because {@code setLevel} is called after deserialization.
     *
     * @param tag the compound tag to read logistics data from
     * @param registries registry access for deserializing ItemStacks and other registry objects
     */
    protected void loadLogisticsData(CompoundTag tag, HolderLookup.Provider registries) {
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
     * @param view the value input to read legacy data from
     */
    protected void loadLegacyData(ValueInput view) {
        // Default: no legacy data migration needed
    }

    @Override
    protected final void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        CompoundTag logisticsData = new CompoundTag();
        // level is non-null during disk save; BuiltInRegistries is the fallback for tests
        HolderLookup.Provider registries = level != null
                ? level.registryAccess()
                : RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        saveLogisticsData(logisticsData, registries);
        if (!logisticsData.isEmpty()) {
            view.store(DATA_KEY, CompoundTag.CODEC, logisticsData);
        }
    }

    @Override
    protected final void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        HolderLookup.Provider registries = view.lookup();
        // Try to read new format first; fall back to legacy root-level keys
        view.read(DATA_KEY, CompoundTag.CODEC).ifPresentOrElse(
                tag -> loadLogisticsData(tag, registries),
                () -> loadLegacyData(view)
        );
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
