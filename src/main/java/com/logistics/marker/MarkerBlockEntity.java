package com.logistics.marker;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
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
        super(MarkerBlockEntities.MARKER_BLOCK_ENTITY, pos, state);
    }

    /**
     * Toggle marker activation when right-clicked with wrench.
     */
    public void toggleActivation(PlayerEntity player) {
        if (world == null || world.isClient()) return;

        boolean currentlyActive = getCachedState().get(MarkerBlock.ACTIVE);

        if (currentlyActive) {
            // Deactivate all connected markers first, then this one
            deactivateConnectedMarkers();
            deactivate();
            player.sendMessage(Text.literal("Marker deactivated"), true);
        } else {
            // Always activate the marker
            MarkerManager.ActivationResult result = MarkerManager.tryActivateMarker(world, pos);
            if (result.success()) {
                player.sendMessage(Text.literal("Marker activated - " + result.message()), true);
            } else {
                if ("No other markers found nearby".equals(result.message())) {
                    // Activate solo (no connections, just project beams)
                    activateSolo();
                    player.sendMessage(Text.literal("Marker activated"), true);
                } else {
                    player.sendMessage(Text.literal("Marker activation failed - " + result.message()), true);
                }
            }
        }
    }

    /**
     * Activate this marker without connections (solo mode - just projects beams).
     */
    public void activateSolo() {
        if (world == null) return;

        connectedMarkers.clear();
        this.boundMin = null;
        this.boundMax = null;
        this.isCornerMarker = false;

        world.setBlockState(pos, getCachedState().with(MarkerBlock.ACTIVE, true));
        markDirty();
        syncToClients();
    }

    /**
     * Activate this marker with connections to other markers.
     */
    public void activate(List<BlockPos> connections, @Nullable BlockPos min, @Nullable BlockPos max, boolean isCorner) {
        if (world == null) return;

        connectedMarkers.clear();
        connectedMarkers.addAll(connections);
        this.boundMin = min;
        this.boundMax = max;
        this.isCornerMarker = isCorner;

        world.setBlockState(pos, getCachedState().with(MarkerBlock.ACTIVE, true));
        markDirty();
        syncToClients();
    }

    /**
     * Deactivate this marker.
     */
    public void deactivate() {
        if (world == null) return;

        connectedMarkers.clear();
        boundMin = null;
        boundMax = null;
        isCornerMarker = false;

        world.setBlockState(pos, getCachedState().with(MarkerBlock.ACTIVE, false));
        markDirty();
        syncToClients();
    }

    /**
     * Deactivate all markers connected to this one.
     */
    public void deactivateConnectedMarkers() {
        if (world == null) return;

        for (BlockPos connectedPos : new ArrayList<>(connectedMarkers)) {
            BlockEntity entity = world.getBlockEntity(connectedPos);
            if (entity instanceof MarkerBlockEntity marker) {
                marker.deactivate();
            }
        }
    }

    public boolean isActive() {
        return getCachedState().get(MarkerBlock.ACTIVE);
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
        if (world != null && !world.isClient()) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);

        NbtCompound data = new NbtCompound();

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

        view.put("MarkerData", NbtCompound.CODEC, data);
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);

        connectedMarkers.clear();
        boundMin = null;
        boundMax = null;
        isCornerMarker = false;

        view.read("MarkerData", NbtCompound.CODEC).ifPresent(data -> {
            // Load connected markers
            data.getIntArray("ConnectedMarkers").ifPresent(positions -> {
                for (int i = 0; i < positions.length / 3; i++) {
                    connectedMarkers.add(new BlockPos(
                            positions[i * 3],
                            positions[i * 3 + 1],
                            positions[i * 3 + 2]));
                }
            });

            // Load bounds
            if (data.contains("MinX")) {
                int minX = data.getInt("MinX").orElse(0);
                int minY = data.getInt("MinY").orElse(0);
                int minZ = data.getInt("MinZ").orElse(0);
                boundMin = new BlockPos(minX, minY, minZ);
            }
            if (data.contains("MaxX")) {
                int maxX = data.getInt("MaxX").orElse(0);
                int maxY = data.getInt("MaxY").orElse(0);
                int maxZ = data.getInt("MaxZ").orElse(0);
                boundMax = new BlockPos(maxX, maxY, maxZ);
            }

            isCornerMarker = data.getBoolean("IsCorner").orElse(false);
        });
    }

    @Override
    public void onBlockReplaced(BlockPos pos, BlockState oldState) {
        super.onBlockReplaced(pos, oldState);

        // Deactivate connected markers when this marker is removed
        if (world != null && !world.isClient() && isActive()) {
            deactivateConnectedMarkers();
        }
    }

    @Nullable @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
        return createNbt(registryLookup);
    }
}
