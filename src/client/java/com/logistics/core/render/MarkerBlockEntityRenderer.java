package com.logistics.core.render;

import com.logistics.LogisticsMod;
import com.logistics.core.marker.MarkerBlockEntity;
import com.logistics.core.marker.MarkerManager;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * Renders laser beams and ghost cube outline for active markers.
 */
public class MarkerBlockEntityRenderer implements BlockEntityRenderer<MarkerBlockEntity, MarkerRenderState> {
    private static final Identifier BEAM_MODEL_ID = Identifier.of(LogisticsMod.MOD_ID, "block/core/marker_beam");

    // No tinting needed - texture is pre-colored blue (#0132FD)
    private static final float BEAM_RED = 1.0f;
    private static final float BEAM_GREEN = 1.0f;
    private static final float BEAM_BLUE = 1.0f;

    public MarkerBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {}

    @Override
    public MarkerRenderState createRenderState() {
        return new MarkerRenderState();
    }

    @Override
    public void updateRenderState(
            MarkerBlockEntity entity,
            MarkerRenderState state,
            float tickDelta,
            Vec3d cameraPos,
            @Nullable net.minecraft.client.render.command.ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlay) {
        BlockEntityRenderState.updateBlockEntityRenderState(entity, state, crumblingOverlay);

        state.active = entity.isActive();
        state.markerPos = entity.getPos();
        state.connectedMarkers.clear();
        state.connectedMarkers.addAll(entity.getConnectedMarkers());
        state.boundMin = entity.getBoundMin();
        state.boundMax = entity.getBoundMax();
        state.isCornerMarker = entity.isCornerMarker();

        // Calculate beam lengths
        if (state.active) {
            if (entity.getWorld() != null) {
                calculateBeamLengths(state, entity.getPos());
            }
        } else {
            state.beamNorth = 0;
            state.beamSouth = 0;
            state.beamEast = 0;
            state.beamWest = 0;
        }
    }

    private void calculateBeamLengths(MarkerRenderState state, BlockPos pos) {
        if (!state.connectedMarkers.isEmpty()) {
            // Connected mode - draw beams to form rectangle outline
            state.beamNorth = 0;
            state.beamSouth = 0;
            state.beamEast = 0;
            state.beamWest = 0;

            int posX = pos.getX();
            int posZ = pos.getZ();

            // Compute the rectangle bounds from this marker + connected markers
            int minX = posX;
            int maxX = posX;
            int minZ = posZ;
            int maxZ = posZ;

            for (BlockPos connected : state.connectedMarkers) {
                minX = Math.min(minX, connected.getX());
                maxX = Math.max(maxX, connected.getX());
                minZ = Math.min(minZ, connected.getZ());
                maxZ = Math.max(maxZ, connected.getZ());
            }

            // Check which corners have markers
            boolean hasMarkerAtNW = hasMarkerAt(state, pos, minX, minZ); // (minX, minZ)
            boolean hasMarkerAtNE = hasMarkerAt(state, pos, maxX, minZ); // (maxX, minZ)
            boolean hasMarkerAtSW = hasMarkerAt(state, pos, minX, maxZ); // (minX, maxZ)

            // North edge (z = minZ): from (minX, minZ) to (maxX, minZ)
            if (posZ == minZ) {
                if (posX == minX) {
                    // At west end of north edge - draw east
                    state.beamEast = maxX - minX;
                } else if (posX == maxX && !hasMarkerAtNW) {
                    // At east end and no marker at west end - draw west
                    state.beamWest = maxX - minX;
                }
            }

            // South edge (z = maxZ): from (minX, maxZ) to (maxX, maxZ)
            if (posZ == maxZ) {
                if (posX == minX) {
                    // At west end of south edge - draw east
                    state.beamEast = maxX - minX;
                } else if (posX == maxX && !hasMarkerAtSW) {
                    // At east end and no marker at west end - draw west
                    state.beamWest = maxX - minX;
                }
            }

            // West edge (x = minX): from (minX, minZ) to (minX, maxZ)
            if (posX == minX) {
                if (posZ == minZ) {
                    // At north end of west edge - draw south
                    state.beamSouth = maxZ - minZ;
                } else if (posZ == maxZ && !hasMarkerAtNW) {
                    // At south end and no marker at north end - draw north
                    state.beamNorth = maxZ - minZ;
                }
            }

            // East edge (x = maxX): from (maxX, minZ) to (maxX, maxZ)
            if (posX == maxX) {
                if (posZ == minZ) {
                    // At north end of east edge - draw south
                    state.beamSouth = maxZ - minZ;
                } else if (posZ == maxZ && !hasMarkerAtNE) {
                    // At south end and no marker at north end - draw north
                    state.beamNorth = maxZ - minZ;
                }
            }
        } else {
            // Solo mode - project beams in all directions up to MAX_MARKER_DISTANCE
            state.beamNorth = MarkerManager.MAX_MARKER_DISTANCE;
            state.beamSouth = MarkerManager.MAX_MARKER_DISTANCE;
            state.beamEast = MarkerManager.MAX_MARKER_DISTANCE;
            state.beamWest = MarkerManager.MAX_MARKER_DISTANCE;
        }
    }

    private boolean hasMarkerAt(MarkerRenderState state, BlockPos thisPos, int x, int z) {
        // Check if this marker or any connected marker is at the given X,Z position
        if (thisPos.getX() == x && thisPos.getZ() == z) {
            return true;
        }
        for (BlockPos connected : state.connectedMarkers) {
            if (connected.getX() == x && connected.getZ() == z) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void render(
            MarkerRenderState state,
            MatrixStack matrices,
            OrderedRenderCommandQueue queue,
            CameraRenderState cameraState) {
        if (!state.active) {
            return;
        }

        BlockStateModel beamModel = ModelRegistry.getModel(BEAM_MODEL_ID);
        if (beamModel == null) {
            return;
        }

        RenderLayer renderLayer = RenderLayers.cutout();

        // Render beams in each direction
        // Model extends in +Z, so rotate to point in the desired direction
        if (state.beamNorth > 0) {
            renderBeamInDirection(
                    matrices, queue, beamModel, renderLayer, state.lightmapCoordinates, 180, state.beamNorth);
        }
        if (state.beamSouth > 0) {
            renderBeamInDirection(
                    matrices, queue, beamModel, renderLayer, state.lightmapCoordinates, 0, state.beamSouth);
        }
        if (state.beamEast > 0) {
            renderBeamInDirection(
                    matrices, queue, beamModel, renderLayer, state.lightmapCoordinates, 90, state.beamEast);
        }
        if (state.beamWest > 0) {
            renderBeamInDirection(
                    matrices, queue, beamModel, renderLayer, state.lightmapCoordinates, -90, state.beamWest);
        }
    }

    private void renderBeamInDirection(
            MatrixStack matrices,
            OrderedRenderCommandQueue queue,
            BlockStateModel beamModel,
            RenderLayer renderLayer,
            int lightmap,
            float yRotation,
            int length) {
        // Render beam segments starting from center of marker
        for (int i = 0; i < length; i++) {
            matrices.push();

            // Move to center of marker block (Y is at base of beam model)
            matrices.translate(0.5, -0.0625, 0.5);

            // Rotate to face the correct direction (model extends in +Z)
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yRotation));

            // Move to segment position - start at center, end at center of last block
            // i=0 places segment at 0, covering 0 to 1 (starts at marker center in local coords)
            matrices.translate(0, 0, i);

            // Move back from center for model rendering
            matrices.translate(-0.5, 0.0, 0.0);

            queue.submitBlockStateModel(
                    matrices,
                    renderLayer,
                    beamModel,
                    BEAM_RED,
                    BEAM_GREEN,
                    BEAM_BLUE,
                    lightmap,
                    OverlayTexture.DEFAULT_UV,
                    0);

            matrices.pop();
        }
    }

    @Override
    public int getRenderDistance() {
        return 256; // Visible from far away
    }
}
