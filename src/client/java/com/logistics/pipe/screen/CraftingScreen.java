package com.logistics.pipe.screen;

import com.logistics.core.lib.resource.ResourceId;
import com.logistics.pipe.ui.CraftingScreenHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * Client-side screen for the Crafting Logistics Pipe GUI.
 * Displays a 3x3 ghost crafting grid, a ghost result slot, and a blocking mode toggle.
 */
public class CraftingScreen extends AbstractContainerScreen<CraftingScreenHandler> {
    private static final ResourceId BACKGROUND_TEXTURE =
            ResourceId.in("logistics", "textures/gui/pipe/crafting.png");

    private static final List<String> CRAFT_STATE_NAMES =
            List.of("gui.logistics.crafting.state.idle", "gui.logistics.crafting.state.active");

    private Button blockingButton;

    public CraftingScreen(CraftingScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.titleLabelY = 6;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();

        // Import recipe from adjacent autocrafter (only when backed by a pipe entity)
        if (menu.supportsAutocrafterImport()) {
            addRenderableWidget(Button.builder(
                    Component.translatable("gui.logistics.crafting.import"),
                    button -> {
                        if (minecraft != null && minecraft.gameMode != null) {
                            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1);
                        }
                    })
                    .bounds(leftPos + 100, topPos + 15, 68, 14)
                    .build());
        }

        // Blocking mode toggle button
        this.blockingButton = Button.builder(
                getBlockingText(),
                button -> {
                    if (minecraft != null && minecraft.gameMode != null) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
                    }
                })
                .bounds(leftPos + 100, topPos + 60, 68, 14)
                .build();
        addRenderableWidget(blockingButton);
    }

    private Component getBlockingText() {
        return menu.isBlocking()
                ? Component.translatable("gui.logistics.crafting.blocking.on")
                : Component.translatable("gui.logistics.crafting.blocking.off");
    }

    private Component getCraftStateText() {
        int state = menu.getCraftState();
        String key = state >= 0 && state < CRAFT_STATE_NAMES.size()
                ? CRAFT_STATE_NAMES.get(state)
                : CRAFT_STATE_NAMES.get(0);
        return Component.translatable(key);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.blit(
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
        // Update blocking button text before rendering so the label is current this frame
        blockingButton.setMessage(getBlockingText());
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        // Draw craft state label
        Component stateText = getCraftStateText();
        graphics.text(font, stateText, leftPos + 8, topPos + 58, 0x404040, false);

        extractTooltip(graphics, mouseX, mouseY);
    }
}
