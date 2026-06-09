package com.logistics.core.lib.power;

import com.logistics.core.lib.power.AbstractEngineBlockEntity.HeatStage;
import java.util.Set;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Heat-stage tint colors for engine blocks, shared by every loader's block tint source.
 *
 * <p>Kept loader-agnostic (no client imports) so it can be unit tested. The client-side
 * {@code BlockTintSource} adapters delegate here for both the color and the set of relevant
 * properties.
 *
 * <p><strong>Why {@link #RELEVANT_PROPERTIES} matters:</strong> in 26.1 a block's tint is baked
 * once per "model group", and two block states share a group unless a property the tint source
 * declares as relevant differs between them ({@code ModelGroupCollector} keys groups on
 * {@code BlockColors.getColoringProperties}). The engine model is identical across heat stages, so
 * unless {@link AbstractEngineBlockEntity#STAGE} is declared relevant, all stages collapse into one
 * cached tint and the heat color never updates in-world.
 */
public final class EngineHeatTint {

    private EngineHeatTint() {}

    /** Block-state properties that affect the engine tint; must include STAGE (see class docs). */
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
