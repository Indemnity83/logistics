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
 * Client-side GUI for the Fuel Engine: a fuel tank and a coolant tank flanking a temperature readout,
 * with an energy buffer bar. Numeric detail (temperature, generation, committed reserves, thermal
 * shutdown) is shown as hover tooltips.
 */
public class FuelEngineScreen extends AbstractContainerScreen<FuelEngineScreenHandler> {

    private static final ResourceId TEXTURE = LogisticsMod.modId("textures/gui/power/fuel_engine.png");
    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;

    private static final ResourceId CHARGE = LogisticsMod.modId("automation/charge");
    private static final int CHARGE_EMPTY_TINT = 0xFF404040;

    private static final int TANK_TOP = 18;
    private static final int TANK_BOTTOM = 76;
    private static final int TANK_WIDTH = 16;
    private static final int TANK_HEIGHT = TANK_BOTTOM - TANK_TOP;
    private static final int FUEL_TANK_LEFT = 74;
    private static final int COOLANT_TANK_LEFT = 147;
    private static final int ENERGY_LEFT = 10;
    private static final int ENERGY_TOP = 19;
    private static final int ENERGY_WIDTH = 12;
    private static final int ENERGY_HEIGHT = 30;

    public FuelEngineScreen(FuelEngineScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        hoverTooltip(graphics, mouseX, mouseY, FUEL_TANK_LEFT, TANK_TOP, TANK_WIDTH, TANK_HEIGHT, fuelTooltip());
        hoverTooltip(graphics, mouseX, mouseY, COOLANT_TANK_LEFT, TANK_TOP, TANK_WIDTH, TANK_HEIGHT, coolantTooltip());
        hoverTooltip(graphics, mouseX, mouseY, ENERGY_LEFT, ENERGY_TOP, ENERGY_WIDTH, ENERGY_HEIGHT, statusTooltip());
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

        graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED, CHARGE.toIdentifier(),
                ENERGY_WIDTH, ENERGY_HEIGHT, 0, 0,
                leftPos + ENERGY_LEFT, topPos + ENERGY_TOP, ENERGY_WIDTH, ENERGY_HEIGHT, CHARGE_EMPTY_TINT);
        int energyHeight = menu.getEnergyBarHeight(ENERGY_HEIGHT);
        if (energyHeight > 0) {
            graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED, CHARGE.toIdentifier(),
                    ENERGY_WIDTH, ENERGY_HEIGHT, 0, ENERGY_HEIGHT - energyHeight,
                    leftPos + ENERGY_LEFT, topPos + ENERGY_TOP + (ENERGY_HEIGHT - energyHeight), ENERGY_WIDTH, energyHeight);
        }

        renderTank(graphics, FUEL_TANK_LEFT, menu.getFuelId(), menu.getFuelFillFraction());
        renderTank(graphics, COOLANT_TANK_LEFT, menu.getCoolantId(), menu.getCoolantFillFraction());

        Component temp = Component.literal(menu.getTemperature() + "°");
        int color = menu.isThermalShutdown() ? 0xFFC03020 : 0xFF404040;
        graphics.text(font, temp, leftPos + 104 - font.width(temp) / 2, topPos + 40, color, false);
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
