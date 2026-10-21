package com.logistics.forestry.farm;

import com.logistics.LogisticsMod;
import com.logistics.core.lib.client.screen.tab.MachineTabs;
import com.logistics.core.lib.client.screen.tab.SideTabHost;
import com.logistics.core.lib.client.screen.tab.SideTabScreen;
import com.logistics.core.lib.client.screen.tab.SideTabsWidget;
import com.logistics.core.lib.client.screen.tab.TabAnchor;
import com.logistics.core.lib.resource.ResourceId;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;

/**
 * The screen every farm opens: the energy gauge on the left, the status lamp and its readout beside
 * it, and the farm's 3×5 inventory on the right, all laid out in the GUI texture.
 * The standard machine side tabs sit outside the image, with each farm type's own Info text.
 */
public class FarmScreen extends AbstractContainerScreen<FarmMenu> implements SideTabScreen {

    private static final ResourceId TEXTURE = LogisticsMod.modId("textures/gui/forestry/farm.png");
    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;

    private static final ResourceId CHARGE = LogisticsMod.modId("automation/charge");
    private static final int CHARGE_EMPTY_TINT = 0xFF404040;
    private static final int CHARGE_SPRITE_WIDTH = 12;
    private static final int CHARGE_SPRITE_HEIGHT = 30;

    // The vertical energy gauge (screen-local interior), 12x52.
    private static final int GAUGE_WIDTH = 12;
    private static final int GAUGE_HEIGHT = 52;
    private static final int GAUGE_TOP = 17;
    private static final int ENERGY_LEFT = 8;

    // The lamp sits in the texture's socket, centred over the status readout between gauge and grid.
    private static final int LAMP_LEFT = 46;
    private static final int LAMP_TOP = 18;
    private static final int LAMP_SIZE = 8;
    private static final int READOUT_CENTER = 50;
    private static final int READOUT_TOP = 31;
    private static final int READOUT_WIDTH = 54;
    private static final int READOUT_MAX_LINES = 4;
    private static final int READOUT_BOTTOM = 68;

    private static final int LABEL_COLOR = 0xFF404040;

    // Lamp sprites in the texture's spare region, in FarmGauges.Lamp order.
    private static final int LAMP_U = 176, LAMP_V = 0, LAMP_U_STEP = 10;

    private final SideTabHost tabs;

    public FarmScreen(FarmMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.tabs = MachineTabs.machine(FarmScreenText.tabKey(title), menu, menu.controls(),
                new TabAnchor(() -> leftPos, () -> topPos, () -> imageWidth, () -> width, () -> height));
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(tabs.attach(SideTabsWidget::new));
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top) {
        return tabs.clickedOutside(super.hasClickedOutside(mouseX, mouseY, left, top), mouseX, mouseY);
    }

    @Override
    public SideTabHost sideTabs() {
        return tabs;
    }

    /** The title is centred each frame; with the tier shown it arrives with the synced data. */
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // Tier hidden from the title while nothing can change it.
        // Component tieredTitle = FarmScreenText.title(this.title, menu.tier());
        Component tieredTitle = this.title;
        this.titleLabelX = (this.imageWidth - this.font.width(tieredTitle)) / 2;
        graphics.text(this.font, tieredTitle, this.titleLabelX, this.titleLabelY, LABEL_COLOR, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, LABEL_COLOR, false);
        renderReadout(graphics);
    }

    /** The status under the lamp, wrapped to the gap between gauge and grid; the tooltip has it in full. */
    private void renderReadout(GuiGraphicsExtractor graphics) {
        List<FormattedCharSequence> lines =
                font.split(FarmScreenText.status(menu.status(), menu.emptyPlots()), READOUT_WIDTH);
        int y = READOUT_TOP;
        for (FormattedCharSequence line : lines.subList(0, Math.min(lines.size(), READOUT_MAX_LINES))) {
            graphics.text(this.font, line, READOUT_CENTER - font.width(line) / 2, y, LABEL_COLOR, false);
            y += font.lineHeight;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        hoverTooltip(graphics, mouseX, mouseY, ENERGY_LEFT, GAUGE_TOP, GAUGE_WIDTH, GAUGE_HEIGHT,
                List.of(Component.translatable("tooltip.logistics.farm.energy", menu.energy(), menu.energyCapacity())));
        hoverTooltip(graphics, mouseX, mouseY, READOUT_CENTER - READOUT_WIDTH / 2, LAMP_TOP,
                READOUT_WIDTH, READOUT_BOTTOM - LAMP_TOP,
                FarmScreenText.statusTooltip(menu.status(), menu.areaSize(), menu.plotsTotal(), menu.emptyPlots()));
    }

    private void hoverTooltip(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, int x, int y, int w, int h, List<Component> lines) {
        if (mouseX >= leftPos + x && mouseX < leftPos + x + w && mouseY >= topPos + y && mouseY < topPos + y + h) {
            graphics.setTooltipForNextFrame(this.font, lines, Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        blitTexture(graphics, 0, 0, 0, 0, imageWidth, imageHeight);
        renderEnergy(graphics);

        int lampU = LAMP_U + FarmGauges.lamp(menu.status()).ordinal() * LAMP_U_STEP;
        blitTexture(graphics, LAMP_LEFT, LAMP_TOP, lampU, LAMP_V, LAMP_SIZE, LAMP_SIZE);
    }

    private void blitTexture(GuiGraphicsExtractor graphics, int x, int y, int u, int v, int w, int h) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED, TEXTURE.toIdentifier(),
                leftPos + x, topPos + y, u, v, w, h, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    /** The 12x30 charge sprite tiled bottom-up over the 52 px gauge: dimmed for the empty bar, then the fill. */
    private void renderEnergy(GuiGraphicsExtractor graphics) {
        renderChargeColumn(graphics, GAUGE_HEIGHT, CHARGE_EMPTY_TINT);
        int fill = FarmGauges.fillPixels(menu.energy(), menu.energyCapacity(), GAUGE_HEIGHT);
        if (fill > 0) {
            renderChargeColumn(graphics, fill, 0xFFFFFFFF);
        }
    }

    private void renderChargeColumn(GuiGraphicsExtractor graphics, int fill, int tint) {
        int fillTop = GAUGE_HEIGHT - fill;
        for (int segBottom = GAUGE_HEIGHT; segBottom > fillTop; segBottom -= CHARGE_SPRITE_HEIGHT) {
            int segTop = segBottom - CHARGE_SPRITE_HEIGHT;
            int drawTop = Math.max(segTop, fillTop);
            graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED, CHARGE.toIdentifier(),
                    CHARGE_SPRITE_WIDTH, CHARGE_SPRITE_HEIGHT,
                    0, drawTop - segTop,
                    leftPos + ENERGY_LEFT, topPos + GAUGE_TOP + drawTop,
                    GAUGE_WIDTH, segBottom - drawTop,
                    tint);
        }
    }
}
