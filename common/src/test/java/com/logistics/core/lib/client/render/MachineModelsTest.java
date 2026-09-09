package com.logistics.core.lib.client.render;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.LogisticsMod;
import com.mojang.blaze3d.platform.NativeImage;
import java.util.List;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MachineModels part cache")
class MachineModelsTest {

    private static final String KEY = "marker_beam";

    /** Rounding slack for the atlas UVs packed into each quad vertex. */
    private static final float EPSILON = 1e-4f;

    /**
     * A sprite stitched at a known atlas position. Re-stitching the block atlas replaces every
     * sprite object and moves the texture, which is what a resource reload does.
     */
    private static TextureAtlasSprite spriteAt(int x, int y) {
        SpriteContents contents = new SpriteContents(
                LogisticsMod.modId("block/core/marker_beam").toIdentifier(),
                new FrameSize(16, 16),
                new NativeImage(16, 16, false));
        return new StitchedSprite(contents, x, y);
    }

    private static final class StitchedSprite extends TextureAtlasSprite {
        StitchedSprite(SpriteContents contents, int x, int y) {
            super(LogisticsMod.modId("textures/atlas/blocks").toIdentifier(), contents, 64, 64, x, y, 0);
        }
    }

    /** Every vertex must sample inside {@code sprite}'s atlas region, and name it as its material. */
    private static void assertSamples(List<BlockStateModelPart> parts, TextureAtlasSprite sprite) {
        assertThat(parts).hasSize(1);
        List<BakedQuad> quads = parts.getFirst().getQuads(null);
        assertThat(quads).isNotEmpty();
        for (BakedQuad quad : quads) {
            assertThat(quad.materialInfo().sprite()).isSameAs(sprite);
            for (int vertex = 0; vertex < BakedQuad.VERTEX_COUNT; vertex++) {
                long uv = quad.packedUV(vertex);
                assertThat(UVPair.unpackU(uv))
                        .isGreaterThanOrEqualTo(sprite.getU0() - EPSILON)
                        .isLessThanOrEqualTo(sprite.getU1() + EPSILON);
                assertThat(UVPair.unpackV(uv))
                        .isGreaterThanOrEqualTo(sprite.getV0() - EPSILON)
                        .isLessThanOrEqualTo(sprite.getV1() + EPSILON);
            }
        }
    }

    @Test
    @DisplayName("parts sample the sprite currently in the atlas, before and after a re-stitch")
    void rebakesAgainstTheCurrentSpriteAfterAReload() {
        TextureAtlasSprite before = spriteAt(0, 0);
        assertSamples(MachineModels.parts(KEY, key -> before), before);

        TextureAtlasSprite after = spriteAt(32, 32);
        assertSamples(MachineModels.parts(KEY, key -> after), after);
    }

    @Test
    @DisplayName("parts are reused while the sprite is unchanged")
    void reusesPartsWhileTheSpriteIsUnchanged() {
        TextureAtlasSprite sprite = spriteAt(16, 0);
        assertThat(MachineModels.parts(KEY, key -> sprite)).isSameAs(MachineModels.parts(KEY, key -> sprite));
    }

    @Test
    @DisplayName("an unknown key yields no parts")
    void unknownKeyYieldsNoParts() {
        TextureAtlasSprite sprite = spriteAt(0, 16);
        assertThat(MachineModels.parts("not_a_machine_model", key -> sprite)).isEmpty();
    }
}
