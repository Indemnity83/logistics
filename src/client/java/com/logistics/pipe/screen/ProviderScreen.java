package com.logistics.pipe.screen;

import com.logistics.core.lib.resource.ResourceId;
import com.logistics.pipe.ui.ProviderScreenHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Client-side screen for the Provider GUI.
 * Displays mode selection to control which items are available for extraction.
 */
public class ProviderScreen extends AbstractContainerScreen<ProviderScreenHandler> {
    private static final ResourceId BACKGROUND_TEXTURE =
            ResourceId.in("logistics", "textures/gui/pipe/provider.png");

    private Button modeButton;
    private CycleButton<Boolean> filterButton;

    public ProviderScreen(ProviderScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.titleLabelY = 6;
        this.inventoryLabelY = 48;
    }

    @Override
    protected void init() {
        super.init();

        int buttonWidth = 75;
        int buttonHeight = 20;
        int smallButtonHeight = 11;

        // Create filter Include/Exclude toggle button (top right, aligned with filter slots)
        int filterButtonX = leftPos + 93; // Right-aligned with 9 filter slots (8 + 162 - 75)
        int filterButtonY = topPos + 4;

        this.filterButton = CycleButton.booleanBuilder(
                Component.translatable("gui.logistics.filter.exclude"),
                Component.translatable("gui.logistics.filter.include"),
                menu.isFilterInverted()
        ).displayOnlyValue()
        .create(
                filterButtonX,
                filterButtonY,
                buttonWidth,
                smallButtonHeight,
                Component.empty(),
                (button, newValue) -> {
                    // Send button click to server (button ID = 1)
                    if (minecraft != null && minecraft.gameMode != null) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1);
                    }
                }
        );

        addRenderableWidget(filterButton);

        // Create mode selection button (right side, below filter slots)
        int modeButtonX = leftPos + 93;
        int modeButtonY = topPos + 37;

        this.modeButton = Button.builder(
                getModeButtonText(),
                button -> {
                    // Send button click to server (button ID = 0)
                    if (minecraft != null && minecraft.gameMode != null) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
                    }
                })
                .bounds(modeButtonX, modeButtonY, buttonWidth, buttonHeight)
                .build();

        addRenderableWidget(modeButton);
    }

    private Component getModeButtonText() {
        return Component.translatable(getModeName(menu.getMode()));
    }

    private String getModeName(int mode) {
        return switch (mode) {
            case 0 -> "gui.logistics.provider.mode.supply";
            case 1 -> "gui.logistics.provider.mode.reserve";
            case 2 -> "gui.logistics.provider.mode.guarded";
            case 3 -> "gui.logistics.provider.mode.seeded";
            case 4 -> "gui.logistics.provider.mode.sample";
            default -> "gui.logistics.provider.mode.supply";
        };
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // Draw background texture
        context.blit(
                RenderPipelines.GUI_TEXTURED,
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
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        // Update button text in case mode changed
        modeButton.setMessage(getModeButtonText());

        // Update filter button value in case it changed
        filterButton.setValue(menu.isFilterInverted());

        extractTooltip(graphics, mouseX, mouseY);
    }
}
