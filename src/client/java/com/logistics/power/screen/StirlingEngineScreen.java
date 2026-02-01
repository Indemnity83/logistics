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
 * Displays fuel slot with burn progress flame.
 */
public class StirlingEngineScreen extends HandledScreen<StirlingEngineScreenHandler> {
    private static final Identifier BACKGROUND_TEXTURE =
            Identifier.of("logistics", "textures/gui/power/stirling_engine.png");

    // Reuse vanilla's lit flame sprite
    private static final Identifier LIT_PROGRESS_SPRITE = Identifier.ofVanilla("container/furnace/lit_progress");

    // Flame position (below the fuel slot)
    private static final int FLAME_X = 80;
    private static final int FLAME_Y = 22;

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

        // Draw flame progress
        if (handler.isBurning()) {
            int flameHeight = 14;
            float progress = handler.getBurnProgress();
            int pixelsToShow = (int) ((1.0f - progress) * (flameHeight - 1)) + 1;
            int yOffset = flameHeight - pixelsToShow;
            context.drawGuiTexture(
                    RenderPipelines.GUI_TEXTURED,
                    LIT_PROGRESS_SPRITE,
                    14,
                    14,
                    0,
                    yOffset,
                    x + FLAME_X,
                    y + FLAME_Y + yOffset,
                    14,
                    pixelsToShow);
        }
    }
}
