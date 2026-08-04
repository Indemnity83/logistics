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
 * <p>Header strip shows the selected fluid (still sprite + name) and the current buffer, with a numeric
 * target-amount field flanked by -/+ step buttons and a Clear button for the fluid filter. Edits are
 * sent with {@link SetFluidSupplierPacket}; the fluid is set in-world by right-clicking the pipe with a
 * filled bucket, so there is no fluid picker here. Reuses the satellite background (176x122).
 */
public class FluidSupplierScreen extends AbstractContainerScreen<FluidSupplierScreenHandler> {
    private static final ResourceId BACKGROUND_TEXTURE =
            ResourceId.in("logistics", "textures/gui/pipe/satellite.png");

    private static final int MAX_TARGET = (int) FluidSupplierModule.MAX_TARGET_MB;
    private static final int STEP_MB = 250;

    private static final int GAUGE_X = 8;
    private static final int GAUGE_Y = 8;
    private static final int GAUGE_SIZE = 16;

    private static final int ROW_Y = 7;
    private static final int MINUS_X = 92;
    private static final int FIELD_X = 108;
    private static final int PLUS_X = 144;

    private EditBox amountField;

    public FluidSupplierScreen(FluidSupplierScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title, 176, 122);
    }

    @Override
    protected void init() {
        super.init();

        addRenderableWidget(Button.builder(Component.literal("-"), b -> step(-STEP_MB))
                .bounds(leftPos + MINUS_X, topPos + ROW_Y, 14, 14)
                .build());

        amountField = new EditBox(this.font, leftPos + FIELD_X, topPos + ROW_Y, 34, 14, Component.literal("Amount"));
        amountField.setMaxLength(6);
        amountField.setCentered(true);
        amountField.setValue(String.valueOf(menu.getTargetMb()));
        addRenderableWidget(amountField);

        addRenderableWidget(Button.builder(Component.literal("+"), b -> step(STEP_MB))
                .bounds(leftPos + PLUS_X, topPos + ROW_Y, 14, 14)
                .build());

        addRenderableWidget(Button.builder(
                        Component.translatable("gui.logistics.fluid_supplier.clear"), b -> clearFluid())
                .bounds(leftPos + MINUS_X, topPos + 22, 66, 13)
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

        // Fluid gauge: dark backing, then the fluid's still sprite (tinted) if a filter is set.
        int gx = leftPos + GAUGE_X;
        int gy = topPos + GAUGE_Y;
        graphics.fill(gx, gy, gx + GAUGE_SIZE, gy + GAUGE_SIZE, 0xFF2B2B2B);
        Fluid fluid = currentFluid();
        if (fluid != Fluids.EMPTY) {
            FluidBoxRenderer.Appearance appearance = FluidBoxRenderer.resolveForGui(fluid);
            if (appearance != null) {
                TextureAtlasSprite sprite = appearance.sprite();
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, gx, gy, GAUGE_SIZE, GAUGE_SIZE, appearance.tint());
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Fluid fluid = currentFluid();
        Component name = fluid != Fluids.EMPTY
                ? FluidDisplay.name(fluid)
                : Component.translatable("gui.logistics.fluid_supplier.no_fluid");
        graphics.text(font, name, GAUGE_X + GAUGE_SIZE + 4, GAUGE_Y + 1, 0xFF404040, false);

        Component buffer = Component.translatable("gui.logistics.fluid_supplier.buffer", menu.getBufferMb());
        graphics.text(font, buffer, GAUGE_X + GAUGE_SIZE + 4, GAUGE_Y + 13, 0xFF404040, false);

        // Player inventory label (title comes from the pipe name in the window bar).
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF404040, false);
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
        if (mouseX >= leftPos + GAUGE_X && mouseX < leftPos + GAUGE_X + GAUGE_SIZE
                && mouseY >= topPos + GAUGE_Y && mouseY < topPos + GAUGE_Y + GAUGE_SIZE) {
            graphics.setTooltipForNextFrame(this.font, gaugeTooltip(), java.util.Optional.empty(), mouseX, mouseY);
        }
    }

    private List<Component> gaugeTooltip() {
        List<Component> lines = new ArrayList<>();
        Fluid fluid = currentFluid();
        lines.add(fluid != Fluids.EMPTY
                ? FluidDisplay.name(fluid)
                : Component.translatable("gui.logistics.fluid_supplier.no_fluid"));
        lines.add(Component.translatable("gui.logistics.fluid_supplier.target", menu.getTargetMb()));
        lines.add(Component.translatable("gui.logistics.fluid_supplier.buffer", menu.getBufferMb()));
        return lines;
    }

    private Fluid currentFluid() {
        return BuiltInRegistries.FLUID.byId(menu.getFluidId());
    }
}
