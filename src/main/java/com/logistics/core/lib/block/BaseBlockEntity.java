package com.logistics.core.lib.block;

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
 * <p>Subclasses override {@link #saveCustomData(CompoundTag)} and {@link #loadCustomData(CompoundTag)}
 * instead of dealing with {@link ValueInput}/{@link ValueOutput} directly.
 */
public abstract class BaseBlockEntity extends BlockEntity {

    protected BaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // ==================== Disk Persistence ====================

    /**
     * Save custom block entity data to NBT.
     * Override this instead of {@link #saveAdditional(ValueOutput)}.
     *
     * @param tag the compound tag to write custom data into
     */
    protected void saveCustomData(CompoundTag tag) {
        // Default: no custom data
    }

    /**
     * Load custom block entity data from NBT.
     * Override this instead of {@link #loadAdditional(ValueInput)}.
     *
     * @param tag the compound tag to read custom data from
     */
    protected void loadCustomData(CompoundTag tag) {
        // Default: no custom data
    }

    @Override
    protected final void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        CompoundTag customData = new CompoundTag();
        saveCustomData(customData);
        if (!customData.isEmpty()) {
            view.store("CustomData", CompoundTag.CODEC, customData);
        }
    }

    @Override
    protected final void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        view.read("CustomData", CompoundTag.CODEC).ifPresent(this::loadCustomData);
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
     * </ul>
     */
    protected final void markDirtyAndSync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            // Push block updates to clients (redraw + blockstate listeners)
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