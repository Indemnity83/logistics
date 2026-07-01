package com.logistics.neoforge.client;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsCore;
import com.logistics.LogisticsFluid;
import com.logistics.LogisticsPipe;
import com.logistics.LogisticsPower;
import com.logistics.pipe.render.FluidPipeBlockEntityRenderer;
import com.logistics.pipe.render.FluidPumpBlockEntityRenderer;
import com.logistics.pipe.render.GlassTankBlockEntityRenderer;
import com.logistics.automation.alloysmelter.AlloySmelterScreen;
import com.logistics.automation.kiln.KilnScreen;
import com.logistics.automation.sawmill.SawmillScreen;
import com.logistics.core.lib.platform.ClientNetworking;
import com.logistics.automation.macerator.MaceratorScreen;
import com.logistics.pipe.screen.AdvancedExtractorScreen;
import com.logistics.pipe.screen.ChassisScreen;
import com.logistics.pipe.screen.CraftingScreen;
import com.logistics.pipe.screen.ItemFilterScreen;
import com.logistics.pipe.screen.ModSinkScreen;
import com.logistics.pipe.screen.ProcessScreen;
import com.logistics.pipe.screen.ProviderScreen;
import com.logistics.pipe.screen.RequesterScreen;
import com.logistics.pipe.screen.SatelliteScreen;
import com.logistics.pipe.screen.SinkScreen;
import com.logistics.pipe.screen.SupplierScreen;
import com.logistics.automation.render.LaserQuarryBlockEntityRenderer;
import com.logistics.core.render.MarkerBlockEntityRenderer;
import com.logistics.pipe.network.packet.SyncRequesterInventoryPacket;
import com.logistics.pipe.render.PipeBlockEntityRenderer;
import com.logistics.power.render.CableBlockEntityRenderer;
import com.logistics.power.render.EngineHeatTintSource;
import com.logistics.power.screen.StirlingEngineScreen;
import com.logistics.neoforge.client.render.NeoForgeEngineBlockEntityRenderer;
import com.logistics.neoforge.fluids.NeoForgeFluids;
import com.logistics.core.lib.resource.ResourceId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class NeoForgeClientSetup {
    private NeoForgeClientSetup() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(NeoForgeClientSetup::onClientSetup);
        modBus.addListener(NeoForgeClientSetup::registerScreens);
        modBus.addListener(NeoForgeClientSetup::registerRenderers);
        modBus.addListener(NeoForgeClientSetup::registerBlockColors);
        modBus.addListener(NeoForgeClientSetup::registerClientPayloadHandlers);
        modBus.addListener(NeoForgeClientSetup::registerFluidExtensions);
    }

    /** Supplies each custom fluid's still/flow textures + flat tint to NeoForge's fluid renderer. */
    private static void registerFluidExtensions(RegisterClientExtensionsEvent event) {
        Map<String, LogisticsCore.FluidDef> defs = new HashMap<>();
        for (LogisticsCore.FluidDef def : LogisticsCore.CUSTOM_FLUIDS) {
            defs.put(def.name(), def);
        }
        NeoForgeFluids.types().forEach((name, type) -> {
            LogisticsCore.FluidDef def = defs.get(name);
            Identifier still = textureId(def.still());
            Identifier flow = textureId(def.flow());
            int tint = def.tint();
            event.registerFluidType(new IClientFluidTypeExtensions() {
                public Identifier getStillTexture() {
                    return still;
                }

                public Identifier getFlowingTexture() {
                    return flow;
                }

                public int getTintColor() {
                    return tint;
                }
            }, type);
        });
    }

    private static Identifier textureId(String texture) {
        int colon = texture.indexOf(':');
        return ResourceId.in(texture.substring(0, colon), texture.substring(colon + 1)).toIdentifier();
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        ClientNetworking.register(ClientPacketDistributor::sendToServer);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(LogisticsAutomation.MENU.MACERATOR, MaceratorScreen::new);

        event.register(LogisticsPipe.SCREEN.ITEM_FILTER, ItemFilterScreen::new);
        event.register(LogisticsPipe.SCREEN.REQUESTER, RequesterScreen::new);
        event.register(LogisticsPipe.SCREEN.SUPPLIER, SupplierScreen::new);
        event.register(LogisticsPipe.SCREEN.PROVIDER, ProviderScreen::new);
        event.register(LogisticsPipe.SCREEN.SINK, SinkScreen::new);
        event.register(LogisticsPipe.SCREEN.CRAFTING, CraftingScreen::new);
        event.register(LogisticsPipe.SCREEN.PROCESS, ProcessScreen::new);
        event.register(LogisticsPipe.SCREEN.SATELLITE, SatelliteScreen::new);
        event.register(LogisticsPipe.SCREEN.CHASSIS_MK1, ChassisScreen::new);
        event.register(LogisticsPipe.SCREEN.CHASSIS_MK2, ChassisScreen::new);
        event.register(LogisticsPipe.SCREEN.CHASSIS_MK3, ChassisScreen::new);
        event.register(LogisticsPipe.SCREEN.CHASSIS_MK4, ChassisScreen::new);
        event.register(LogisticsPipe.SCREEN.CHASSIS_MK5, ChassisScreen::new);
        event.register(LogisticsPipe.SCREEN.ADVANCED_EXTRACTOR, AdvancedExtractorScreen::new);
        event.register(LogisticsPipe.SCREEN.MOD_SINK, ModSinkScreen::new);

        event.register(LogisticsPower.SCREEN.STIRLING_ENGINE, StirlingEngineScreen::new);

        event.register(LogisticsAutomation.MENU.KILN, KilnScreen::new);
        event.register(LogisticsAutomation.MENU.SAWMILL, SawmillScreen::new);
        event.register(LogisticsAutomation.MENU.ALLOY_SMELTER, AlloySmelterScreen::new);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                LogisticsPower.ENTITY.REDSTONE_ENGINE_BLOCK_ENTITY,
                NeoForgeEngineBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(
                LogisticsPower.ENTITY.STIRLING_ENGINE_BLOCK_ENTITY,
                NeoForgeEngineBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(
                LogisticsPower.ENTITY.CREATIVE_ENGINE_BLOCK_ENTITY,
                NeoForgeEngineBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(
                LogisticsPipe.ENTITY.PIPE_BLOCK_ENTITY,
                PipeBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(
                LogisticsPower.ENTITY.CABLE_BLOCK_ENTITY,
                CableBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(
                LogisticsCore.ENTITY.MARKER_BLOCK_ENTITY,
                MarkerBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(
                LogisticsAutomation.ENTITY.LASER_QUARRY_BLOCK_ENTITY,
                LaserQuarryBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(
                LogisticsFluid.ENTITY.FLUID_PIPE_BLOCK_ENTITY,
                FluidPipeBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(
                LogisticsFluid.ENTITY.GLASS_TANK_BLOCK_ENTITY,
                GlassTankBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(
                LogisticsFluid.ENTITY.FLUID_PUMP_BLOCK_ENTITY,
                FluidPumpBlockEntityRenderer::new);
    }

    private static void registerBlockColors(RegisterColorHandlersEvent.BlockTintSources event) {
        event.register(
                List.of(EngineHeatTintSource.INSTANCE),
                LogisticsPower.BLOCK.REDSTONE_ENGINE,
                LogisticsPower.BLOCK.STIRLING_ENGINE,
                LogisticsPower.BLOCK.CREATIVE_ENGINE);
    }

    private static void registerClientPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(SyncRequesterInventoryPacket.TYPE, (packet, context) -> {
            var screen = Minecraft.getInstance().gui.screen();
            if (screen instanceof RequesterScreen requesterScreen) {
                requesterScreen.updateAvailableItems(packet.pipePos(), packet.items(), packet.amounts());
            }
        });
    }
}
