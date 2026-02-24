package com.logistics.core.render;

import com.logistics.LogisticsCore;
import com.logistics.core.marker.MarkerBlockEntity;
import com.logistics.core.marker.MarkerManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.model.loading.v1.FabricBakedModelManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

import java.util.List;

/**
 * Renders laser beams for active markers in MC 1.21.1.
 * Uses baked model segments for quad-based beams with proper thickness.
 */
public class MarkerBlockEntityRenderer implements BlockEntityRenderer<MarkerBlockEntity> {
    private static final ResourceLocation BEAM_MODEL_ID = LogisticsCore.model("marker_beam").toIdentifier();

    private final ModelBlockRenderer modelRenderer;

    public MarkerBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
        this.modelRenderer = ctx.getBlockRenderDispatcher().getModelRenderer();
    }

    @Override
    public void render(
            MarkerBlockEntity entity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {
        if (!entity.isActive()) {
            return;
        }

        BakedModel beamModel = getBeamModel();
        if (beamModel == null) {
            return;
        }

        BeamLengths beams = calculateBeamLengths(entity);
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.cutout());

        if (beams.north > 0) {
            renderBeamInDirection(entity, beamModel, poseStack, buffer, packedOverlay, 180, beams.north);
        }
        if (beams.south > 0) {
            renderBeamInDirection(entity, beamModel, poseStack, buffer, packedOverlay, 0, beams.south);
        }
        if (beams.east > 0) {
            renderBeamInDirection(entity, beamModel, poseStack, buffer, packedOverlay, 90, beams.east);
        }
        if (beams.west > 0) {
            renderBeamInDirection(entity, beamModel, poseStack, buffer, packedOverlay, -90, beams.west);
        }
    }

    private void renderBeamInDirection(
            MarkerBlockEntity entity,
            BakedModel beamModel,
            PoseStack poseStack,
            VertexConsumer buffer,
            int packedOverlay,
            float yRotation,
            int length) {
        for (int i = 0; i < length; i++) {
            poseStack.pushPose();

            // Move to center of marker block (Y is at base of beam model)
            poseStack.translate(0.5, -0.0625, 0.5);

            // Rotate to face the correct direction (model extends in +Z)
            poseStack.mulPose(Axis.YP.rotationDegrees(yRotation));

            // Move to segment position
            poseStack.translate(0, 0, i);

            // Shift back to align model center with block center
            poseStack.translate(-0.5, 0.0, 0.0);

            modelRenderer.tesselateWithoutAO(
                    entity.getLevel(),
                    beamModel,
                    entity.getBlockState(),
                    entity.getBlockPos(),
                    poseStack,
                    buffer,
                    false,
                    RandomSource.create(),
                    42L,
                    packedOverlay);

            poseStack.popPose();
        }
    }

    private BakedModel getBeamModel() {
        FabricBakedModelManager modelManager = (FabricBakedModelManager) Minecraft.getInstance().getModelManager();
        BakedModel model = modelManager.getModel(BEAM_MODEL_ID);
        if (model == null || model == Minecraft.getInstance().getModelManager().getMissingModel()) {
            return null;
        }
        return model;
    }

    private BeamLengths calculateBeamLengths(MarkerBlockEntity entity) {
        List<BlockPos> connectedMarkers = entity.getConnectedMarkers();
        BlockPos pos = entity.getBlockPos();

        if (!connectedMarkers.isEmpty()) {
            // Connected mode - draw beams to form rectangle outline
            int north = 0, south = 0, east = 0, west = 0;

            int posX = pos.getX();
            int posZ = pos.getZ();

            // Compute the rectangle bounds from this marker + connected markers
            int minX = posX;
            int maxX = posX;
            int minZ = posZ;
            int maxZ = posZ;

            for (BlockPos connected : connectedMarkers) {
                minX = Math.min(minX, connected.getX());
                maxX = Math.max(maxX, connected.getX());
                minZ = Math.min(minZ, connected.getZ());
                maxZ = Math.max(maxZ, connected.getZ());
            }

            // Check which corners have markers
            boolean hasMarkerAtNW = hasMarkerAt(entity, pos, minX, minZ);
            boolean hasMarkerAtNE = hasMarkerAt(entity, pos, maxX, minZ);
            boolean hasMarkerAtSW = hasMarkerAt(entity, pos, minX, maxZ);

            // North edge (z = minZ): from (minX, minZ) to (maxX, minZ)
            if (posZ == minZ) {
                if (posX == minX) {
                    east = maxX - minX;
                } else if (posX == maxX && !hasMarkerAtNW) {
                    west = maxX - minX;
                }
            }

            // South edge (z = maxZ): from (minX, maxZ) to (maxX, maxZ)
            if (posZ == maxZ) {
                if (posX == minX) {
                    east = maxX - minX;
                } else if (posX == maxX && !hasMarkerAtSW) {
                    west = maxX - minX;
                }
            }

            // West edge (x = minX): from (minX, minZ) to (minX, maxZ)
            if (posX == minX) {
                if (posZ == minZ) {
                    south = maxZ - minZ;
                } else if (posZ == maxZ && !hasMarkerAtNW) {
                    north = maxZ - minZ;
                }
            }

            // East edge (x = maxX): from (maxX, minZ) to (maxX, maxZ)
            if (posX == maxX) {
                if (posZ == minZ) {
                    south = maxZ - minZ;
                } else if (posZ == maxZ && !hasMarkerAtNE) {
                    north = maxZ - minZ;
                }
            }

            return new BeamLengths(north, south, east, west);
        } else {
            // Solo mode - project beams in all directions
            int distance = MarkerManager.MAX_MARKER_DISTANCE;
            return new BeamLengths(distance, distance, distance, distance);
        }
    }

    private boolean hasMarkerAt(MarkerBlockEntity entity, BlockPos thisPos, int x, int z) {
        if (thisPos.getX() == x && thisPos.getZ() == z) {
            return true;
        }
        for (BlockPos connected : entity.getConnectedMarkers()) {
            if (connected.getX() == x && connected.getZ() == z) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getViewDistance() {
        return 256; // Visible from far away
    }

    private record BeamLengths(int north, int south, int east, int west) {}
}
