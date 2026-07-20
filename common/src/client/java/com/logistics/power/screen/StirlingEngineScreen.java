package com.logistics.power.screen;

import com.logistics.LogisticsMod;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.power.engine.ui.StirlingEngineScreenHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Client-side screen for the Stirling Engine: a fuel slot, an energy buffer gauge, and a burn flame that
 * starts full and crops down as the current fuel item is consumed.
 */
public class StirlingEngineScreen extends AbstractContainerScreen<StirlingEngineScreenHandler> {
    private static final ResourceId BACKGROUND_TEXTURE =
            LogisticsMod.modId("textures/gui/power/stirling_engine.png");

    private static final ResourceId CHARGE = LogisticsMod.modId("automation/charge");
    private static final ResourceId FLAME = ResourceId.in("minecraft", "container/furnace/lit_progress");

    // Energy gauge frame is 14x32; the charge fill is its 12x30 interior.
    private static final int ENERGY_LEFT = 155;
    private static final int ENERGY_TOP = 19;
    private static final int ENERGY_WIDTH = 12;
    private static final int ENERGY_HEIGHT = 30;

    // Burn flame, directly below the energy gauge.
    private static final int FLAME_X = 154;
    private static final int FLAME_Y = 53;
    private static final int FLAME_SIZE = 14;

    public StirlingEngineScreen(StirlingEngineScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics context, float delta, int mouseX, int mouseY) {
        context.blit(
                RenderPipelines.GUI_TEXTURED, BACKGROUND_TEXTURE.toIdentifier(),
                leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);

        int energyHeight = menu.getEnergyBarHeight(ENERGY_HEIGHT);
        if (energyHeight > 0) {
            context.blitSprite(
                    RenderPipelines.GUI_TEXTURED, CHARGE.toIdentifier(),
                    ENERGY_WIDTH, ENERGY_HEIGHT, 0, ENERGY_HEIGHT - energyHeight,
                    leftPos + ENERGY_LEFT, topPos + ENERGY_TOP + (ENERGY_HEIGHT - energyHeight), ENERGY_WIDTH, energyHeight);
        }

        renderFlame(context, fuelRemaining());
    }

    /** Remaining burn of the current fuel item (1 just after ignition, 0 when spent). */
    private float fuelRemaining() {
        int fuelTime = menu.getFuelTime();
        return fuelTime <= 0 ? 0f : (float) menu.getBurnTime() / fuelTime;
    }

    /** Flame starts full and crops from the top down to zero as the fuel is consumed. */
    private void renderFlame(GuiGraphics context, float fraction) {
        if (fraction <= 0f) {
            return;
        }
        int shown = Math.max(1, Math.round(fraction * FLAME_SIZE));
        int yOffset = FLAME_SIZE - shown;
        context.blitSprite(
                RenderPipelines.GUI_TEXTURED, FLAME.toIdentifier(),
                FLAME_SIZE, FLAME_SIZE, 0, yOffset,
                leftPos + FLAME_X, topPos + FLAME_Y + yOffset, FLAME_SIZE, shown);
    }
}
