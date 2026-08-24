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

/**
 * Fabric has no equivalent to NeoForge's {@code IClientFluidTypeExtensions.modifyFogRender}, so this
 * mirrors NeoForge's own {@code ClientHooks#onSetupFog} patch directly: darken/shorten fog like lava
 * while the camera is submerged in Crude Oil (issue #836), rather than a screen-space HUD overlay
 * (which drew over the crosshair/hotbar — vanilla's actual submersion "vision" effect is a fog change,
 * not a quad). Values mirror {@code LavaFogEnvironment}'s 0.25-1 block visibility, color swapped for a
 * near-black oil tint instead of lava's orange.
 */
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
