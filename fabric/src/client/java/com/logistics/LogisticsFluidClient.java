package com.logistics;

import com.logistics.core.lib.resource.ResourceId;
import com.logistics.fabric.fluids.FabricFluids;
import com.logistics.pipe.render.FluidPipeBlockEntityRenderer;
import com.logistics.pipe.render.FluidPumpBlockEntityRenderer;
import com.logistics.pipe.render.GlassTankBlockEntityRenderer;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry;
import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;

import static com.logistics.LogisticsMod.LOGGER;

/**
 * Fluid client registration. No longer a standalone {@code ClientDomainBootstrap}: fluid is part of the
 * pipe domain, so its renderers are registered from {@link LogisticsPipeClient#initClient()}.
 */
public final class LogisticsFluidClient {

    private LogisticsFluidClient() {}

    public static void registerClient() {
        LOGGER.info("Registering fluid (client)");

        BlockEntityRenderers.register(
                LogisticsFluid.ENTITY.FLUID_PIPE_BLOCK_ENTITY, FluidPipeBlockEntityRenderer::new);
        BlockEntityRenderers.register(
                LogisticsFluid.ENTITY.GLASS_TANK_BLOCK_ENTITY, GlassTankBlockEntityRenderer::new);
        BlockEntityRenderers.register(
                LogisticsFluid.ENTITY.FLUID_PUMP_BLOCK_ENTITY, FluidPumpBlockEntityRenderer::new);

        registerFluidRenderers();
    }

    /** Renders each custom fluid as vanilla water (still + flow) with a flat per-fluid tint. */
    private static void registerFluidRenderers() {
        Material still = waterMaterial("block/water_still");
        Material flow = waterMaterial("block/water_flow");
        Material overlay = waterMaterial("block/water_overlay");

        Map<String, Integer> tints = new HashMap<>();
        for (LogisticsFluid.FluidDef def : LogisticsFluid.CUSTOM_FLUIDS) {
            tints.put(def.name(), def.tint());
        }

        FabricFluids.sources().forEach((name, source) -> {
            FluidModel.Unbaked model = new FluidModel.Unbaked(
                    still, flow, overlay, BlockTintSources.constant(tints.get(name) & 0xFFFFFF));
            FluidRenderingRegistry.register(source, source.getFlowing(), model);
        });
    }

    private static Material waterMaterial(String texture) {
        Identifier id = ResourceId.in("minecraft", texture).toIdentifier();
        return new Material(id);
    }
}
