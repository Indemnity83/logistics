package com.logistics.power.screen;

import com.logistics.LogisticsMod;
import com.logistics.core.lib.client.render.FluidBoxRenderer;
import com.logistics.core.lib.fluids.FluidDisplay;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.power.engine.ui.ReactionEngineScreenHandler;
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
 * Client-side screen for the Reaction Engine: a reactant tank, a catalyst slot, and a progress arrow that
 * <b>fills as the reaction proceeds</b>. Bufferless, so there is no energy gauge — hovering the tank shows
 * the reactant, and the reaction status/output are shown on the Jade HUD.
 */
public class ReactionEngineScreen extends AbstractContainerScreen<ReactionEngineScreenHandler> {
    private static final ResourceId BACKGROUND_TEXTURE =
            LogisticsMod.modId("textures/gui/power/reaction_engine.png");
    private static final ResourceId ARROW = ResourceId.in("minecraft", "container/furnace/burn_progress");

    // Progress arrow (empty track baked into the background at panel 80,35; the vanilla arrow overlays it).
    private static final int ARROW_X = 80;
    private static final int ARROW_Y = 35;
    private static final int ARROW_WIDTH = 24;
    private static final int ARROW_HEIGHT = 16;

    // Reactant tank window (18x60 frame at panel 108,13; the 16x58 interior is inset 1px).
    private static final int TANK_LEFT = 109;
    private static final int TANK_TOP = 14;
    private static final int TANK_HEIGHT = 58;
    private static final int TANK_BOTTOM = TANK_TOP + TANK_HEIGHT;
    private static final int TANK_WIDTH = 16;

    public ReactionEngineScreen(ReactionEngineScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        hoverTooltip(graphics, mouseX, mouseY, TANK_LEFT, TANK_TOP, TANK_WIDTH, TANK_HEIGHT,
                tankTooltip(menu.getReactantFluidId(), menu.getReactantAmount()));
    }

    private void hoverTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int x, int y, int w, int h, List<Component> lines) {
        if (mouseX >= leftPos + x && mouseX < leftPos + x + w && mouseY >= topPos + y && mouseY < topPos + y + h) {
            graphics.setTooltipForNextFrame(this.font, lines, Optional.empty(), mouseX, mouseY);
        }
    }

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

    private static String modName(String namespace) {
        return namespace.isEmpty()
                ? namespace
                : Character.toUpperCase(namespace.charAt(0)) + namespace.substring(1);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED, BACKGROUND_TEXTURE.toIdentifier(),
                leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);

        renderTank(graphics, TANK_LEFT, menu.getReactantFluidId(), menu.getReactantFillFraction());

        int arrowWidth = menu.getProgressArrowWidth();
        if (arrowWidth > 0) {
            graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED, ARROW.toIdentifier(),
                    ARROW_WIDTH, ARROW_HEIGHT, 0, 0,
                    leftPos + ARROW_X, topPos + ARROW_Y, arrowWidth, ARROW_HEIGHT);
        }
    }

    /**
     * Renders the reactant's still sprite tiled from the tank-window bottom up to the fill line (never
     * stretched, clipped to the fill rect) over the tank frame the GUI texture already draws.
     */
    private void renderTank(GuiGraphicsExtractor graphics, int left, int fluidId, float fraction) {
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
        int tint = appearance.tint();
        int x0 = leftPos + left;
        int bottomY = topPos + TANK_BOTTOM;
        int fillPixels = Math.round(fraction * TANK_HEIGHT);
        graphics.enableScissor(x0, bottomY - fillPixels, x0 + TANK_WIDTH, bottomY);
        for (int drawn = 0; drawn < fillPixels; drawn += 16) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x0, bottomY - drawn - 16, 16, 16, tint);
        }
        graphics.disableScissor();
    }
}
