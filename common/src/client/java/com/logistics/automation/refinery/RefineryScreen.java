package com.logistics.automation.refinery;

import com.logistics.LogisticsMod;
import com.logistics.core.lib.client.render.FluidBoxRenderer;
import com.logistics.core.lib.fluids.FluidDisplay;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Client-side GUI screen for the Refinery: an input tank and an output tank flanking a progress arrow,
 * an energy bar, and the byproduct output slot.
 */
public class RefineryScreen extends AbstractContainerScreen<RefineryScreenHandler> {

    private static final ResourceLocation TEXTURE =
        LogisticsMod.modId("textures/gui/automation/refinery.png").toIdentifier();
    private static final ResourceLocation CHARGE = LogisticsMod.modId("automation/charge").toIdentifier();

    // Tank windows in the GUI texture (screen-local): 16px wide, y 18..75; the texture draws the frames.
    private static final int TANK_TOP = 18;
    private static final int TANK_BOTTOM = 76;
    private static final int TANK_WIDTH = 16;
    private static final int TANK_HEIGHT = TANK_BOTTOM - TANK_TOP; // 58
    private static final int INPUT_TANK_LEFT = 74;
    private static final int OUTPUT_TANK_LEFT = 147;

    public RefineryScreen(RefineryScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        hoverTooltip(graphics, mouseX, mouseY, INPUT_TANK_LEFT, TANK_TOP, TANK_WIDTH, TANK_HEIGHT,
                tankTooltip(menu.getInputFluidId(), menu.getInputAmountMb()));
        hoverTooltip(graphics, mouseX, mouseY, OUTPUT_TANK_LEFT, TANK_TOP, TANK_WIDTH, TANK_HEIGHT,
                tankTooltip(menu.getOutputFluidId(), menu.getOutputAmountMb()));
    }

    /** Shows {@code lines} as a tooltip while the mouse is within the given screen-local rect. */
    private void hoverTooltip(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, int w, int h, List<Component> lines) {
        if (isHovering(x, y, w, h, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
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
    protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        int arrowWidth = menu.getProgressArrowWidth();
        if (arrowWidth > 0) {
            graphics.blit(TEXTURE, leftPos + 105, topPos + 35, 199, 35, arrowWidth, 16);
        }

        graphics.setColor(0.25f, 0.25f, 0.25f, 1.0f);
        graphics.blitSprite(CHARGE, 12, 30, 0, 0, leftPos + 10, topPos + 19, 12, 30);
        graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        int energyHeight = menu.getEnergyBarHeight();
        if (energyHeight > 0) {
            graphics.blitSprite(CHARGE, 12, 30, 0, 30 - energyHeight,
                leftPos + 10, topPos + 19 + (30 - energyHeight), 12, energyHeight);
        }

        renderTank(graphics, INPUT_TANK_LEFT, menu.getInputFluidId(), menu.getInputFillFraction());
        renderTank(graphics, OUTPUT_TANK_LEFT, menu.getOutputFluidId(), menu.getOutputFillFraction());
    }

    /**
     * Renders one tank's fluid: the still sprite tiled from the window bottom up to the fill line (never
     * stretched, clipped to the fill rect) directly over the tank frame the GUI texture already draws.
     */
    private void renderTank(GuiGraphics graphics, int left, int fluidId, float fraction) {
        if (fluidId < 0 || fraction <= 0f) {
            return;
        }
        Fluid fluid = BuiltInRegistries.FLUID.byId(fluidId);
        if (fluid == Fluids.EMPTY) {
            return;
        }
        FluidBoxRenderer.Appearance appearance = FluidBoxRenderer.resolveForGui(fluid);
        if (appearance == null) {
            return;
        }
        TextureAtlasSprite sprite = appearance.sprite();
        int tint = FluidBoxRenderer.opaque(appearance.tint());
        float r = ((tint >> 16) & 0xFF) / 255f;
        float g = ((tint >> 8) & 0xFF) / 255f;
        float b = (tint & 0xFF) / 255f;
        int x0 = leftPos + left;
        int bottomY = topPos + TANK_BOTTOM;
        int fillPixels = Math.round(fraction * TANK_HEIGHT);
        graphics.enableScissor(x0, bottomY - fillPixels, x0 + TANK_WIDTH, bottomY);
        graphics.setColor(r, g, b, 1.0f);
        for (int drawn = 0; drawn < fillPixels; drawn += 16) {
            graphics.blit(x0, bottomY - drawn - 16, 0, 16, 16, sprite);
        }
        graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        graphics.disableScissor();
    }
}
