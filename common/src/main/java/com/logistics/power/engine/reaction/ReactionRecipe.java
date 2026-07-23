package com.logistics.power.engine.reaction;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

/**
 * A single Reaction Engine reaction — a first-class object (not a boolean) so future reactions can vary
 * independently without redesigning the simulation API. It <b>owns</b> its output rate and duration via
 * accessors that default to the {@link ReactionEngineProfile} today; per-recipe overrides (and, later,
 * tint colour / sound) live here so the simulation always asks the recipe, never the profile directly.
 *
 * @param reactant         the liquid consumed (250 mB per reaction)
 * @param catalyst         the solid consumed (1 per reaction)
 * @param outputOverride   RF/t for this reaction, or null to use the profile default
 * @param durationOverride burn ticks for this reaction, or null to use the profile default
 */
public record ReactionRecipe(
        Fluid reactant,
        Item catalyst,
        @Nullable Long outputOverride,
        @Nullable Integer durationOverride) {

    /** RF/t this reaction generates while running. */
    public long outputPerTick(ReactionEngineProfile profile) {
        return outputOverride != null ? outputOverride : profile.outputPerTick();
    }

    /** How many ticks this reaction runs once committed. */
    public int durationTicks(ReactionEngineProfile profile) {
        return durationOverride != null ? durationOverride : profile.reactionDurationTicks();
    }
}
