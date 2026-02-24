package com.logistics.core.fabricator;

import com.logistics.LogisticsCore;
import com.logistics.LogisticsMod;
import com.logistics.core.lib.resource.ResourceId;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Client-side GUI screen for the Kiln.
 * Renders the GUI texture and progress bars.
 */
public class KilnScreen extends AbstractContainerScreen<KilnScreenHandler> {

    private static final ResourceId TEXTURE = LogisticsMod.modId("textures/gui/core/kiln.png");

    // GUI texture dimensions (standard)
    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;

    // Reuse vanilla's lit flame sprite
    private static final ResourceId LIT_PROGRESS_SPRITE = ResourceId.in("minecraft", "container/furnace/lit_progress");

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
            TEXTURE.toIdentifier(),
            leftPos,
            topPos,
            0,
            0,
            imageWidth,
            imageHeight,
            TEXTURE_WIDTH,
            TEXTURE_HEIGHT);

        // Render fuel burn indicator (full flame when burning, simplified from progress bar)
        if (menu.isBurning()) {
            // Render at same position as old code when fully lit (topPos + 49 - 13)
            graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                LIT_PROGRESS_SPRITE.toIdentifier(),
                14,
                14,
                0,
                0,
                leftPos + 19,
                topPos + 36,
                14,
                13);
        }

        // Render glass tank level using actual fluid texture (2000 mB = 2 buckets)
        // Display box: x=46-61 (16px wide), y=46-68 (23px tall)
        // Scale: 1000 mB = 12px, 2000 mB = 24px, but we cap display at 23px
        // Tank appears visually full at 1916+ mB
        long glassAmount = menu.getGlassAmount();
        int tankHeight = Math.min(23, (int) (24 * glassAmount / 2000)); // Cap at 23px (hide top pixel)
        if (tankHeight > 0) {
            // Render fluid texture
            FluidVariant moltenGlass = FluidVariant.of(LogisticsCore.FLUID.MOLTEN_GLASS_STILL);
            renderFluid(graphics, moltenGlass, leftPos + 46, topPos + 69 - tankHeight, 16, tankHeight);

            // Render overlay markings on top
            // Overlay texture: x=200-215 (16px wide), y=59-81 (23px tall)
            graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE.toIdentifier(),
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
                    TEXTURE.toIdentifier(),
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
                    TEXTURE.toIdentifier(),
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
        int temperature = menu.getTemperature();

        // Calculate window position in the strip
        // At temp=0, window left edge at x=0
        // At temp=2000, window right edge at x=244 (left edge at x=223)
        int stripWidth = 244;
        int windowWidth = 21;
        int maxWindowLeft = stripWidth - windowWidth; // 223

        int windowLeft = (int) (maxWindowLeft * Math.min(temperature, 2000) / 2000.0);

        // Render the window portion of the strip
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            TEXTURE.toIdentifier(),
            leftPos + 16,           // Destination X
            topPos + 18,            // Destination Y
            windowLeft,             // Source X (sliding window position)
            174,                    // Source Y
            windowWidth,            // Width
            10,                     // Height
            TEXTURE_WIDTH,
            TEXTURE_HEIGHT);
    }

    /**
     * Renders a fluid texture scaled to fill the specified area.
     * The fluid texture will tile and animate if the fluid has animation frames.
     *
     * @param graphics the GuiGraphics context
     * @param fluid the fluid variant to render
     * @param x the left edge of the render area
     * @param y the top edge of the render area
     * @param width the width of the render area
     * @param height the height of the render area
     */
    private void renderFluid(GuiGraphics graphics, FluidVariant fluid, int x, int y, int width, int height) {
        TextureAtlasSprite sprite = FluidVariantRendering.getSprite(fluid);
        if (sprite == null) {
            return;
        }

        int color = FluidVariantRendering.getColor(fluid);

        // Get sprite dimensions in pixels (usually 16x16 for fluids)
        int spriteWidth = sprite.contents().width();
        int spriteHeight = sprite.contents().height();

        // Calculate UV coordinates for the sprite in the atlas
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

        // Calculate atlas dimensions in pixels
        int atlasWidth = (int) (spriteWidth / (u1 - u0));
        int atlasHeight = (int) (spriteHeight / (v1 - v0));

        // Tile vertically to fill the area
        int fullTiles = height / spriteHeight;
        int remainingHeight = height % spriteHeight;

        // Render full tiles
        for (int i = 0; i < fullTiles; i++) {
            graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                sprite.atlasLocation(),
                x,
                y + i * spriteHeight,
                (int) (u0 * atlasWidth),      // source X in atlas
                (int) (v0 * atlasHeight),     // source Y in atlas
                width,
                spriteHeight,
                atlasWidth,
                atlasHeight
            );
        }

        // Render partial tile at the bottom if needed
        if (remainingHeight > 0) {
            graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                sprite.atlasLocation(),
                x,
                y + fullTiles * spriteHeight,
                (int) (u0 * atlasWidth),
                (int) (v0 * atlasHeight),
                width,
                remainingHeight,
                atlasWidth,
                atlasHeight
            );
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
