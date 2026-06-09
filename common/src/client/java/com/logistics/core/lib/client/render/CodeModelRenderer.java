package com.logistics.core.lib.client.render;

import java.util.List;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.LightTexture;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Draws code-generated {@link BakedQuad}s (from {@link MachineModels} / {@link PipeGeometry} via
 * {@link VanillaQuadBaker}) directly through a {@link VertexConsumer} on MC 1.21.1's classic
 * immediate-mode block-entity render path — the loader-agnostic equivalent of the newer branches'
 * {@code SubmitNodeCollector.submitBlockModel}.
 *
 * <p>Quads carrying a tint index receive {@code (r,g,b)}; untinted quads render white.
 */
public final class CodeModelRenderer {
    private CodeModelRenderer() {}

    /** Draw {@code quads} untinted (white) at full block-light convenience overload. */
    public static void draw(List<BakedQuad> quads, PoseStack poseStack, VertexConsumer buffer,
            int packedLight, int packedOverlay) {
        draw(quads, 0xFFFFFF, poseStack, buffer, packedLight, packedOverlay);
    }

    /** Draw {@code quads}, applying {@code tintColor} (0xRRGGBB) to tint-indexed quads only. */
    public static void draw(List<BakedQuad> quads, int tintColor, PoseStack poseStack, VertexConsumer buffer,
            int packedLight, int packedOverlay) {
        float tr = (tintColor >> 16 & 0xFF) / 255.0f;
        float tg = (tintColor >> 8 & 0xFF) / 255.0f;
        float tb = (tintColor & 0xFF) / 255.0f;
        for (BakedQuad quad : quads) {
            float r = quad.isTinted() ? tr : 1.0f;
            float g = quad.isTinted() ? tg : 1.0f;
            float b = quad.isTinted() ? tb : 1.0f;
            buffer.putBulkData(poseStack.last(), quad, r, g, b, 1.0f, packedLight, packedOverlay);
        }
    }

    /** Full-bright light value, for emissive parts (beams) that should ignore world lighting. */
    public static int fullBright() {
        return LightTexture.pack(15, 15);
    }

    public static int noOverlay() {
        return OverlayTexture.NO_OVERLAY;
    }
}
