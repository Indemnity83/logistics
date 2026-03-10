package com.logistics.pipe.screen;

import com.logistics.core.lib.resource.ResourceId;
import com.logistics.pipe.ui.ChassisScreenHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.Map;

/**
 * Client-side screen for the Chassis Logistics Pipe GUI.
 * Renders a different background texture depending on the number of active module slots
 * (1 for MkI, 2 for MkII, 3 for MkIII, 4 for MkIV, 8 for MkV).
 */
public class ChassisScreen extends AbstractContainerScreen<ChassisScreenHandler> {
    private static final Map<Integer, ResourceId> BACKGROUNDS = Map.of(
            1, ResourceId.in("logistics", "textures/gui/pipe/chassis_mk1.png"),
            2, ResourceId.in("logistics", "textures/gui/pipe/chassis_mk2.png"),
            3, ResourceId.in("logistics", "textures/gui/pipe/chassis_mk3.png"),
            4, ResourceId.in("logistics", "textures/gui/pipe/chassis_mk4.png"),
            8, ResourceId.in("logistics", "textures/gui/pipe/chassis_mk5.png"));

    private static final ResourceId FALLBACK_BACKGROUND =
            ResourceId.in("logistics", "textures/gui/pipe/chassis_mk1.png");

    public ChassisScreen(ChassisScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 183;
        this.titleLabelY = 6;
        this.inventoryLabelY = 90;
    }

    // Mirror the slot positions from ChassisScreenHandler
    private static final int[] SLOT_X = {26, 26, 26, 26, 106, 106, 106, 106};
    private static final int[] SLOT_Y = {18, 36, 54, 72,  18,  36,  54,  72};
    private static final int BUTTON_SIZE = 10;
    private static final int BUTTON_GAP = 5;

    @Override
    protected void init() {
        super.init();
        for (int i = 0; i < menu.getSlotCount(); i++) {
            int bx = leftPos + SLOT_X[i] - BUTTON_GAP - BUTTON_SIZE;
            int by = topPos + SLOT_Y[i] + (18 - BUTTON_SIZE) / 2 - 1;
            addRenderableWidget(Button.builder(Component.literal("!"), btn -> {})
                    .bounds(bx, by, BUTTON_SIZE, BUTTON_SIZE)
                    .build());
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        ResourceId texture = BACKGROUNDS.getOrDefault(menu.getSlotCount(), FALLBACK_BACKGROUND);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                texture.toIdentifier(),
                leftPos,
                topPos,
                0,
                0,
                imageWidth,
                imageHeight,
                256,
                256);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
