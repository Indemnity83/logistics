package com.logistics.power.engine.reaction;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.Test;

/**
 * Client-safe behavior of the datapack recipe lookups: with no {@code RecipeManager} (the client has none),
 * every query reports "no match" instead of throwing, so the engine's filters stay permissive on the client
 * rather than crashing. Positive recognition of the launch recipe needs loaded datapacks and is covered by
 * {@code ReactionEngineGameTest}.
 */
class ReactionEngineReactionsTest extends MinecraftTestEnvironment {

    @Test
    void findWithoutRecipeManagerIsNoMatch() {
        assertThat(ReactionEngineReactions.find((RecipeManager) null, Fluids.LAVA, new ItemStack(Items.ECHO_SHARD)))
                .isNull();
    }

    @Test
    void isReactantWithoutRecipeManagerIsFalse() {
        assertThat(ReactionEngineReactions.isReactant(null, Fluids.LAVA)).isFalse();
    }

    @Test
    void isCatalystWithoutRecipeManagerIsFalse() {
        assertThat(ReactionEngineReactions.isCatalyst(null, new ItemStack(Items.ECHO_SHARD))).isFalse();
    }
}
