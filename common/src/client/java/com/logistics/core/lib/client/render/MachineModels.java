package com.logistics.core.lib.client.render;

import com.logistics.core.lib.client.render.BoxGeometry.Element;
import com.logistics.core.lib.client.render.BoxGeometry.Face;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import com.logistics.LogisticsMod;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

/**
 * Code-generated box geometry for the engine / laser-quarry / marker render parts that
 * formerly loaded as standalone JSON models. Each entry is the model's texture (path under
 * {@code block/}) plus its cuboid elements + per-face UVs, transcribed verbatim from the
 * original model JSONs. Consumed by the block-entity renderers via {@link BoxGeometry}.
 */
public final class MachineModels {
    private MachineModels() {}

    /** A machine model: its texture base (under {@code block/}) and code-generated elements. */
    public record Model(String textureBase, List<Element> elements) {}

    private static final Map<String, Model> MODELS = Map.ofEntries(
        Map.entry("redstone_engine_bellow", new Model("core/redstone_engine", List.of(
            new Element(2f, 0f, 2f, 14f, 8f, 14f, List.of(
                    new Face(Direction.NORTH, 0.5f, 12f, 3.5f, 14f, 0),
                    new Face(Direction.EAST, 0.5f, 12f, 3.5f, 14f, 0),
                    new Face(Direction.SOUTH, 0.5f, 12f, 3.5f, 14f, 0),
                    new Face(Direction.WEST, 0.5f, 12f, 3.5f, 14f, 0)))))),
        Map.entry("redstone_engine_piston", new Model("core/redstone_engine", List.of(
            new Element(0f, 0f, 0f, 16f, 4f, 16f, List.of(
                    new Face(Direction.NORTH, 4f, 14f, 8f, 15f, 0),
                    new Face(Direction.EAST, 0f, 14f, 4f, 15f, 0),
                    new Face(Direction.SOUTH, 12f, 14f, 16f, 15f, 0),
                    new Face(Direction.WEST, 8f, 14f, 12f, 15f, 0),
                    new Face(Direction.UP, 8f, 14f, 4f, 10f, 0),
                    new Face(Direction.DOWN, 12f, 10f, 8f, 14f, 0)))))),
        Map.entry("stirling_engine_bellow", new Model("power/stirling_engine", List.of(
            new Element(2f, 0f, 2f, 14f, 8f, 14f, List.of(
                    new Face(Direction.NORTH, 0.5f, 12f, 3.5f, 14f, 0),
                    new Face(Direction.EAST, 0.5f, 12f, 3.5f, 14f, 0),
                    new Face(Direction.SOUTH, 0.5f, 12f, 3.5f, 14f, 0),
                    new Face(Direction.WEST, 0.5f, 12f, 3.5f, 14f, 0)))))),
        Map.entry("fuel_engine_bellow", new Model("power/fuel_engine", List.of(
            new Element(2f, 0f, 2f, 14f, 8f, 14f, List.of(
                    new Face(Direction.NORTH, 0.5f, 12f, 3.5f, 14f, 0),
                    new Face(Direction.EAST, 0.5f, 12f, 3.5f, 14f, 0),
                    new Face(Direction.SOUTH, 0.5f, 12f, 3.5f, 14f, 0),
                    new Face(Direction.WEST, 0.5f, 12f, 3.5f, 14f, 0)))))),
        Map.entry("stirling_engine_piston", new Model("power/stirling_engine", List.of(
            new Element(0f, 0f, 0f, 16f, 4f, 16f, List.of(
                    new Face(Direction.NORTH, 4f, 14f, 8f, 15f, 0),
                    new Face(Direction.EAST, 0f, 14f, 4f, 15f, 0),
                    new Face(Direction.SOUTH, 12f, 14f, 16f, 15f, 0),
                    new Face(Direction.WEST, 8f, 14f, 12f, 15f, 0),
                    new Face(Direction.UP, 8f, 14f, 4f, 10f, 0),
                    new Face(Direction.DOWN, 12f, 10f, 8f, 14f, 0)))))),
        Map.entry("steam_engine_bellow", new Model("power/steam_engine", List.of(
            new Element(2f, 0f, 2f, 14f, 8f, 14f, List.of(
                    new Face(Direction.NORTH, 0.5f, 12f, 3.5f, 14f, 0),
                    new Face(Direction.EAST, 0.5f, 12f, 3.5f, 14f, 0),
                    new Face(Direction.SOUTH, 0.5f, 12f, 3.5f, 14f, 0),
                    new Face(Direction.WEST, 0.5f, 12f, 3.5f, 14f, 0)))))),
        Map.entry("steam_engine_piston", new Model("power/steam_engine", List.of(
            new Element(0f, 0f, 0f, 16f, 4f, 16f, List.of(
                    new Face(Direction.NORTH, 4f, 14f, 8f, 15f, 0),
                    new Face(Direction.EAST, 0f, 14f, 4f, 15f, 0),
                    new Face(Direction.SOUTH, 12f, 14f, 16f, 15f, 0),
                    new Face(Direction.WEST, 8f, 14f, 12f, 15f, 0),
                    new Face(Direction.UP, 8f, 14f, 4f, 10f, 0),
                    new Face(Direction.DOWN, 12f, 10f, 8f, 14f, 0)))))),
        Map.entry("fuel_engine_piston", new Model("power/fuel_engine", List.of(
            new Element(0f, 0f, 0f, 16f, 4f, 16f, List.of(
                    new Face(Direction.NORTH, 4f, 14f, 8f, 15f, 0),
                    new Face(Direction.EAST, 0f, 14f, 4f, 15f, 0),
                    new Face(Direction.SOUTH, 12f, 14f, 16f, 15f, 0),
                    new Face(Direction.WEST, 8f, 14f, 12f, 15f, 0),
                    new Face(Direction.UP, 8f, 14f, 4f, 10f, 0),
                    new Face(Direction.DOWN, 12f, 10f, 8f, 14f, 0)))))),
        Map.entry("creative_engine_bellow", new Model("power/creative_engine", List.of(
            new Element(2f, 0f, 2f, 14f, 8f, 14f, List.of(
                    new Face(Direction.NORTH, 0.5f, 12f, 3.5f, 14f, 0),
                    new Face(Direction.EAST, 0.5f, 12f, 3.5f, 14f, 0),
                    new Face(Direction.SOUTH, 0.5f, 12f, 3.5f, 14f, 0),
                    new Face(Direction.WEST, 0.5f, 12f, 3.5f, 14f, 0)))))),
        Map.entry("creative_engine_piston", new Model("power/creative_engine", List.of(
            new Element(0f, 0f, 0f, 16f, 4f, 16f, List.of(
                    new Face(Direction.NORTH, 4f, 14f, 8f, 15f, 0),
                    new Face(Direction.EAST, 0f, 14f, 4f, 15f, 0),
                    new Face(Direction.SOUTH, 12f, 14f, 16f, 15f, 0),
                    new Face(Direction.WEST, 8f, 14f, 12f, 15f, 0),
                    new Face(Direction.UP, 8f, 14f, 4f, 10f, 0),
                    new Face(Direction.DOWN, 12f, 10f, 8f, 14f, 0)))))),
        Map.entry("redstone_engine_core", new Model("power/redstone_engine", List.of(
            new Element(4f, 4f, 4f, 12f, 16f, 12f, List.of(
                    new Face(Direction.NORTH, 10f, 7f, 12f, 10f, 0),
                    new Face(Direction.EAST, 8f, 7f, 10f, 10f, 0),
                    new Face(Direction.SOUTH, 14f, 7f, 16f, 10f, 0),
                    new Face(Direction.WEST, 12f, 7f, 14f, 10f, 0),
                    new Face(Direction.UP, 12f, 7f, 10f, 5f, 0),
                    new Face(Direction.DOWN, 14f, 5f, 12f, 7f, 0)))))),
        Map.entry("stirling_engine_core", new Model("power/stirling_engine", List.of(
            new Element(4f, 4f, 4f, 12f, 16f, 12f, List.of(
                    new Face(Direction.NORTH, 10f, 7f, 12f, 10f, 0),
                    new Face(Direction.EAST, 8f, 7f, 10f, 10f, 0),
                    new Face(Direction.SOUTH, 14f, 7f, 16f, 10f, 0),
                    new Face(Direction.WEST, 12f, 7f, 14f, 10f, 0),
                    new Face(Direction.UP, 12f, 7f, 10f, 5f, 0),
                    new Face(Direction.DOWN, 14f, 5f, 12f, 7f, 0)))))),
        Map.entry("steam_engine_core", new Model("power/steam_engine", List.of(
            new Element(4f, 4f, 4f, 12f, 16f, 12f, List.of(
                    new Face(Direction.NORTH, 10f, 7f, 12f, 10f, 0),
                    new Face(Direction.EAST, 8f, 7f, 10f, 10f, 0),
                    new Face(Direction.SOUTH, 14f, 7f, 16f, 10f, 0),
                    new Face(Direction.WEST, 12f, 7f, 14f, 10f, 0),
                    new Face(Direction.UP, 12f, 7f, 10f, 5f, 0),
                    new Face(Direction.DOWN, 14f, 5f, 12f, 7f, 0)))))),
        Map.entry("creative_engine_core", new Model("power/creative_engine", List.of(
            new Element(4f, 4f, 4f, 12f, 16f, 12f, List.of(
                    new Face(Direction.NORTH, 10f, 7f, 12f, 10f, 0),
                    new Face(Direction.EAST, 8f, 7f, 10f, 10f, 0),
                    new Face(Direction.SOUTH, 14f, 7f, 16f, 10f, 0),
                    new Face(Direction.WEST, 12f, 7f, 14f, 10f, 0),
                    new Face(Direction.UP, 12f, 7f, 10f, 5f, 0),
                    new Face(Direction.DOWN, 14f, 5f, 12f, 7f, 0)))))),
        Map.entry("fuel_engine_core", new Model("power/fuel_engine", List.of(
            new Element(4f, 4f, 4f, 12f, 16f, 12f, List.of(
                    new Face(Direction.NORTH, 10f, 7f, 12f, 10f, 0),
                    new Face(Direction.EAST, 8f, 7f, 10f, 10f, 0),
                    new Face(Direction.SOUTH, 14f, 7f, 16f, 10f, 0),
                    new Face(Direction.WEST, 12f, 7f, 14f, 10f, 0),
                    new Face(Direction.UP, 12f, 7f, 10f, 5f, 0),
                    new Face(Direction.DOWN, 14f, 5f, 12f, 7f, 0)))))),
        Map.entry("magmatic_engine_bellow", new Model("power/magmatic_engine", List.of(
            new Element(2f, 0f, 2f, 14f, 8f, 14f, List.of(
                    new Face(Direction.NORTH, 0.5f, 12f, 3.5f, 14f, 0),
                    new Face(Direction.EAST, 0.5f, 12f, 3.5f, 14f, 0),
                    new Face(Direction.SOUTH, 0.5f, 12f, 3.5f, 14f, 0),
                    new Face(Direction.WEST, 0.5f, 12f, 3.5f, 14f, 0)))))),
        Map.entry("magmatic_engine_piston", new Model("power/magmatic_engine", List.of(
            new Element(0f, 0f, 0f, 16f, 4f, 16f, List.of(
                    new Face(Direction.NORTH, 4f, 14f, 8f, 15f, 0),
                    new Face(Direction.EAST, 0f, 14f, 4f, 15f, 0),
                    new Face(Direction.SOUTH, 12f, 14f, 16f, 15f, 0),
                    new Face(Direction.WEST, 8f, 14f, 12f, 15f, 0),
                    new Face(Direction.UP, 8f, 14f, 4f, 10f, 0),
                    new Face(Direction.DOWN, 12f, 10f, 8f, 14f, 0)))))),
        Map.entry("magmatic_engine_core", new Model("power/magmatic_engine", List.of(
            new Element(4f, 4f, 4f, 12f, 16f, 12f, List.of(
                    new Face(Direction.NORTH, 10f, 7f, 12f, 10f, 0),
                    new Face(Direction.EAST, 8f, 7f, 10f, 10f, 0),
                    new Face(Direction.SOUTH, 14f, 7f, 16f, 10f, 0),
                    new Face(Direction.WEST, 12f, 7f, 14f, 10f, 0),
                    new Face(Direction.UP, 12f, 7f, 10f, 5f, 0),
                    new Face(Direction.DOWN, 14f, 5f, 12f, 7f, 0)))))),
        Map.entry("marker_beam", new Model("core/marker_beam", List.of(
            new Element(7.1f, 8.1f, 0f, 8.9f, 9.9f, 16f, List.of(
                    new Face(Direction.NORTH, 0f, 4f, 2f, 12f, 0),
                    new Face(Direction.EAST, 0f, 0f, 16f, 8f, 0),
                    new Face(Direction.SOUTH, 14f, 4f, 16f, 12f, 0),
                    new Face(Direction.WEST, 0f, 8f, 16f, 16f, 0),
                    new Face(Direction.UP, 16f, 16f, 0f, 8f, 90),
                    new Face(Direction.DOWN, 16f, 16f, 0f, 8f, 90)))))),
        Map.entry("construction_beam", new Model("core/construction_beam", List.of(
            new Element(7.1f, 8.1f, 0f, 8.9f, 9.9f, 16f, List.of(
                    new Face(Direction.NORTH, 0f, 4f, 2f, 12f, 0),
                    new Face(Direction.EAST, 0f, 0f, 16f, 8f, 0),
                    new Face(Direction.SOUTH, 14f, 4f, 16f, 12f, 0),
                    new Face(Direction.WEST, 0f, 8f, 16f, 16f, 0),
                    new Face(Direction.UP, 16f, 16f, 0f, 8f, 90),
                    new Face(Direction.DOWN, 16f, 16f, 0f, 8f, 90)))))),
        Map.entry("fluid_pump_tube", new Model("fluid/fluid_pump_pipe", List.of(
            new Element(4f, 0f, 4f, 12f, 16f, 12f, List.of(
                    new Face(Direction.NORTH, 0f, 0f, 8f, 16f, 0),
                    new Face(Direction.EAST, 0f, 0f, 8f, 16f, 0),
                    new Face(Direction.SOUTH, 0f, 0f, 8f, 16f, 0),
                    new Face(Direction.WEST, 0f, 0f, 8f, 16f, 0),
                    new Face(Direction.DOWN, 8f, 8f, 16f, 16f, 0)))))),
        Map.entry("laser_quarry_gantry_arm", new Model("automation/quary_gantry_arm", List.of(
            new Element(5f, 5f, 0f, 11f, 11f, 16f, List.of(
                    new Face(Direction.NORTH, 0f, 1f, 1f, 2f, 0),
                    new Face(Direction.EAST, 0f, 0f, 16f, 6f, 0),
                    new Face(Direction.SOUTH, 0f, 1f, 1f, 2f, 0),
                    new Face(Direction.WEST, 0f, 0f, 16f, 6f, 0),
                    new Face(Direction.UP, 16f, 0f, 0f, 6f, 90),
                    new Face(Direction.DOWN, 0f, 6f, 16f, 0f, 90))),
            new Element(5.5f, 5.5f, 0.01f, 10.5f, 10.5f, 15.99f, List.of(
                    new Face(Direction.NORTH, 0f, 6f, 5f, 11f, 0),
                    new Face(Direction.EAST, 0f, 6f, 16f, 11f, 0),
                    new Face(Direction.SOUTH, 11f, 6f, 16f, 11f, 0),
                    new Face(Direction.WEST, 0f, 6f, 16f, 11f, 0),
                    new Face(Direction.UP, 0f, 6f, 16f, 11f, 90),
                    new Face(Direction.DOWN, 0f, 6f, 16f, 11f, 90)))))),
        Map.entry("laser_quarry_drill", new Model("automation/quary_drill", List.of(
            new Element(7f, 2f, 7f, 9f, 16f, 9f, List.of(
                    new Face(Direction.NORTH, 4f, 2f, 6f, 16f, 0),
                    new Face(Direction.EAST, 2f, 2f, 4f, 16f, 0),
                    new Face(Direction.SOUTH, 4f, 2f, 6f, 16f, 0),
                    new Face(Direction.WEST, 2f, 2f, 4f, 16f, 0),
                    new Face(Direction.UP, 2f, 2f, 4f, 4f, 0),
                    new Face(Direction.DOWN, 14f, 5f, 16f, 7f, 0))))))
    );

    public static Model get(String key) {
        return MODELS.get(key);
    }

    private static final Map<String, BlockStateModel> MODEL_CACHE = new HashMap<>();
    private static final Map<String, BlockStateModel> TINTED_MODEL_CACHE = new HashMap<>();

    /**
     * Build (and cache) the code-generated model for a machine model key, ready for
     * {@code SubmitNodeCollector.submitBlockModel}. On 1.21.11 {@code submitBlockModel} takes a
     * model (not a part list), so the procedurally baked parts are wrapped in a
     * {@link BlockStateModel}. Geometry is static, so models are cached by key (assumes no
     * mid-session resource reload, matching the pipe/cable renderers). Returns an empty model
     * for an unknown key.
     */
    public static BlockStateModel model(String key) {
        return MODEL_CACHE.computeIfAbsent(key, k -> build(k, -1));
    }

    /**
     * Like {@link #model(String)} but bakes the quads with tint index 0, so the per-call
     * {@code r,g,b} passed to {@code submitBlockModel} actually tints them (the submit path only
     * tints quads that carry a tint index). Used for the engine heat core.
     */
    public static BlockStateModel tintedModel(String key) {
        return TINTED_MODEL_CACHE.computeIfAbsent(key, k -> build(k, 0));
    }

    private static BlockStateModel build(String key, int tintIndex) {
        Model model = MODELS.get(key);
        if (model == null) {
            return new ProceduralModel(List.of(), null);
        }
        TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager()
                .getAtlasOrThrow(AtlasIds.BLOCKS)
                .getSprite(LogisticsMod.modId("block/" + model.textureBase()).toIdentifier());
        VanillaQuadBaker baker = new VanillaQuadBaker(sprite, tintIndex, true, 0);
        BoxGeometry.emit(baker::quad, model.elements(), sprite);
        List<BlockModelPart> parts = baker.isEmpty() ? List.of() : List.of(baker.toPart());
        return new ProceduralModel(parts, sprite);
    }

    /**
     * Minimal {@link BlockStateModel} wrapping pre-built parts; {@code collectParts} is
     * deterministic and ignores the random source.
     */
    private record ProceduralModel(List<BlockModelPart> parts, @Nullable TextureAtlasSprite sprite)
            implements BlockStateModel {
        @Override
        public void collectParts(RandomSource random, List<BlockModelPart> out) {
            out.addAll(parts);
        }

        @Override
        public TextureAtlasSprite particleIcon() {
            return sprite;
        }
    }
}
