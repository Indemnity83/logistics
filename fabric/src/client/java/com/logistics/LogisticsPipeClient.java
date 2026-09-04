package com.logistics;

import com.logistics.core.bootstrap.ClientDomainBootstrap;
import com.logistics.pipe.item.MarkingFluidItem;
import com.logistics.pipe.network.packet.SyncRequesterInventoryPacket;
import com.logistics.pipe.render.PipeBlockEntityRenderer;
import com.logistics.pipe.screen.ItemFilterScreen;
import com.logistics.pipe.screen.RequesterScreen;
import com.logistics.core.DebugLog;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import com.logistics.core.lib.compat.ClientScreenCompat;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;

import static com.logistics.LogisticsMod.LOGGER;

public final class LogisticsPipeClient implements ClientDomainBootstrap {

    @Override
    public void initClient() {
        LOGGER.info("Registering pipe (client)");

        // Register pipe blocks for cutout rendering (transparent textures)
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsPipe.BLOCK.STONE_TRANSPORT_PIPE, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsPipe.BLOCK.ITEM_PASSTHROUGH_PIPE, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsPipe.BLOCK.COPPER_TRANSPORT_PIPE, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsPipe.BLOCK.ITEM_EXTRACTOR_PIPE, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsPipe.BLOCK.ITEM_MERGER_PIPE, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsPipe.BLOCK.GOLD_TRANSPORT_PIPE, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsPipe.BLOCK.ITEM_FILTER_PIPE, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsPipe.BLOCK.ITEM_INSERTION_PIPE, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsPipe.BLOCK.ITEM_VOID_PIPE, RenderType.cutout());

        // Fluid pipes also use transparent textures (cutout); the glass tank uses translucent so the
        // contained fluid shows through. On 1.21.1 model render_type JSON is ignored, so register here.
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsPipe.BLOCK.COPPER_FLUID_PIPE, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsPipe.BLOCK.STONE_FLUID_PIPE, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsPipe.BLOCK.GOLD_FLUID_PIPE, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsPipe.BLOCK.INSERTION_FLUID_PIPE, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsPipe.BLOCK.MERGER_FLUID_PIPE, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsPipe.BLOCK.FLUID_EXTRACTOR_PIPE, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsPipe.BLOCK.VOID_FLUID_PIPE, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsPipe.BLOCK.BYPASS_FLUID_PIPE, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsPipe.BLOCK.GLASS_TANK, RenderType.translucent());

        // Register pipe block entity renderer
        BlockEntityRenderers.register(LogisticsPipe.ENTITY.PIPE_BLOCK_ENTITY, PipeBlockEntityRenderer::new);

        registerMarkingFluidColors();

        // Fluid is part of the pipe domain; register its client renderers here.
        LogisticsFluidClient.registerClient();

        MenuScreens.register(LogisticsPipe.SCREEN.ITEM_FILTER, ItemFilterScreen::new);
        MenuScreens.register(LogisticsPipe.SCREEN.REQUESTER, com.logistics.pipe.screen.RequesterScreen::new);
        MenuScreens.register(LogisticsPipe.SCREEN.SUPPLIER, com.logistics.pipe.screen.SupplierScreen::new);
        MenuScreens.register(LogisticsPipe.SCREEN.FLUID_SUPPLIER, com.logistics.pipe.screen.FluidSupplierScreen::new);
        MenuScreens.register(LogisticsPipe.SCREEN.PROVIDER, com.logistics.pipe.screen.ProviderScreen::new);
        MenuScreens.register(LogisticsPipe.SCREEN.SINK, com.logistics.pipe.screen.SinkScreen::new);
        MenuScreens.register(LogisticsPipe.SCREEN.CRAFTING, com.logistics.pipe.screen.CraftingScreen::new);
        MenuScreens.register(LogisticsPipe.SCREEN.PROCESS, com.logistics.pipe.screen.ProcessScreen::new);
        MenuScreens.register(LogisticsPipe.SCREEN.SATELLITE, com.logistics.pipe.screen.SatelliteScreen::new);
        MenuScreens.register(LogisticsPipe.SCREEN.CHASSIS_MK1, com.logistics.pipe.screen.ChassisScreen::new);
        MenuScreens.register(LogisticsPipe.SCREEN.CHASSIS_MK2, com.logistics.pipe.screen.ChassisScreen::new);
        MenuScreens.register(LogisticsPipe.SCREEN.CHASSIS_MK3, com.logistics.pipe.screen.ChassisScreen::new);
        MenuScreens.register(LogisticsPipe.SCREEN.CHASSIS_MK4, com.logistics.pipe.screen.ChassisScreen::new);
        MenuScreens.register(LogisticsPipe.SCREEN.CHASSIS_MK5, com.logistics.pipe.screen.ChassisScreen::new);
        MenuScreens.register(LogisticsPipe.SCREEN.ADVANCED_EXTRACTOR, com.logistics.pipe.screen.AdvancedExtractorScreen::new);
        MenuScreens.register(LogisticsPipe.SCREEN.MOD_SINK, com.logistics.pipe.screen.ModSinkScreen::new);

        DebugLog.register("render");
        registerPacketReceivers();
    }

    private void registerPacketReceivers() {
        // Sync requester inventory from server
        ClientPlayNetworking.registerGlobalReceiver(SyncRequesterInventoryPacket.TYPE, (packet, context) -> {
            context.client().execute(() -> {
                // Update the requester screen if the payload is addressed to the open menu
                if (ClientScreenCompat.currentScreen() instanceof RequesterScreen requesterScreen) {
                    requesterScreen.applySync(packet);
                }
            });
        });
    }

    /**
     * Tints layer1 (overlay) of the marking fluid item with the corresponding dye color.
     * layer0 (the bottle/container) is left untinted.
     */
    private void registerMarkingFluidColors() {
        Item[] items = {
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
            LogisticsPipe.ITEM.BLACK_MARKING_FLUID,
        };

        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> {
            if (tintIndex == 0) return -1;

            if (stack.getItem() instanceof MarkingFluidItem fluid) {
                DyeColor color = fluid.getColor();
                return color.getFireworkColor() | 0xFF000000;
            }
            return -1;
        }, items);
    }
}
