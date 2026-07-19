package com.logistics.neoforge.client;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsCore;
import com.logistics.LogisticsPipe;
import com.logistics.LogisticsPower;
import com.logistics.pipe.render.FluidPipeBlockEntityRenderer;
import com.logistics.pipe.render.FluidPumpBlockEntityRenderer;
import com.logistics.pipe.render.GlassTankBlockEntityRenderer;
import com.logistics.automation.alloysmelter.AlloySmelterScreen;
import com.logistics.automation.crucible.CrucibleScreen;
import com.logistics.automation.refinery.RefineryScreen;
import com.logistics.automation.fabricator.SequentialFabricatorScreen;
import com.logistics.automation.fabricator.SyncFabricatorOutputsPacket;
import com.logistics.automation.jei.ClientMachineRecipes;
import com.logistics.automation.jei.SyncMachineRecipesPacket;
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
import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier; // raw-id-ok
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class NeoForgeClientSetup {
    private NeoForgeClientSetup() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(NeoForgeClientSetup::onClientSetup);
        modBus.addListener(NeoForgeClientSetup::registerScreens);
        modBus.addListener(NeoForgeClientSetup::registerRenderers);
        modBus.addListener(NeoForgeClientSetup::registerBlockColors);
        modBus.addListener(NeoForgeClientSetup::registerClientPayloadHandlers);
        modBus.addListener(NeoForgeClientSetup::registerFluidExtensions);
        modBus.addListener(NeoForgeClientSetup::registerFluidModels);
        NeoForge.EVENT_BUS.addListener(NeoForgeClientSetup::onClientDisconnect);
    }

    private static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientMachineRecipes.clear();
    }

    /**
     * Registers each custom fluid's model (still/flow sprites + flat tint) into the vanilla fluid model set
     * — this is what the tank/pipe {@code FluidBoxRenderer} reads on 26.2. Mirrors Fabric's
     * {@code FluidRenderingRegistry} call; {@link #registerFluidExtensions} only covers item-container tint.
     */
    private static void registerFluidModels(RegisterFluidModelsEvent event) {
        Map<String, LogisticsCore.FluidDef> defs = new HashMap<>();
        for (LogisticsCore.FluidDef def : LogisticsCore.CUSTOM_FLUIDS) {
            defs.put(def.name(), def);
        }
        var flowings = NeoForgeFluids.flowings();
        NeoForgeFluids.sources().forEach((name, source) -> {
            LogisticsCore.FluidDef def = defs.get(name);
            Material overlay = def.overlay() != null ? material(def.overlay()) : null;
            FluidModel.Unbaked model = new FluidModel.Unbaked(
                    material(def.still()), material(def.flow()), overlay,
                    BlockTintSources.constant(def.tint() | 0xFF000000));
            event.register(model, source, flowings.get(name));
        });
    }

    /** Builds a sprite material from a {@code "namespace:path"} texture id. */
    private static Material material(String texture) {
        return new Material(textureId(texture));
    }

    /** Supplies each custom fluid's still/flow textures + flat tint to NeoForge's fluid renderer. */
    private static void registerFluidExtensions(RegisterClientExtensionsEvent event) {
        Map<String, LogisticsCore.FluidDef> defs = new HashMap<>();
        for (LogisticsCore.FluidDef def : LogisticsCore.CUSTOM_FLUIDS) {
            defs.put(def.name(), def);
        }
        NeoForgeFluids.types().forEach((name, type) -> {
            LogisticsCore.FluidDef def = defs.get(name);
            event.registerFluidType(new IClientFluidTypeExtensions() {
                public Identifier getStillTexture() { // raw-id-ok
                    return textureId(def.still());
                }

                public Identifier getFlowingTexture() { // raw-id-ok
                    return textureId(def.flow());
                }

                public int getTintColor() {
                    return def.tint();
                }
            }, type);
        });
    }

    private static Identifier textureId(String texture) { // raw-id-ok
        return ResourceId.parse(texture).toIdentifier();
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
        event.register(LogisticsAutomation.MENU.CRUCIBLE, CrucibleScreen::new);
        event.register(LogisticsAutomation.MENU.REFINERY, RefineryScreen::new);
        event.register(LogisticsAutomation.MENU.SEQUENTIAL_FABRICATOR, SequentialFabricatorScreen::new);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                LogisticsCore.ENTITY.REDSTONE_ENGINE_BLOCK_ENTITY,
                NeoForgeEngineBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(
                LogisticsPower.ENTITY.STIRLING_ENGINE_BLOCK_ENTITY,
                NeoForgeEngineBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(
                LogisticsPower.ENTITY.FUEL_ENGINE_BLOCK_ENTITY,
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
                LogisticsPipe.ENTITY.FLUID_PIPE_BLOCK_ENTITY,
                FluidPipeBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(
                LogisticsPipe.ENTITY.GLASS_TANK_BLOCK_ENTITY,
                GlassTankBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(
                LogisticsAutomation.ENTITY.FLUID_PUMP_BLOCK_ENTITY,
                FluidPumpBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(
                LogisticsAutomation.ENTITY.CRUCIBLE_BLOCK_ENTITY,
                com.logistics.automation.crucible.CrucibleBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(
                LogisticsAutomation.ENTITY.REFINERY_BLOCK_ENTITY,
                com.logistics.automation.refinery.RefineryBlockEntityRenderer::new);
    }

    private static void registerBlockColors(RegisterColorHandlersEvent.BlockTintSources event) {
        event.register(
                List.of(EngineHeatTintSource.INSTANCE),
                LogisticsCore.BLOCK.REDSTONE_ENGINE,
                LogisticsPower.BLOCK.STIRLING_ENGINE,
                LogisticsPower.BLOCK.FUEL_ENGINE,
                LogisticsPower.BLOCK.CREATIVE_ENGINE);
    }

    private static void registerClientPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(SyncRequesterInventoryPacket.TYPE, (packet, context) -> {
            var screen = Minecraft.getInstance().gui.screen();
            if (screen instanceof RequesterScreen requesterScreen) {
                requesterScreen.updateAvailableItems(packet.pipePos(), packet.items(), packet.amounts());
            }
        });
        event.register(SyncFabricatorOutputsPacket.TYPE, (packet, context) -> {
            var screen = Minecraft.getInstance().gui.screen();
            if (screen instanceof SequentialFabricatorScreen fabricatorScreen) {
                fabricatorScreen.updateOutputs(packet.pos(), packet.toOutputs());
            }
        });
        event.register(SyncMachineRecipesPacket.TYPE, (packet, context) -> ClientMachineRecipes.set(packet));
    }
}
