package com.logistics.pipe.screen;

import com.logistics.core.lib.client.render.FluidBoxRenderer;
import com.logistics.core.lib.fluids.FluidDisplay;
import com.logistics.core.lib.network.FulfillmentMode;
import com.logistics.core.lib.platform.ClientNetworking;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.pipe.modules.FluidSupplierModule;
import com.logistics.pipe.modules.FluidSupplierModule.MinThreshold;
import com.logistics.pipe.network.packet.ClickFluidSupplierGaugePacket;
import com.logistics.pipe.network.packet.SetFluidSupplierPacket;
import com.logistics.pipe.ui.FluidSupplierScreenHandler;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;

/**
 * Client-side screen for the Fluid Supplier Pipe.
 *
 * <p>Layout (top to bottom): a fluid slot with the target-amount field to its right (no label text —
 * that info lives in the gauge's tooltip); a row of plain vanilla buttons (1/10/100/1000) that add
 * that amount to the target; a row mirroring it that subtracts; and a row with the Fulfillment and Min.
 * Threshold mode buttons. Every button here is a stock {@link Button} at {@link Button#DEFAULT_HEIGHT}
 * with no custom widget or text scaling — kept plain deliberately so the layout is easy to iterate on.
 * The fluid slot doubles as the filter control: click it while carrying a bucket-like item to set (or
 * replace) the filter, or click it with nothing relevant carried to clear an existing filter — mirroring
 * the in-world bucket interaction, so there is no separate fluid picker or Clear button. Edits are sent
 * with {@link SetFluidSupplierPacket} (target/modes) and {@link ClickFluidSupplierGaugePacket} (fluid
 * filter).
 */
public class FluidSupplierScreen extends AbstractContainerScreen<FluidSupplierScreenHandler> {
    private static final ResourceId BACKGROUND_TEXTURE =
            ResourceId.in("logistics", "textures/gui/pipe/fluid_supplier.png");
    private static final int TEXT_COLOR = 0xFF404040;

    private static final int MAX_TARGET = (int) FluidSupplierModule.MAX_TARGET_MB;
    private static final int[] STEP_AMOUNTS = {1, 10, 100, 1000};
    // Sized to each label's text width plus consistent padding, rather than one uniform width.
    private static final int[] STEP_BUTTON_WIDTHS = {19, 25, 31, 37};

    // Fluid slot is baked into the background texture with its border at (47, 18); the icon sits 1px in.
    // Doubles as the filter control: click it (see #mouseClicked) to set/replace from a carried bucket,
    // or to clear an existing filter. The target field sits to its right, with no label — the name is
    // shown only in the gauge's tooltip.
    private static final int GAUGE_X = 48;
    private static final int GAUGE_Y = 19;
    private static final int GAUGE_SIZE = 16;

    private static final int FIELD_X = 68;
    private static final int FIELD_Y = 20;
    private static final int FIELD_WIDTH = 42;
    private static final int FIELD_HEIGHT = 14;

    // "+" row: adds each button's amount to the target ("1"/"10"/"100"/"1000" labels, no sign). "-" row
    // mirrors it, subtracting. Both rows share the same button columns, one row apart.
    private static final int ADD_ROW_Y = 40;
    private static final int STEP_BUTTON_HEIGHT = Button.DEFAULT_HEIGHT - 6;
    private static final int SUBTRACT_ROW_Y = ADD_ROW_Y + STEP_BUTTON_HEIGHT + 2;
    private static final int STEP_ROW_BUTTON_GAP = 1;
    private static final int SIGN_LABEL_GAP = 10;
    private static final int SIGN_LABEL_Y_NUDGE = 2;

    // Mode rows: "Min. Mode: [ button ]" then "Partial: [ button ]" — label at the same left column as
    // the "+"/"-" signs above, button filling the rest of the row out to the shared right edge.
    private static final int MIN_MODE_ROW_Y = SUBTRACT_ROW_Y + STEP_BUTTON_HEIGHT + 4;
    private static final int PARTIAL_ROW_Y = MIN_MODE_ROW_Y + STEP_BUTTON_HEIGHT + 2;
    private static final int BUTTON_LABEL_GAP = 4;
    // Floor so a long translated label can't push a mode button's computed width to zero/negative.
    private static final int MIN_MODE_BUTTON_WIDTH = 20;

    private EditBox amountField;
    private Button fulfillmentModeButton;
    private Button minThresholdButton;
    private int labelColumnX;

    // Locally-authoritative copies of the three fields SetFluidSupplierPacket always sends together,
    // seeded once from the menu at open. Each cycle/step callback updates its own field here and reads
    // the other two from here too — never from `menu` — so two rapid edits (e.g. a step click
    // immediately followed by a mode click, before the server's echo lands) can't have the second
    // packet clobber the first with a stale value read back from not-yet-updated menu state.
    private int pendingTargetMb;
    private int pendingFulfillmentMode;
    private int pendingMinThreshold;

    public FluidSupplierScreen(FluidSupplierScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = FluidSupplierScreenHandler.PANEL_HEIGHT;
        this.titleLabelY = 6;
        this.inventoryLabelY = FluidSupplierScreenHandler.INVENTORY_LABEL_Y;
    }

    @Override
    protected void init() {
        super.init();

        pendingTargetMb = menu.getTargetMb();
        pendingFulfillmentMode = menu.getFulfillmentModeOrdinal();
        pendingMinThreshold = menu.getMinThresholdOrdinal();

        amountField = new EditBox(
                this.font, leftPos + FIELD_X, topPos + FIELD_Y, FIELD_WIDTH, FIELD_HEIGHT, Component.literal("Amount"));
        amountField.setMaxLength(6);
        amountField.setCentered(true);
        amountField.setValue(String.valueOf(menu.getTargetMb()));
        addRenderableWidget(amountField);

        // All four rows below share one footprint — sign/label column, then buttons out to a right edge
        // — sized to the step rows' content, then centered as a single block in the panel (rather than
        // right-aligned, which left a lopsided gap on the left).
        int stepRowWidth = 0;
        for (int width : STEP_BUTTON_WIDTHS) stepRowWidth += width;
        stepRowWidth += (STEP_BUTTON_WIDTHS.length - 1) * STEP_ROW_BUTTON_GAP;
        int blockWidth = stepRowWidth + SIGN_LABEL_GAP;

        labelColumnX = (imageWidth - blockWidth) / 2;
        int labelColumnXAbs = leftPos + labelColumnX;
        int rowRightAbs = labelColumnXAbs + blockWidth;

        int x = labelColumnXAbs + SIGN_LABEL_GAP;
        for (int i = 0; i < STEP_AMOUNTS.length; i++) {
            int amount = STEP_AMOUNTS[i];
            int width = STEP_BUTTON_WIDTHS[i];

            addRenderableWidget(Button.builder(Component.literal(String.valueOf(amount)), b -> step(amount))
                    .bounds(x, topPos + ADD_ROW_Y, width, STEP_BUTTON_HEIGHT)
                    .build());

            addRenderableWidget(Button.builder(Component.literal(String.valueOf(amount)), b -> step(-amount))
                    .bounds(x, topPos + SUBTRACT_ROW_Y, width, STEP_BUTTON_HEIGHT)
                    .build());

            x += width + STEP_ROW_BUTTON_GAP;
        }

        // Each mode button fills the row from right after its own label out to the shared right edge,
        // so a longer label ("Min. Mode:") naturally leaves its button narrower than "Partial:"'s.
        Component minLabel = Component.translatable("gui.logistics.fluid_supplier.min_threshold_label");
        int minButtonX = Math.min(
                labelColumnXAbs + font.width(minLabel) + BUTTON_LABEL_GAP, rowRightAbs - MIN_MODE_BUTTON_WIDTH);
        minThresholdButton = Button.builder(minThresholdText(), b -> cycleMinThreshold())
                .bounds(minButtonX, topPos + MIN_MODE_ROW_Y, rowRightAbs - minButtonX, STEP_BUTTON_HEIGHT)
                .build();
        addRenderableWidget(minThresholdButton);

        Component partialLabel = Component.translatable("gui.logistics.fluid_supplier.fulfillment_label");
        int partialButtonX = Math.min(
                labelColumnXAbs + font.width(partialLabel) + BUTTON_LABEL_GAP, rowRightAbs - MIN_MODE_BUTTON_WIDTH);
        fulfillmentModeButton = Button.builder(fulfillmentModeText(), b -> cycleFulfillmentMode())
                .bounds(partialButtonX, topPos + PARTIAL_ROW_Y, rowRightAbs - partialButtonX, STEP_BUTTON_HEIGHT)
                .build();
        addRenderableWidget(fulfillmentModeButton);
    }

    private int parseField() {
        try {
            return Math.max(0, Math.min(MAX_TARGET, Integer.parseInt(amountField.getValue().trim())));
        } catch (NumberFormatException e) {
            return pendingTargetMb;
        }
    }

    private void applyTarget(int target) {
        pendingTargetMb = Math.max(0, Math.min(MAX_TARGET, target));
        amountField.setValue(String.valueOf(pendingTargetMb));
        sendState(pendingTargetMb, pendingFulfillmentMode, pendingMinThreshold);
    }

    private void step(int delta) {
        applyTarget(parseField() + delta);
    }

    /** Commits whatever's currently typed in the field (even if Enter was never pressed) to pending. */
    private void commitFieldToPending() {
        pendingTargetMb = parseField();
        amountField.setValue(String.valueOf(pendingTargetMb));
    }

    private void cycleFulfillmentMode() {
        commitFieldToPending();
        pendingFulfillmentMode = (pendingFulfillmentMode + 1) % FulfillmentMode.values().length;
        sendState(pendingTargetMb, pendingFulfillmentMode, pendingMinThreshold);
    }

    private void cycleMinThreshold() {
        commitFieldToPending();
        pendingMinThreshold = (pendingMinThreshold + 1) % MinThreshold.values().length;
        sendState(pendingTargetMb, pendingFulfillmentMode, pendingMinThreshold);
    }

    private void sendState(int targetMb, int fulfillmentModeOrdinal, int minThresholdOrdinal) {
        ClientNetworking.send(new SetFluidSupplierPacket(targetMb, fulfillmentModeOrdinal, minThresholdOrdinal));
    }

    // "Allow partial delivery?" — Yes/No, distinct from the shared gui.logistics.mode.partial/.full
    // keys the item Supplier's mode button uses, since that button cycles a different, longer set of
    // modes (Bulk 50%/100%, Infinite, ...) where "Partial"/"Full" reads correctly on their own.
    private Component fulfillmentModeText() {
        FulfillmentMode mode = FulfillmentMode.values()[menu.getFulfillmentModeOrdinal()];
        return Component.translatable("gui.logistics.fluid_supplier.fulfillment." + mode.name().toLowerCase(Locale.ROOT));
    }

    private Component minThresholdText() {
        MinThreshold threshold = MinThreshold.values()[menu.getMinThresholdOrdinal()];
        return Component.translatable(
                "gui.logistics.fluid_supplier.min_threshold." + threshold.name().toLowerCase(Locale.ROOT));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        if (isOverGauge(event.x(), event.y())) {
            ClientNetworking.send(new ClickFluidSupplierGaugePacket());
            return true;
        }
        return super.mouseClicked(event, consumed);
    }

    private boolean isOverGauge(double mouseX, double mouseY) {
        int gx = leftPos + GAUGE_X;
        int gy = topPos + GAUGE_Y;
        return mouseX >= gx && mouseX < gx + GAUGE_SIZE && mouseY >= gy && mouseY < gy + GAUGE_SIZE;
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (amountField.isFocused()) {
            if (keyEvent.key() == InputConstants.KEY_RETURN || keyEvent.key() == InputConstants.KEY_NUMPADENTER) {
                applyTarget(parseField());
                return true;
            }
            if (keyEvent.key() != InputConstants.KEY_ESCAPE) {
                amountField.keyPressed(keyEvent);
                return true;
            }
        }
        return super.keyPressed(keyEvent);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                BACKGROUND_TEXTURE.toIdentifier(),
                leftPos, topPos, 0, 0,
                imageWidth, imageHeight, 256, 256);

        // The slot border is baked into the texture; only paint the fluid sprite on top when set.
        Fluid fluid = currentFluid();
        if (fluid != Fluids.EMPTY) {
            FluidBoxRenderer.Appearance appearance = FluidBoxRenderer.resolveForGui(fluid);
            if (appearance != null) {
                TextureAtlasSprite sprite = appearance.sprite();
                int gx = leftPos + GAUGE_X;
                int gy = topPos + GAUGE_Y;
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, gx, gy, GAUGE_SIZE, GAUGE_SIZE, appearance.tint());
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, TEXT_COLOR, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT_COLOR, false);

        int rowLabelYOffset = (STEP_BUTTON_HEIGHT - font.lineHeight) / 2 + SIGN_LABEL_Y_NUDGE;
        graphics.drawString(font, Component.literal("+"), labelColumnX, ADD_ROW_Y + rowLabelYOffset, TEXT_COLOR, false);
        graphics.drawString(font, Component.literal("-"), labelColumnX, SUBTRACT_ROW_Y + rowLabelYOffset, TEXT_COLOR, false);
        graphics.drawString(font, Component.translatable("gui.logistics.fluid_supplier.min_threshold_label"),
                labelColumnX, MIN_MODE_ROW_Y + rowLabelYOffset, TEXT_COLOR, false);
        graphics.drawString(font, Component.translatable("gui.logistics.fluid_supplier.fulfillment_label"),
                labelColumnX, PARTIAL_ROW_Y + rowLabelYOffset, TEXT_COLOR, false);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        // Keep the field in sync with the server value while the player is not editing it.
        if (!amountField.isFocused()) {
            String synced = String.valueOf(menu.getTargetMb());
            if (!amountField.getValue().equals(synced)) {
                amountField.setValue(synced);
            }
        }

        // Keep the mode button labels in sync in case another client changed them.
        fulfillmentModeButton.setMessage(fulfillmentModeText());
        minThresholdButton.setMessage(minThresholdText());

        // Tooltip over the fluid gauge: name, target, buffer.
        if (mouseX >= leftPos + GAUGE_X - 1 && mouseX < leftPos + GAUGE_X + GAUGE_SIZE + 1
                && mouseY >= topPos + GAUGE_Y - 1 && mouseY < topPos + GAUGE_Y + GAUGE_SIZE + 1) {
            graphics.setTooltipForNextFrame(this.font, gaugeTooltip(), Optional.empty(), mouseX, mouseY);
        }
    }

    private List<Component> gaugeTooltip() {
        List<Component> lines = new ArrayList<>();
        Fluid fluid = currentFluid();
        lines.add(fluid != Fluids.EMPTY
                ? FluidDisplay.name(fluid)
                : Component.translatable("message.logistics.fluid_supplier.no_fluid"));
        lines.add(Component.translatable("gui.logistics.fluid_supplier.target", menu.getTargetMb()));
        lines.add(Component.translatable("gui.logistics.fluid_supplier.buffer", menu.getBufferMb()));
        return lines;
    }

    private Fluid currentFluid() {
        return BuiltInRegistries.FLUID.byId(menu.getFluidId());
    }
}
