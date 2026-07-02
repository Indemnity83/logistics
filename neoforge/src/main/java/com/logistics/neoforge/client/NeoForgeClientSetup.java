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
import com.logistics.automation.crucible.CrucibleScreen;
import com.logistics.automation.kiln.KilnScreen;
import com.logistics.core.lib.client.render.FluidBoxRenderer;
import com.logistics.core.lib.client.render.FluidSpriteLookup;
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
import com.logistics.core.lib.power.AbstractEngineBlockEntity.HeatStage;
import com.logistics.core.lib.power.EngineHeatTint;
import com.logistics.neoforge.NeoForgePacketRegistration;
import com.logistics.neoforge.client.render.NeoForgeEngineBlockEntityRenderer;
import com.logistics.neoforge.client.render.NeoForgeModelLoader;
import com.logistics.pipe.item.MarkingFluidItem;
import com.logistics.pipe.network.packet.SyncRequesterInventoryPacket;
import com.logistics.pipe.render.PipeBlockEntityRenderer;
import com.logistics.power.screen.StirlingEngineScreen;
import com.logistics.neoforge.fluids.NeoForgeFluids;
import com.logistics.core.lib.resource.ResourceId;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
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
import net.neoforged.neoforge.network.PacketDistributor;

public final class NeoForgeClientSetup {
    private NeoForgeClientSetup() {}

    public static void register(IEventBus modBus) {
        NeoForgePacketRegistration.registerSyncRequesterInventoryHandler(NeoForgeClientSetup::handleSyncRequesterInventory);
        modBus.addListener(NeoForgeClientSetup::onClientSetup);
        modBus.addListener(NeoForgeClientSetup::registerScreens);
        modBus.addListener(NeoForgeClientSetup::registerRenderers);
        modBus.addListener(NeoForgeClientSetup::registerItemColors);
        modBus.addListener(NeoForgeClientSetup::registerFluidExtensions);
        modBus.addListener(NeoForgeModelLoader::registerGeometryLoaders);
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
        event.registerBlockEntityRenderer(
                LogisticsAutomation.ENTITY.CRUCIBLE_BLOCK_ENTITY,
                com.logistics.automation.crucible.CrucibleBlockEntityRenderer::new);
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
                LogisticsPower.BLOCK.REDSTONE_ENGINE,
                LogisticsPower.BLOCK.STIRLING_ENGINE,
                LogisticsPower.BLOCK.CREATIVE_ENGINE);
    }

    private static void handleSyncRequesterInventory(SyncRequesterInventoryPacket packet) {
        var screen = Minecraft.getInstance().screen;
        if (screen instanceof RequesterScreen requesterScreen) {
            requesterScreen.updateAvailableItems(packet.pipePos(), packet.items(), packet.amounts());
        }
    }
}
