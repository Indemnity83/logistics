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
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * The screen every farm opens. Soil slot frames are drawn from the spare region of the GUI texture
 * only when the farm lays soil; otherwise that area stays plain.
 * One button under the soil row starts a surveying farm, then shows or hides a working farm's area.
 * A tend farm's menu ({@link FarmMenu.Layout#TEND}) has neither: its slot area is drawn plain, with a
 * frame under each of its generic slots.
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

    private static final ResourceId GERMLING_GHOST = LogisticsMod.modId("forestry/germling_slot");
    private static final ResourceId SOIL_GHOST = LogisticsMod.modId("forestry/soil_slot");
    private static final float GHOST_ALPHA = 0.3f;

    // The vertical energy gauge (screen-local interior), 12x52.
    private static final int GAUGE_WIDTH = 12;
    private static final int GAUGE_HEIGHT = 52;
    private static final int GAUGE_TOP = 17;
    private static final int ENERGY_LEFT = 8;

    private static final int LAMP_LEFT = 100;
    private static final int LAMP_TOP = 56;
    private static final int LAMP_SIZE = 8;

    private static final int LABEL_COLOR = 0xFF404040;

    private static final int BUTTON_LEFT = 42;
    private static final int BUTTON_TOP = 54;
    private static final int BUTTON_WIDTH = 54;
    private static final int BUTTON_HEIGHT = 14;

    // Spare-region UVs in the GUI texture.
    private static final int SOIL_ROW_U = 176, SOIL_ROW_V = 0, SOIL_ROW_WIDTH = 72, SOIL_ROW_HEIGHT = 18;
    private static final int SOIL_ROW_X = 41, SOIL_ROW_Y = 34;
    private static final int LAMP_U = 206, LAMP_V = 30, LAMP_U_STEP = 10;

    // The tend layout: the ring layout's slot area painted over in the panel colour, and the lamp beside the gauge.
    private static final int PANEL_COLOR = 0xFFC6C6C6;
    private static final int SLOT_AREA_LEFT = 40, SLOT_AREA_TOP = 16, SLOT_AREA_RIGHT = 172, SLOT_AREA_BOTTOM = 72;
    private static final int SLOT_FRAME_SIZE = 18;
    private static final int TEND_LAMP_LEFT = 40;
    private static final int TEND_LAMP_TOP = 40;

    private Button stateButton;
    private FarmState shownState;
    private boolean shownPreview;
    private int shownClear = -1;
    private int shownFlagged = -1;
    private int shownProtected = -1;

    private final SideTabHost tabs;

    public FarmScreen(FarmMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.tabs = MachineTabs.machine(FarmScreenText.tabKey(title), menu, menu.controls(),
                new TabAnchor(() -> leftPos, () -> topPos, () -> imageWidth, () -> width, () -> height));
    }

    @Override
    protected void init() {
        super.init();
        stateButton = Button.builder(FarmScreenText.buttonLabel(menu.state(), menu.previewShown()), button -> {
                    if (minecraft != null && minecraft.gameMode != null) {
                        minecraft.gameMode.handleInventoryButtonClick(
                                menu.containerId, FarmScreenText.buttonId(menu.state()));
                    }
                })
                .bounds(leftPos + BUTTON_LEFT, topPos + BUTTON_TOP, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        shownState = null;
        updateButton();
        if (!tends()) {
            addRenderableWidget(stateButton);
        }
        addRenderableWidget(tabs.attach(SideTabsWidget::new));
    }

    private boolean tends() {
        return menu.layout() == FarmMenu.Layout.TEND;
    }

    private int lampLeft() {
        return tends() ? TEND_LAMP_LEFT : LAMP_LEFT;
    }

    private int lampTop() {
        return tends() ? TEND_LAMP_TOP : LAMP_TOP;
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top) {
        return tabs.clickedOutside(super.hasClickedOutside(mouseX, mouseY, left, top), mouseX, mouseY);
    }

    @Override
    public SideTabHost sideTabs() {
        return tabs;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButton();
    }

    /** The label and tooltip follow the synced state, preview and survey; rebuilt only when they change. */
    private void updateButton() {
        FarmState state = menu.state();
        boolean preview = menu.previewShown();
        int clear = menu.surveyClear();
        int flagged = menu.surveyFlagged();
        int skipped = menu.surveyProtected();
        if (state == shownState
                && preview == shownPreview
                && clear == shownClear
                && flagged == shownFlagged
                && skipped == shownProtected) {
            return;
        }
        shownState = state;
        shownPreview = preview;
        shownClear = clear;
        shownFlagged = flagged;
        shownProtected = skipped;
        stateButton.setMessage(FarmScreenText.buttonLabel(state, preview));
        stateButton.setTooltip(state == FarmState.SURVEY
                ? Tooltip.create(FarmScreenText.startTooltip(clear, flagged, skipped))
                : null);
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
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        hoverTooltip(graphics, mouseX, mouseY, ENERGY_LEFT, GAUGE_TOP, GAUGE_WIDTH, GAUGE_HEIGHT,
                Component.translatable("tooltip.logistics.farm.energy", menu.energy(), menu.energyCapacity()));
        hoverTooltip(graphics, mouseX, mouseY, lampLeft(), lampTop(), LAMP_SIZE, LAMP_SIZE,
                FarmScreenText.statusTooltip(
                        menu.status(), menu.tier(), menu.areaSize(), menu.plotsUsable(), menu.plotsBlocked()));
    }

    private void hoverTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int x, int y, int w, int h, Component line) {
        hoverTooltip(graphics, mouseX, mouseY, x, y, w, h, List.of(line));
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

        if (tends()) {
            renderTendSlots(graphics);
        } else if (menu.laysSoil()) {
            blitTexture(graphics, SOIL_ROW_X, SOIL_ROW_Y, SOIL_ROW_U, SOIL_ROW_V, SOIL_ROW_WIDTH, SOIL_ROW_HEIGHT);
        }

        renderEnergy(graphics);

        int lampU = LAMP_U + FarmGauges.lamp(menu.status()).ordinal() * LAMP_U_STEP;
        blitTexture(graphics, lampLeft(), lampTop(), lampU, LAMP_V, LAMP_SIZE, LAMP_SIZE);

        if (!tends()) {
            for (int i = 0; i < 4; i++) {
                renderGhost(graphics, menu.getSlot(FarmMachine.GERMLING_START + i), GERMLING_GHOST);
                renderGhost(graphics, menu.getSlot(FarmMachine.SOIL_START + i), SOIL_GHOST);
            }
        }
    }

    /** Paints over the ring layout's slots and frames each of the tend layout's own. */
    private void renderTendSlots(GuiGraphicsExtractor graphics) {
        graphics.fill(leftPos + SLOT_AREA_LEFT, topPos + SLOT_AREA_TOP,
                leftPos + SLOT_AREA_RIGHT, topPos + SLOT_AREA_BOTTOM, PANEL_COLOR);
        for (int i = 0; i < menu.layout().slots(); i++) {
            Slot slot = menu.getSlot(i);
            blitTexture(graphics, slot.x - 1, slot.y - 1, SOIL_ROW_U, SOIL_ROW_V, SLOT_FRAME_SIZE, SLOT_FRAME_SIZE);
        }
    }

    private void blitTexture(GuiGraphicsExtractor graphics, int x, int y, int u, int v, int w, int h) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED, TEXTURE.toIdentifier(),
                leftPos + x, topPos + y, u, v, w, h, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    /** Faded hint of what goes in an empty, active slot. */
    private void renderGhost(GuiGraphicsExtractor graphics, Slot slot, ResourceId sprite) {
        if (!slot.isActive() || slot.hasItem()) {
            return;
        }
        graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED, sprite.toIdentifier(),
                leftPos + slot.x, topPos + slot.y, 16, 16, GHOST_ALPHA);
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
