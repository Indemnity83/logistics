package com.logistics.core.lib.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

/**
 * Shared helpers for drawing a fluid's still sprite as a box, used by both the fluid pipe and tank
 * renderers. On MC 26.1 the fluid sprite and tint are resolved through the unified
 * {@link FluidModel} set, so this is fully loader-agnostic and lives in shared client code.
 */
public final class FluidBoxRenderer {

    private FluidBoxRenderer() {}

    /** The still sprite and ARGB tint for a fluid; {@code tint} is opaque white when untinted. */
    public record Appearance(TextureAtlasSprite sprite, int tint) {}

    /** Resolve a fluid's still sprite and world tint, or {@code null} if it has no model. */
    @Nullable
    public static Appearance resolve(Fluid fluid, @Nullable Level level, BlockPos pos) {
        FluidState fluidState = fluid.defaultFluidState();
        FluidModel model = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluidState);
        TextureAtlasSprite sprite = model.stillMaterial().sprite();
        if (sprite == null) {
            return null;
        }
        var tintSource = model.tintSource();
        int tint = tintSource != null && level instanceof BlockAndTintGetter tintGetter
                ? tintSource.colorInWorld(fluidState.createLegacyBlock(), tintGetter, pos)
                : 0xFFFFFFFF;
        return new Appearance(sprite, tint);
    }

    /**
     * Resolve a fluid's still sprite and a world-independent tint for GUI rendering. Uses the tint
     * source's default {@link net.minecraft.client.color.block.BlockTintSource#color color} (biome-less),
     * so grayscale fluids like water get their base colour while our baked fluids stay opaque white.
     */
    @Nullable
    public static Appearance resolveForGui(Fluid fluid) {
        FluidState fluidState = fluid.defaultFluidState();
        FluidModel model = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluidState);
        TextureAtlasSprite sprite = model.stillMaterial().sprite();
        if (sprite == null) {
            return null;
        }
        var tintSource = model.tintSource();
        int tint = tintSource != null ? opaque(tintSource.color(fluidState.createLegacyBlock())) : 0xFFFFFFFF;
        return new Appearance(sprite, tint);
    }

    /** Forces an ARGB colour opaque (some tint sources omit alpha). */
    public static int opaque(int color) {
        return (color & 0xFF000000) == 0 ? color | 0xFF000000 : color;
    }

    /**
     * Render a fluid box spanning {@code [x0,x1] × [y0,y1] × [z0,z1]} using {@code sprite}. The four walls
     * are always drawn; the top (+Y) face only when {@code renderTop} (omit it when fluid continues above,
     * so a stacked column reads as one surface) and the bottom (-Y) face only when {@code renderBottom}
     * (a tank sitting on a surface omits it; a free-floating box in a pipe keeps it).
     */
    public static void renderBox(
            PoseStack.Pose entry,
            VertexConsumer buffer,
            TextureAtlasSprite sprite,
            int color,
            int light,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            boolean renderTop,
            boolean renderBottom) {
        if (renderTop) {
            quad(entry, buffer, sprite, color, light, 0, 1, 0, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
        }
        if (renderBottom) {
            quad(entry, buffer, sprite, color, light, 0, -1, 0, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1);
        }
        quad(entry, buffer, sprite, color, light, 0, 0, -1, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0);
        quad(entry, buffer, sprite, color, light, 0, 0, 1, x1, y0, z1, x0, y0, z1, x0, y1, z1, x1, y1, z1);
        quad(entry, buffer, sprite, color, light, -1, 0, 0, x0, y0, z1, x0, y0, z0, x0, y1, z0, x0, y1, z1);
        quad(entry, buffer, sprite, color, light, 1, 0, 0, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0);
    }

    /**
     * Render a single flat quad on the {@code -Z} (north) plane at depth {@code z}, spanning
     * {@code [x0,x1] × [y0,y1]} — a fluid gauge sitting flush on a block face (front + back, so it shows
     * from both sides), with the same position-based UV as {@link #renderBox}. Rotate the matrix to place
     * it on another face.
     */
    public static void renderFaceQuad(
            PoseStack.Pose entry, VertexConsumer buffer, TextureAtlasSprite sprite, int color, int light,
            float x0, float y0, float x1, float y1, float z) {
        quad(entry, buffer, sprite, color, light, 0, 0, -1, x0, y0, z, x1, y0, z, x1, y1, z, x0, y1, z);
    }

    private static void quad(
            PoseStack.Pose entry, VertexConsumer buffer, TextureAtlasSprite sprite, int color, int light,
            float nx, float ny, float nz,
            float x1, float y1, float z1, float x2, float y2, float z2,
            float x3, float y3, float z3, float x4, float y4, float z4) {
        // Front face.
        vertex(entry, buffer, sprite, color, light, nx, ny, nz, x1, y1, z1);
        vertex(entry, buffer, sprite, color, light, nx, ny, nz, x2, y2, z2);
        vertex(entry, buffer, sprite, color, light, nx, ny, nz, x3, y3, z3);
        vertex(entry, buffer, sprite, color, light, nx, ny, nz, x4, y4, z4);
        // Back face (reverse winding, flipped normal) so it shows from both sides regardless of culling.
        vertex(entry, buffer, sprite, color, light, -nx, -ny, -nz, x4, y4, z4);
        vertex(entry, buffer, sprite, color, light, -nx, -ny, -nz, x3, y3, z3);
        vertex(entry, buffer, sprite, color, light, -nx, -ny, -nz, x2, y2, z2);
        vertex(entry, buffer, sprite, color, light, -nx, -ny, -nz, x1, y1, z1);
    }

    /**
     * Emits one vertex, deriving its UV from its block-local position rather than stretching the whole sprite
     * across the face. The two in-plane axes (chosen from the face normal) index the sprite at a fixed texel
     * density — 1 block = one full tile — so pixels stay square and the texture tiles across larger surfaces
     * instead of stretching. Coordinates are block-local in {@code [0,1]}; {@code y} is flipped so the still
     * sprite reads upright (top of sprite at the top of the box).
     */
    private static void vertex(
            PoseStack.Pose entry, VertexConsumer buffer, TextureAtlasSprite sprite, int color, int light,
            float nx, float ny, float nz, float x, float y, float z) {
        float uFrac;
        float vFrac;
        if (ny != 0) { // top/bottom face: in-plane axes are x and z
            uFrac = x;
            vFrac = z;
        } else if (nz != 0) { // north/south face: x across, y up
            uFrac = x;
            vFrac = 1.0F - y;
        } else { // east/west face: z across, y up
            uFrac = z;
            vFrac = 1.0F - y;
        }
        float u = sprite.getU0() + uFrac * (sprite.getU1() - sprite.getU0());
        float v = sprite.getV0() + vFrac * (sprite.getV1() - sprite.getV0());
        buffer.addVertex(entry, x, y, z)
                .setColor(color)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(entry, nx, ny, nz);
    }
}
