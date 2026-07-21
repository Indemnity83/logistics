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
 * Client-side GUI for the Steam Engine: a fuel slot, a water tank gauge, a firebox flame (bright while
 * boiling, dimmed while stoked), and the energy-buffer gauge — which for now shows stored pressure as a
 * white fill (a fuller energy/pressure split is TBD). Numeric detail is shown as hover tooltips.
 */
public class SteamEngineScreen extends AbstractContainerScreen<SteamEngineScreenHandler> {

    private static final ResourceId TEXTURE = LogisticsMod.modId("textures/gui/power/steam_engine.png");
    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;

    private static final ResourceId FLAME = ResourceId.in("minecraft", "container/furnace/lit_progress");

    // Water tank gauge (fluid fills the interior).
    private static final int WATER_LEFT = 109;
    private static final int WATER_TOP = 14;
    private static final int WATER_WIDTH = 16;
    private static final int WATER_HEIGHT = 58;
    private static final int WATER_BOTTOM = WATER_TOP + WATER_HEIGHT;

    // Energy-buffer gauge — temporarily filled by stored pressure (white overlay).
    private static final int GAUGE_LEFT = 155;
    private static final int GAUGE_TOP = 19;
    private static final int GAUGE_WIDTH = 12;
    private static final int GAUGE_HEIGHT = 30;
    private static final int GAUGE_BOTTOM = GAUGE_TOP + GAUGE_HEIGHT;

    // Firebox flame, below the energy gauge.
    private static final int FLAME_X = 154;
    private static final int FLAME_Y = 53;
    private static final int FLAME_SIZE = 14;

    public SteamEngineScreen(SteamEngineScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        hoverTooltip(graphics, mouseX, mouseY, WATER_LEFT, WATER_TOP, WATER_WIDTH, WATER_HEIGHT, waterTooltip());
        hoverTooltip(graphics, mouseX, mouseY, GAUGE_LEFT, GAUGE_TOP, GAUGE_WIDTH, GAUGE_HEIGHT, gaugeTooltip());
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
        lines.add(fireboxLine(menu.getFirebox()));
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
        renderPressureInGauge(graphics);
        renderFlame(graphics);
    }

    /** Stored pressure shown in the energy-buffer gauge as a white overlay (energy/pressure split is TBD). */
    private void renderPressureInGauge(GuiGraphicsExtractor graphics) {
        int fillPixels = Math.round(menu.getPressureFraction() * GAUGE_HEIGHT);
        if (fillPixels <= 0) {
            return;
        }
        int x0 = leftPos + GAUGE_LEFT;
        int bottom = topPos + GAUGE_BOTTOM;
        graphics.fill(x0, bottom - fillPixels, x0 + GAUGE_WIDTH, bottom, 0xC8FFFFFF);
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
