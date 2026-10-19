package com.logistics.forestry.farm;

import com.logistics.LogisticsMod;
import com.logistics.core.lib.client.render.FluidBoxRenderer;
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
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.material.Fluids;

/**
 * The screen every farm opens. Soil slot frames and the water gauge are drawn from the spare region
 * of the GUI texture only when the farm lays soil or has a tank; otherwise those areas stay plain.
 * One button under the soil row starts a surveying farm, then shows or hides a working farm's area.
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

    // Vertical gauges (screen-local interiors); both 12x52.
    private static final int GAUGE_WIDTH = 12;
    private static final int GAUGE_HEIGHT = 52;
    private static final int GAUGE_TOP = 17;
    private static final int ENERGY_LEFT = 8;
    private static final int WATER_LEFT = 24;

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
    private static final int TANK_FRAME_U = 176, TANK_FRAME_V = 20;
    private static final int TANK_TICKS_U = 192, TANK_TICKS_V = 21;
    private static final int LAMP_U = 206, LAMP_V = 30, LAMP_U_STEP = 10;

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
        addRenderableWidget(stateButton);
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

    /** The title carries the tier, which arrives with the synced data, so it is centred each frame. */
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Component tieredTitle = FarmScreenText.title(this.title, menu.tier());
        this.titleLabelX = (this.imageWidth - this.font.width(tieredTitle)) / 2;
        graphics.text(this.font, tieredTitle, this.titleLabelX, this.titleLabelY, LABEL_COLOR, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, LABEL_COLOR, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        hoverTooltip(graphics, mouseX, mouseY, ENERGY_LEFT, GAUGE_TOP, GAUGE_WIDTH, GAUGE_HEIGHT,
                Component.translatable("tooltip.logistics.farm.energy", menu.energy(), menu.energyCapacity()));
        if (hasTank()) {
            hoverTooltip(graphics, mouseX, mouseY, WATER_LEFT, GAUGE_TOP, GAUGE_WIDTH, GAUGE_HEIGHT,
                    Component.translatable("tooltip.logistics.farm.water", menu.waterMb(), menu.waterCapacityMb()));
        }
        hoverTooltip(graphics, mouseX, mouseY, LAMP_LEFT, LAMP_TOP, LAMP_SIZE, LAMP_SIZE,
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

    private boolean hasTank() {
        return menu.waterCapacityMb() > 0;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        blitTexture(graphics, 0, 0, 0, 0, imageWidth, imageHeight);

        if (menu.laysSoil()) {
            blitTexture(graphics, SOIL_ROW_X, SOIL_ROW_Y, SOIL_ROW_U, SOIL_ROW_V, SOIL_ROW_WIDTH, SOIL_ROW_HEIGHT);
        }

        renderEnergy(graphics);
        if (hasTank()) {
            renderWater(graphics);
        }

        int lampU = LAMP_U + FarmGauges.lamp(menu.status()).ordinal() * LAMP_U_STEP;
        blitTexture(graphics, LAMP_LEFT, LAMP_TOP, lampU, LAMP_V, LAMP_SIZE, LAMP_SIZE);

        for (int i = 0; i < 4; i++) {
            renderGhost(graphics, menu.getSlot(FarmMachine.GERMLING_START + i), GERMLING_GHOST);
            renderGhost(graphics, menu.getSlot(FarmMachine.SOIL_START + i), SOIL_GHOST);
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

    /** Tank frame, then water tiled bottom-up (never stretched, clipped to the fill), then the tick marks. */
    private void renderWater(GuiGraphicsExtractor graphics) {
        blitTexture(graphics, WATER_LEFT - 1, GAUGE_TOP - 1, TANK_FRAME_U, TANK_FRAME_V, GAUGE_WIDTH + 2, GAUGE_HEIGHT + 2);

        int fill = FarmGauges.fillPixels(menu.waterMb(), menu.waterCapacityMb(), GAUGE_HEIGHT);
        FluidBoxRenderer.Appearance appearance = fill > 0 ? FluidBoxRenderer.resolveForGui(Fluids.WATER) : null;
        if (appearance != null) {
            TextureAtlasSprite sprite = appearance.sprite();
            int x0 = leftPos + WATER_LEFT;
            int bottomY = topPos + GAUGE_TOP + GAUGE_HEIGHT;
            graphics.enableScissor(x0, bottomY - fill, x0 + GAUGE_WIDTH, bottomY);
            for (int drawn = 0; drawn < fill; drawn += 16) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x0, bottomY - drawn - 16, 16, 16, appearance.tint());
            }
            graphics.disableScissor();
        }

        blitTexture(graphics, WATER_LEFT, GAUGE_TOP, TANK_TICKS_U, TANK_TICKS_V, GAUGE_WIDTH, GAUGE_HEIGHT);
    }
}
