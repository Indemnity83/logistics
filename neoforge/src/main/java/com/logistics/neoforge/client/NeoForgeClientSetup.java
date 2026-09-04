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
import com.logistics.core.lib.jei.SyncMachineRecipesPacket;
import com.logistics.power.engine.reaction.ReactionRecipeSyncPacket;
import com.logistics.power.engine.reaction.jei.ReactionJeiSyncAdapter;
import com.logistics.automation.kiln.KilnScreen;
import com.logistics.core.lib.client.render.FluidBoxRenderer;
import com.logistics.core.lib.client.render.FluidSpriteLookup;
import com.logistics.automation.sawmill.SawmillScreen;
import com.logistics.automation.transposer.TransposerScreen;
import com.logistics.core.lib.platform.ClientNetworking;
import com.logistics.automation.macerator.MaceratorScreen;
import com.logistics.pipe.screen.AdvancedExtractorScreen;
import com.logistics.pipe.screen.ChassisScreen;
import com.logistics.pipe.screen.CraftingScreen;
import com.logistics.pipe.screen.FluidSupplierScreen;
import com.logistics.pipe.screen.ItemFilterScreen;
import com.logistics.pipe.screen.ModSinkScreen;
import com.logistics.pipe.screen.ProcessScreen;
import com.logistics.pipe.screen.ProviderScreen;
import com.logistics.pipe.screen.RequesterScreen;
import com.logistics.pipe.screen.SatelliteScreen;
import com.logistics.pipe.screen.SinkScreen;
import com.logistics.pipe.screen.SupplierScreen;
import com.logistics.pipe.client.FluidPacketRendering;
import com.logistics.pipe.client.FluidPacketSpecialRenderer;
import com.logistics.automation.render.LaserQuarryBlockEntityRenderer;
import com.logistics.core.render.MarkerBlockEntityRenderer;
import com.logistics.pipe.network.packet.SyncRequesterInventoryPacket;
import com.logistics.pipe.render.PipeBlockEntityRenderer;
import com.logistics.power.render.CableBlockEntityRenderer;
import com.logistics.power.screen.ReactionEngineScreen;
import com.logistics.power.screen.MagmaticEngineScreen;
import com.logistics.power.screen.SteamEngineScreen;
import com.logistics.power.screen.FuelEngineScreen;
import com.logistics.power.screen.StirlingEngineScreen;
import com.logistics.neoforge.client.render.NeoForgeEngineBlockEntityRenderer;
import com.logistics.core.lib.compat.ClientScreenCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.world.level.material.FluidState;
import com.logistics.neoforge.fluids.NeoForgeFluids;
import com.logistics.core.fluid.CrudeOilSubmersion;
import com.logistics.core.lib.resource.ResourceId;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import net.minecraft.resources.Identifier; // raw-id-ok
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4f;

public final class NeoForgeClientSetup {
    private NeoForgeClientSetup() {}

    // Crude Oil submersion fog visibility/color. Anchors, not final.
    private static final float CRUDE_OIL_FOG_START = 0.25f;
    private static final float CRUDE_OIL_FOG_END = 1.0f;
    private static final Vector4f CRUDE_OIL_FOG_COLOR = new Vector4f(0.03f, 0.02f, 0.015f, 1.0f);

    public static void register(IEventBus modBus) {
        modBus.addListener(NeoForgeClientSetup::onClientSetup);
        modBus.addListener(NeoForgeClientSetup::registerScreens);
        modBus.addListener(NeoForgeClientSetup::registerRenderers);
        modBus.addListener(NeoForgeClientSetup::registerSpecialModelRenderers);
        modBus.addListener(NeoForgeClientSetup::registerClientPayloadHandlers);
        modBus.addListener(NeoForgeClientSetup::registerFluidExtensions);
        NeoForge.EVENT_BUS.addListener(NeoForgeClientSetup::onClientDisconnect);
    }

    private static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientMachineRecipes.clear();
        ReactionJeiSyncAdapter.INSTANCE.clear();
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

                /** Shrinks fog to near-lava-tight visibility while the camera is submerged in Crude Oil. */
                @Override
                public void modifyFogRender(
                        Camera camera, @Nullable FogEnvironment environment,
                        float renderDistance, float partialTick, FogData fogData) {
                    if (!"crude_oil".equals(name)) {
                        return;
                    }
                    if (!CrudeOilSubmersion.isCameraSubmerged(
                            camera.entity().level(), camera.blockPosition(), camera.position().y)) {
                        return;
                    }
                    fogData.environmentalStart = CRUDE_OIL_FOG_START;
                    fogData.environmentalEnd = CRUDE_OIL_FOG_END;
                    fogData.skyEnd = fogData.environmentalEnd;
                    fogData.cloudEnd = fogData.environmentalEnd;
                }

                /** Darkens fog color to near-black while the camera is submerged in Crude Oil. */
                @Override
                public Vector4f modifyFogColor(
                        Camera camera, float partialTick, ClientLevel level,
                        int renderDistance, float darkenWorldAmount, Vector4f fluidFogColor) {
                    if (!"crude_oil".equals(name)) {
                        return fluidFogColor;
                    }
                    if (!CrudeOilSubmersion.isCameraSubmerged(level, camera.blockPosition(), camera.position().y)) {
                        return fluidFogColor;
                    }
                    return CRUDE_OIL_FOG_COLOR;
                }
            }, type);
        });
    }

    private static Identifier textureId(String texture) { // raw-id-ok
        return ResourceId.parse(texture).toIdentifier();
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        ClientNetworking.register(ClientPacketDistributor::sendToServer);

        // Resolve fluid still sprite + tint through NeoForge's client fluid extensions (1.21.x has no
        // unified vanilla fluid model). Shared fluid pipe/tank renderers call this via FluidSpriteLookup.
        FluidSpriteLookup.register((fluid, level, pos) -> {
            FluidState fluidState = fluid.defaultFluidState();
            IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluidState);
            var still = ext.getStillTexture();
            if (still == null) {
                return null;
            }
            TextureAtlasSprite sprite = Minecraft.getInstance()
                    .getAtlasManager()
                    .getAtlasOrThrow(AtlasIds.BLOCKS)
                    .getSprite(still);
            return new FluidBoxRenderer.Appearance(sprite, ext.getTintColor());
        });
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(LogisticsAutomation.MENU.MACERATOR, MaceratorScreen::new);

        event.register(LogisticsPipe.SCREEN.ITEM_FILTER, ItemFilterScreen::new);
        event.register(LogisticsPipe.SCREEN.REQUESTER, RequesterScreen::new);
        event.register(LogisticsPipe.SCREEN.SUPPLIER, SupplierScreen::new);
        event.register(LogisticsPipe.SCREEN.FLUID_SUPPLIER, FluidSupplierScreen::new);
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
        event.register(LogisticsPower.SCREEN.REACTION_ENGINE, ReactionEngineScreen::new);
        event.register(LogisticsPower.SCREEN.MAGMATIC_ENGINE, MagmaticEngineScreen::new);
        event.register(LogisticsPower.SCREEN.STEAM_ENGINE, SteamEngineScreen::new);
        event.register(LogisticsPower.SCREEN.FUEL_ENGINE, FuelEngineScreen::new);

        event.register(LogisticsAutomation.MENU.KILN, KilnScreen::new);
        event.register(LogisticsAutomation.MENU.SAWMILL, SawmillScreen::new);
        event.register(LogisticsAutomation.MENU.ALLOY_SMELTER, AlloySmelterScreen::new);
        event.register(LogisticsAutomation.MENU.CRUCIBLE, CrucibleScreen::new);
        event.register(LogisticsAutomation.MENU.REFINERY, RefineryScreen::new);
        event.register(LogisticsAutomation.MENU.SEQUENTIAL_FABRICATOR, SequentialFabricatorScreen::new);
        event.register(LogisticsAutomation.MENU.TRANSPOSER, TransposerScreen::new);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                LogisticsCore.ENTITY.REDSTONE_ENGINE_BLOCK_ENTITY,
                NeoForgeEngineBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(
                LogisticsPower.ENTITY.STIRLING_ENGINE_BLOCK_ENTITY,
                NeoForgeEngineBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(
                LogisticsPower.ENTITY.REACTION_ENGINE_BLOCK_ENTITY,
                NeoForgeEngineBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(
                LogisticsPower.ENTITY.MAGMATIC_ENGINE_BLOCK_ENTITY,
                NeoForgeEngineBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(
                LogisticsPower.ENTITY.STEAM_ENGINE_BLOCK_ENTITY,
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

    private static void registerSpecialModelRenderers(RegisterSpecialModelRendererEvent event) {
        event.register(FluidPacketRendering.ID.toIdentifier(), FluidPacketSpecialRenderer.Unbaked.MAP_CODEC);
    }

    private static void registerClientPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(SyncRequesterInventoryPacket.TYPE, (packet, context) -> {
            var screen = ClientScreenCompat.currentScreen();
            if (screen instanceof RequesterScreen requesterScreen) {
                requesterScreen.applySync(packet);
            }
        });
        event.register(SyncFabricatorOutputsPacket.TYPE, (packet, context) -> {
            var screen = ClientScreenCompat.currentScreen();
            if (screen instanceof SequentialFabricatorScreen fabricatorScreen) {
                fabricatorScreen.updateOutputs(packet.pos(), packet.toOutputs());
            }
        });
        event.register(SyncMachineRecipesPacket.TYPE, (packet, context) -> ClientMachineRecipes.set(packet));
        event.register(ReactionRecipeSyncPacket.TYPE, (packet, context) -> ReactionJeiSyncAdapter.INSTANCE.set(packet));
    }
}
