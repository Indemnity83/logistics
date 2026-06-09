package com.logistics.core.lib.power;

import com.logistics.core.lib.power.AbstractEngineBlockEntity.HeatStage;
import java.util.Set;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Heat-stage tint colors for engine blocks, shared by every loader's engine renderer.
 *
 * <p>Kept loader-agnostic (no client imports) so it can be unit tested.
 *
 * <p><strong>1.21.1 note:</strong> unlike 26.x (which tints the static block model via a
 * {@code BlockTintSource} and re-bakes per heat stage), this branch renders the engine's heat
 * core directly in the block-entity renderer and tints it per-frame from the live block state.
 * The renderer reads {@link #color(BlockState)} every frame, so the color always tracks the
 * current {@link AbstractEngineBlockEntity#STAGE} with no model-group / tint-cache caveats.
 * {@link #RELEVANT_PROPERTIES} is retained for parity with the other branches.
 */
public final class EngineHeatTint {

    private EngineHeatTint() {}

    /** Block-state properties that affect the engine tint; must include STAGE. */
    public static final Set<Property<?>> RELEVANT_PROPERTIES = Set.of(AbstractEngineBlockEntity.STAGE);

    /** Returns the ARGB tint for the given heat stage. */
    public static int color(HeatStage stage) {
        return switch (stage) {
            case COLD -> 0xFF3366CC;
            case COOL -> 0xFF33CC33;
            case WARM -> 0xFFCCCC33;
            case HOT -> 0xFFCC3333;
            case OVERHEAT -> 0xFF191919;
        };
    }

    /** Returns the ARGB tint for an engine block state, read from its {@code STAGE} property. */
    public static int color(BlockState state) {
        return color(state.getValue(AbstractEngineBlockEntity.STAGE));
    }
}
