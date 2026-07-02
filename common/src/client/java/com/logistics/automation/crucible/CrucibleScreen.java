package com.logistics.automation.crucible;

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
 * Client-side GUI screen for the Crucible.
 */
public class CrucibleScreen extends AbstractContainerScreen<CrucibleScreenHandler> {

    private static final ResourceLocation TEXTURE =
        LogisticsMod.modId("textures/gui/automation/crucible.png").toIdentifier();
    private static final ResourceLocation CHARGE = LogisticsMod.modId("automation/charge").toIdentifier();

    // Tank rectangle in the GUI (screen-local): (116,18) top-left to (131,75) bottom-right.
    private static final int TANK_LEFT = 116;
    private static final int TANK_TOP = 18;
    private static final int TANK_BOTTOM = 76;
    private static final int TANK_WIDTH = 16;
    private static final int TANK_HEIGHT = TANK_BOTTOM - TANK_TOP; // 58
    // The overlay (glass/frame drawn over the fluid) sits 64px to the right of the tank in the texture.
    private static final int OVERLAY_U = TANK_LEFT + 64;

    public CrucibleScreen(CrucibleScreenHandler handler, Inventory inventory, Component title) {
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
    protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        int arrowWidth = menu.getProgressArrowWidth();
        if (arrowWidth > 0) {
            graphics.blit(TEXTURE, leftPos + 79, topPos + 35, 199, 35, arrowWidth, 16);
        }

        graphics.setColor(0.25f, 0.25f, 0.25f, 1.0f);
        graphics.blitSprite(CHARGE, 12, 30, 0, 0, leftPos + 10, topPos + 19, 12, 30);
        graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        int energyHeight = menu.getEnergyBarHeight();
        if (energyHeight > 0) {
            graphics.blitSprite(CHARGE, 12, 30, 0, 30 - energyHeight,
                leftPos + 10, topPos + 19 + (30 - energyHeight), 12, energyHeight);
        }

        renderTank(graphics);
    }

    /**
     * Renders the tank contents: the fluid's still sprite tiled from the bottom up to the fill line (never
     * stretched), then the GUI overlay on top so the fluid reads as being behind the glass.
     */
    private void renderTank(GuiGraphics graphics) {
        int fluidId = menu.getTankFluidId();
        float fraction = menu.getTankFillFraction();
        if (fluidId >= 0 && fraction > 0f) {
            Fluid fluid = BuiltInRegistries.FLUID.byId(fluidId);
            if (fluid != Fluids.EMPTY) {
                FluidBoxRenderer.Appearance appearance = FluidBoxRenderer.resolveForGui(fluid);
                if (appearance != null) {
                    TextureAtlasSprite sprite = appearance.sprite();
                    int tint = FluidBoxRenderer.opaque(appearance.tint());
                    float r = ((tint >> 16) & 0xFF) / 255f;
                    float g = ((tint >> 8) & 0xFF) / 255f;
                    float b = (tint & 0xFF) / 255f;
                    int fillPixels = Math.round(fraction * TANK_HEIGHT);
                    int x0 = leftPos + TANK_LEFT;
                    int bottomY = topPos + TANK_BOTTOM;
                    // Clip to the filled rect, then tile full 16px sprites bottom-up (never stretched).
                    graphics.enableScissor(x0, bottomY - fillPixels, x0 + TANK_WIDTH, bottomY);
                    graphics.setColor(r, g, b, 1.0f);
                    for (int drawn = 0; drawn < fillPixels; drawn += 16) {
                        graphics.blit(x0, bottomY - drawn - 16, 0, 16, 16, sprite);
                    }
                    graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
                    graphics.disableScissor();
                }
            }
        }

        // Overlay (glass/frame) — always drawn fully, on top of the fluid.
        graphics.blit(TEXTURE, leftPos + TANK_LEFT, topPos + TANK_TOP, OVERLAY_U, TANK_TOP, TANK_WIDTH, TANK_HEIGHT);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(TANK_LEFT, TANK_TOP, TANK_WIDTH, TANK_HEIGHT, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, tankTooltip(), mouseX, mouseY);
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
}
