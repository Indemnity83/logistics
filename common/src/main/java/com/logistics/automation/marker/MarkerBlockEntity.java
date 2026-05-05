package com.logistics.automation.marker;

import com.logistics.LogisticsAutomation;
import com.logistics.core.lib.BaseBlockEntity;
import com.logistics.core.lib.storage.NbtCompat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Block entity for markers that stores connection and bounding box data.
 */
public class MarkerBlockEntity extends BaseBlockEntity {
    // Connected marker positions (up to 2 horizontal + 1 vertical)
    private final List<BlockPos> connectedMarkers = new ArrayList<>();

    // Bounding box when valid triangle is formed
    private BlockPos boundMin = null;
    private BlockPos boundMax = null;
    private boolean isCornerMarker = false;

    public MarkerBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsAutomation.ENTITY.MARKER_BLOCK_ENTITY, pos, state);
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
        markDirtyAndSync();
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
        markDirtyAndSync();
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
        markDirtyAndSync();
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

    @Override
    protected void saveLogisticsData(CompoundTag data, HolderLookup.Provider registries) {
        super.saveLogisticsData(data, registries);

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
            data.putInt("BoundMinX", boundMin.getX());
            data.putInt("BoundMinY", boundMin.getY());
            data.putInt("BoundMinZ", boundMin.getZ());
        }
        if (boundMax != null) {
            data.putInt("BoundMaxX", boundMax.getX());
            data.putInt("BoundMaxY", boundMax.getY());
            data.putInt("BoundMaxZ", boundMax.getZ());
        }

        data.putBoolean("IsCornerMarker", isCornerMarker);
    }

    @Override
    protected void loadLogisticsData(CompoundTag data, HolderLookup.Provider registries) {
        super.loadLogisticsData(data, registries);

        connectedMarkers.clear();
        boundMin = null;
        boundMax = null;
        isCornerMarker = false;

        loadConnectedMarkers(data);
        loadBounds(data);
        if (!hasValidBounds()) {
            isCornerMarker = false;
        }
    }

    @Override
    protected void loadLegacyData(net.minecraft.world.level.storage.ValueInput view) {
        super.loadLegacyData(view);

        view.read("MarkerData", net.minecraft.nbt.CompoundTag.CODEC).ifPresent(data -> {
            connectedMarkers.clear();
            boundMin = null;
            boundMax = null;
            isCornerMarker = false;

            loadConnectedMarkers(data);

            // Convert old key names to new format for loadBounds
            CompoundTag massaged = new CompoundTag();
            if (data.contains("MinX")) massaged.putInt("BoundMinX", NbtCompat.getInt(data, "MinX", 0));
            if (data.contains("MinY")) massaged.putInt("BoundMinY", NbtCompat.getInt(data, "MinY", 0));
            if (data.contains("MinZ")) massaged.putInt("BoundMinZ", NbtCompat.getInt(data, "MinZ", 0));
            if (data.contains("MaxX")) massaged.putInt("BoundMaxX", NbtCompat.getInt(data, "MaxX", 0));
            if (data.contains("MaxY")) massaged.putInt("BoundMaxY", NbtCompat.getInt(data, "MaxY", 0));
            if (data.contains("MaxZ")) massaged.putInt("BoundMaxZ", NbtCompat.getInt(data, "MaxZ", 0));
            if (data.contains("IsCorner")) massaged.putBoolean("IsCornerMarker", NbtCompat.getBoolean(data, "IsCorner", false));

            loadBounds(massaged);
            if (!hasValidBounds()) {
                isCornerMarker = false;
            }
        });
    }

    private void loadBounds(CompoundTag data) {
        boolean hasMin = data.contains("BoundMinX");
        boolean hasMax = data.contains("BoundMaxX");
        if (hasMin && hasMax) {
            int minX = NbtCompat.getInt(data, "BoundMinX", 0);
            int minY = NbtCompat.getInt(data, "BoundMinY", 0);
            int minZ = NbtCompat.getInt(data, "BoundMinZ", 0);
            boundMin = new BlockPos(minX, minY, minZ);

            int maxX = NbtCompat.getInt(data, "BoundMaxX", 0);
            int maxY = NbtCompat.getInt(data, "BoundMaxY", 0);
            int maxZ = NbtCompat.getInt(data, "BoundMaxZ", 0);
            boundMax = new BlockPos(maxX, maxY, maxZ);
        }

        if (data.contains("IsCornerMarker")) {
            isCornerMarker = NbtCompat.getBoolean(data, "IsCornerMarker", false);
        }
    }

    private void loadConnectedMarkers(CompoundTag data) {
        NbtCompat.ifHasIntArray(data, "ConnectedMarkers", positions -> {
            for (int i = 0; i < positions.length / 3; i++) {
                connectedMarkers.add(new BlockPos(positions[i * 3], positions[i * 3 + 1], positions[i * 3 + 2]));
            }
        });
    }
}
