package com.logistics.core.fabricator;

import com.logistics.LogisticsMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Client-side GUI screen for the Kiln.
 * Renders the GUI texture and progress bars.
 */
public class KilnScreen extends AbstractContainerScreen<KilnScreenHandler> {

    private static final Identifier TEXTURE = LogisticsMod.getIdentifier("textures/gui/core/kiln.png");

    // GUI texture dimensions (standard)
    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;

    // Reuse vanilla's lit flame sprite
    private static final Identifier LIT_PROGRESS_SPRITE = Identifier.withDefaultNamespace("container/furnace/lit_progress");

    public KilnScreen(KilnScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        // Center the title
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        // Render main GUI texture
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            TEXTURE,
            leftPos,
            topPos,
            0,
            0,
            imageWidth,
            imageHeight,
            TEXTURE_WIDTH,
            TEXTURE_HEIGHT);

        // Render fuel burn progress (flame icon) using vanilla sprite
        int burnProgress = menu.getBurnProgress();
        if (burnProgress > 0) {
            int flameHeight = 13;
            int pixelsToShow = burnProgress;
            int yOffset = flameHeight - pixelsToShow;
            graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                LIT_PROGRESS_SPRITE,
                14,
                14,
                0,
                yOffset,
                leftPos + 19,
                topPos + 49 - pixelsToShow,
                14,
                pixelsToShow);
        }

        // Render glass tank level (2000 mB = 2 buckets)
        // Display box: x=46-61 (16px wide), y=46-68 (23px tall)
        // Overlay texture: x=200-215 (16px wide), y=59-81 (23px tall)
        // Scale: 1000 mB = 12px, 2000 mB = 24px, but we cap display at 23px
        // Tank appears visually full at 1916+ mB
        long glassAmount = menu.getGlassAmount();
        int tankHeight = Math.min(23, (int) (24 * glassAmount / 2000)); // Cap at 23px (hide top pixel)
        if (tankHeight > 0) {
            graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                leftPos + 46,
                topPos + 69 - tankHeight,
                200,
                82 - tankHeight,
                16,
                tankHeight,
                TEXTURE_WIDTH,
                TEXTURE_HEIGHT);
        }

        // Render melting arrow (fills down from top)
        // Display box: x=47-60 (13px wide), y=35-41 (6px tall)
        // Overlay texture: x=201-214 (13px wide), y=46-53 (7px tall)
        int meltProgress = menu.getMeltProgress();
        if (meltProgress > 0) {
            // Arrow fills from top to bottom over 2 seconds (40 ticks)
            int arrowHeight = Math.min(7, (7 * meltProgress) / 40);
            if (arrowHeight > 0) {
                graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    leftPos + 47,
                    topPos + 35,
                    201,
                    46,
                    13,
                    arrowHeight,
                    TEXTURE_WIDTH,
                    TEXTURE_HEIGHT);
            }
        }

        // Render annealing arrow (fills left to right)
        // Display box: x=128-156 (28px wide), y=21-39 (18px tall)
        // Overlay texture: x=187-215 (28px wide), y=21-39 (18px tall)
        int annealProgress = menu.getAnnealProgress();
        if (annealProgress > 0) {
            // Arrow fills from left to right based on percentage (0-100)
            int arrowWidth = Math.min(28, (28 * annealProgress) / 100);
            if (arrowWidth > 0) {
                graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    leftPos + 128,
                    topPos + 21,
                    187,
                    21,
                    arrowWidth,
                    18,
                    TEXTURE_WIDTH,
                    TEXTURE_HEIGHT);
            }
        }

        // Render temperature gauge with sliding window
        // Display box: x=16-37 (21px wide), y=18-28 (10px tall)
        // Source strip: x=0-244 (244px wide), y=174-184 (10px tall)
        int heat = menu.getHeat();

        // Calculate window position in the strip
        // At heat=0, window left edge at x=0
        // At heat=2000, window right edge at x=244 (left edge at x=223)
        int stripWidth = 244;
        int windowWidth = 21;
        int maxWindowLeft = stripWidth - windowWidth; // 223

        int windowLeft = (int) (maxWindowLeft * Math.min(heat, 2000) / 2000.0);

        // Render the window portion of the strip
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            TEXTURE,
            leftPos + 16,           // Destination X
            topPos + 18,            // Destination Y
            windowLeft,             // Source X (sliding window position)
            174,                    // Source Y
            windowWidth,            // Width
            10,                     // Height
            TEXTURE_WIDTH,
            TEXTURE_HEIGHT);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        renderTooltip(graphics, mouseX, mouseY);

    `@Override`
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        renderTooltip(graphics, mouseX, mouseY);
    }
    }
}
