package com.logistics.neoforge.client.render;

import com.logistics.power.cable.CableTier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.data.AtlasIds;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.neoforged.neoforge.client.model.block.CustomBlockModelDefinition;

public record NeoForgeCableBlockModelDefinition(CableTier tier) implements CustomBlockModelDefinition {
    public static final MapCodec<NeoForgeCableBlockModelDefinition> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(tierCodec().fieldOf("tier").forGetter(NeoForgeCableBlockModelDefinition::tier))
                    .apply(instance, NeoForgeCableBlockModelDefinition::new));

    @Override
    public Map<BlockState, BlockStateModel.UnbakedRoot> instantiate(
            StateDefinition<Block, BlockState> states,
            Supplier<String> sourceSupplier) {
        NeoForgeCableUnbakedRoot root = new NeoForgeCableUnbakedRoot(tier);
        Map<BlockState, BlockStateModel.UnbakedRoot> models = new HashMap<>();
        for (BlockState state : states.getPossibleStates()) {
            models.put(state, root);
        }
        return models;
    }

    @Override
    public MapCodec<? extends CustomBlockModelDefinition> codec() {
        return CODEC;
    }

    private static Codec<CableTier> tierCodec() {
        return Codec.STRING.xmap(NeoForgeCableBlockModelDefinition::tierFromId, CableTier::id);
    }

    private static CableTier tierFromId(String id) {
        for (CableTier tier : CableTier.values()) {
            if (tier.id().equals(id) || tier.name().toLowerCase(Locale.ROOT).equals(id)) {
                return tier;
            }
        }
        throw new IllegalArgumentException("Unknown cable tier: " + id);
    }

    private static final class NeoForgeCableUnbakedRoot implements BlockStateModel.UnbakedRoot {
        private final NeoForgeCableModel model;
        private final Object equalityGroup = new Object();

        private NeoForgeCableUnbakedRoot(CableTier tier) {
            this.model = new NeoForgeCableModel(tier);
        }

        @Override
        public BlockStateModel bake(BlockState state, ModelBaker baker) {
            return model;
        }

        @Override
        public Object visualEqualityGroup(BlockState state) {
            return equalityGroup;
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            resolver.markDependency(AtlasIds.BLOCKS);
        }
    }
}
