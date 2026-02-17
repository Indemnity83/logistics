package com.logistics;

import com.logistics.core.bootstrap.DomainBootstrap;
import com.logistics.pipe.screen.ItemFilterScreen;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;

import java.util.stream.Stream;

import static com.logistics.LogisticsMod.LOGGER;

public final class LogisticsPipeClient implements DomainBootstrap {
    public LogisticsPipeClient() {
        // Public constructor for direct instantiation
    }

    @Override
    public void initCommon() {
        // Client-only bootstrap; common init handled in LogisticsPipe
    }

    @Override
    public void initClient() {
        LOGGER.info("Registering pipe (client)");
        // TODO: Restore model registry once rendering is ported to MC 1.21.1
        // ModelRegistry.register();

        // TODO: Re-enable render layer map once API compatibility is resolved
        // These calls configure transparent rendering for pipes
        // BlockRenderLayerMap.INSTANCE.putBlock(PipeBlocks.STONE_TRANSPORT_PIPE, RenderType.cutout());
        // ... (other pipes)

        // TODO: Restore pipe block entity renderer once rendering is ported to MC 1.21.1
        // BlockEntityRenderers.register(LogisticsPipe.ENTITY.PIPE_BLOCK_ENTITY, PipeBlockEntityRenderer::new);

        registerMarkingFluidColors();

        MenuScreens.register(LogisticsPipe.SCREEN.ITEM_FILTER, ItemFilterScreen::new);
    }

    /**
     * Tints layer1 (overlay) of the marking fluid item with the corresponding dye color.
     * layer0 (the bottle/container) is left untinted.
     */
    private void registerMarkingFluidColors() {
        Item[] items = Stream.of(DyeColor.values())
                .map(LogisticsPipe::getMarkingFluidItem)
                .toArray(Item[]::new);

        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> {
            if (tintIndex == 0) return -1;

            DyeColor color = LogisticsPipe.getMarkingFluidColor(stack);
            return color != null ? (color.getFireworkColor() | 0xFF000000) : -1;
        }, items);
    }
}
