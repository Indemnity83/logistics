package com.logistics.core.lib.client.render;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.LogisticsMod;
import com.mojang.blaze3d.platform.NativeImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MachineModels model cache")
class MachineModelsTest {

    private static final String KEY = "marker_beam";

    /** Rounding slack for the atlas UVs packed into each quad vertex. */
    private static final float EPSILON = 1e-4f;

    /**
     * A sprite stitched at a known atlas position. Re-stitching the block atlas replaces every sprite
     * object and moves the texture, which is what a resource reload does.
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

    /** A cache of its own per call, so one case cannot seed another. */
    private static BlockStateModel model(Map<String, BlockStateModel> cache, String key, TextureAtlasSprite sprite) {
        return MachineModels.cached(cache, key, -1, k -> sprite);
    }

    private static List<BlockModelPart> partsOf(BlockStateModel model) {
        List<BlockModelPart> parts = new ArrayList<>();
        model.collectParts(RandomSource.create(0L), parts);
        return parts;
    }

    /** Every vertex must sample inside {@code sprite}'s atlas region, and name it as its sprite. */
    private static void assertSamples(BlockStateModel model, TextureAtlasSprite sprite) {
        List<BlockModelPart> parts = partsOf(model);
        assertThat(parts).hasSize(1);
        List<BakedQuad> quads = parts.getFirst().getQuads(null);
        assertThat(quads).isNotEmpty();
        for (BakedQuad quad : quads) {
            assertThat(quad.sprite()).isSameAs(sprite);
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
    @DisplayName("a model samples the sprite currently in the atlas, before and after a re-stitch")
    void rebakesAgainstTheCurrentSpriteAfterAReload() {
        Map<String, BlockStateModel> cache = new HashMap<>();

        TextureAtlasSprite before = spriteAt(0, 0);
        assertSamples(model(cache, KEY, before), before);

        TextureAtlasSprite after = spriteAt(32, 32);
        assertSamples(model(cache, KEY, after), after);
    }

    @Test
    @DisplayName("a model is reused while the sprite is unchanged")
    void reusesTheModelWhileTheSpriteIsUnchanged() {
        Map<String, BlockStateModel> cache = new HashMap<>();
        TextureAtlasSprite sprite = spriteAt(16, 0);

        assertThat(model(cache, KEY, sprite)).isSameAs(model(cache, KEY, sprite));
    }

    @Test
    @DisplayName("the particle icon follows the re-stitched sprite too")
    void particleIconTracksTheCurrentSprite() {
        Map<String, BlockStateModel> cache = new HashMap<>();

        TextureAtlasSprite before = spriteAt(0, 0);
        assertThat(model(cache, KEY, before).particleIcon()).isSameAs(before);

        TextureAtlasSprite after = spriteAt(32, 32);
        assertThat(model(cache, KEY, after).particleIcon()).isSameAs(after);
    }

    @Test
    @DisplayName("the tinted cache is validated the same way")
    void tintedModelsAreValidatedToo() {
        Map<String, BlockStateModel> cache = new HashMap<>();

        TextureAtlasSprite before = spriteAt(0, 0);
        BlockStateModel first = MachineModels.cached(cache, KEY, 0, k -> before);
        assertThat(partsOf(first).getFirst().getQuads(null)).allMatch(BakedQuad::isTinted);

        TextureAtlasSprite after = spriteAt(32, 32);
        BlockStateModel rebaked = MachineModels.cached(cache, KEY, 0, k -> after);

        assertThat(rebaked).isNotSameAs(first);
        assertSamples(rebaked, after);
        assertThat(partsOf(rebaked).getFirst().getQuads(null)).allMatch(BakedQuad::isTinted);
    }

    @Test
    @DisplayName("an unknown key yields an empty model")
    void unknownKeyYieldsAnEmptyModel() {
        Map<String, BlockStateModel> cache = new HashMap<>();
        TextureAtlasSprite sprite = spriteAt(0, 16);

        assertThat(partsOf(model(cache, "not_a_machine_model", sprite))).isEmpty();
        assertThat(cache).isEmpty();
    }
}
