package com.logistics.pipe.client;

import com.logistics.core.lib.client.render.FluidBoxRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jetbrains.annotations.Nullable;

/**
 * 26.2 item-render adapter for the hidden fluid-packet item. Wires the vanilla {@link SpecialModelRenderer}
 * {@code submit} path to the shared, version-agnostic {@link FluidPacketRendering} core. Draws the carried
 * fluid's still sprite (16px, animated) as a flat quad behind the frame texture (a bubble ring with a
 * transparent center window).
 *
 * <p>Both sprites are resolved at RENDER time (in {@link #submit}) via the {@code AtlasManager}, never at
 * bake time — special-model bake runs before atlases are initialized, so a bake-time
 * {@code context.sprites().get(...)} throws "Atlas not initialized". The frame is stitched onto the blocks
 * atlas via {@code assets/logistics/atlases/blocks.json} and looked up the same way
 * {@link com.logistics.pipe.render.PipeBlockEntityRenderer} resolves its sprites.
 */
public final class FluidPacketSpecialRenderer implements SpecialModelRenderer<Fluid> {
    private static final float FRAME_Z = FluidPacketRendering.FRAME_Z;
    private static final float FRAME_BACK_Z = FluidPacketRendering.FRAME_BACK_Z;
    private static final float FLUID_Z = FluidPacketRendering.FLUID_Z;

    private FluidPacketSpecialRenderer() {}

    @Override
    @Nullable
    public Fluid extractArgument(ItemStack stack) {
        return FluidPacketRendering.fluidOf(stack);
    }

    /** The frame sprite off the ITEMS atlas — resolved lazily at render, when atlases are initialized. */
    private static TextureAtlasSprite frameSprite() {
        return Minecraft.getInstance().getAtlasManager()
                .getAtlasOrThrow(AtlasIds.ITEMS)
                .getSprite(FluidPacketRendering.FRAME_TEXTURE.toIdentifier());
    }

    @Override
    public void submit(
            @Nullable Fluid fluid,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int lightCoords,
            int overlayCoords,
            boolean hasFoil,
            int outlineColor) {
        if (fluid != null) {
            FluidBoxRenderer.Appearance appearance = FluidPacketRendering.fluidAppearance(fluid);
            if (appearance != null) {
                TextureAtlasSprite sprite = appearance.sprite();
                int color = FluidBoxRenderer.opaque(appearance.tint());
                collector.submitCustomGeometry(
                        poseStack,
                        RenderTypes.translucentMovingBlock(),
                        (entry, buffer) -> FluidBoxRenderer.renderFaceQuad(
                                entry, buffer, sprite, color, lightCoords, 0.0F, 0.0F, 1.0F, 1.0F, FLUID_Z));
            }
        }
        TextureAtlasSprite frame = frameSprite();
        collector.submitCustomGeometry(
                poseStack,
                RenderTypes.itemCutout(TextureAtlas.LOCATION_ITEMS),
                (entry, buffer) -> {
                    // A frame plane in front of and behind the fluid, so it frames from either viewing side.
                    FluidBoxRenderer.renderFaceQuad(
                            entry, buffer, frame, 0xFFFFFFFF, lightCoords, 0.0F, 0.0F, 1.0F, 1.0F, FRAME_Z);
                    FluidBoxRenderer.renderFaceQuad(
                            entry, buffer, frame, 0xFFFFFFFF, lightCoords, 0.0F, 0.0F, 1.0F, 1.0F, FRAME_BACK_Z);
                });
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        for (float x = 0.0F; x <= 1.0F; x += 1.0F) {
            for (float y = 0.0F; y <= 1.0F; y += 1.0F) {
                output.accept(new Vector3f(x, y, FRAME_BACK_Z));
                output.accept(new Vector3f(x, y, FRAME_Z));
            }
        }
    }

    /** Zero-data unbaked form; the renderer resolves its sprites at render time, so bake does no atlas work. */
    public record Unbaked() implements SpecialModelRenderer.Unbaked<Fluid> {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(Unbaked::new);

        @Override
        public SpecialModelRenderer<Fluid> bake(SpecialModelRenderer.BakingContext context) {
            return new FluidPacketSpecialRenderer();
        }

        @Override
        public MapCodec<? extends SpecialModelRenderer.Unbaked<Fluid>> type() {
            return MAP_CODEC;
        }
    }
}
