package com.logistics.pipe.registry;

import com.logistics.core.bootstrap.ClientDomainBootstrap;
import com.logistics.core.render.ModelRegistry;
import com.logistics.pipe.render.PipeBlockEntityRenderer;
import com.logistics.pipe.screen.ItemFilterScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public final class PipeClientBootstrap implements ClientDomainBootstrap {
    @Override
    public void initCommon() {
        // Client-only bootstrap; common init handled in main bootstrap.
    }

    @Override
    public void initClient() {
        ModelRegistry.register();

        // TODO: Re-enable render layer map once API compatibility is resolved
        // These calls configure transparent rendering for pipes
        // BlockRenderLayerMap.INSTANCE.putBlock(PipeBlocks.STONE_TRANSPORT_PIPE, RenderType.cutout());
        // ... (other pipes)

        BlockEntityRenderers.register(PipeBlockEntities.PIPE_BLOCK_ENTITY, PipeBlockEntityRenderer::new);

        MenuScreens.register(PipeScreenHandlers.ITEM_FILTER, ItemFilterScreen::new);
    }
}
