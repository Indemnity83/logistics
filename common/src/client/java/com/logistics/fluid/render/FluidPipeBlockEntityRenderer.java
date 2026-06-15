package com.logistics.fluid.render;

import com.logistics.LogisticsMod;
import com.logistics.core.lib.client.render.FluidBoxRenderer;
import com.logistics.core.lib.client.render.VanillaQuadBaker;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.fluid.block.FluidConnection;
import com.logistics.fluid.block.entity.FluidPipeBlockEntity;
import com.logistics.fluid.pipe.FluidColumnGeometry;
import com.logistics.pipe.render.model.PipeGeometry;
import com.logistics.pipe.render.model.PipeModelResolver;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Renders fluid pipes: the pipe body (core + connection arms) via the shared code-baked geometry
 * pipeline, and the contained fluid using the fluid's own still sprite/tint (via the shared
 * {@link FluidBoxRenderer}), filled in the central node proportional to the buffer level.
 */
public class FluidPipeBlockEntityRenderer
        implements BlockEntityRenderer<FluidPipeBlockEntity, FluidPipeRenderState> {

    private static final String MODEL_PREFIX = "block/pipe/";

    // Opaque-white tint for the pipe body parts; hoisted out of the render loop to avoid per-frame allocation.
    private static final int[] WHITE_TINT = {0xFFFFFFFF};

    // Pipe geometry in block units: the core spans 4..12 px (0.25..0.75); arms run from the core face
    // to the block edge. The fluid is inset 0.1 px from the inner walls on all four longitudinal sides.
    private static final float INSET = 0.1F / 16F;
    private static final float CORE_MIN = 4.0F / 16F;
    private static final float CORE_MAX = 12.0F / 16F;
    private static final float A = CORE_MIN + INSET; // inset inner wall (low side)
    private static final float B = CORE_MAX - INSET; // inset inner wall (high side)
    // Minimum fluid column height to draw; below this a partial buffer reads as a flat plane, so skip it.
    private static final float MIN_VISIBLE_THICKNESS = 0.4F / 16F;
    // The rendered surface snaps to coarse bands so it stays steady instead of jittering with every tiny
    // buffer change (visual fullness is deliberately decoupled from the exact mB amount).
    private static final int RENDER_BANDS = 8;

    // Body geometry never changes at runtime — cache parts by model id and sprites by texture base.
    private final Map<ResourceId, List<BlockStateModelPart>> partsCache = new HashMap<>();
    private final Map<String, TextureAtlasSprite> spriteCache = new HashMap<>();

    public FluidPipeBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public FluidPipeRenderState createRenderState() {
        return new FluidPipeRenderState();
    }

    @Override
    public void extractRenderState(
            FluidPipeBlockEntity entity,
            FluidPipeRenderState state,
            float tickDelta,
            Vec3 cameraPos,
            ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderState.extractBase(entity, state, crumblingOverlay);

        String base = baseName(entity);
        state.models.clear();
        state.models.add(modelInfo(base + "_core", null));
        for (Direction direction : Direction.values()) {
            boolean connected = entity.connection(direction) != FluidConnection.NONE;
            state.connectedArms[direction.get3DDataValue()] = connected;
            if (connected) {
                state.models.add(modelInfo(base + "_arm", direction));
            }
        }
        for (FluidPipeRenderState.ModelRenderInfo info : state.models) {
            info.parts = partsCache.computeIfAbsent(info.modelId, this::buildParts);
        }

        extractFluid(entity, state);
    }

    private void extractFluid(FluidPipeBlockEntity entity, FluidPipeRenderState state) {
        long amount = entity.tank().getAmount();
        long capacity = entity.tank().getCapacity();
        Level level = entity.getLevel();
        state.hasFluid = amount > 0 && capacity > 0 && !entity.tank().isEmpty() && level != null;
        state.fillRatio = state.hasFluid ? bandedFillRatio(amount, capacity) : 0.0F;
        if (!state.hasFluid) {
            state.sprite = null;
            return;
        }

        FluidBoxRenderer.Appearance appearance =
                FluidBoxRenderer.resolve(entity.tank().getFluidKey().getFluid(), level, entity.getBlockPos());
        if (appearance == null) {
            state.hasFluid = false;
            state.sprite = null;
            return;
        }
        state.sprite = appearance.sprite();
        state.tintColor = appearance.tint();
    }

    /** Snap the fill to coarse bands; a non-empty pipe always reads at least one band so it stays visible. */
    private static float bandedFillRatio(long amount, long capacity) {
        float raw = Math.min(1.0F, (float) amount / capacity);
        float banded = Math.round(raw * RENDER_BANDS) / (float) RENDER_BANDS;
        return Math.max(1.0F / RENDER_BANDS, banded);
    }

    private static String baseName(FluidPipeBlockEntity entity) {
        return entity.kind().isExtractor() ? "fluid_extractor_pipe" : "copper_fluid_pipe";
    }

    private static FluidPipeRenderState.ModelRenderInfo modelInfo(String base, Direction armDirection) {
        return new FluidPipeRenderState.ModelRenderInfo(LogisticsMod.modId(MODEL_PREFIX + base), armDirection);
    }

    private List<BlockStateModelPart> buildParts(ResourceId modelId) {
        String base = modelId.getPath().substring(MODEL_PREFIX.length());
        List<BlockStateModelPart> parts = new ArrayList<>();
        for (PipeModelResolver.Layer layer : PipeModelResolver.resolve(base)) {
            TextureAtlasSprite sprite = sprite(layer.textureBase());
            VanillaQuadBaker baker = new VanillaQuadBaker(sprite, layer.tintIndex(), true, 0);
            PipeGeometry.emit(baker::quad, layer.shape(), sprite);
            if (!baker.isEmpty()) {
                parts.add(baker.toPart());
            }
        }
        return parts;
    }

    private TextureAtlasSprite sprite(String textureBase) {
        return spriteCache.computeIfAbsent(textureBase, b -> Minecraft.getInstance().getAtlasManager()
                .getAtlasOrThrow(AtlasIds.BLOCKS)
                .getSprite(LogisticsMod.modId("block/pipe/" + b).toIdentifier()));
    }

    @Override
    public void submit(
            FluidPipeRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState camera) {
        for (FluidPipeRenderState.ModelRenderInfo info : state.models) {
            if (info.parts == null || info.parts.isEmpty()) {
                continue;
            }
            if (info.armDirection != null) {
                matrices.pushPose();
                matrices.translate(0.5, 0.5, 0.5);
                applyDirectionRotation(matrices, info.armDirection);
                matrices.translate(-0.5, -0.5, -0.5);
            }
            queue.submitBlockModel(
                    matrices,
                    RenderTypes.cutoutMovingBlock(),
                    info.parts,
                    WHITE_TINT,
                    state.lightCoords,
                    OverlayTexture.NO_OVERLAY,
                    0);
            if (info.armDirection != null) {
                matrices.popPose();
            }
        }

        if (state.hasFluid && state.sprite != null && state.fillRatio > 0.0F) {
            TextureAtlasSprite sprite = state.sprite;
            int color = FluidBoxRenderer.opaque(state.tintColor);
            int light = state.lightCoords;
            float ratio = Math.min(1.0F, state.fillRatio);
            boolean down = state.connectedArms[Direction.DOWN.get3DDataValue()];
            boolean up = state.connectedArms[Direction.UP.get3DDataValue()];

            // The surface is a function of fill over the core alone, so the level lines up across pipe shapes.
            // A down arm sits below it (full whenever wet); an up arm only fills once the pipe is full.
            FluidColumnGeometry.Column column = FluidColumnGeometry.of(
                    ratio, down, up, CORE_MIN + INSET, CORE_MAX - INSET, INSET, 1.0F - INSET);

            // Below a visible thickness the column would render as a flat plane — treat as empty.
            if (column.top() - column.bottom() < MIN_VISIBLE_THICKNESS) {
                return;
            }

            // Vertical column (down arm + core + up arm share the same inset cross-section).
            float columnBottom = column.bottom();
            float columnTop = column.top();
            queue.submitCustomGeometry(
                    matrices,
                    RenderTypes.translucentMovingBlock(),
                    (entry, buffer) -> FluidBoxRenderer.renderBox(
                            entry, buffer, sprite, color, light, A, columnBottom, A, B, columnTop, B, true, true));

            // Horizontal arms fill to the core surface, matching the central column.
            float hBottom = CORE_MIN + INSET;
            float hTop = column.surface();
            if (hTop > hBottom) {
                submitHorizontalArm(state, queue, matrices, sprite, color, light, Direction.NORTH, hBottom, hTop);
                submitHorizontalArm(state, queue, matrices, sprite, color, light, Direction.SOUTH, hBottom, hTop);
                submitHorizontalArm(state, queue, matrices, sprite, color, light, Direction.WEST, hBottom, hTop);
                submitHorizontalArm(state, queue, matrices, sprite, color, light, Direction.EAST, hBottom, hTop);
            }
        }
    }

    private static void submitHorizontalArm(
            FluidPipeRenderState state, SubmitNodeCollector queue, PoseStack matrices,
            TextureAtlasSprite sprite, int color, int light, Direction direction, float hBottom, float hTop) {
        if (!state.connectedArms[direction.get3DDataValue()]) {
            return;
        }
        float x0;
        float z0;
        float x1;
        float z1;
        switch (direction) {
            case NORTH -> { x0 = A; z0 = 0.0F; x1 = B; z1 = A; }
            case SOUTH -> { x0 = A; z0 = B; x1 = B; z1 = 1.0F; }
            case WEST -> { x0 = 0.0F; z0 = A; x1 = A; z1 = B; }
            case EAST -> { x0 = B; z0 = A; x1 = 1.0F; z1 = B; }
            default -> { return; }
        }
        queue.submitCustomGeometry(
                matrices,
                RenderTypes.translucentMovingBlock(),
                (entry, buffer) ->
                        FluidBoxRenderer.renderBox(entry, buffer, sprite, color, light, x0, hBottom, z0, x1, hTop, z1, true, true));
    }

    private static void applyDirectionRotation(PoseStack matrices, Direction direction) {
        switch (direction) {
            case SOUTH -> matrices.mulPose(Axis.YP.rotationDegrees(180));
            case EAST -> matrices.mulPose(Axis.YP.rotationDegrees(-90));
            case WEST -> matrices.mulPose(Axis.YP.rotationDegrees(90));
            case UP -> matrices.mulPose(Axis.XP.rotationDegrees(90));
            case DOWN -> matrices.mulPose(Axis.XP.rotationDegrees(-90));
            default -> {}
        }
    }
}
