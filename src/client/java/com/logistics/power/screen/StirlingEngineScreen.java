package com.logistics.power.screen;

import com.logistics.power.engine.ui.StirlingEngineScreenHandler;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Client-side screen for the Stirling Engine GUI.
 * Displays fuel slot, burn progress flame, heat gauge, energy, and output rate.
 *
 * BuildCraft-aligned display:
 * - Heat gauge: 0-250 (overheat at 100% = 250)
 * - Energy: displayed in RF (0-10,000 RF buffer)
 * - Output: PID-controlled 3-10 RF/tick
 */
public class StirlingEngineScreen extends HandledScreen<StirlingEngineScreenHandler> {
    // Uses vanilla furnace texture as placeholder until custom texture is created
    private static final Identifier BACKGROUND_TEXTURE = Identifier.ofVanilla("textures/gui/container/furnace.png");

    // Max heat value (BuildCraft-aligned)
    private static final int MAX_HEAT = 250;

    // Flame icon position and size (matches vanilla furnace positions)
    private static final int FLAME_X = 56;
    private static final int FLAME_Y = 36;
    private static final int FLAME_WIDTH = 14;
    private static final int FLAME_HEIGHT = 14;
    private static final int FLAME_TEXTURE_X = 176;
    private static final int FLAME_TEXTURE_Y = 0;

    // Heat gauge position and size (right side of GUI)
    private static final int HEAT_GAUGE_X = 152;
    private static final int HEAT_GAUGE_Y = 17;
    private static final int HEAT_GAUGE_WIDTH = 8;
    private static final int HEAT_GAUGE_HEIGHT = 52;
    private static final int HEAT_GAUGE_TEXTURE_X = 176;
    private static final int HEAT_GAUGE_TEXTURE_Y = 14;

    public StirlingEngineScreen(StirlingEngineScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        backgroundWidth = 176;
        backgroundHeight = 166;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        // Draw main background
        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                BACKGROUND_TEXTURE,
                x,
                y,
                0,
                0,
                backgroundWidth,
                backgroundHeight,
                256,
                256);

        // Draw flame progress (burns from top to bottom)
        if (handler.isBurning()) {
            float progress = handler.getBurnProgress();
            int burnedHeight = (int) (progress * FLAME_HEIGHT);
            context.drawTexture(
                    RenderPipelines.GUI_TEXTURED,
                    BACKGROUND_TEXTURE,
                    x + FLAME_X,
                    y + FLAME_Y + burnedHeight,
                    FLAME_TEXTURE_X,
                    FLAME_TEXTURE_Y + burnedHeight,
                    FLAME_WIDTH,
                    FLAME_HEIGHT - burnedHeight,
                    256,
                    256);
        }

        // Draw heat gauge (fills from bottom to top)
        float heatProgress = handler.getHeatProgress();
        if (heatProgress > 0) {
            int heatHeight = (int) (heatProgress * HEAT_GAUGE_HEIGHT);
            int heatY = HEAT_GAUGE_HEIGHT - heatHeight;
            context.drawTexture(
                    RenderPipelines.GUI_TEXTURED,
                    BACKGROUND_TEXTURE,
                    x + HEAT_GAUGE_X,
                    y + HEAT_GAUGE_Y + heatY,
                    HEAT_GAUGE_TEXTURE_X,
                    HEAT_GAUGE_TEXTURE_Y + heatY,
                    HEAT_GAUGE_WIDTH,
                    heatHeight,
                    256,
                    256);
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(textRenderer, title, titleX, titleY, 0x404040, false);
        context.drawText(
                textRenderer, playerInventoryTitle, playerInventoryTitleX, playerInventoryTitleY, 0x404040, false);

        // Draw energy amount (converted from 100 RF units to kRF display)
        int energy = handler.getEnergy();
        float energyKRF = energy / 10.0f;
        Text energyText = Text.literal(String.format("%.1f kRF", energyKRF));
        context.drawText(textRenderer, energyText, 8, 50, 0x404040, false);

        // Draw power (generation rate)
        float powerRate = handler.getGenerationRate();
        Text powerText = Text.literal(String.format("%.1f RF/t", powerRate));
        context.drawText(textRenderer, powerText, 8, 60, 0x404040, false);

        // Draw overheat warning if overheated
        if (handler.isOverheated()) {
            Text overheatText = Text.literal("OVERHEAT!");
            int textWidth = textRenderer.getWidth(overheatText);
            context.drawText(textRenderer, overheatText, (backgroundWidth - textWidth) / 2, 70, 0xFF0000, false);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawMouseoverTooltip(DrawContext context, int mouseX, int mouseY) {
        super.drawMouseoverTooltip(context, mouseX, mouseY);

        // Heat gauge tooltip
        if (isPointInHeatGauge(mouseX, mouseY)) {
            int heat = handler.getHeat();
            String status = handler.isOverheated() ? " (OVERHEAT)" : "";
            Text heatText = Text.literal("Heat: " + heat + "/" + MAX_HEAT + status);
            context.drawTooltip(textRenderer, heatText, mouseX, mouseY);
        }
    }

    private boolean isPointInHeatGauge(int mouseX, int mouseY) {
        int gaugeX = x + HEAT_GAUGE_X;
        int gaugeY = y + HEAT_GAUGE_Y;
        return mouseX >= gaugeX
                && mouseX < gaugeX + HEAT_GAUGE_WIDTH
                && mouseY >= gaugeY
                && mouseY < gaugeY + HEAT_GAUGE_HEIGHT;
    }
}
