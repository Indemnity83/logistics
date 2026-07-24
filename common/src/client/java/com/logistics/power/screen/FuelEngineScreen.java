package com.logistics.power.screen;

import com.logistics.LogisticsMod;
import com.logistics.core.lib.client.render.FluidBoxRenderer;
import com.logistics.core.lib.fluids.FluidDisplay;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.power.engine.ui.FuelEngineScreenHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Client-side GUI for the Fuel Engine: a fuel tank and a coolant tank (right-aligned), an energy buffer
 * gauge, and a burn flame that crops down as the committed fuel reserve is spent. Numeric detail
 * (temperature, generation, committed reserves, thermal shutdown) is shown as hover tooltips.
 */
public class FuelEngineScreen extends AbstractContainerScreen<FuelEngineScreenHandler> {

    private static final ResourceId TEXTURE = LogisticsMod.modId("textures/gui/power/fuel_engine.png");
    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;

    private static final ResourceId CHARGE = LogisticsMod.modId("automation/charge");
    private static final ResourceId FLAME = ResourceId.in("minecraft", "container/furnace/lit_progress");

    // Tank frames are 18x60 in the texture; fluid fills the 16x58 interior (1px border).
    private static final int TANK_TOP = 14;
    private static final int TANK_BOTTOM = TANK_TOP + 58;
    private static final int TANK_WIDTH = 16;
    private static final int TANK_HEIGHT = TANK_BOTTOM - TANK_TOP;
    private static final int FUEL_TANK_LEFT = 88;
    private static final int COOLANT_TANK_LEFT = 109;
    // Frame bounds for hover tooltips.
    private static final int FUEL_FRAME_LEFT = 87;
    private static final int COOLANT_FRAME_LEFT = 108;
    private static final int TANK_FRAME_TOP = 13;
    private static final int TANK_FRAME_WIDTH = 18;
    private static final int TANK_FRAME_HEIGHT = 60;

    // Energy gauge frame is 14x32; the charge fill is its 12x30 interior.
    private static final int ENERGY_FRAME_LEFT = 154;
    private static final int ENERGY_FRAME_TOP = 18;
    private static final int ENERGY_LEFT = 155;
    private static final int ENERGY_TOP = 19;
    private static final int ENERGY_WIDTH = 12;
    private static final int ENERGY_HEIGHT = 30;

    // Burn flame, directly below the energy gauge.
    private static final int FLAME_X = 154;
    private static final int FLAME_Y = 53;
    private static final int FLAME_SIZE = 14;

    public FuelEngineScreen(FuelEngineScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        hoverTooltip(graphics, mouseX, mouseY,
                FUEL_FRAME_LEFT, TANK_FRAME_TOP, TANK_FRAME_WIDTH, TANK_FRAME_HEIGHT, fuelTooltip());
        hoverTooltip(graphics, mouseX, mouseY,
                COOLANT_FRAME_LEFT, TANK_FRAME_TOP, TANK_FRAME_WIDTH, TANK_FRAME_HEIGHT, coolantTooltip());
        hoverTooltip(graphics, mouseX, mouseY,
                ENERGY_FRAME_LEFT, ENERGY_FRAME_TOP, FLAME_SIZE, 32, statusTooltip());
    }

    private void hoverTooltip(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, int x, int y, int w, int h, List<Component> lines) {
        if (mouseX >= leftPos + x && mouseX < leftPos + x + w && mouseY >= topPos + y && mouseY < topPos + y + h) {
            graphics.setTooltipForNextFrame(this.font, lines, Optional.empty(), mouseX, mouseY);
        }
    }

    private List<Component> fuelTooltip() {
        List<Component> lines = new ArrayList<>(tankTooltip(menu.getFuelId(), menu.getFuelAmountMb()));
        if (menu.getCommittedFuel() > 0) {
            lines.add(Component.translatable("tooltip.logistics.fuel_engine.committed_fuel", menu.getCommittedFuel())
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        return lines;
    }

    private List<Component> coolantTooltip() {
        List<Component> lines = new ArrayList<>(tankTooltip(menu.getCoolantId(), menu.getCoolantAmountMb()));
        if (menu.getCommittedCooling() > 0) {
            lines.add(Component.translatable("tooltip.logistics.fuel_engine.committed_cooling", menu.getCommittedCooling())
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        return lines;
    }

    private List<Component> statusTooltip() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(
                "tooltip.logistics.fuel_engine.temperature", menu.getTemperature(), menu.getMaxTemperature()));
        lines.add(Component.translatable("tooltip.logistics.fuel_engine.generation", menu.getGeneration())
                .withStyle(ChatFormatting.GRAY));
        if (menu.isThermalShutdown()) {
            lines.add(Component.translatable("tooltip.logistics.fuel_engine.shutdown").withStyle(ChatFormatting.RED));
            lines.add(Component.translatable("tooltip.logistics.fuel_engine.shutdown_hint")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
        return lines;
    }

    private List<Component> tankTooltip(int fluidId, int amountMb) {
        if (fluidId < 0 || amountMb <= 0) {
            return List.of(Component.translatable("tooltip.logistics.fluid.tank_empty"));
        }
        Fluid fluid = BuiltInRegistries.FLUID.byId(fluidId);
        Component amount = Component.translatable("tooltip.logistics.fluid.tank_amount", amountMb)
                .withStyle(ChatFormatting.GRAY);
        return List.of(FluidDisplay.name(fluid), amount);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED, TEXTURE.toIdentifier(),
                leftPos, topPos, 0, 0, imageWidth, imageHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        renderTank(graphics, FUEL_TANK_LEFT, menu.getFuelId(), menu.getFuelFillFraction());
        renderTank(graphics, COOLANT_TANK_LEFT, menu.getCoolantId(), menu.getCoolantFillFraction());

        int energyHeight = menu.getEnergyBarHeight(ENERGY_HEIGHT);
        if (energyHeight > 0) {
            graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED, CHARGE.toIdentifier(),
                    ENERGY_WIDTH, ENERGY_HEIGHT, 0, ENERGY_HEIGHT - energyHeight,
                    leftPos + ENERGY_LEFT, topPos + ENERGY_TOP + (ENERGY_HEIGHT - energyHeight), ENERGY_WIDTH, energyHeight);
        }

        renderFlame(graphics, menu.getFuelBurnFraction());
    }

    /** Flame starts full and crops from the top down to zero as the committed fuel reserve is spent. */
    private void renderFlame(GuiGraphicsExtractor graphics, float fraction) {
        if (fraction <= 0f) {
            return;
        }
        int shown = Math.max(1, Math.round(fraction * FLAME_SIZE));
        int yOffset = FLAME_SIZE - shown;
        graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED, FLAME.toIdentifier(),
                FLAME_SIZE, FLAME_SIZE, 0, yOffset,
                leftPos + FLAME_X, topPos + FLAME_Y + yOffset, FLAME_SIZE, shown);
    }

    private void renderTank(GuiGraphicsExtractor graphics, int left, int fluidId, float fraction) {
        if (fluidId < 0 || fraction <= 0f) {
            return;
        }
        Fluid fluid = BuiltInRegistries.FLUID.byId(fluidId);
        if (fluid == Fluids.EMPTY) {
            return;
        }
        FluidBoxRenderer.Appearance appearance = FluidBoxRenderer.resolveForGui(fluid);
        if (appearance == null) {
            return;
        }
        TextureAtlasSprite sprite = appearance.sprite();
        int tint = appearance.tint();
        int x0 = leftPos + left;
        int bottomY = topPos + TANK_BOTTOM;
        int fillPixels = Math.round(fraction * TANK_HEIGHT);
        graphics.enableScissor(x0, bottomY - fillPixels, x0 + TANK_WIDTH, bottomY);
        for (int drawn = 0; drawn < fillPixels; drawn += 16) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x0, bottomY - drawn - 16, 16, 16, tint);
        }
        graphics.disableScissor();
    }
}
