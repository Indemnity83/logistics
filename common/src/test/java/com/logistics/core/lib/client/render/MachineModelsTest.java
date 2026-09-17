package com.logistics.core.lib.client.render;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.core.lib.resource.ResourceId;
import com.mojang.blaze3d.platform.NativeImage;
import java.util.List;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.server.packs.resources.ResourceMetadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MachineModels quad cache")
class MachineModelsTest {

    private static final String KEY = "marker_beam";

    /** Rounding slack for the atlas UVs packed into each quad vertex. */
    private static final float EPSILON = 1e-4f;

    /** Vanilla's block vertex format: 8 ints per vertex, with u at offset 4 and v at offset 5. */
    private static final int INTS_PER_VERTEX = 8;

    private static final int U_OFFSET = 4;
    private static final int V_OFFSET = 5;

    /**
     * A sprite stitched at a known atlas position. Re-stitching the block atlas replaces every sprite
     * object and moves the texture, which is what a resource reload does.
     */
    private static TextureAtlasSprite spriteAt(int x, int y) {
        SpriteContents contents = new SpriteContents(
                ResourceId.in("logistics", "block/core/marker_beam").toIdentifier(),
                new FrameSize(16, 16),
                new NativeImage(16, 16, false),
                ResourceMetadata.EMPTY);
        return new StitchedSprite(contents, x, y);
    }

    private static final class StitchedSprite extends TextureAtlasSprite {
        StitchedSprite(SpriteContents contents, int x, int y) {
            super(ResourceId.in("logistics", "textures/atlas/blocks").toIdentifier(), contents, 64, 64, x, y);
        }
    }

    /** Every vertex must sample inside {@code sprite}'s atlas region, and name it as its sprite. */
    private static void assertSamples(List<BakedQuad> quads, TextureAtlasSprite sprite) {
        assertThat(quads).isNotEmpty();
        for (BakedQuad quad : quads) {
            assertThat(quad.getSprite()).isSameAs(sprite);

            int[] vertices = quad.getVertices();
            // Pin the stride the offsets below assume, so a format change fails here rather than
            // silently reading the wrong ints.
            assertThat(vertices.length).isEqualTo(4 * INTS_PER_VERTEX);

            for (int vertex = 0; vertex < 4; vertex++) {
                float u = Float.intBitsToFloat(vertices[vertex * INTS_PER_VERTEX + U_OFFSET]);
                float v = Float.intBitsToFloat(vertices[vertex * INTS_PER_VERTEX + V_OFFSET]);
                assertThat(u)
                        .isGreaterThanOrEqualTo(sprite.getU0() - EPSILON)
                        .isLessThanOrEqualTo(sprite.getU1() + EPSILON);
                assertThat(v)
                        .isGreaterThanOrEqualTo(sprite.getV0() - EPSILON)
                        .isLessThanOrEqualTo(sprite.getV1() + EPSILON);
            }
        }
    }

    @Test
    @DisplayName("quads sample the sprite currently in the atlas, before and after a re-stitch")
    void rebakesAgainstTheCurrentSpriteAfterAReload() {
        TextureAtlasSprite before = spriteAt(0, 0);
        assertSamples(MachineModels.quads(KEY, key -> before), before);

        TextureAtlasSprite after = spriteAt(32, 32);
        assertSamples(MachineModels.quads(KEY, key -> after), after);
    }

    @Test
    @DisplayName("quads are reused while the sprite is unchanged")
    void reusesQuadsWhileTheSpriteIsUnchanged() {
        TextureAtlasSprite sprite = spriteAt(16, 0);

        assertThat(MachineModels.quads(KEY, key -> sprite)).isSameAs(MachineModels.quads(KEY, key -> sprite));
    }

    @Test
    @DisplayName("an unknown key yields no quads")
    void unknownKeyYieldsNoQuads() {
        TextureAtlasSprite sprite = spriteAt(0, 16);

        assertThat(MachineModels.quads("not_a_machine_model", key -> sprite)).isEmpty();
    }
}
