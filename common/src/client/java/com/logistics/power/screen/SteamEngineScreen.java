package com.logistics.power.screen;

import com.logistics.LogisticsMod;
import com.logistics.core.lib.client.render.FluidBoxRenderer;
import com.logistics.core.lib.fluids.FluidDisplay;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.power.engine.steam.SteamFireboxState;
import com.logistics.power.engine.ui.SteamEngineScreenHandler;
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
 * Client-side GUI for the Steam Engine: a fuel slot with a firebox flame (bright while boiling, dimmed
 * while stoked), a water tank gauge, a pressure gauge with threshold markers at the operating, relight,
 * and target pressures, and an energy buffer gauge. Numeric detail is shown as hover tooltips.
 */
public class SteamEngineScreen extends AbstractContainerScreen<SteamEngineScreenHandler> {

    private static final ResourceId TEXTURE = LogisticsMod.modId("textures/gui/power/steam_engine.png");
    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;

    private static final ResourceId CHARGE = LogisticsMod.modId("automation/charge");
    private static final ResourceId FLAME = ResourceId.in("minecraft", "container/furnace/lit_progress");

    // Firebox flame, below the fuel slot.
    private static final int FLAME_X = 44;
    private static final int FLAME_Y = 53;
    private static final int FLAME_SIZE = 14;

    // Water tank gauge (fluid fills a 16x48 interior).
    private static final int WATER_LEFT = 80;
    private static final int WATER_TOP = 18;
    private static final int WATER_WIDTH = 16;
    private static final int WATER_HEIGHT = 48;
    private static final int WATER_BOTTOM = WATER_TOP + WATER_HEIGHT;

    // Pressure gauge (a vertical filled bar with threshold marker lines).
    private static final int PRESSURE_LEFT = 104;
    private static final int PRESSURE_TOP = 18;
    private static final int PRESSURE_WIDTH = 8;
    private static final int PRESSURE_HEIGHT = 48;
    private static final int PRESSURE_BOTTOM = PRESSURE_TOP + PRESSURE_HEIGHT;

    // Energy buffer gauge.
    private static final int ENERGY_LEFT = 140;
    private static final int ENERGY_TOP = 18;
    private static final int ENERGY_WIDTH = 12;
    private static final int ENERGY_HEIGHT = 48;

    public SteamEngineScreen(SteamEngineScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        hoverTooltip(graphics, mouseX, mouseY, WATER_LEFT, WATER_TOP, WATER_WIDTH, WATER_HEIGHT, waterTooltip());
        hoverTooltip(graphics, mouseX, mouseY,
                PRESSURE_LEFT, PRESSURE_TOP, PRESSURE_WIDTH, PRESSURE_HEIGHT, pressureTooltip());
        hoverTooltip(graphics, mouseX, mouseY, ENERGY_LEFT, ENERGY_TOP, ENERGY_WIDTH, ENERGY_HEIGHT, statusTooltip());
    }

    private void hoverTooltip(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, int x, int y, int w, int h, List<Component> lines) {
        if (mouseX >= leftPos + x && mouseX < leftPos + x + w && mouseY >= topPos + y && mouseY < topPos + y + h) {
            graphics.setTooltipForNextFrame(this.font, lines, Optional.empty(), mouseX, mouseY);
        }
    }

    private List<Component> waterTooltip() {
        int id = menu.getWaterId();
        int amount = menu.getWaterAmountMb();
        if (id < 0 || amount <= 0) {
            return List.of(Component.translatable("tooltip.logistics.fluid.tank_empty"));
        }
        Fluid fluid = BuiltInRegistries.FLUID.byId(id);
        return List.of(
                FluidDisplay.name(fluid),
                Component.translatable("tooltip.logistics.steam_engine.water", amount).withStyle(ChatFormatting.GRAY));
    }

    private List<Component> pressureTooltip() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(
                "tooltip.logistics.steam_engine.pressure", menu.getPressure(), menu.getMaxPressure()));
        lines.add(Component.translatable("tooltip.logistics.steam_engine.output", menu.getGeneration())
                .withStyle(ChatFormatting.GRAY));
        lines.add(fireboxLine(menu.getFirebox()));
        return lines;
    }

    private List<Component> statusTooltip() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("tooltip.logistics.steam_engine.output", menu.getGeneration()));
        if (menu.getCommittedBurn() > 0) {
            lines.add(Component.translatable("tooltip.logistics.steam_engine.burn_reserve", menu.getCommittedBurn())
                    .withStyle(ChatFormatting.GRAY));
        }
        lines.add(Component.translatable("jade.logistics.engine.status." + statusName(menu))
                .withStyle(ChatFormatting.GRAY));
        return lines;
    }

    private static String statusName(SteamEngineScreenHandler menu) {
        return menu.getStatus().name().toLowerCase(java.util.Locale.ROOT);
    }

    private Component fireboxLine(SteamFireboxState firebox) {
        return Component.translatable("jade.logistics.engine.firebox." + firebox.name().toLowerCase(java.util.Locale.ROOT))
                .withStyle(ChatFormatting.GOLD);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED, TEXTURE.toIdentifier(),
                leftPos, topPos, 0, 0, imageWidth, imageHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        renderTank(graphics, WATER_LEFT, menu.getWaterId(), menu.getWaterFillFraction());
        renderPressureGauge(graphics);

        int energyHeight = menu.getEnergyBarHeight(ENERGY_HEIGHT);
        if (energyHeight > 0) {
            graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED, CHARGE.toIdentifier(),
                    ENERGY_WIDTH, ENERGY_HEIGHT, 0, ENERGY_HEIGHT - energyHeight,
                    leftPos + ENERGY_LEFT, topPos + ENERGY_TOP + (ENERGY_HEIGHT - energyHeight), ENERGY_WIDTH, energyHeight);
        }

        renderFlame(graphics);
    }

    /** Vertical pressure bar with threshold marker lines at operating / relight / target pressures. */
    private void renderPressureGauge(GuiGraphicsExtractor graphics) {
        int x0 = leftPos + PRESSURE_LEFT;
        int bottom = topPos + PRESSURE_BOTTOM;
        // Background well.
        graphics.fill(x0, topPos + PRESSURE_TOP, x0 + PRESSURE_WIDTH, bottom, 0xFF20140A);
        int fillPixels = Math.round(menu.getPressureFraction() * PRESSURE_HEIGHT);
        if (fillPixels > 0) {
            graphics.fill(x0, bottom - fillPixels, x0 + PRESSURE_WIDTH, bottom, 0xFF57C4E0);
        }
        int max = menu.getMaxPressure();
        marker(graphics, x0, bottom, menu.getOperatingPressure(), max, 0xFF3CB043); // operating (green)
        marker(graphics, x0, bottom, menu.getRelightPressure(), max, 0xFFE0A030); // relight (amber)
        marker(graphics, x0, bottom, menu.getTargetPressure(), max, 0xFFC03030); // target (red)
    }

    private void marker(GuiGraphicsExtractor graphics, int x0, int bottom, int value, int max, int color) {
        if (max <= 0 || value <= 0) {
            return;
        }
        int y = bottom - Math.round(Math.min(1f, value / (float) max) * PRESSURE_HEIGHT);
        graphics.fill(x0, y, x0 + PRESSURE_WIDTH, y + 1, color);
    }

    /** Firebox flame: full-bright while boiling, dimmed while stoked, hidden while off. */
    private void renderFlame(GuiGraphicsExtractor graphics) {
        SteamFireboxState firebox = menu.getFirebox();
        if (firebox == SteamFireboxState.OFF) {
            return;
        }
        float fraction = menu.getBurnFraction();
        int shown = fraction > 0f ? Math.max(1, Math.round(fraction * FLAME_SIZE)) : FLAME_SIZE;
        int yOffset = FLAME_SIZE - shown;
        graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED, FLAME.toIdentifier(),
                FLAME_SIZE, FLAME_SIZE, 0, yOffset,
                leftPos + FLAME_X, topPos + FLAME_Y + yOffset, FLAME_SIZE, shown);
        if (firebox == SteamFireboxState.STOKED) {
            // Damp the flame with a translucent overlay to read as "kept hot, not boiling".
            graphics.fill(
                    leftPos + FLAME_X, topPos + FLAME_Y + yOffset,
                    leftPos + FLAME_X + FLAME_SIZE, topPos + FLAME_Y + FLAME_SIZE, 0x80000000);
        }
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
        int bottomY = topPos + WATER_BOTTOM;
        int fillPixels = Math.round(fraction * WATER_HEIGHT);
        graphics.enableScissor(x0, bottomY - fillPixels, x0 + WATER_WIDTH, bottomY);
        for (int drawn = 0; drawn < fillPixels; drawn += 16) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x0, bottomY - drawn - 16, 16, 16, tint);
        }
        graphics.disableScissor();
    }
}
