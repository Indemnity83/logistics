package com.logistics.pipe.client;

import com.logistics.core.lib.client.render.FluidBoxRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jetbrains.annotations.Nullable;

/**
 * 26.2 item-render adapter for the hidden fluid-packet item. This is the version-specific entry point only —
 * it wires the vanilla {@link SpecialModelRenderer} {@code submit} path to the shared, version-agnostic
 * {@link FluidPacketRendering} core (fluid lookup, sprite/tint resolution, geometry constants). Registered
 * under {@link FluidPacketRendering#ID} on both loaders via {@code SpecialModelRenderers.ID_MAPPER} (Fabric)
 * / {@code RegisterSpecialModelRendererEvent} (NeoForge). Other branches supply their own adapter (classic
 * {@code render(...)} on 26.1/1.21.11, a BEWLR on 1.21.1) over the same core.
 */
public final class FluidPacketSpecialRenderer implements SpecialModelRenderer<Fluid> {
    private static final float FRAME_Z = FluidPacketRendering.FRAME_Z;
    private static final float FLUID_Z = FluidPacketRendering.FLUID_Z;

    private final TextureAtlasSprite frame;

    private FluidPacketSpecialRenderer(TextureAtlasSprite frame) {
        this.frame = frame;
    }

    @Override
    @Nullable
    public Fluid extractArgument(ItemStack stack) {
        return FluidPacketRendering.fluidOf(stack);
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
        collector.submitCustomGeometry(
                poseStack,
                RenderTypes.itemCutout(TextureAtlas.LOCATION_ITEMS),
                (entry, buffer) -> FluidBoxRenderer.renderFaceQuad(
                        entry, buffer, frame, 0xFFFFFFFF, lightCoords, 0.0F, 0.0F, 1.0F, 1.0F, FRAME_Z));
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        for (float x = 0.0F; x <= 1.0F; x += 1.0F) {
            for (float y = 0.0F; y <= 1.0F; y += 1.0F) {
                output.accept(new Vector3f(x, y, FLUID_Z));
                output.accept(new Vector3f(x, y, FRAME_Z));
            }
        }
    }

    /** Zero-data unbaked form; resolves the frame sprite from the item atlas at bake time. */
    public record Unbaked() implements SpecialModelRenderer.Unbaked<Fluid> {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(Unbaked::new);

        @Override
        public SpecialModelRenderer<Fluid> bake(SpecialModelRenderer.BakingContext context) {
            TextureAtlasSprite frame = context.sprites()
                    .get(new SpriteId(TextureAtlas.LOCATION_ITEMS, FluidPacketRendering.FRAME_TEXTURE.toIdentifier()));
            return new FluidPacketSpecialRenderer(frame);
        }

        @Override
        public MapCodec<? extends SpecialModelRenderer.Unbaked<Fluid>> type() {
            return MAP_CODEC;
        }
    }
}
