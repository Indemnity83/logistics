package com.logistics.pipe.render.model;

import com.logistics.core.lib.pipe.PipeShape;
import java.util.List;

/**
 * Maps a pipe part-model id (the base name the pipe/module selection logic produces, e.g.
 * {@code copper_transport_pipe_arm_extended}) to the code-rendered layers that replace its
 * former JSON model: a {@link PipeShape}, the texture base name, and a tint index.
 *
 * <p>This is the single place the (otherwise JSON-only) model→texture mapping now lives. The
 * texture rules were verified to reproduce all 86 concrete pipe model JSONs exactly:
 * <ul>
 *   <li>arm and arm_extended share one texture (geometry differs, texture does not) — drop {@code _extended};</li>
 *   <li>{@code item_*} pipes drop the {@code item_} prefix ({@code passthrough}→{@code passthru});</li>
 *   <li>{@code *_transport_pipe} → {@code *_pipe};</li>
 *   <li>logistics arms/features all use the shared {@code logistics_pipe_arm} texture; logistics cores keep their own name.</li>
 * </ul>
 * The two filter arms additionally carry a tinted {@code filter_pipe_overlay} layer.
 */
public final class PipeModelResolver {
    private PipeModelResolver() {}

    /** A single code-rendered layer: shape, texture base (under {@code block/pipe/}), and tint index (-1 = untinted). */
    public record Layer(PipeShape shape, String textureBase, int tintIndex) {}

    public static List<Layer> resolve(String modelBase) {
        if (modelBase.equals("pipe_markings")) {
            return List.of(new Layer(PipeShape.MARKINGS, "pipe_markings", 0));
        }
        PipeShape shape = shapeOf(modelBase);
        String texture = textureOf(modelBase);
        if (modelBase.equals("item_filter_pipe_arm") || modelBase.equals("item_filter_pipe_arm_extended")) {
            // Base arm (untinted) + directional, tinted filter band overlay.
            return List.of(
                    new Layer(shape, texture, -1),
                    new Layer(shape, "filter_pipe_overlay", 0));
        }
        if (texture.equals("logistics_pipe_arm")) {
            // Untinted base (dark edge pixels) + tinted overlay (the lighter center pixels) so only
            // the bright region carries the power-status tint (green/red) from
            // NetworkRouterModule#getArmTint; the dark edges stay neutral.
            return List.of(
                    new Layer(shape, texture, -1),
                    new Layer(shape, "logistics_pipe_arm_overlay", 0));
        }
        return List.of(new Layer(shape, texture, -1));
    }

    private static PipeShape shapeOf(String base) {
        if (base.contains("_arm_extended") || base.contains("_feature_extended")) {
            return PipeShape.ARM_EXTENDED;
        }
        if (base.contains("_arm") || base.contains("_feature")) {
            return PipeShape.ARM;
        }
        return PipeShape.CORE;
    }

    private static String textureOf(String base) {
        String t = base.replace("_arm_extended", "_arm").replace("_feature_extended", "_feature");
        if (t.startsWith("item_")) {
            return t.substring("item_".length()).replace("passthrough", "passthru");
        }
        if (t.contains("_transport_pipe")) {
            return t.replace("_transport_pipe", "_pipe");
        }
        if (t.contains("_logistics_pipe")) {
            return (t.endsWith("_arm") || t.endsWith("_feature")) ? "logistics_pipe_arm" : t;
        }
        return t;
    }
}
