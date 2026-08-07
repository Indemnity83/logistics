package com.logistics.automation.crucible;

import com.logistics.LogisticsMod;
import com.logistics.core.lib.client.render.FluidBoxRenderer;
import com.logistics.core.lib.fluids.FluidDisplay;
import com.logistics.core.lib.resource.ResourceId;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Client-side GUI screen for the Crucible.
 */
public class CrucibleScreen extends AbstractContainerScreen<CrucibleScreenHandler> {

    private static final ResourceId TEXTURE = LogisticsMod.modId("textures/gui/automation/crucible.png");
    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;

    private static final ResourceId CHARGE = LogisticsMod.modId("automation/charge");
    private static final int CHARGE_EMPTY_TINT = 0xFF404040;

    // Droplet progress gauge: the empty (gray) droplet is baked into the main background art itself at
    // this position, always visible; only the filled (white) overlay needs drawing, revealed from the
    // droplet's point (its left edge) growing toward its round end as progress advances. Unlike the
    // Transposer's gauge, this one never reverses direction, so there's no mirroring.
    private static final int GAUGE_X = 80, GAUGE_Y = 34;
    private static final int GAUGE_WIDTH = 25, GAUGE_HEIGHT = 17;
    private static final int GAUGE_U = 200, GAUGE_V = 34;

    // Tank rectangle in the GUI (screen-local): (116,18) top-left to (131,75) bottom-right
    // (nudged 5px down from the design origin so the title doesn't overlap the tank).
    private static final int TANK_LEFT = 116;
    private static final int TANK_TOP = 18;
    private static final int TANK_BOTTOM = 76; // 1px taller on the bottom
    private static final int TANK_WIDTH = 16; // 1px wider on the right
    private static final int TANK_HEIGHT = TANK_BOTTOM - TANK_TOP; // 58
    // The overlay (glass/frame drawn over the fluid) sits 64px to the right of the tank in the texture.
    private static final int OVERLAY_U = TANK_LEFT + 64;

    public CrucibleScreen(CrucibleScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        hoverTooltip(graphics, mouseX, mouseY, TANK_LEFT, TANK_TOP, TANK_WIDTH, TANK_HEIGHT, tankTooltip());
    }

    /** Shows {@code lines} as a tooltip while the mouse is within the given screen-local rect. */
    private void hoverTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int x, int y, int w, int h, List<Component> lines) {
        if (mouseX >= leftPos + x && mouseX < leftPos + x + w && mouseY >= topPos + y && mouseY < topPos + y + h) {
            graphics.setTooltipForNextFrame(this.font, lines, Optional.empty(), mouseX, mouseY);
        }
    }

    /** Vanilla item-style tank tooltip: fluid name, then amount, then the mod name in blue italic. */
    private List<Component> tankTooltip() {
        int fluidId = menu.getTankFluidId();
        int amountMb = menu.getTankAmountMb();
        if (fluidId < 0 || amountMb <= 0) {
            return List.of(Component.translatable("tooltip.logistics.fluid.tank_empty"));
        }
        Fluid fluid = BuiltInRegistries.FLUID.byId(fluidId);
        var id = BuiltInRegistries.FLUID.getKey(fluid);
        Component amount = Component.translatable("tooltip.logistics.fluid.tank_amount", amountMb)
                .withStyle(ChatFormatting.GRAY);
        Component mod = Component.literal(modName(id.getNamespace()))
                .withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC);
        return List.of(FluidDisplay.name(fluid), amount, mod);
    }

    /** Best-effort friendly mod name from a namespace (first letter capitalised). */
    private static String modName(String namespace) {
        return namespace.isEmpty()
                ? namespace
                : Character.toUpperCase(namespace.charAt(0)) + namespace.substring(1);
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

        renderProgressGauge(graphics);

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
     * Draws the filled portion of the droplet gauge on top of the empty droplet already baked into the
     * background, revealed from its point (left edge) growing toward its round end (right edge) as
     * progress advances.
     */
    private void renderProgressGauge(GuiGraphicsExtractor graphics) {
        int fillWidth = menu.getProgressFillWidth();
        if (fillWidth <= 0) {
            return;
        }
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            TEXTURE.toIdentifier(),
            leftPos + GAUGE_X, topPos + GAUGE_Y,
            GAUGE_U, GAUGE_V,
            fillWidth, GAUGE_HEIGHT,
            TEXTURE_WIDTH, TEXTURE_HEIGHT);
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
                FluidBoxRenderer.Appearance appearance = FluidBoxRenderer.resolveForGui(fluid);
                if (appearance != null) {
                    TextureAtlasSprite sprite = appearance.sprite();
                    int tint = appearance.tint();
                    int fillPixels = Math.round(fraction * TANK_HEIGHT);
                    int x0 = leftPos + TANK_LEFT;
                    int bottomY = topPos + TANK_BOTTOM;
                    // Clip to the filled rect, then tile full 16px sprites bottom-up (never stretched); the
                    // scissor trims the top tile to the fill line and the tank width. blitSprite carries the
                    // fluid's animation frame and applies the tint (grayscale water becomes blue).
                    graphics.enableScissor(x0, bottomY - fillPixels, x0 + TANK_WIDTH, bottomY);
                    for (int drawn = 0; drawn < fillPixels; drawn += 16) {
                        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x0, bottomY - drawn - 16, 16, 16, tint);
                    }
                    graphics.disableScissor();
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
