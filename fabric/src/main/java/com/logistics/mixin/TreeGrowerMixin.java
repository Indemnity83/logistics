package com.logistics.mixin;

import com.logistics.forestry.humus.HumusTrees;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A germling on Humus grows the Humus tree from {@link HumusTrees} instead of its vanilla feature,
 * and grows alone even when vanilla would only grow it as part of a 2x2 group. Its tree definition
 * is the one vanilla would have picked: the 2x2 one where there is one, as dark oak has no other.
 */
@Mixin(TreeGrower.class)
public abstract class TreeGrowerMixin {

    @Shadow
    private @Nullable ResourceKey<Feature> getConfiguredMegaFeature(RandomSource random) {
        throw new AssertionError();
    }

    @Shadow
    private @Nullable ResourceKey<Feature> getConfiguredFeature(RandomSource random, boolean hasFlowers) {
        throw new AssertionError();
    }

    @Inject(method = "growTree", at = @At("HEAD"), cancellable = true)
    private void logistics$growHumusTree(
            ServerLevel level,
            ChunkGenerator generator,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            CallbackInfoReturnable<Boolean> cir) {
        if (!HumusTrees.growsHumusTree(level, pos)) {
            return;
        }
        ResourceKey<Feature> key = getConfiguredMegaFeature(random);
        if (key == null) {
            key = getConfiguredFeature(random, false);
        }
        Feature definition = key == null
                ? null
                : level.registryAccess().lookupOrThrow(Registries.FEATURE).get(key).map(Holder::value).orElse(null);
        cir.setReturnValue(HumusTrees.grow(level, generator, pos, state, random, definition));
    }

    @Inject(method = "canGrow", at = @At("HEAD"), cancellable = true)
    private void logistics$humusTreeGrowsAlone(
            ServerLevel level, BlockPos pos, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (HumusTrees.growsHumusTree(level, pos)) {
            cir.setReturnValue(true);
        }
    }
}
