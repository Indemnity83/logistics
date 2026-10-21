package com.logistics.neoforge;

import com.logistics.forestry.humus.HumusTrees;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.BonemealEvent;
import net.neoforged.neoforge.event.level.BlockGrowFeatureEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Routes germlings on Humus to {@link HumusTrees}. The Humus tree is grown at the first growth event
 * a germling fires and the event cancelled, so no vanilla tree follows. Its tree definition is the
 * germling's single-tree one, or else the one the event carries: the 2x2 one where the germling has
 * no other (dark oak, pale oak).
 */
public final class NeoForgeHumusTrees {

    private NeoForgeHumusTrees() {}

    public static void register(IEventBus gameBus) {
        gameBus.addListener(NeoForgeHumusTrees::onGrowFeature);
        gameBus.addListener(NeoForgeHumusTrees::onBonemeal);
    }

    private static void onGrowFeature(BlockGrowFeatureEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !HumusTrees.growsHumusTree(level, event.getPos())) {
            return;
        }
        BlockPos pos = event.getPos();
        BlockState germling = level.getBlockState(pos);
        Holder<Feature> definition = singleTreeDefinition(level, germling, event.getRandom());
        if (definition == null) {
            definition = event.getFeature();
        }
        HumusTrees.grow(
                level,
                level.getChunkSource().getGenerator(),
                pos,
                germling,
                event.getRandom(),
                definition == null ? null : definition.value());
        event.setCanceled(true);
    }

    /** The germling's single-tree definition; {@code null} when it has none or is no sapling. */
    @Nullable
    private static Holder<Feature> singleTreeDefinition(ServerLevel level, BlockState germling, RandomSource random) {
        if (!(germling.getBlock() instanceof SaplingBlock sapling)) {
            return null;
        }
        ResourceKey<Feature> key = sapling.treeGrower.getConfiguredFeature(random, false);
        return key == null
                ? null
                : level.registryAccess().lookupOrThrow(Registries.FEATURE).get(key).orElse(null);
    }

    /**
     * Vanilla refuses bone meal on a lone sapling that only grows as a 2x2 group. On Humus it grows
     * alone, so the bone meal is applied here as vanilla would apply it.
     */
    private static void onBonemeal(BonemealEvent event) {
        BlockPos pos = event.getPos();
        BlockState state = event.getState();
        if (event.isValidBonemealTarget()
                || !(event.getLevel() instanceof ServerLevel level)
                || !(state.getBlock() instanceof BonemealableBlock block)
                || !HumusTrees.growsHumusTree(level, pos)) {
            return;
        }
        if (block.isBonemealSuccess(level, level.getRandom(), pos, state, event.getSource())) {
            block.performBonemeal(level, level.getRandom(), pos, state, event.getSource());
        }
        event.getStack().shrink(1);
        event.setSuccessful(true);
    }
}
