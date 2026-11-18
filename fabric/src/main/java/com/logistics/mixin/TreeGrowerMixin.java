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
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A germling on Humus grows the Humus tree from {@link HumusTrees} instead of its vanilla feature,
 * and grows alone even when vanilla would only grow it as part of a 2x2 group: this replaces the
 * whole of {@code growTree}, before vanilla looks for the group. Its tree definition
 * is the germling's single-tree one, or its 2x2 one only where it has no other (dark oak).
 */
@Mixin(TreeGrower.class)
public abstract class TreeGrowerMixin {

    @Shadow
    private @Nullable ResourceKey<ConfiguredFeature<?, ?>> getConfiguredMegaFeature(RandomSource random) {
        throw new AssertionError();
    }

    @Shadow
    private @Nullable ResourceKey<ConfiguredFeature<?, ?>> getConfiguredFeature(RandomSource random, boolean hasFlowers) {
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
        ResourceKey<ConfiguredFeature<?, ?>> key = getConfiguredFeature(random, false);
        if (key == null) {
            key = getConfiguredMegaFeature(random);
        }
        ConfiguredFeature<?, ?> definition = key == null
                ? null
                : level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).get(key).map(Holder::value).orElse(null);
        cir.setReturnValue(HumusTrees.grow(level, generator, pos, state, random, definition));
    }
}
