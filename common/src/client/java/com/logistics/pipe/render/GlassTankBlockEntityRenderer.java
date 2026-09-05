package com.logistics.pipe.render;

import com.logistics.core.lib.client.render.FluidBoxRenderer;
import com.logistics.core.lib.tank.TankCellLookup;
import com.logistics.pipe.block.entity.GlassTankBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.Level;

/**
 * Renders the fluid inside a glass tank: the fluid's own still sprite/tint (via {@link FluidBoxRenderer})
 * filled to the cell's level. When a connected tank above holds fluid, the surface is hidden and the
 * fluid reaches the block top so a vertical stack reads as one continuous column.
 *
 * <p>MC 1.21.1 uses the classic {@link BlockEntityRenderer} API (no render-state / SubmitNodeCollector):
 * the fluid box is drawn directly into a {@code translucent} buffer.
 */
public class GlassTankBlockEntityRenderer implements BlockEntityRenderer<GlassTankBlockEntity> {

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
    public void render(
            GlassTankBlockEntity entity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {
        long amount = entity.tank().getAmount();
        long capacity = entity.tank().getCapacity();
        Level level = entity.getLevel();
        boolean hasFluid = amount > 0 && capacity > 0 && !entity.tank().isEmpty() && level != null;
        if (!hasFluid) {
            return;
        }
        float fillRatio = Math.min(1.0F, (float) amount / capacity);
        if (fillRatio <= 0.0F) {
            return;
        }
        // A gas settles against the ceiling (FluidColumn.settle fills top-down for it), so its free
        // surface is the bottom face and its continuation neighbour is below, not above.
        boolean gas = TankCellLookup.isGas(entity.tank().getFluidKey());
        boolean renderSurface = !(gas ? entity.hasFluidBelow() : entity.hasFluidAbove());

        FluidBoxRenderer.Appearance appearance =
                FluidBoxRenderer.resolve(entity.tank().getFluidKey().getFluid(), level, entity.getBlockPos());
        if (appearance == null) {
            return;
        }
        TextureAtlasSprite sprite = appearance.sprite();
        int color = FluidBoxRenderer.opaque(appearance.tint());

        // Surface tracks the fill, capped a hair below the block top to avoid z-fighting; if fluid continues
        // above, fill to the full block top (seamless stack) and hide the surface.
        // Keep the surface at/above the floor so a tiny fill never inverts the box's Y bounds.
        float surface = Math.max(FLOOR, Math.min(fillRatio, CEILING));
        float y0 = gas ? (renderSurface ? 1.0F - surface : 0.0F) : FLOOR;
        float y1 = gas ? CEILING : (renderSurface ? surface : 1.0F);
        boolean top = !gas && renderSurface;
        boolean bottom = gas && renderSurface;
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.translucent());
        FluidBoxRenderer.renderBox(
                poseStack.last(), buffer, sprite, color, packedLight, MIN, y0, MIN, MAX, y1, MAX, top, bottom);
    }
}
