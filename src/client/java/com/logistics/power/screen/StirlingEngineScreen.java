package com.logistics.power.screen;

import com.logistics.power.engine.ui.StirlingEngineScreenHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Client-side screen for the Stirling Engine GUI.
 * Displays fuel slot with burn progress flame.
 */
public class StirlingEngineScreen extends AbstractContainerScreen<StirlingEngineScreenHandler> {
    private static final Identifier BACKGROUND_TEXTURE =
            Identifier.fromNamespaceAndPath("logistics", "textures/gui/power/stirling_engine.png");

    // Reuse vanilla's lit flame sprite
    private static final Identifier LIT_PROGRESS_SPRITE = Identifier.withDefaultNamespace("container/furnace/lit_progress");

    // Flame position (below the fuel slot)
    private static final int FLAME_X = 80;
    private static final int FLAME_Y = 22;

    public StirlingEngineScreen(StirlingEngineScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics context, float delta, int mouseX, int mouseY) {
        // Draw main background
        context.blit(
                RenderPipelines.GUI_TEXTURED,
                BACKGROUND_TEXTURE,
                leftPos,
                topPos,
                0,
                0,
                imageWidth,
                imageHeight,
                256,
                256);

        // Draw flame progress
        if (menu.isBurning()) {
            int flameHeight = 14;
            float progress = menu.getBurnProgress();
            int pixelsToShow = (int) ((1.0f - progress) * (flameHeight - 1)) + 1;
            int yOffset = flameHeight - pixelsToShow;
            context.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    LIT_PROGRESS_SPRITE,
                    14,
                    14,
                    0,
                    yOffset,
                    leftPos + FLAME_X,
                    topPos + FLAME_Y + yOffset,
                    14,
                    pixelsToShow);
        }
    }
}
