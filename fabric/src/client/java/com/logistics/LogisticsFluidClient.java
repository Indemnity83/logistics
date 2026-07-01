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

    /** Renders each custom fluid from its own still/flow sprites with a flat per-fluid tint. */
    private static void registerFluidRenderers() {
        Map<String, LogisticsFluid.FluidDef> defs = new HashMap<>();
        for (LogisticsFluid.FluidDef def : LogisticsFluid.CUSTOM_FLUIDS) {
            defs.put(def.name(), def);
        }

        FabricFluids.sources().forEach((name, source) -> {
            LogisticsFluid.FluidDef def = defs.get(name);
            Material overlay = def.overlay() != null ? material(def.overlay()) : null;
            FluidModel.Unbaked model = new FluidModel.Unbaked(
                    material(def.still()), material(def.flow()), overlay,
                    BlockTintSources.constant(def.tint() & 0xFFFFFF));
            FluidRenderingRegistry.register(source, source.getFlowing(), model);
        });
    }

    /** Builds a sprite material from a {@code "namespace:path"} texture id. */
    private static Material material(String texture) {
        int colon = texture.indexOf(':');
        Identifier id = ResourceId.in(texture.substring(0, colon), texture.substring(colon + 1)).toIdentifier();
        return new Material(id);
    }
}
