package com.logistics.automation.crucible;

import com.logistics.LogisticsMod;
import com.logistics.core.lib.client.render.FluidBoxRenderer;
import com.logistics.core.lib.resource.ResourceId;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Client-side GUI screen for the Magma Crucible.
 */
public class MagmaCrucibleScreen extends AbstractContainerScreen<MagmaCrucibleScreenHandler> {

    private static final ResourceId TEXTURE = LogisticsMod.modId("textures/gui/automation/magma_crucible.png");
    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;

    private static final ResourceId CHARGE = LogisticsMod.modId("automation/charge");
    private static final int CHARGE_EMPTY_TINT = 0xFF404040;

    // Tank rectangle in the GUI (screen-local): (116,13) top-left to (131,70) bottom-right.
    private static final int TANK_LEFT = 116;
    private static final int TANK_TOP = 13;
    private static final int TANK_BOTTOM = 70;
    private static final int TANK_WIDTH = 15; // 131 - 116
    private static final int TANK_HEIGHT = 57; // 70 - 13
    // The overlay (glass/frame drawn over the fluid) sits 64px to the right of the tank in the texture.
    private static final int OVERLAY_U = TANK_LEFT + 64;

    public MagmaCrucibleScreen(MagmaCrucibleScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            TEXTURE.toIdentifier(),
            leftPos, topPos,
            0, 0,
            imageWidth, imageHeight,
            TEXTURE_WIDTH, TEXTURE_HEIGHT);

        int arrowWidth = menu.getProgressArrowWidth();
        if (arrowWidth > 0) {
            graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE.toIdentifier(),
                leftPos + 79, topPos + 35,
                199, 35,
                arrowWidth, 16,
                TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }

        graphics.blitSprite(
            RenderPipelines.GUI_TEXTURED,
            CHARGE.toIdentifier(),
            12, 30,
            0, 0,
            leftPos + 10, topPos + 19,
            12, 30,
            CHARGE_EMPTY_TINT);
        int energyHeight = menu.getEnergyBarHeight();
        if (energyHeight > 0) {
            graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                CHARGE.toIdentifier(),
                12, 30,
                0, 30 - energyHeight,
                leftPos + 10, topPos + 19 + (30 - energyHeight),
                12, energyHeight);
        }

        renderTank(graphics);
    }

    /**
     * Renders the tank contents: the fluid's still sprite tiled from the bottom up to the fill line (never
     * stretched), then the GUI overlay on top so the fluid reads as being behind the glass.
     */
    private void renderTank(GuiGraphicsExtractor graphics) {
        int fluidId = menu.getTankFluidId();
        float fraction = menu.getTankFillFraction();
        if (fluidId >= 0 && fraction > 0f) {
            Fluid fluid = BuiltInRegistries.FLUID.byId(fluidId);
            if (fluid != Fluids.EMPTY) {
                FluidBoxRenderer.Appearance appearance = FluidBoxRenderer.resolve(fluid, null, BlockPos.ZERO);
                if (appearance != null) {
                    TextureAtlasSprite sprite = appearance.sprite();
                    var atlas = sprite.atlasLocation();
                    float su0 = sprite.getU0();
                    float su1 = sprite.getU1();
                    float sv0 = sprite.getV0();
                    float sv1 = sprite.getV1();
                    int tileWidth = Math.min(16, TANK_WIDTH);
                    float u1 = su0 + (su1 - su0) * (tileWidth / 16f);
                    int fillPixels = Math.round(fraction * TANK_HEIGHT);
                    int drawn = 0;
                    while (drawn < fillPixels) {
                        int tileHeight = Math.min(16, fillPixels - drawn);
                        int screenY = topPos + TANK_BOTTOM - drawn - tileHeight;
                        // Show the bottom `tileHeight` texels of the sprite so each pixel is 1:1 (no stretch).
                        float v0 = sv1 - (sv1 - sv0) * (tileHeight / 16f);
                        graphics.blit(atlas, leftPos + TANK_LEFT, screenY, tileWidth, tileHeight, su0, v0, u1, sv1);
                        drawn += tileHeight;
                    }
                }
            }
        }

        // Overlay (glass/frame) — always drawn fully, on top of the fluid.
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            TEXTURE.toIdentifier(),
            leftPos + TANK_LEFT, topPos + TANK_TOP,
            OVERLAY_U, TANK_TOP,
            TANK_WIDTH, TANK_HEIGHT,
            TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }
}
