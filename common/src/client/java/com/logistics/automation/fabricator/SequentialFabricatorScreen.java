package com.logistics.automation.fabricator;

import com.logistics.LogisticsMod;
import com.logistics.automation.fabricator.FabricatorProcessorComponent.Output;
import com.logistics.core.lib.platform.ClientNetworking;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * Client GUI for the Sequential Fabricator: a material grid on the left, an energy gauge and progress
 * arrow, and a grid of craftable-output buttons on the right. Clicking an output toggles it into the
 * build queue; active/queued state is shown by a red overlay on the button.
 */
public class SequentialFabricatorScreen extends AbstractContainerScreen<SequentialFabricatorScreenHandler> {

    private static final ResourceLocation TEXTURE =
            LogisticsMod.modId("textures/gui/automation/sequential_fabricator.png").toIdentifier();
    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;

    // Shared static energy-gauge sprite (drawn dark for empty + bright for fill), same as the other machines.
    private static final ResourceLocation CHARGE = LogisticsMod.modId("automation/charge").toIdentifier();

    // Output selection grid: 2 columns × 4 rows over the texture's baked slot cells.
    private static final int OUTPUT_COLS = 2;
    private static final int OUTPUT_ROWS = 4;
    private static final int OUTPUT_ORIGIN_X = 127;
    private static final int OUTPUT_ORIGIN_Y = 16;

    // Progress arrow: the filled sprite lives at UV (199, 35), 24×16, drawn over the baked arrow.
    private static final int ARROW_U = 199;
    private static final int ARROW_V = 35;
    private static final int ARROW_X = 97;
    private static final int ARROW_Y = 44;

    private final List<FabricatorOutputButton> outputButtons = new ArrayList<>();

    public SequentialFabricatorScreen(SequentialFabricatorScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 186;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = this.imageHeight - 94;
        outputButtons.clear();
        for (int row = 0; row < OUTPUT_ROWS; row++) {
            for (int col = 0; col < OUTPUT_COLS; col++) {
                int x = leftPos + OUTPUT_ORIGIN_X + col * FabricatorOutputButton.SIZE;
                int y = topPos + OUTPUT_ORIGIN_Y + row * FabricatorOutputButton.SIZE;
                FabricatorOutputButton button = new FabricatorOutputButton(x, y, this::onOutputClicked);
                outputButtons.add(button);
                addRenderableWidget(button);
            }
        }
        refreshOutputs();
    }

    private void onOutputClicked(FabricatorOutputButton button) {
        if (button.getRecipeId() != null) {
            ClientNetworking.send(new ToggleFabricatorSelectionPacket(
                    getMenu().getPos(), button.getRecipeId().toString()));
        }
    }

    private void refreshOutputs() {
        List<Output> outputs = getMenu().getClientOutputs();
        for (int i = 0; i < outputButtons.size(); i++) {
            FabricatorOutputButton button = outputButtons.get(i);
            if (i < outputs.size()) {
                Output output = outputs.get(i);
                button.setOutput(output.result(), output.id(), output.state());
                button.visible = true;
            } else {
                button.clear();
                button.visible = false;
            }
        }
    }

    /** Called by the client packet receiver with the latest craftable outputs. */
    public void updateOutputs(BlockPos pos, List<Output> outputs) {
        getMenu().setPos(pos);
        getMenu().setClientOutputs(outputs);
        refreshOutputs();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        // Energy gauge (same placement as the other machine GUIs): dark empty bar, bright fill from the bottom.
        graphics.setColor(0.25f, 0.25f, 0.25f, 1.0f);
        graphics.blitSprite(CHARGE, 12, 30, 0, 0, leftPos + 10, topPos + 19, 12, 30);
        graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        int energyHeight = getMenu().getEnergyBarHeight();
        if (energyHeight > 0) {
            graphics.blitSprite(CHARGE, 12, 30, 0, 30 - energyHeight,
                    leftPos + 10, topPos + 19 + (30 - energyHeight), 12, energyHeight);
        }

        // Progress arrow for the active recipe.
        int arrowWidth = getMenu().getProgressArrowWidth();
        if (arrowWidth > 0) {
            graphics.blit(TEXTURE, leftPos + ARROW_X, topPos + ARROW_Y,
                    ARROW_U, ARROW_V, arrowWidth, 16, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        for (FabricatorOutputButton button : outputButtons) {
            if (button.visible && button.isHoveredOrFocused() && !button.getItem().isEmpty()) {
                graphics.renderTooltip(this.font, button.getItem(), mouseX, mouseY);
                break;
            }
        }
    }
}
