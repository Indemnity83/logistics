package com.logistics.neoforge;

import com.logistics.forestry.humus.HumusTrees;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.BonemealEvent;
import net.neoforged.neoforge.event.level.BlockGrowFeatureEvent;

/**
 * Routes germlings on Humus to {@link HumusTrees}. The first growth event a germling fires carries
 * the tree definition vanilla picked, the 2x2 one where there is one, as dark oak has no other; the
 * Humus tree is grown from it and the event cancelled, so no vanilla tree follows.
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
        Holder<Feature> definition = event.getFeature();
        HumusTrees.grow(
                level,
                level.getChunkSource().getGenerator(),
                pos,
                level.getBlockState(pos),
                event.getRandom(),
                definition == null ? null : definition.value());
        event.setCanceled(true);
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
