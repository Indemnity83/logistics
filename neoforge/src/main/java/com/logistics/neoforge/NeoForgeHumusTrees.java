package com.logistics.neoforge;

import com.logistics.forestry.humus.HumusTrees;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.level.BlockGrowFeatureEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Routes germlings on Humus to {@link HumusTrees}. The Humus tree is grown at the first growth event
 * a germling fires and the event cancelled, so no vanilla tree follows. Its tree definition is the
 * germling's single-tree one, or else the one the event carries: the 2x2 one where the germling has
 * no other (dark oak). Bone meal needs no handler: vanilla accepts it on any sapling
 * below the build limit, and a lone 2x2-only sapling reaches this event through its 2x2 definition.
 */
public final class NeoForgeHumusTrees {

    private NeoForgeHumusTrees() {}

    public static void register(IEventBus gameBus) {
        gameBus.addListener(NeoForgeHumusTrees::onGrowFeature);
    }

    private static void onGrowFeature(BlockGrowFeatureEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !HumusTrees.growsHumusTree(level, event.getPos())) {
            return;
        }
        BlockPos pos = event.getPos();
        BlockState germling = level.getBlockState(pos);
        Holder<ConfiguredFeature<?, ?>> definition = singleTreeDefinition(level, germling, event.getRandom());
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
    private static Holder<ConfiguredFeature<?, ?>> singleTreeDefinition(ServerLevel level, BlockState germling, RandomSource random) {
        if (!(germling.getBlock() instanceof SaplingBlock sapling)) {
            return null;
        }
        ResourceKey<ConfiguredFeature<?, ?>> key = sapling.treeGrower.getConfiguredFeature(random, false);
        return key == null
                ? null
                : level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).get(key).orElse(null);
    }
}
