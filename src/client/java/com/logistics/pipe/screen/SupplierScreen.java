package com.logistics.pipe.screen;

import com.logistics.core.lib.resource.ResourceId;
import com.logistics.pipe.ui.SupplierScreenHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Client-side screen for the Supplier GUI.
 * Displays 9 supply slots with item and target amount, plus mode selection.
 */
public class SupplierScreen extends AbstractContainerScreen<SupplierScreenHandler> {
    private static final ResourceId BACKGROUND_TEXTURE =
            ResourceId.in("logistics", "textures/gui/pipe/supplier.png");

    private Button modeButton;

    public SupplierScreen(SupplierScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelY = 6;
        this.inventoryLabelY = 48;
    }

    @Override
    protected void init() {
        super.init();

        // Create mode selection button
        int buttonWidth = 75;
        int buttonHeight = 20;
        int buttonX = leftPos + 93;
        int buttonY = topPos + 37;

        this.modeButton = Button.builder(
                getModeButtonText(),
                button -> {
                    // Send button click to server (button ID = 0)
                    if (minecraft != null && minecraft.gameMode != null) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
                    }
                })
                .bounds(buttonX, buttonY, buttonWidth, buttonHeight)
                .build();

        addRenderableWidget(modeButton);
    }

    private Component getModeButtonText() {
        return Component.translatable(getModeName(menu.getMode()));
    }

    private String getModeName(int mode) {
        return switch (mode) {
            case 0 -> "gui.logistics.mode.stocked";
            case 1 -> "gui.logistics.mode.infinite";
            case 2 -> "gui.logistics.mode.partial";
            case 3 -> "gui.logistics.mode.full";
            default -> "gui.logistics.mode.partial";
        };
    }

    @Override
    protected void renderBg(GuiGraphics context, float delta, int mouseX, int mouseY) {
        // Draw background texture
        // MC 1.21.1: blit() without RenderPipeline param
        context.blit(
                BACKGROUND_TEXTURE.toIdentifier(),
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

        // Update button text in case mode changed
        modeButton.setMessage(getModeButtonText());

        renderTooltip(graphics, mouseX, mouseY);
    }
}
