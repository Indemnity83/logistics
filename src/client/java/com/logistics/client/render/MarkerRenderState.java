package com.logistics.client.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.util.math.BlockPos;

/**
 * Render state for marker block entities.
 */
public class MarkerRenderState extends BlockEntityRenderState {
    public boolean active = false;
    public BlockPos markerPos = BlockPos.ORIGIN;
    public final List<BlockPos> connectedMarkers = new ArrayList<>();
    public BlockPos boundMin = null;
    public BlockPos boundMax = null;
    public boolean isCornerMarker = false;

    // Beam lengths in each direction (0 = no beam, >0 = beam length in blocks)
    public int beamNorth = 0;
    public int beamSouth = 0;
    public int beamEast = 0;
    public int beamWest = 0;
}
