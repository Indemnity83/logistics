package com.logistics.power.engine.reaction;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Property-math for the Reaction Engine profile and recipe accessors. Pure — no Minecraft bootstrap
 * (reactant/catalyst recognition needs the registry and is covered by the game test).
 */
class ReactionEnginePropertiesTest {

    // Config defaults: 200 RF/t, 500-tick reaction, 4,000 mB tank, 250 mB batch.
    private static final ReactionEngineProfile PROFILE = ReactionEngineProfile.of(200, 500, 4_000, 250);

    @Test
    void totalEnergyPerReactionIsOutputTimesDuration() {
        assertThat(PROFILE.totalEnergyPerReaction()).isEqualTo(100_000L); // 200 * 500
    }

    @Test
    void profileExposesConfigValues() {
        assertThat(PROFILE.outputPerTick()).isEqualTo(200L);
        assertThat(PROFILE.reactionDurationTicks()).isEqualTo(500);
        assertThat(PROFILE.reactantTankCapacityMb()).isEqualTo(4_000);
        assertThat(PROFILE.batchMb()).isEqualTo(250);
    }

    @Test
    void recipeDefersToProfileWithoutOverrides() {
        ReactionRecipe r = new ReactionRecipe(null, null, null, null);
        assertThat(r.outputPerTick(PROFILE)).isEqualTo(200L);
        assertThat(r.durationTicks(PROFILE)).isEqualTo(500);
    }

    @Test
    void recipeOverridesWinOverProfile() {
        ReactionRecipe harder = new ReactionRecipe(null, null, 500L, null);
        assertThat(harder.outputPerTick(PROFILE)).isEqualTo(500L);
        assertThat(harder.durationTicks(PROFILE)).isEqualTo(500); // duration still defaults

        ReactionRecipe longer = new ReactionRecipe(null, null, null, 2_000);
        assertThat(longer.outputPerTick(PROFILE)).isEqualTo(200L); // output still defaults
        assertThat(longer.durationTicks(PROFILE)).isEqualTo(2_000);
    }
}
