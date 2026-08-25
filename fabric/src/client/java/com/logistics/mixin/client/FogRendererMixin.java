package com.logistics.mixin.client;

import com.logistics.core.fluid.CrudeOilSubmersion;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Darkens and shortens fog while the camera is submerged in Crude Oil, mirroring
 * {@code LavaFogEnvironment}. Fog color and distance are two separate vanilla code paths on this
 * version ({@code computeFogColor} returns the color directly; {@code FogData} carries distances
 * only, with no color field), so each needs its own hook.
 */
@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
    // Anchors, not final — tune by playtest.
    private static final float CRUDE_OIL_FOG_START = 0.25f;
    private static final float CRUDE_OIL_FOG_END = 1.0f;
    private static final Vector4f CRUDE_OIL_FOG_COLOR = new Vector4f(0.03f, 0.02f, 0.015f, 1.0f);

    @Inject(method = "computeFogColor", at = @At("RETURN"), cancellable = true)
    private void logistics$crudeOilFogColor(
            Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount,
            CallbackInfoReturnable<Vector4f> cir) {
        if (CrudeOilSubmersion.isCameraSubmerged(level, camera.blockPosition(), camera.position().y)) {
            cir.setReturnValue(CRUDE_OIL_FOG_COLOR);
        }
    }

    @Redirect(
            method = "setupFog",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/fog/environment/FogEnvironment;"
                            + "setupFog(Lnet/minecraft/client/renderer/fog/FogData;Lnet/minecraft/client/Camera;"
                            + "Lnet/minecraft/client/multiplayer/ClientLevel;FLnet/minecraft/client/DeltaTracker;)V"))
    private void logistics$crudeOilFogDistance(
            FogEnvironment environment, FogData fogData, Camera camera, ClientLevel level,
            float renderDistance, DeltaTracker deltaTracker) {
        environment.setupFog(fogData, camera, level, renderDistance, deltaTracker);
        if (CrudeOilSubmersion.isCameraSubmerged(level, camera.blockPosition(), camera.position().y)) {
            fogData.environmentalStart = CRUDE_OIL_FOG_START;
            fogData.environmentalEnd = CRUDE_OIL_FOG_END;
            fogData.skyEnd = fogData.environmentalEnd;
            fogData.cloudEnd = fogData.environmentalEnd;
        }
    }
}
