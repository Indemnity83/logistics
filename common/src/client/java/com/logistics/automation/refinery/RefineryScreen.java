package com.logistics.automation.refinery;

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
 * Client-side GUI screen for the Refinery: an input tank and an output tank flanking a progress arrow,
 * an energy bar, and the byproduct output slot.
 */
public class RefineryScreen extends AbstractContainerScreen<RefineryScreenHandler> {

    private static final ResourceId TEXTURE = LogisticsMod.modId("textures/gui/automation/refinery.png");
    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;

    private static final ResourceId CHARGE = LogisticsMod.modId("automation/charge");
    private static final int CHARGE_EMPTY_TINT = 0xFF404040;

    private static final int TANK_TOP = 18;
    private static final int TANK_BOTTOM = 76;
    private static final int TANK_WIDTH = 16;
    private static final int TANK_HEIGHT = TANK_BOTTOM - TANK_TOP; // 58
    private static final int INPUT_TANK_LEFT = 26;
    private static final int OUTPUT_TANK_LEFT = 98;

    private static final int TANK_BORDER = 0xFF373737;
    private static final int TANK_BACKING = 0xFF101010;

    public RefineryScreen(RefineryScreenHandler handler, Inventory inventory, Component title) {
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
        hoverTooltip(graphics, mouseX, mouseY, INPUT_TANK_LEFT, TANK_TOP, TANK_WIDTH, TANK_HEIGHT,
                tankTooltip(menu.getInputFluidId(), menu.getInputAmountMb()));
        hoverTooltip(graphics, mouseX, mouseY, OUTPUT_TANK_LEFT, TANK_TOP, TANK_WIDTH, TANK_HEIGHT,
                tankTooltip(menu.getOutputFluidId(), menu.getOutputAmountMb()));
    }

    /** Shows {@code lines} as a tooltip while the mouse is within the given screen-local rect. */
    private void hoverTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int x, int y, int w, int h, List<Component> lines) {
        if (mouseX >= leftPos + x && mouseX < leftPos + x + w && mouseY >= topPos + y && mouseY < topPos + y + h) {
            graphics.setTooltipForNextFrame(this.font, lines, Optional.empty(), mouseX, mouseY);
        }
    }

    /** Vanilla item-style tank tooltip: fluid name, then amount, then the mod name in blue italic. */
    private List<Component> tankTooltip(int fluidId, int amountMb) {
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

        int arrowWidth = menu.getProgressArrowWidth();
        if (arrowWidth > 0) {
            graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE.toIdentifier(),
                leftPos + 56, topPos + 35,
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

        renderTank(graphics, INPUT_TANK_LEFT, menu.getInputFluidId(), menu.getInputFillFraction());
        renderTank(graphics, OUTPUT_TANK_LEFT, menu.getOutputFluidId(), menu.getOutputFillFraction());
    }

    /**
     * Renders one tank: a dark backing with a border so an empty tank is visible, then the fluid's still
     * sprite tiled from the bottom up to the fill line (never stretched, clipped to the fill rect).
     */
    private void renderTank(GuiGraphicsExtractor graphics, int left, int fluidId, float fraction) {
        int x0 = leftPos + left;
        int top = topPos + TANK_TOP;
        int bottomY = topPos + TANK_BOTTOM;
        graphics.fill(x0 - 1, top - 1, x0 + TANK_WIDTH + 1, bottomY + 1, TANK_BORDER);
        graphics.fill(x0, top, x0 + TANK_WIDTH, bottomY, TANK_BACKING);

        if (fluidId >= 0 && fraction > 0f) {
            Fluid fluid = BuiltInRegistries.FLUID.byId(fluidId);
            if (fluid != Fluids.EMPTY) {
                FluidBoxRenderer.Appearance appearance = FluidBoxRenderer.resolveForGui(fluid);
                if (appearance != null) {
                    TextureAtlasSprite sprite = appearance.sprite();
                    int tint = appearance.tint();
                    int fillPixels = Math.round(fraction * TANK_HEIGHT);
                    graphics.enableScissor(x0, bottomY - fillPixels, x0 + TANK_WIDTH, bottomY);
                    for (int drawn = 0; drawn < fillPixels; drawn += 16) {
                        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x0, bottomY - drawn - 16, 16, 16, tint);
                    }
                    graphics.disableScissor();
                }
            }
        }
    }
}
