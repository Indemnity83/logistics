package com.logistics.core.marker;

import com.logistics.core.registry.CoreBlockEntities;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.protocol.Packet;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Block entity for markers that stores connection and bounding box data.
 */
public class MarkerBlockEntity extends BlockEntity {
    // Connected marker positions (up to 2 horizontal + 1 vertical)
    private final List<BlockPos> connectedMarkers = new ArrayList<>();

    // Bounding box when valid triangle is formed
    private BlockPos boundMin = null;
    private BlockPos boundMax = null;
    private boolean isCornerMarker = false;

    public MarkerBlockEntity(BlockPos pos, BlockState state) {
        super(CoreBlockEntities.MARKER_BLOCK_ENTITY, pos, state);
    }

    /**
     * Toggle marker activation when right-clicked with wrench.
     */
    public void toggleActivation(Player player) {
        if (level == null || level.isClientSide()) return;

        boolean currentlyActive = getBlockState().getValue(MarkerBlock.ACTIVE);

        if (currentlyActive) {
            // Deactivate all connected markers first, then this one
            deactivateConnectedMarkers();
            deactivate();
            player.displayClientMessage(Component.translatable("marker.deactivated"), true);
        } else {
            // Always activate the marker
            MarkerManager.ActivationResult result = MarkerManager.tryActivateMarker(level, worldPosition);
            switch (result.status()) {
                case SUCCESS -> {
                    if (result.detailKey() != null) {
                        Object[] args = result.detailArgs() == null ? new Object[0] : result.detailArgs();
                        player.displayClientMessage(Component.translatable(result.detailKey(), args), true);
                    } else {
                        player.displayClientMessage(Component.translatable("marker.activated"), true);
                    }
                }
                case NO_CONNECTIONS -> {
                    // Activate solo (no connections, just project beams)
                    activateSolo();
                    player.displayClientMessage(Component.translatable("marker.activated.solo"), true);
                }
                case FAILURE -> {
                    if (result.detailKey() != null) {
                        Object[] args = result.detailArgs() == null ? new Object[0] : result.detailArgs();
                        player.displayClientMessage(Component.translatable(result.detailKey(), args), true);
                    } else {
                        player.displayClientMessage(Component.translatable("marker.activation.failed"), true);
                    }
                }
                default -> {}
            }
        }
    }

    /**
     * Activate this marker without connections (solo mode - just projects beams).
     */
    public void activateSolo() {
        if (level == null) return;

        connectedMarkers.clear();
        this.boundMin = null;
        this.boundMax = null;
        this.isCornerMarker = false;

        level.setBlock(worldPosition, getBlockState().setValue(MarkerBlock.ACTIVE, true), 3);
        setChanged();
        syncToClients();
    }

    /**
     * Activate this marker with connections to other markers.
     */
    public void activate(List<BlockPos> connections, @Nullable BlockPos min, @Nullable BlockPos max, boolean isCorner) {
        if (level == null) return;

        connectedMarkers.clear();
        connectedMarkers.addAll(connections);
        this.boundMin = min;
        this.boundMax = max;
        this.isCornerMarker = isCorner;

        level.setBlock(worldPosition, getBlockState().setValue(MarkerBlock.ACTIVE, true), 3);
        setChanged();
        syncToClients();
    }

    /**
     * Deactivate this marker.
     */
    public void deactivate() {
        if (level == null) return;

        connectedMarkers.clear();
        boundMin = null;
        boundMax = null;
        isCornerMarker = false;

        level.setBlock(worldPosition, getBlockState().setValue(MarkerBlock.ACTIVE, false), 3);
        setChanged();
        syncToClients();
    }

    /**
     * Deactivate all markers connected to this one.
     */
    public void deactivateConnectedMarkers() {
        if (level == null) return;

        for (BlockPos connectedPos : new ArrayList<>(connectedMarkers)) {
            BlockEntity entity = level.getBlockEntity(connectedPos);
            if (entity instanceof MarkerBlockEntity marker) {
                marker.deactivate();
            }
        }
    }

    public boolean isActive() {
        return getBlockState().getValue(MarkerBlock.ACTIVE);
    }

    public List<BlockPos> getConnectedMarkers() {
        return new ArrayList<>(connectedMarkers);
    }

    @Nullable public BlockPos getBoundMin() {
        return boundMin;
    }

    @Nullable public BlockPos getBoundMax() {
        return boundMax;
    }

    public boolean isCornerMarker() {
        return isCornerMarker;
    }

    public boolean hasValidBounds() {
        return boundMin != null && boundMax != null;
    }

    private void syncToClients() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);

        CompoundTag data = new CompoundTag();

        // Save connected markers
        if (!connectedMarkers.isEmpty()) {
            int[] positions = new int[connectedMarkers.size() * 3];
            for (int i = 0; i < connectedMarkers.size(); i++) {
                BlockPos p = connectedMarkers.get(i);
                positions[i * 3] = p.getX();
                positions[i * 3 + 1] = p.getY();
                positions[i * 3 + 2] = p.getZ();
            }
            data.putIntArray("ConnectedMarkers", positions);
        }

        // Save bounds
        if (boundMin != null) {
            data.putInt("MinX", boundMin.getX());
            data.putInt("MinY", boundMin.getY());
            data.putInt("MinZ", boundMin.getZ());
        }
        if (boundMax != null) {
            data.putInt("MaxX", boundMax.getX());
            data.putInt("MaxY", boundMax.getY());
            data.putInt("MaxZ", boundMax.getZ());
        }

        data.putBoolean("IsCorner", isCornerMarker);

        view.store("MarkerData", CompoundTag.CODEC, data);
    }

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);

        view.read("MarkerData", CompoundTag.CODEC).ifPresent(data -> {
            connectedMarkers.clear();
            boundMin = null;
            boundMax = null;
            isCornerMarker = false;

            // Load connected markers
            if (data.contains("ConnectedMarkers")) {
                data.getIntArray("ConnectedMarkers").ifPresent(positions -> {
                    for (int i = 0; i < positions.length / 3; i++) {
                        connectedMarkers.add(new BlockPos(positions[i * 3], positions[i * 3 + 1], positions[i * 3 + 2]));
                    }
                });
            }

            // Load bounds
            boolean hasMin = data.contains("MinX");
            boolean hasMax = data.contains("MaxX");
            if (hasMin && hasMax) {
                int minX = data.getInt("MinX").orElse(0);
                int minY = data.getInt("MinY").orElse(0);
                int minZ = data.getInt("MinZ").orElse(0);
                boundMin = new BlockPos(minX, minY, minZ);

                int maxX = data.getInt("MaxX").orElse(0);
                int maxY = data.getInt("MaxY").orElse(0);
                int maxZ = data.getInt("MaxZ").orElse(0);
                boundMax = new BlockPos(maxX, maxY, maxZ);
            }

            if (data.contains("IsCorner")) {
                isCornerMarker = data.getBoolean("IsCorner").orElse(false);
            }
        });
    }

    @Nullable @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
