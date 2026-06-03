package com.logistics.power.render;

import com.logistics.core.lib.client.render.VanillaQuadBaker;
import com.logistics.power.cable.CableBlock;
import com.logistics.power.cable.CableBlockEntity;
import com.logistics.power.cable.CableTier;
import com.logistics.power.render.model.CableGeometry;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.EnumMap;
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
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * Renders cables fully in code via {@link CableGeometry} and {@link VanillaQuadBaker},
 * using only vanilla rendering APIs (no Fabric FRAPI / model-loading). Shared by both
 * loaders. The visible geometry is dynamic (driven by the neighbor-derived connection
 * mask), so it is drawn per-frame as a block entity rather than baked into the chunk mesh.
 *
 * <p>Geometry depends only on (tier, connectionMask), so built parts are cached by that key.
 * Sprites are cached per tier. Both caches assume no mid-session resource reload (matching
 * {@code PipeBlockEntityRenderer}); a reconnect rebuilds them.
 */
public class CableBlockEntityRenderer implements BlockEntityRenderer<CableBlockEntity, CableRenderState> {

    private final Map<Long, List<BlockStateModelPart>> partsCache = new HashMap<>();
    private final Map<CableTier, TextureAtlasSprite> spriteCache = new EnumMap<>(CableTier.class);

    public CableBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public CableRenderState createRenderState() {
        return new CableRenderState();
    }

    @Override
    public void extractRenderState(
            CableBlockEntity entity,
            CableRenderState state,
            float tickDelta,
            Vec3 cameraPos,
            ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderState.extractBase(entity, state, crumblingOverlay);
        CableTier tier = entity.getBlockState().getBlock() instanceof CableBlock cable
                ? cable.tier()
                : CableTier.COPPER;
        state.parts = partsFor(tier, entity.getRenderConnectionMask());
    }

    @Override
    public void submit(
            CableRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraState) {
        if (state.parts == null || state.parts.isEmpty()) {
            return;
        }
        queue.submitBlockModel(
                matrices,
                RenderTypes.cutoutMovingBlock(),
                state.parts,
                new int[]{0xFFFFFFFF},
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                0);
    }

    private List<BlockStateModelPart> partsFor(CableTier tier, int connectionMask) {
        long key = ((long) tier.ordinal() << 32) | (connectionMask & 0xFFFFFFFFL);
        return partsCache.computeIfAbsent(key, ignored -> build(tier, connectionMask));
    }

    private List<BlockStateModelPart> build(CableTier tier, int connectionMask) {
        TextureAtlasSprite sprite = sprite(tier);
        VanillaQuadBaker baker = new VanillaQuadBaker(sprite, -1, true, 0);
        CableGeometry.emit(baker::quad, connectionMask, CableGeometry.TextureUvs.fromSprite(sprite), direction -> true);
        return baker.isEmpty() ? List.of() : List.of(baker.toPart());
    }

    private TextureAtlasSprite sprite(CableTier tier) {
        return spriteCache.computeIfAbsent(tier, t -> Minecraft.getInstance().getAtlasManager()
                .getAtlasOrThrow(AtlasIds.BLOCKS)
                .getSprite(Identifier.fromNamespaceAndPath("logistics", "block/power/" + t.id())));
    }
}
