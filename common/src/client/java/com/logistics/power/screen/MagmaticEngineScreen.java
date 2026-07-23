package com.logistics.power.screen;

import com.logistics.LogisticsMod;
import com.logistics.core.lib.client.render.FluidBoxRenderer;
import com.logistics.core.lib.fluids.FluidDisplay;
import com.logistics.core.lib.power.HeatStage;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.power.engine.ui.MagmaticEngineScreenHandler;
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
 * Client-side GUI for the Magmatic Engine: a lava tank, an RF-buffer gauge, and a status readout (thermal
 * state, heat soak, attempted/delivered/wasted generation, burn time). Numeric detail is shown as hover
 * tooltips. Simpler than the Fuel Engine — no coolant, no temperature bar, no shutdown UI.
 */
public class MagmaticEngineScreen extends AbstractContainerScreen<MagmaticEngineScreenHandler> {

    private static final ResourceId TEXTURE = LogisticsMod.modId("textures/gui/power/magmatic_engine.png");
    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;

    private static final ResourceId CHARGE = LogisticsMod.modId("automation/charge");
    private static final ResourceId FLAME = ResourceId.in("minecraft", "container/furnace/lit_progress");

    // Lava tank frame is 18x60 in the texture (right-aligned); fluid fills the 16x58 interior (1px border).
    private static final int TANK_TOP = 14;
    private static final int TANK_BOTTOM = TANK_TOP + 58;
    private static final int TANK_WIDTH = 16;
    private static final int TANK_HEIGHT = TANK_BOTTOM - TANK_TOP;
    private static final int TANK_LEFT = 109;
    private static final int TANK_FRAME_LEFT = 108;
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

    // Lit-flame indicator, directly below the energy gauge (running = burning a batch).
    private static final int FLAME_X = 154;
    private static final int FLAME_Y = 53;
    private static final int FLAME_SIZE = 14;

    public MagmaticEngineScreen(MagmaticEngineScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        hoverTooltip(graphics, mouseX, mouseY,
                TANK_FRAME_LEFT, TANK_FRAME_TOP, TANK_FRAME_WIDTH, TANK_FRAME_HEIGHT, tankTooltip());
        hoverTooltip(graphics, mouseX, mouseY,
                ENERGY_FRAME_LEFT, ENERGY_FRAME_TOP, 14, 32, statusTooltip());
    }

    private void hoverTooltip(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, int x, int y, int w, int h, List<Component> lines) {
        if (mouseX >= leftPos + x && mouseX < leftPos + x + w && mouseY >= topPos + y && mouseY < topPos + y + h) {
            graphics.setTooltipForNextFrame(this.font, lines, Optional.empty(), mouseX, mouseY);
        }
    }

    private List<Component> tankTooltip() {
        int fluidId = menu.getLavaId();
        int amountMb = menu.getLavaAmountMb();
        if (fluidId < 0 || amountMb <= 0) {
            return List.of(Component.translatable("tooltip.logistics.fluid.tank_empty"));
        }
        Fluid fluid = BuiltInRegistries.FLUID.byId(fluidId);
        Component amount = Component.translatable("tooltip.logistics.fluid.tank_amount", amountMb)
                .withStyle(ChatFormatting.GRAY);
        return List.of(FluidDisplay.name(fluid), amount);
    }

    private List<Component> statusTooltip() {
        List<Component> lines = new ArrayList<>();
        HeatStage stage = menu.getStage();
        lines.add(Component.translatable(thermalKey(stage)).withStyle(thermalColor(stage)));
        lines.add(Component.translatable("tooltip.logistics.magmatic_engine.temperature", menu.getTemperature())
                .withStyle(ChatFormatting.GRAY));
        if (menu.isLit()) {
            lines.add(Component.translatable("tooltip.logistics.magmatic_engine.generation", menu.getAttempted())
                    .withStyle(ChatFormatting.GRAY));
            if (menu.getWasted() > 0) {
                lines.add(Component.translatable("tooltip.logistics.magmatic_engine.wasting", menu.getWasted())
                        .withStyle(ChatFormatting.GOLD));
            }
        }
        lines.add(Component.translatable(statusKey()).withStyle(ChatFormatting.GRAY));
        return lines;
    }

    private String statusKey() {
        boolean powered = menu.isPowered();
        boolean lit = menu.isLit();
        boolean bufferFull = menu.getEnergyBarHeight(10_000) >= 10_000;
        boolean hasLava = menu.getLavaAmountMb() > 0;
        if (!powered) {
            return lit
                    ? "tooltip.logistics.magmatic_engine.status.paused"
                    : "tooltip.logistics.magmatic_engine.status.redstone_disabled";
        }
        if (lit) {
            return bufferFull
                    ? "tooltip.logistics.magmatic_engine.status.output_full"
                    : "tooltip.logistics.magmatic_engine.status.generating";
        }
        if (hasLava) {
            return "tooltip.logistics.magmatic_engine.status.waiting";
        }
        return "tooltip.logistics.magmatic_engine.status.no_lava";
    }

    private static String thermalKey(HeatStage stage) {
        return switch (stage) {
            case WARM -> "tooltip.logistics.magmatic_engine.thermal.hot";
            case COOL -> "tooltip.logistics.magmatic_engine.thermal.warm";
            default -> "tooltip.logistics.magmatic_engine.thermal.cold";
        };
    }

    private static ChatFormatting thermalColor(HeatStage stage) {
        return switch (stage) {
            case WARM -> ChatFormatting.YELLOW;
            case COOL -> ChatFormatting.GREEN;
            default -> ChatFormatting.BLUE;
        };
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED, TEXTURE.toIdentifier(),
                leftPos, topPos, 0, 0, imageWidth, imageHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        renderTank(graphics, TANK_LEFT, menu.getLavaId(), menu.getLavaFillFraction());

        int energyHeight = menu.getEnergyBarHeight(ENERGY_HEIGHT);
        if (energyHeight > 0) {
            graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED, CHARGE.toIdentifier(),
                    ENERGY_WIDTH, ENERGY_HEIGHT, 0, ENERGY_HEIGHT - energyHeight,
                    leftPos + ENERGY_LEFT, topPos + ENERGY_TOP + (ENERGY_HEIGHT - energyHeight), ENERGY_WIDTH, energyHeight);
        }

        renderFlame(graphics);
    }

    /** Vanilla flame overlaid into the baked silhouette while the engine is lit (burning a batch). */
    private void renderFlame(GuiGraphicsExtractor graphics) {
        if (!menu.isLit()) {
            return;
        }
        graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED, FLAME.toIdentifier(),
                FLAME_SIZE, FLAME_SIZE, 0, 0,
                leftPos + FLAME_X, topPos + FLAME_Y, FLAME_SIZE, FLAME_SIZE);
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
