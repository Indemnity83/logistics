package com.logistics.mixin;

import com.logistics.core.lib.farmland.FarmlandHydration;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.FarmlandBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Farmland that finds no water nearby asks the {@link FarmlandHydration} sources holding it.
 * {@code isNearWater}'s only caller is {@code randomTick}, so a source is asked once per random tick.
 */
@Mixin(FarmlandBlock.class)
public abstract class FarmlandBlockMixin {

    @Inject(method = "isNearWater", at = @At("RETURN"), cancellable = true)
    private static void logistics$hydrationSources(LevelReader level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && level instanceof ServerLevel serverLevel && FarmlandHydration.hydrate(serverLevel, pos)) {
            cir.setReturnValue(true);
        }
    }
}
