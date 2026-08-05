package com.logistics.pipe.screen;

import com.logistics.core.lib.client.render.FluidBoxRenderer;
import com.logistics.core.lib.fluids.FluidDisplay;
import com.logistics.core.lib.platform.ClientNetworking;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.pipe.modules.FluidSupplierModule;
import com.logistics.pipe.network.packet.SetFluidSupplierPacket;
import com.logistics.pipe.ui.FluidSupplierScreenHandler;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Client-side screen for the Fluid Supplier Pipe.
 *
 * <p>Header strip mimics the classic LogisticsPipes fluid-supplier layout: a slot showing the selected
 * fluid, its name and buffered amount, a numeric target-amount field flanked by -/+ step buttons, and a
 * Clear button for the fluid filter. Edits are sent with {@link SetFluidSupplierPacket}; the fluid itself
 * is set in-world by right-clicking the pipe with a filled bucket, so there is no fluid picker here.
 */
public class FluidSupplierScreen extends AbstractContainerScreen<FluidSupplierScreenHandler> {
    private static final ResourceId BACKGROUND_TEXTURE =
            ResourceId.in("logistics", "textures/gui/pipe/fluid_supplier.png");
    private static final int TEXT_COLOR = 0xFF404040;

    private static final int MAX_TARGET = (int) FluidSupplierModule.MAX_TARGET_MB;
    private static final int STEP_MB = 250;

    // Fluid slot is baked into the background texture with its border at (47, 18); the icon sits 1px in,
    // centered on the two-line name/buffer text block to its right.
    private static final int GAUGE_X = 48;
    private static final int GAUGE_Y = 19;
    private static final int GAUGE_SIZE = 16;

    private static final int INFO_X = 70;
    private static final int FLUID_LABEL_X = 8;
    private static final int FLUID_LABEL_Y = 22;
    private static final int NAME_Y = 18;
    private static final int BUFFER_Y = 27;

    private static final int TARGET_ROW_Y = 38;
    private static final int TARGET_LABEL_X = 8;
    private static final int TARGET_LABEL_Y = 41;
    private static final int MINUS_X = 46;
    private static final int FIELD_X = 62;
    private static final int FIELD_WIDTH = 40;
    private static final int PLUS_X = 104;
    private static final int MB_LABEL_X = 122;
    private static final int MB_LABEL_Y = 41;

    private static final int CLEAR_BUTTON_X = 8;
    private static final int CLEAR_BUTTON_Y = 54;
    private static final int CLEAR_BUTTON_WIDTH = 90;
    private static final int CLEAR_BUTTON_HEIGHT = 13;

    private EditBox amountField;

    public FluidSupplierScreen(FluidSupplierScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title, 176, 166);
        this.titleLabelY = 6;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();

        addRenderableWidget(Button.builder(Component.literal("-"), b -> step(-STEP_MB))
                .bounds(leftPos + MINUS_X, topPos + TARGET_ROW_Y, 14, 14)
                .build());

        amountField = new EditBox(
                this.font, leftPos + FIELD_X, topPos + TARGET_ROW_Y, FIELD_WIDTH, 14, Component.literal("Amount"));
        amountField.setMaxLength(6);
        amountField.setCentered(true);
        amountField.setValue(String.valueOf(menu.getTargetMb()));
        addRenderableWidget(amountField);

        addRenderableWidget(Button.builder(Component.literal("+"), b -> step(STEP_MB))
                .bounds(leftPos + PLUS_X, topPos + TARGET_ROW_Y, 14, 14)
                .build());

        addRenderableWidget(Button.builder(
                        Component.translatable("gui.logistics.fluid_supplier.clear"), b -> clearFluid())
                .bounds(leftPos + CLEAR_BUTTON_X, topPos + CLEAR_BUTTON_Y, CLEAR_BUTTON_WIDTH, CLEAR_BUTTON_HEIGHT)
                .build());
    }

    private int parseField() {
        try {
            return Math.max(0, Math.min(MAX_TARGET, Integer.parseInt(amountField.getValue().trim())));
        } catch (NumberFormatException e) {
            return menu.getTargetMb();
        }
    }

    private void applyTarget(int target) {
        int clamped = Math.max(0, Math.min(MAX_TARGET, target));
        amountField.setValue(String.valueOf(clamped));
        ClientNetworking.send(new SetFluidSupplierPacket(clamped, false));
    }

    private void step(int delta) {
        applyTarget(parseField() + delta);
    }

    private void clearFluid() {
        ClientNetworking.send(new SetFluidSupplierPacket(menu.getTargetMb(), true));
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
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
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
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, titleLabelX, titleLabelY, TEXT_COLOR, false);

        graphics.text(font, Component.translatable("gui.logistics.fluid_supplier.fluid_label"),
                FLUID_LABEL_X, FLUID_LABEL_Y, TEXT_COLOR, false);

        Fluid fluid = currentFluid();
        Component name = fluid != Fluids.EMPTY
                ? FluidDisplay.name(fluid)
                : Component.translatable("message.logistics.fluid_supplier.no_fluid");
        graphics.text(font, name, INFO_X, NAME_Y, TEXT_COLOR, false);

        Component buffer = Component.translatable("gui.logistics.fluid_supplier.buffer", menu.getBufferMb());
        graphics.text(font, buffer, INFO_X, BUFFER_Y, TEXT_COLOR, false);

        graphics.text(font, Component.translatable("gui.logistics.fluid_supplier.target_label"),
                TARGET_LABEL_X, TARGET_LABEL_Y, TEXT_COLOR, false);
        graphics.text(font, Component.translatable("gui.logistics.fluid_supplier.mb_unit"),
                MB_LABEL_X, MB_LABEL_Y, TEXT_COLOR, false);

        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT_COLOR, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        // Keep the field in sync with the server value while the player is not editing it.
        if (!amountField.isFocused()) {
            String synced = String.valueOf(menu.getTargetMb());
            if (!amountField.getValue().equals(synced)) {
                amountField.setValue(synced);
            }
        }

        // Tooltip over the fluid gauge: name, target, buffer.
        if (mouseX >= leftPos + GAUGE_X - 1 && mouseX < leftPos + GAUGE_X + GAUGE_SIZE + 1
                && mouseY >= topPos + GAUGE_Y - 1 && mouseY < topPos + GAUGE_Y + GAUGE_SIZE + 1) {
            graphics.setTooltipForNextFrame(this.font, gaugeTooltip(), java.util.Optional.empty(), mouseX, mouseY);
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
