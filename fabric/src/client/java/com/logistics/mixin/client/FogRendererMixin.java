package com.logistics.mixin.client;

import com.logistics.core.fluid.CrudeOilSubmersion;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Darkens and shortens fog while the camera is submerged in Crude Oil, mirroring {@code LavaFogEnvironment}. */
@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
    // Anchors, not final — tune by playtest.
    private static final float CRUDE_OIL_FOG_START = 0.25f;
    private static final float CRUDE_OIL_FOG_END = 1.0f;

    @Inject(method = "setupFog", at = @At("RETURN"))
    private void logistics$crudeOilFog(
            Camera camera, int renderDistance, DeltaTracker deltaTracker, float partialTick, ClientLevel level,
            CallbackInfoReturnable<FogData> cir) {
        if (!CrudeOilSubmersion.isCameraSubmerged(level, camera.blockPosition(), camera.position().y)) {
            return;
        }
        FogData fogData = cir.getReturnValue();
        fogData.environmentalStart = CRUDE_OIL_FOG_START;
        fogData.environmentalEnd = CRUDE_OIL_FOG_END;
        fogData.skyEnd = fogData.environmentalEnd;
        fogData.cloudEnd = fogData.environmentalEnd;
        fogData.color.set(0.03f, 0.02f, 0.015f, 1.0f);
    }
}
