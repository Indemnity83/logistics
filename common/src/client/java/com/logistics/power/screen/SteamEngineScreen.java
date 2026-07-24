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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Client-side GUI for the Steam Engine: a fuel slot, a water tank gauge, a firebox flame (bright while
 * firing, dimmed while stoked), a boiler-heat thermometer, and the energy-buffer gauge — which for now
 * shows stored pressure as a white fill (a fuller energy/pressure split is TBD). Numeric detail is shown
 * as hover tooltips.
 */
public class SteamEngineScreen extends AbstractContainerScreen<SteamEngineScreenHandler> {

    private static final ResourceId TEXTURE = LogisticsMod.modId("textures/gui/power/steam_engine.png");
    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;

    private static final ResourceId FLAME = ResourceId.in("minecraft", "container/furnace/lit_progress");

    // Water tank gauge (fluid fills the interior; the hover test uses the full 18x60 frame).
    private static final int WATER_LEFT = 109;
    private static final int WATER_TOP = 14;
    private static final int WATER_WIDTH = 16;
    private static final int WATER_HEIGHT = 58;
    private static final int WATER_BOTTOM = WATER_TOP + WATER_HEIGHT;
    private static final int WATER_FRAME_LEFT = 108;
    private static final int WATER_FRAME_TOP = 13;
    private static final int WATER_FRAME_WIDTH = 18;
    private static final int WATER_FRAME_HEIGHT = 60;

    // Energy-buffer gauge — temporarily filled by stored pressure (white overlay); hover uses the 14x32 frame.
    private static final int GAUGE_LEFT = 155;
    private static final int GAUGE_TOP = 19;
    private static final int GAUGE_WIDTH = 12;
    private static final int GAUGE_HEIGHT = 30;
    private static final int GAUGE_BOTTOM = GAUGE_TOP + GAUGE_HEIGHT;
    private static final int GAUGE_FRAME_LEFT = 154;
    private static final int GAUGE_FRAME_TOP = 18;
    private static final int GAUGE_FRAME_WIDTH = 14;
    private static final int GAUGE_FRAME_HEIGHT = 32;

    // Firebox flame, below the energy gauge.
    private static final int FLAME_X = 154;
    private static final int FLAME_Y = 53;
    private static final int FLAME_SIZE = 14;

    // Boiler-heat thermometer — placeholder position in the gap between the fuel slot and the water gauge
    // (final placement is a texture decision); a red fill by heat fraction with a tick at the boiling point.
    private static final int HEAT_LEFT = 101;
    private static final int HEAT_TOP = 14;
    private static final int HEAT_WIDTH = 6;
    private static final int HEAT_HEIGHT = 58;
    private static final int HEAT_BOTTOM = HEAT_TOP + HEAT_HEIGHT;

    public SteamEngineScreen(SteamEngineScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        hoverTooltip(graphics, mouseX, mouseY,
                WATER_FRAME_LEFT, WATER_FRAME_TOP, WATER_FRAME_WIDTH, WATER_FRAME_HEIGHT, waterTooltip());
        hoverTooltip(graphics, mouseX, mouseY,
                GAUGE_FRAME_LEFT, GAUGE_FRAME_TOP, GAUGE_FRAME_WIDTH, GAUGE_FRAME_HEIGHT, gaugeTooltip());
        hoverTooltip(graphics, mouseX, mouseY, HEAT_LEFT, HEAT_TOP, HEAT_WIDTH, HEAT_HEIGHT, heatTooltip());
    }

    private void hoverTooltip(
            GuiGraphics graphics, int mouseX, int mouseY, int x, int y, int w, int h, List<Component> lines) {
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
                Component.translatable("tooltip.logistics.fluid.tank_amount", amount).withStyle(ChatFormatting.GRAY));
    }

    private List<Component> gaugeTooltip() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(
                "tooltip.logistics.steam_engine.pressure", menu.getPressure(), menu.getMaxPressure()));
        lines.add(Component.translatable("tooltip.logistics.steam_engine.output", menu.getGeneration())
                .withStyle(ChatFormatting.GRAY));
        if (menu.getCommittedBurn() > 0) {
            lines.add(Component.translatable("tooltip.logistics.steam_engine.burn_reserve", menu.getCommittedBurn())
                    .withStyle(ChatFormatting.GRAY));
        }
        if (menu.isSafetyValveActive()) {
            lines.add(Component.translatable("tooltip.logistics.steam_engine.safety_valve")
                    .withStyle(ChatFormatting.GOLD));
        }
        lines.add(fireboxLine(menu.getFirebox()));
        lines.add(Component.translatable("jade.logistics.engine.status." + statusName(menu))
                .withStyle(ChatFormatting.GRAY));
        return lines;
    }

    private List<Component> heatTooltip() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(
                "tooltip.logistics.steam_engine.heat", menu.getBoilerHeat(), menu.getMaxHeat()));
        boolean boiling = menu.getBoilerHeat() >= menu.getBoilingHeat();
        lines.add(Component.translatable(
                        boiling
                                ? "tooltip.logistics.steam_engine.heat_boiling"
                                : "tooltip.logistics.steam_engine.heat_warming")
                .withStyle(ChatFormatting.GRAY));
        if (menu.isSafetyValveActive()) {
            lines.add(Component.translatable("tooltip.logistics.steam_engine.safety_valve")
                    .withStyle(ChatFormatting.GOLD));
        }
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
    protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED, TEXTURE.toIdentifier(),
                leftPos, topPos, 0, 0, imageWidth, imageHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        renderTank(graphics, WATER_LEFT, menu.getWaterId(), menu.getWaterFillFraction());
        renderHeatBar(graphics);
        renderPressureInGauge(graphics);
        renderFlame(graphics);
    }

    /** Boiler heat as a red thermometer fill, with a tick mark at the boiling point (placeholder placement). */
    private void renderHeatBar(GuiGraphics graphics) {
        int x0 = leftPos + HEAT_LEFT;
        int bottom = topPos + HEAT_BOTTOM;
        int fillPixels = Math.round(menu.getHeatFraction() * HEAT_HEIGHT);
        if (fillPixels > 0) {
            int color = menu.isSafetyValveActive() ? 0xFFFF5030 : 0xFFC83010;
            graphics.fill(x0, bottom - fillPixels, x0 + HEAT_WIDTH, bottom, color);
        }
        int maxHeat = menu.getMaxHeat();
        if (maxHeat > 0) {
            int markPixels = Math.round(menu.getBoilingHeat() / (float) maxHeat * HEAT_HEIGHT);
            int y = bottom - markPixels;
            graphics.fill(x0, y, x0 + HEAT_WIDTH, y + 1, 0xFF202020); // boiling-point marker
        }
    }

    /** Stored pressure shown in the energy-buffer gauge as a white overlay (energy/pressure split is TBD). */
    private void renderPressureInGauge(GuiGraphics graphics) {
        int fillPixels = Math.round(menu.getPressureFraction() * GAUGE_HEIGHT);
        if (fillPixels <= 0) {
            return;
        }
        int x0 = leftPos + GAUGE_LEFT;
        int bottom = topPos + GAUGE_BOTTOM;
        graphics.fill(x0, bottom - fillPixels, x0 + GAUGE_WIDTH, bottom, 0xC8FFFFFF);
    }

    /** Firebox flame: full-bright while firing, dimmed while stoked, hidden while off. */
    private void renderFlame(GuiGraphics graphics) {
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

    private void renderTank(GuiGraphics graphics, int left, int fluidId, float fraction) {
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
