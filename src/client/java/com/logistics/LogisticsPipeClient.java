package com.logistics;

import com.logistics.core.bootstrap.DomainBootstrap;
import com.logistics.pipe.render.PipeBlockEntityRenderer;
import com.logistics.pipe.screen.ItemFilterScreen;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.logistics.LogisticsMod.LOGGER;

public final class LogisticsPipeClient implements DomainBootstrap {
    public LogisticsPipeClient() {
        ModelLoadingPlugin.register(pluginContext -> {
            List<ResourceLocation> models = collectPipeModelIds();
            if (!models.isEmpty()) {
                pluginContext.addModels(models.toArray(new ResourceLocation[0]));
            }
        });
    }

    @Override
    public void initCommon() {
        // Client-only bootstrap; common init handled in LogisticsPipe
    }

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

        // Register pipe block entity renderer
        BlockEntityRenderers.register(LogisticsPipe.ENTITY.PIPE_BLOCK_ENTITY, PipeBlockEntityRenderer::new);

        registerMarkingFluidColors();

        MenuScreens.register(LogisticsPipe.SCREEN.ITEM_FILTER, ItemFilterScreen::new);
    }

    /**
     * Tints layer1 (overlay) of the marking fluid item with the corresponding dye color.
     * layer0 (the bottle/container) is left untinted.
     */
    private void registerMarkingFluidColors() {
        Item[] items = java.util.stream.Stream.of(DyeColor.values())
                .map(LogisticsPipe::getMarkingFluidItem)
                .toArray(Item[]::new);

        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> {
            if (tintIndex == 0) return -1;

            DyeColor color = LogisticsPipe.getMarkingFluidColor(stack);
            return color != null ? (color.getFireworkColor() | 0xFF000000) : -1;
        }, items);
    }

    /**
     * Scans the mod's pipe model directory and returns resource locations for all
     * pipe model JSON files. These are registered with the model loading plugin so
     * they are baked and available for the block entity renderer at runtime.
     *
     * <p>Core models referenced in blockstate JSON files are baked automatically;
     * arm/feature/variant models need explicit registration via this mechanism.
     */
    private static List<ResourceLocation> collectPipeModelIds() {
        Optional<Path> pipeModelDir = FabricLoader.getInstance()
                .getModContainer(LogisticsMod.MOD_ID)
                .flatMap(mod -> mod.findPath("assets/" + LogisticsMod.MOD_ID + "/models/block/pipe"));

        if (pipeModelDir.isEmpty()) {
            LOGGER.warn("Could not find pipe model directory - pipe rendering will be missing");
            return List.of();
        }

        List<ResourceLocation> modelIds = new ArrayList<>();
        try (Stream<Path> files = Files.walk(pipeModelDir.get())) {
            files.filter(Files::isRegularFile).forEach(path -> {
                String relative = pipeModelDir.get().relativize(path).toString().replace('\\', '/');
                if (!relative.endsWith(".json")) return;
                String name = relative.substring(0, relative.length() - ".json".length());
                modelIds.add(LogisticsMod.getResourceLocation("block/pipe/" + name));
            });
        } catch (IOException e) {
            LOGGER.warn("Failed to scan pipe model resources", e);
        }

        return modelIds;
    }
}
