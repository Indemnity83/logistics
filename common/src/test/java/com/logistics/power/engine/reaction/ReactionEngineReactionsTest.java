package com.logistics.power.engine.reaction;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.Test;

/**
 * Registry-backed recognition for the Reaction Engine's launch recipe. The catalyst side (a vanilla item)
 * is fully testable here; reactant recognition needs the mod fluid, which is only registered in the game
 * environment, so it is covered by {@code ReactionEngineGameTest}.
 */
class ReactionEngineReactionsTest extends MinecraftTestEnvironment {

    @Test
    void echoShardIsTheLaunchCatalyst() {
        assertThat(ReactionEngineReactions.isCatalyst(new ItemStack(Items.ECHO_SHARD))).isTrue();
    }

    @Test
    void nonCatalystItemsAreRejected() {
        assertThat(ReactionEngineReactions.isCatalyst(new ItemStack(Items.DIRT))).isFalse();
        assertThat(ReactionEngineReactions.isCatalyst(new ItemStack(Items.COAL))).isFalse();
        assertThat(ReactionEngineReactions.isCatalyst(ItemStack.EMPTY)).isFalse();
    }

    @Test
    void lookupRejectsANonReactantEvenWithAValidCatalyst() {
        assertThat(ReactionEngineReactions.lookup(Fluids.WATER, new ItemStack(Items.ECHO_SHARD))).isNull();
        assertThat(ReactionEngineReactions.lookup(Fluids.EMPTY, new ItemStack(Items.ECHO_SHARD))).isNull();
    }

    @Test
    void waterIsNotARecognizedReactant() {
        assertThat(ReactionEngineReactions.isReactant(Fluids.WATER)).isFalse();
    }
}
