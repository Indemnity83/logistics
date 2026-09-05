package com.logistics.pipe.render;

import com.logistics.LogisticsMod;
import com.logistics.core.lib.client.render.FluidBoxRenderer;
import com.logistics.core.lib.client.render.VanillaQuadBaker;
import com.logistics.core.lib.pipe.CoreDecoration;
import com.logistics.core.lib.pipe.FluidColumnGeometry;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.pipe.FluidPipe;
import com.logistics.pipe.block.FluidConnection;
import com.logistics.pipe.block.entity.FluidPipeBlockEntity;
import com.logistics.pipe.render.model.PipeGeometry;
import com.logistics.pipe.render.model.PipeModelResolver;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Renders fluid pipes: the pipe body (core + connection arms) via the shared code-baked geometry
 * pipeline, and the contained fluid using the fluid's own still sprite/tint (via the shared
 * {@link FluidBoxRenderer}), filled in the central node proportional to the buffer level.
 *
 * <p>MC 1.21.1 uses the classic {@link BlockEntityRenderer} API (no render-state / SubmitNodeCollector):
 * geometry is drawn directly into {@link MultiBufferSource} buffers via {@code putBulkData}, mirroring the
 * item {@code PipeBlockEntityRenderer}.
 */
public class FluidPipeBlockEntityRenderer implements BlockEntityRenderer<FluidPipeBlockEntity> {

    private static final String MODEL_PREFIX = "block/pipe/";

    // Pipe geometry in block units: the core spans 4..12 px (0.25..0.75); arms run from the core face
    // to the block edge. The fluid is inset 0.1 px from the inner walls on all four longitudinal sides.
    private static final float INSET = 0.1F / 16F;
    private static final float CORE_MIN = 4.0F / 16F;
    private static final float CORE_MAX = 12.0F / 16F;
    private static final float A = CORE_MIN + INSET; // inset inner wall (low side)
    private static final float B = CORE_MAX - INSET; // inset inner wall (high side)
    // Minimum fluid thickness/width to draw; below this it reads as a flat plane / hairline, so skip it.
    private static final float MIN_VISIBLE_THICKNESS = 0.4F / 16F;
    // Vertical columns use UVs from their shrinking cross-section; skip before they collapse below one pixel.
    private static final float MIN_VISIBLE_COLUMN_WIDTH = 1.0F / 16F;
    // Floor for the horizontal rising-level thickness when the pipe holds any fluid. A linear level vanishes at
    // a few percent fill (a 20 mB pulse is ~0.6 px), whereas the vertical column stays visible via its
    // area-proportional (sqrt) width — this keeps a small moving pulse readable in horizontal runs too.
    private static final float MIN_FLUID_LEVEL = 1.5F / 16F;
    // Half the inset cross-section — the column's half-width when the pipe is full.
    private static final float MAX_HALF_WIDTH = (B - A) / 2.0F;
    // A pipe with less display fill than this reads as empty (the eased fill decays toward 0 but never hits it).
    private static final float EMPTY_EPSILON = 0.004F;

    // Body geometry never changes at runtime — cache built quads by model id and sprites by texture base.
    private final Map<ResourceId, List<BakedQuad>> codeQuadCache = new HashMap<>();
    private final Map<String, TextureAtlasSprite> spriteCache = new HashMap<>();

    public FluidPipeBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {}

    /** A body model part (core or directional arm) with an optional ARGB tint. */
    private record ModelRenderInfo(ResourceId modelId, int color, @Nullable Direction armDirection) {
        ModelRenderInfo(ResourceId modelId, @Nullable Direction armDirection) {
            this(modelId, 0xFFFFFFFF, armDirection);
        }
    }

    @Override
    public void render(
            FluidPipeBlockEntity entity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {

        FluidPipe def = entity.fluidPipe();
        Level level = entity.getLevel();
        if (level == null) return;

        List<ModelRenderInfo> models = collectModels(entity, def);
        boolean[] connectedArms = new boolean[6];
        for (Direction direction : Direction.values()) {
            connectedArms[direction.get3DDataValue()] = entity.connection(direction) != FluidConnection.NONE;
        }

        VertexConsumer body = bufferSource.getBuffer(RenderType.cutout());
        for (ModelRenderInfo info : models) {
            renderCodeModel(info.modelId(), info.armDirection(), info.color(), poseStack, body, packedLight, packedOverlay);
        }

        renderFluid(entity, connectedArms, poseStack, bufferSource, packedLight, level, partialTick);
    }

    /** Build the ordered list of body parts (core, decorations, arms) for this pipe's current state. */
    private List<ModelRenderInfo> collectModels(FluidPipeBlockEntity entity, @Nullable FluidPipe def) {
        List<ModelRenderInfo> models = new ArrayList<>();
        String base = def != null ? def.modelBase() : "";
        // The merger (output) and extractor (pull) mark their single feature face with the arrow texture,
        // like the item Merger/Extractor pipes.
        Direction feature = def != null && def.usesFeatureFace() ? entity.featureDirection() : null;

        // Hosted modules may override the core/arm model (e.g. weathering) and add core overlays (e.g.
        // markings). Most kinds compose only a transport module, so these return defaults/empty.
        PipeContext ctx = def != null ? entity.createContext() : null;

        ResourceId coreOverride = ctx != null ? def.coreModelOverride(ctx) : null;
        models.add(coreOverride != null
                ? new ModelRenderInfo(coreOverride, null)
                : modelInfo(base + "_core", null));
        if (ctx != null) {
            for (CoreDecoration decoration : def.getCoreDecorations(ctx)) {
                models.add(new ModelRenderInfo(decoration.modelId(), decoration.color(), null));
            }
        }
        for (Direction direction : Direction.values()) {
            if (entity.connection(direction) == FluidConnection.NONE) {
                continue;
            }
            ResourceId armOverride = ctx != null ? def.armModelOverride(ctx, direction) : null;
            if (armOverride != null) {
                models.add(new ModelRenderInfo(armOverride, direction));
            } else {
                String armBase = direction == feature ? base + "_feature" : base + "_arm";
                models.add(modelInfo(armBase, direction));
            }
        }
        return models;
    }

    private static ModelRenderInfo modelInfo(String base, @Nullable Direction armDirection) {
        return new ModelRenderInfo(LogisticsMod.modId(MODEL_PREFIX + base), armDirection);
    }

    /** Build (and cache) the code-generated quads for a fluid pipe part model id. */
    private List<BakedQuad> codeQuads(ResourceId modelId) {
        return codeQuadCache.computeIfAbsent(modelId, id -> {
            String base = id.getPath().substring(MODEL_PREFIX.length());
            List<BakedQuad> out = new ArrayList<>();
            for (PipeModelResolver.Layer layer : PipeModelResolver.resolve(base)) {
                TextureAtlasSprite sprite = sprite(layer.textureBase());
                VanillaQuadBaker baker = new VanillaQuadBaker(sprite, layer.tintIndex(), true, 0);
                PipeGeometry.emit(baker::quad, layer.shape(), sprite);
                out.addAll(baker.toQuads());
            }
            return out;
        });
    }

    private TextureAtlasSprite sprite(String textureBase) {
        return spriteCache.computeIfAbsent(textureBase, b -> Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(LogisticsMod.modId(MODEL_PREFIX + b).toIdentifier()));
    }

    /** Draw code-generated quads, reusing the same rotation + putBulkData path as the item pipe renderer. */
    private void renderCodeModel(ResourceId modelId, @Nullable Direction armDirection, int tintColor,
            PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay) {
        List<BakedQuad> quads = codeQuads(modelId);
        if (quads.isEmpty()) return;

        if (armDirection != null) {
            poseStack.pushPose();
            poseStack.translate(0.5, 0.5, 0.5);
            applyDirectionRotation(poseStack, armDirection);
            poseStack.translate(-0.5, -0.5, -0.5);
        }

        float r = (tintColor >> 16 & 0xFF) / 255.0f;
        float g = (tintColor >> 8 & 0xFF) / 255.0f;
        float b = (tintColor & 0xFF) / 255.0f;
        for (BakedQuad quad : quads) {
            float qr = quad.isTinted() ? r : 1.0f;
            float qg = quad.isTinted() ? g : 1.0f;
            float qb = quad.isTinted() ? b : 1.0f;
            buffer.putBulkData(poseStack.last(), quad, qr, qg, qb, 1.0f, packedLight, packedOverlay);
        }

        if (armDirection != null) {
            poseStack.popPose();
        }
    }

    private void renderFluid(
            FluidPipeBlockEntity entity,
            boolean[] connectedArms,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int light,
            Level level,
            float partialTick) {
        long amount = entity.totalMillibuckets();
        long capacity = entity.capacityMillibuckets();
        float target = amount > 0 && capacity > 0 ? Math.min(1.0F, (float) amount / capacity) : 0.0F;
        // Ease toward the (coarsely synced) target so the fill moves fluidly between steps instead of jittering.
        float nowTicks = level.getGameTime() + partialTick;
        float ratio = entity.advanceDisplayFill(target, nowTicks);
        if (ratio <= EMPTY_EPSILON) {
            return;
        }

        FluidBoxRenderer.Appearance appearance =
                FluidBoxRenderer.resolve(entity.advanceDisplayFluid().getFluid(), level, entity.getBlockPos());
        if (appearance == null) {
            return;
        }
        TextureAtlasSprite sprite = appearance.sprite();
        int color = FluidBoxRenderer.opaque(appearance.tint());
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.translucent());

        float coreBottom = CORE_MIN + INSET;
        float coreTop = CORE_MAX - INSET;
        float floor = INSET;
        float ceiling = 1.0F - INSET;

        boolean down = connectedArms[Direction.DOWN.get3DDataValue()];
        boolean up = connectedArms[Direction.UP.get3DDataValue()];
        boolean hasVertical = down || up;
        boolean hasHorizontal = connectedArms[Direction.NORTH.get3DDataValue()]
                || connectedArms[Direction.SOUTH.get3DDataValue()]
                || connectedArms[Direction.WEST.get3DDataValue()]
                || connectedArms[Direction.EAST.get3DDataValue()];

        // Vertical run: an expanding column whose square cross-section grows from the center toward the
        // walls as it fills (area-proportional, so the cross-section tracks %full). Its vertical extent
        // spans only the connected arms: down-only fills the down arm (floor..coreBottom), up-only fills
        // the up arm (coreBottom..ceiling), both span the whole block.
        if (hasVertical) {
            float halfWidth = FluidColumnGeometry.halfWidth(ratio, MAX_HALF_WIDTH);
            if (halfWidth * 2.0F >= MIN_VISIBLE_COLUMN_WIDTH) {
                float columnBottom = down ? floor : coreBottom;
                float columnTop = up ? ceiling : coreBottom;
                float c0 = 0.5F - halfWidth;
                float c1 = 0.5F + halfWidth;
                FluidBoxRenderer.renderBox(
                        poseStack.last(), buffer, sprite, color, light,
                        c0, columnBottom, c0, c1, columnTop, c1, true, true);
            }
        }

        // Horizontal run (and the isolated-pipe core): a rising level over the core span, floored to a
        // visible thickness so a small pulse reads here the way the sqrt column keeps it visible vertically.
        float surface = Math.max(
                FluidColumnGeometry.surface(ratio, coreBottom, coreTop), coreBottom + MIN_FLUID_LEVEL);
        if (surface - coreBottom >= MIN_VISIBLE_THICKNESS) {
            // Core level-box: bridges horizontal arms through the core / shows a lone pipe's level. Skipped
            // only for a purely vertical run (the narrow column already represents that fluid); any
            // horizontal arm needs the level-box so the arm fluid connects across the core.
            if (hasHorizontal || !hasVertical) {
                FluidBoxRenderer.renderBox(
                        poseStack.last(), buffer, sprite, color, light,
                        A, coreBottom, A, B, surface, B, true, true);
            }
            if (hasHorizontal) {
                renderHorizontalArm(connectedArms, poseStack, buffer, sprite, color, light, Direction.NORTH, coreBottom, surface);
                renderHorizontalArm(connectedArms, poseStack, buffer, sprite, color, light, Direction.SOUTH, coreBottom, surface);
                renderHorizontalArm(connectedArms, poseStack, buffer, sprite, color, light, Direction.WEST, coreBottom, surface);
                renderHorizontalArm(connectedArms, poseStack, buffer, sprite, color, light, Direction.EAST, coreBottom, surface);
            }
        }
    }

    private static void renderHorizontalArm(
            boolean[] connectedArms, PoseStack poseStack, VertexConsumer buffer,
            TextureAtlasSprite sprite, int color, int light, Direction direction, float hBottom, float hTop) {
        if (!connectedArms[direction.get3DDataValue()]) {
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
        FluidBoxRenderer.renderBox(poseStack.last(), buffer, sprite, color, light, x0, hBottom, z0, x1, hTop, z1, true, true);
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
