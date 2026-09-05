package com.logistics.pipe.render;

import com.logistics.core.lib.client.render.FluidBoxRenderer;
import com.logistics.pipe.block.entity.GlassTankBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import com.logistics.core.lib.tank.TankCellLookup;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Renders the fluid inside a glass tank: the fluid's own still sprite/tint (via {@link FluidBoxRenderer})
 * filled to the cell's level. When a connected tank above holds fluid, the surface is hidden and the
 * fluid reaches the block top so a vertical stack reads as one continuous column.
 */
public class GlassTankBlockEntityRenderer
        implements BlockEntityRenderer<GlassTankBlockEntity, GlassTankRenderState> {

    // Walls inset to 2.5..13.5 px (inside the glass), fluid sitting near the block floor and rising to near
    // the full block height when full.
    private static final float MIN = 2.5F / 16F;
    private static final float MAX = 13.5F / 16F;
    // Pull the exposed top/bottom a hair inside the block to avoid z-fighting with the block boundary.
    private static final float EDGE_INSET = 0.01F;
    private static final float FLOOR = EDGE_INSET;
    private static final float CEILING = 1.0F - EDGE_INSET;

    public GlassTankBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public GlassTankRenderState createRenderState() {
        return new GlassTankRenderState();
    }

    @Override
    public void extractRenderState(
            GlassTankBlockEntity entity,
            GlassTankRenderState state,
            float tickDelta,
            Vec3 cameraPos,
            ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderState.extractBase(entity, state, crumblingOverlay);

        long amount = entity.tank().getAmount();
        long capacity = entity.tank().getCapacity();
        Level level = entity.getLevel();
        state.hasFluid = amount > 0 && capacity > 0 && !entity.tank().isEmpty() && level != null;
        state.fillRatio = state.hasFluid ? Math.min(1.0F, (float) amount / capacity) : 0.0F;
        // A gas settles against the ceiling (FluidColumn.settle fills top-down for it), so its free
        // surface is the bottom face and its continuation neighbour is below, not above.
        state.gas = TankCellLookup.isGas(entity.tank().getFluidKey());
        state.renderSurface =
                state.hasFluid && !(state.gas ? entity.hasFluidBelow() : entity.hasFluidAbove());
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

    @Override
    public void submit(
            GlassTankRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState camera) {
        if (!state.hasFluid || state.sprite == null || state.fillRatio <= 0.0F) {
            return;
        }
        TextureAtlasSprite sprite = state.sprite;
        int color = FluidBoxRenderer.opaque(state.tintColor);
        int light = state.lightCoords;
        // The surface tracks the fill, inset a hair from the block face to avoid z-fighting; where the
        // fluid continues into the neighbouring tank the box runs to the full block edge instead and the
        // surface is hidden, so a stack reads as one column. Clamped so a tiny fill never inverts the
        // box's Y bounds.
        float surface = Math.max(FLOOR, Math.min(state.fillRatio, CEILING));
        boolean renderSurface = state.renderSurface;
        // Gas hangs from the ceiling: the box grows downward and the free surface is its bottom face.
        float y0 = state.gas ? (renderSurface ? 1.0F - surface : 0.0F) : FLOOR;
        float y1 = state.gas ? CEILING : (renderSurface ? surface : 1.0F);
        boolean top = !state.gas && renderSurface;
        boolean bottom = state.gas && renderSurface;
        queue.submitCustomGeometry(
                matrices,
                RenderTypes.translucentMovingBlock(),
                (entry, buffer) -> FluidBoxRenderer.renderBox(
                        entry, buffer, sprite, color, light, MIN, y0, MIN, MAX, y1, MAX, top, bottom));
    }
}
