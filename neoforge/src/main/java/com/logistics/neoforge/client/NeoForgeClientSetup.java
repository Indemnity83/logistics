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
import com.logistics.automation.render.LaserQuarryBlockEntityRenderer;
import com.logistics.core.render.MarkerBlockEntityRenderer;
import com.logistics.core.lib.power.HeatStage;
import com.logistics.core.lib.power.EngineHeatTint;
import com.logistics.neoforge.NeoForgePacketRegistration;
import com.logistics.neoforge.client.render.NeoForgeEngineBlockEntityRenderer;
import com.logistics.neoforge.client.render.NeoForgeModelLoader;
import com.logistics.pipe.item.MarkingFluidItem;
import com.logistics.pipe.network.packet.SyncRequesterInventoryPacket;
import com.logistics.pipe.render.PipeBlockEntityRenderer;
import com.logistics.power.screen.ReactionEngineScreen;
import com.logistics.power.screen.MagmaticEngineScreen;
import com.logistics.power.screen.SteamEngineScreen;
import com.logistics.power.screen.FuelEngineScreen;
import com.logistics.power.screen.StirlingEngineScreen;
import com.logistics.neoforge.fluids.NeoForgeFluids;
import com.logistics.core.fluid.CrudeOilSubmersion;
import com.logistics.core.lib.resource.ResourceId;
import java.util.HashMap;
import java.util.Map;
import com.logistics.core.lib.compat.ClientScreenCompat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
import com.logistics.automation.jei.ClientMachineRecipes;
import org.joml.Vector3f;

public final class NeoForgeClientSetup {
    private NeoForgeClientSetup() {}

    // Crude Oil submersion fog visibility/color. Anchors, not final.
    private static final float CRUDE_OIL_FOG_START = 0.25f;
    private static final float CRUDE_OIL_FOG_END = 1.0f;
    private static final Vector3f CRUDE_OIL_FOG_COLOR = new Vector3f(0.03f, 0.02f, 0.015f);

    public static void register(IEventBus modBus) {
        NeoForgePacketRegistration.registerSyncRequesterInventoryHandler(NeoForgeClientSetup::handleSyncRequesterInventory);
        NeoForgePacketRegistration.registerSyncFabricatorOutputsHandler(NeoForgeClientSetup::handleSyncFabricatorOutputs);
        NeoForgePacketRegistration.registerSyncMachineRecipesHandler(NeoForgeClientSetup::handleSyncMachineRecipes);
        modBus.addListener(NeoForgeClientSetup::onClientSetup);
        modBus.addListener(NeoForgeClientSetup::registerScreens);
        modBus.addListener(NeoForgeClientSetup::registerRenderers);
        modBus.addListener(NeoForgeClientSetup::registerItemColors);
        modBus.addListener(NeoForgeClientSetup::registerFluidExtensions);
        modBus.addListener(NeoForgeModelLoader::registerGeometryLoaders);
        NeoForge.EVENT_BUS.addListener(NeoForgeClientSetup::onClientDisconnect);
        NeoForge.EVENT_BUS.addListener(NeoForgeClientSetup::onRenderFog);
    }

    /**
     * Shrinks fog to near-lava-tight visibility while the camera is submerged in Crude Oil.
     * {@code IClientFluidTypeExtensions#modifyFogRender} on this version takes the near/far distance
     * as plain (unmodifiable) primitives, so it can't actually change them — the real hook is this
     * cancellable event, fired right after with the same values and applied back afterward.
     */
    private static void onRenderFog(ViewportEvent.RenderFog event) {
        Camera camera = event.getCamera();
        if (!CrudeOilSubmersion.isCameraSubmerged(
                camera.getEntity().level(), camera.getBlockPosition(), camera.getPosition().y)) {
            return;
        }
        event.setNearPlaneDistance(CRUDE_OIL_FOG_START);
        event.setFarPlaneDistance(CRUDE_OIL_FOG_END);
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
                @Override
                public ResourceLocation getStillTexture() { // raw-id-ok
                    return textureId(def.still());
                }

                @Override
                public ResourceLocation getFlowingTexture() { // raw-id-ok
                    return textureId(def.flow());
                }

                @Override
                public int getTintColor() {
                    return def.tint();
                }

                /** Darkens fog color to near-black while the camera is submerged in Crude Oil. */
                @Override
                public Vector3f modifyFogColor(
                        Camera camera, float partialTick, ClientLevel level,
                        int renderDistance, float darkenWorldAmount, Vector3f fluidFogColor) {
                    if (!"crude_oil".equals(name)) {
                        return fluidFogColor;
                    }
                    if (!CrudeOilSubmersion.isCameraSubmerged(
                            camera.getEntity().level(), camera.getBlockPosition(), camera.getPosition().y)) {
                        return fluidFogColor;
                    }
                    return CRUDE_OIL_FOG_COLOR;
                }
            }, type);
        });
    }

    private static ResourceLocation textureId(String texture) { // raw-id-ok
        return ResourceId.parse(texture).toIdentifier();
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        ClientNetworking.register(payload ->
                PacketDistributor.sendToServer(payload));

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
                    .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                    .apply(still);
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

    /**
     * Registers item tint handlers in code. On MC 1.21.1 the data-driven client item model
     * "tints" array (1.21.4+) is unavailable, so the marking fluid overlay and the engine item
     * core must be tinted through {@link RegisterColorHandlersEvent.Item} just like the Fabric
     * client does via {@code ColorProviderRegistry.ITEM}.
     */
    private static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        // Marking fluid: leave layer0 (bottle) untinted, tint layer1 (overlay) with the dye color.
        event.register((stack, tintIndex) -> {
            if (tintIndex == 0) return -1;
            if (stack.getItem() instanceof MarkingFluidItem fluid) {
                return fluid.getColor().getFireworkColor() | 0xFF000000;
            }
            return -1;
        },
                LogisticsPipe.ITEM.WHITE_MARKING_FLUID,
                LogisticsPipe.ITEM.ORANGE_MARKING_FLUID,
                LogisticsPipe.ITEM.MAGENTA_MARKING_FLUID,
                LogisticsPipe.ITEM.LIGHT_BLUE_MARKING_FLUID,
                LogisticsPipe.ITEM.YELLOW_MARKING_FLUID,
                LogisticsPipe.ITEM.LIME_MARKING_FLUID,
                LogisticsPipe.ITEM.PINK_MARKING_FLUID,
                LogisticsPipe.ITEM.GRAY_MARKING_FLUID,
                LogisticsPipe.ITEM.LIGHT_GRAY_MARKING_FLUID,
                LogisticsPipe.ITEM.CYAN_MARKING_FLUID,
                LogisticsPipe.ITEM.PURPLE_MARKING_FLUID,
                LogisticsPipe.ITEM.BLUE_MARKING_FLUID,
                LogisticsPipe.ITEM.BROWN_MARKING_FLUID,
                LogisticsPipe.ITEM.GREEN_MARKING_FLUID,
                LogisticsPipe.ITEM.RED_MARKING_FLUID,
                LogisticsPipe.ITEM.BLACK_MARKING_FLUID);

        // Engine item: the in-inventory model tints its core (tintIndex 0). Items have no live heat
        // state, so they always show the idle COLD blue (matches 26.x's static engine item tint).
        event.register((stack, tintIndex) -> tintIndex == 0 ? EngineHeatTint.color(HeatStage.COLD) : -1,
                LogisticsCore.BLOCK.REDSTONE_ENGINE,
                LogisticsPower.BLOCK.STIRLING_ENGINE,
                LogisticsPower.BLOCK.REACTION_ENGINE,
                LogisticsPower.BLOCK.MAGMATIC_ENGINE,
                LogisticsPower.BLOCK.STEAM_ENGINE,
                LogisticsPower.BLOCK.FUEL_ENGINE,
                LogisticsPower.BLOCK.CREATIVE_ENGINE);
    }

    private static void handleSyncRequesterInventory(SyncRequesterInventoryPacket packet) {
        var screen = ClientScreenCompat.currentScreen();
        if (screen instanceof RequesterScreen requesterScreen) {
            requesterScreen.applySync(packet);
        }
    }

    private static void handleSyncFabricatorOutputs(SyncFabricatorOutputsPacket packet) {
        var screen = ClientScreenCompat.currentScreen();
        if (screen instanceof SequentialFabricatorScreen fabricatorScreen) {
            fabricatorScreen.updateOutputs(packet.pos(), packet.toOutputs());
        }
    }

    private static void handleSyncMachineRecipes(SyncMachineRecipesPacket packet) {
        ClientMachineRecipes.set(packet);
    }

    private static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientMachineRecipes.clear();
        ReactionJeiSyncAdapter.INSTANCE.clear();
    }
}
