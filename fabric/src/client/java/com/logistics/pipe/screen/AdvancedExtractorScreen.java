package com.logistics.pipe.screen;

import com.logistics.core.lib.resource.ResourceId;
import com.logistics.pipe.ui.AdvancedExtractorScreenHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Client-side screen for the Advanced Extractor filter GUI.
 * Displays 9 filter ghost slots and an Include/Exclude toggle button.
 */
public class AdvancedExtractorScreen extends AbstractContainerScreen<AdvancedExtractorScreenHandler> {
    private static final ResourceId BACKGROUND_TEXTURE =
            ResourceId.in("logistics", "textures/gui/pipe/provider.png");

    private CycleButton<Boolean> filterButton;

    public AdvancedExtractorScreen(AdvancedExtractorScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelY = 6;
        this.inventoryLabelY = 48;
    }

    @Override
    protected void init() {
        super.init();

        int buttonWidth = 75;
        int smallButtonHeight = 11;
        int filterButtonX = leftPos + 93;
        int filterButtonY = topPos + 4;

        this.filterButton = CycleButton.booleanBuilder(
                Component.translatable("gui.logistics.filter.exclude"),
                Component.translatable("gui.logistics.filter.include")
        ).displayOnlyValue()
        .create(
                filterButtonX,
                filterButtonY,
                buttonWidth,
                smallButtonHeight,
                Component.empty(),
                (button, newValue) -> {
                    if (minecraft != null && minecraft.gameMode != null) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
                    }
                });

        addRenderableWidget(filterButton);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        graphics.blit(
                BACKGROUND_TEXTURE.toIdentifier(),
                leftPos, topPos,
                0, 0,
                imageWidth, imageHeight,
                256, 256);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        filterButton.setValue(menu.isFilterInverted());
        renderTooltip(graphics, mouseX, mouseY);
    }
}
