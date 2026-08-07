package com.logistics.automation.transposer;

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
 * Client-side GUI screen for the Transposer: a small input slot over a larger output slot, a droplet
 * progress gauge, an energy bar, and the tank.
 */
public class TransposerScreen extends AbstractContainerScreen<TransposerScreenHandler> {

    private static final ResourceId TEXTURE = LogisticsMod.modId("textures/gui/automation/transposer.png");
    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;

    // Shared static energy-gauge bar (gui/sprites/automation/charge.png), drawn dark for empty + bright for fill.
    private static final ResourceId CHARGE = LogisticsMod.modId("automation/charge");
    private static final int CHARGE_EMPTY_TINT = 0xFF404040;

    // Droplet progress gauge: a gray "empty" frame always drawn, with a white "filled" frame revealed
    // left-to-right on top of it as progress advances (same technique as a growing progress arrow).
    // For a Fill recipe (draining the tank) the whole gauge — both frames — is mirrored in place so it
    // points left and the reveal sweeps right-to-left instead, showing the reversed material flow.
    private static final int GAUGE_X = 82, GAUGE_Y = 24;
    // The sprite itself is 23px wide (x199-221 inclusive), not the 24 it's easy to assume — grabbing 24
    // pulled in one extra column of plain background padding, which lands on the opposite edge once
    // mirrored and reads as the whole gauge shifting by a pixel between orientations.
    //
    // Height is 17, not 16: the filled frame's anti-aliased tip bleeds one row past the flat-colored
    // empty frame's extent, so 16 rows clipped its bottom pixel. The empty frame's extra row just
    // samples more of the surrounding panel background, which is seamless.
    private static final int GAUGE_WIDTH = 23, GAUGE_HEIGHT = 17;
    private static final int GAUGE_U = 199;
    private static final int GAUGE_EMPTY_V = 24;
    private static final int GAUGE_FILLED_V = 42;

    // Tank rectangle in the GUI (screen-local), matching the Crucible placeholder texture.
    private static final int TANK_LEFT = 116;
    private static final int TANK_TOP = 18;
    private static final int TANK_BOTTOM = 76;
    private static final int TANK_WIDTH = 16;
    private static final int TANK_HEIGHT = TANK_BOTTOM - TANK_TOP;
    private static final int OVERLAY_U = TANK_LEFT + 64;

    public TransposerScreen(TransposerScreenHandler handler, Inventory inventory, Component title) {
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

    private void hoverTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int x, int y, int w, int h, List<Component> lines) {
        if (mouseX >= leftPos + x && mouseX < leftPos + x + w && mouseY >= topPos + y && mouseY < topPos + y + h) {
            graphics.setTooltipForNextFrame(this.font, lines, Optional.empty(), mouseX, mouseY);
        }
    }

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

        // Energy gauge: dark "empty" bar full height, then the bright fill over the bottom `energyHeight` px.
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
     * Draws the droplet gauge: the gray empty frame, then the white filled frame clipped to the current
     * progress width. For a Fill recipe (draining the tank), both frames are mirrored in place so the
     * droplet points left instead of right — done by sampling the source region in reverse (negative
     * {@code srcWidth}) while the on-screen rectangle stays put, rather than transforming the pose
     * stack, so the gauge's screen-space footprint never changes.
     *
     * <p>The fill always reveals starting at the droplet's point and growing toward its round end,
     * regardless of orientation: unmirrored the point is the gauge's right edge (the sprite points
     * right); mirrored it's the left edge, since mirroring swaps which screen edge shows which part of
     * the shape.
     */
    private void renderProgressGauge(GuiGraphicsExtractor graphics) {
        int x = leftPos + GAUGE_X;
        int y = topPos + GAUGE_Y;
        boolean mirrored = !menu.isFillMode();

        blitGaugeSpan(graphics, x, GAUGE_WIDTH, y, GAUGE_EMPTY_V, mirrored, x);

        int fillWidth = menu.getProgressFillWidth();
        if (fillWidth > 0) {
            int destX = mirrored ? x : x + GAUGE_WIDTH - fillWidth;
            blitGaugeSpan(graphics, destX, fillWidth, y, GAUGE_FILLED_V, mirrored, x);
        }
    }

    /**
     * Blits a {@code destWidth}-wide slice of the gauge, {@code destOffsetFromLeft} pixels into the
     * gauge's footprint (which starts at {@code gaugeLeft}). When {@code mirrored}, the source region is
     * sampled in reverse (negative {@code srcWidth}) so the slice shows the corresponding mirrored
     * portion of the sprite, while the destination rectangle's width stays positive.
     */
    private void blitGaugeSpan(GuiGraphicsExtractor graphics, int destX, int destWidth, int y, int srcV, boolean mirrored, int gaugeLeft) {
        int offsetFromLeft = destX - gaugeLeft;
        int srcU = mirrored ? GAUGE_U + GAUGE_WIDTH - offsetFromLeft : GAUGE_U + offsetFromLeft;
        int srcWidth = mirrored ? -destWidth : destWidth;
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            TEXTURE.toIdentifier(),
            destX, y,
            srcU, srcV,
            destWidth, GAUGE_HEIGHT,
            srcWidth, GAUGE_HEIGHT,
            TEXTURE_WIDTH, TEXTURE_HEIGHT,
            -1);
    }

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
                    graphics.enableScissor(x0, bottomY - fillPixels, x0 + TANK_WIDTH, bottomY);
                    for (int drawn = 0; drawn < fillPixels; drawn += 16) {
                        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x0, bottomY - drawn - 16, 16, 16, tint);
                    }
                    graphics.disableScissor();
                }
            }
        }

        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            TEXTURE.toIdentifier(),
            leftPos + TANK_LEFT, topPos + TANK_TOP,
            OVERLAY_U, TANK_TOP,
            TANK_WIDTH, TANK_HEIGHT,
            TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }
}
