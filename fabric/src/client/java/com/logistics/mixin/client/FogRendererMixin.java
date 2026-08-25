package com.logistics.mixin.client;

import com.logistics.core.fluid.CrudeOilSubmersion;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Darkens and shortens fog while the camera is submerged in Crude Oil, mirroring
 * {@code LavaFogEnvironment}. {@code setupFog}/{@code setupColor} on this version compute the shader
 * fog uniforms directly (no mutable data object to hook into), so both are tail-injected to overwrite
 * those uniforms afterward — the same point NeoForge's own fluid-fog hooks apply their result.
 */
@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
    // Anchors, not final — tune by playtest.
    private static final float CRUDE_OIL_FOG_START = 0.25f;
    private static final float CRUDE_OIL_FOG_END = 1.0f;

    @Inject(method = "setupColor", at = @At("TAIL"))
    private static void logistics$crudeOilFogColor(
            Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount,
            CallbackInfo ci) {
        if (CrudeOilSubmersion.isCameraSubmerged(level, camera.getBlockPosition(), camera.getPosition().y)) {
            RenderSystem.clearColor(0.03f, 0.02f, 0.015f, 0.0f);
        }
    }

    @Inject(method = "setupFog", at = @At("TAIL"))
    private static void logistics$crudeOilFogDistance(
            Camera camera, FogRenderer.FogMode mode, float renderDistance, boolean isFoggy, float partialTick,
            CallbackInfo ci) {
        Level level = camera.getEntity().level();
        if (CrudeOilSubmersion.isCameraSubmerged(level, camera.getBlockPosition(), camera.getPosition().y)) {
            RenderSystem.setShaderFogStart(CRUDE_OIL_FOG_START);
            RenderSystem.setShaderFogEnd(CRUDE_OIL_FOG_END);
        }
    }
}
