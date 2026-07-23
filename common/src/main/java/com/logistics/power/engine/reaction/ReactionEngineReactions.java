package com.logistics.power.engine.reaction;

import com.logistics.LogisticsCore;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

/**
 * The Reaction Engine's valid (liquid reactant, solid catalyst) pairs. Reactants are keyed by fluid
 * <em>registry-id string</em> — id-keyed, not {@code Fluid}-keyed, so the static table doesn't cache the
 * mod's custom fluids (which register after this class loads); the {@link Fluid} is resolved to its id only
 * at lookup time. Launch pair: Liquid Ender + Echo Shard. Future pairs are one line each here.
 */
public final class ReactionEngineReactions {

    private record Entry(String reactantId, Item catalyst, @Nullable Long outputOverride, @Nullable Integer durationOverride) {}

    private static final List<Entry> RECIPES = List.of(
            new Entry(LogisticsCore.resource("liquid_ender").toString(), Items.ECHO_SHARD, null, null));

    private ReactionEngineReactions() {}

    /** Resolve the reaction for a (reactant, catalyst) pair, or null if the pair isn't a valid recipe. */
    @Nullable
    public static ReactionRecipe lookup(@Nullable Fluid reactant, ItemStack catalyst) {
        if (reactant == null || catalyst.isEmpty()) {
            return null;
        }
        String id = fluidId(reactant);
        if (id == null) {
            return null;
        }
        for (Entry e : RECIPES) {
            if (e.reactantId.equals(id) && catalyst.is(e.catalyst)) {
                return new ReactionRecipe(reactant, e.catalyst, e.outputOverride, e.durationOverride);
            }
        }
        return null;
    }

    /** Whether a fluid participates in any recipe — the tank insert filter. */
    public static boolean isReactant(@Nullable Fluid fluid) {
        if (fluid == null) {
            return false;
        }
        String id = fluidId(fluid);
        return id != null && RECIPES.stream().anyMatch(e -> e.reactantId.equals(id));
    }

    /** Whether an item participates in any recipe — the catalyst-slot filter. */
    public static boolean isCatalyst(ItemStack stack) {
        return !stack.isEmpty() && RECIPES.stream().anyMatch(e -> stack.is(e.catalyst));
    }

    @Nullable
    private static String fluidId(Fluid fluid) {
        var key = BuiltInRegistries.FLUID.getKey(fluid);
        return key == null ? null : key.toString();
    }
}
