package com.logistics.automation.fabricator;

import com.logistics.LogisticsMod;
import com.logistics.core.lib.resource.ResourceId;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A craftable-output slot in the fabricator GUI, drawn over the texture's baked slot cell. Shows the
 * result item and, by selection state, a highlight sprite from the GUI texture: the active (building)
 * item and queued items use distinct sprites. Clicking toggles selection.
 */
public class FabricatorOutputButton extends AbstractWidget {

    public static final int SIZE = 18;

    private static final ResourceId TEXTURE =
            LogisticsMod.modId("textures/gui/automation/sequential_fabricator.png");
    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;

    // Highlight sprites baked into the GUI texture: active at (192,16), queued at (210,16), each 18×18.
    private static final int ACTIVE_U = 192;
    private static final int QUEUED_U = 210;
    private static final int HIGHLIGHT_V = 16;

    private ItemStack item = ItemStack.EMPTY;
    @Nullable private ResourceId recipeId;
    private int state = FabricatorProcessorComponent.STATE_AVAILABLE;
    private final Consumer<FabricatorOutputButton> clickHandler;

    public FabricatorOutputButton(int x, int y, Consumer<FabricatorOutputButton> clickHandler) {
        super(x, y, SIZE, SIZE, Component.empty());
        this.clickHandler = clickHandler;
    }

    public void setOutput(ItemStack item, ResourceId recipeId, int state) {
        this.item = item;
        this.recipeId = recipeId;
        this.state = state;
    }

    public void clear() {
        this.item = ItemStack.EMPTY;
        this.recipeId = null;
        this.state = FabricatorProcessorComponent.STATE_AVAILABLE;
    }

    public ItemStack getItem() {
        return item;
    }

    @Nullable
    public ResourceId getRecipeId() {
        return recipeId;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int highlightU = state == FabricatorProcessorComponent.STATE_ACTIVE ? ACTIVE_U
                : state == FabricatorProcessorComponent.STATE_QUEUED ? QUEUED_U : -1;
        if (highlightU >= 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE.toIdentifier(),
                    getX(), getY(), highlightU, HIGHLIGHT_V, SIZE, SIZE, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }
        if (!item.isEmpty()) {
            graphics.item(item, getX() + 1, getY() + 1);
            graphics.itemDecorations(Minecraft.getInstance().font, item, getX() + 1, getY() + 1, null);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
        if (active && visible && !item.isEmpty() && recipeId != null
                && isMouseOver(mouseButtonEvent.x(), mouseButtonEvent.y())) {
            clickHandler.accept(this);
            return true;
        }
        return super.mouseClicked(mouseButtonEvent, doubleClick);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        if (!item.isEmpty()) {
            defaultButtonNarrationText(output);
        }
    }
}
