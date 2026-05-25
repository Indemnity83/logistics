package com.logistics.neoforge.client;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsCore;
import com.logistics.LogisticsPipe;
import com.logistics.LogisticsPower;
import com.logistics.automation.kiln.KilnScreen;
import com.logistics.core.lib.platform.ClientNetworking;
import com.logistics.core.macerator.MaceratorScreen;
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
import com.logistics.automation.render.MarkerBlockEntityRenderer;
import com.logistics.core.lib.power.AbstractEngineBlockEntity;
import com.logistics.pipe.network.packet.SyncRequesterInventoryPacket;
import com.logistics.pipe.render.PipeBlockEntityRenderer;
import com.logistics.power.screen.StirlingEngineScreen;
import com.logistics.neoforge.client.render.NeoForgeEngineBlockEntityRenderer;
import com.logistics.neoforge.client.render.NeoForgeModelLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class NeoForgeClientSetup {
    private NeoForgeClientSetup() {}

    public static void register(IEventBus modBus) {
        NeoForgeModelLoader.registerPowerModels();
        modBus.addListener(NeoForgeClientSetup::onClientSetup);
        modBus.addListener(NeoForgeClientSetup::registerScreens);
        modBus.addListener(NeoForgeClientSetup::registerRenderers);
        modBus.addListener(NeoForgeClientSetup::registerBlockColors);
        modBus.addListener(NeoForgeModelLoader::registerAdditionalModels);
        modBus.addListener(NeoForgeModelLoader::registerGeometryLoaders);
        modBus.addListener(NeoForgeClientSetup::registerClientPayloadHandlers);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        ClientNetworking.register(payload ->
                PacketDistributor.sendToServer(payload));
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(LogisticsCore.MENU.MACERATOR, MaceratorScreen::new);

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
                LogisticsAutomation.ENTITY.MARKER_BLOCK_ENTITY,
                MarkerBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(
                LogisticsAutomation.ENTITY.LASER_QUARRY_BLOCK_ENTITY,
                LaserQuarryBlockEntityRenderer::new);
    }

    private static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        BlockColor engineColor = (state, level, pos, tintIndex) -> {
            if (tintIndex != 0) {
                return 0xFFFFFF;
            }
            return switch (state.getValue(AbstractEngineBlockEntity.STAGE)) {
                case COLD -> 0x3366CC;
                case COOL -> 0x33CC33;
                case WARM -> 0xCCCC33;
                case HOT -> 0xCC3333;
                case OVERHEAT -> 0x191919;
            };
        };
        event.register(
                engineColor,
                LogisticsPower.BLOCK.REDSTONE_ENGINE,
                LogisticsPower.BLOCK.STIRLING_ENGINE,
                LogisticsPower.BLOCK.CREATIVE_ENGINE);
    }

    private static void registerClientPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(SyncRequesterInventoryPacket.TYPE, SyncRequesterInventoryPacket.CODEC,
                (packet, context) -> context.enqueueWork(() -> {
                    var screen = Minecraft.getInstance().screen;
                    if (screen instanceof RequesterScreen requesterScreen) {
                        requesterScreen.updateAvailableItems(packet.pipePos(), packet.items(), packet.amounts());
                    }
                }));
    }
}
