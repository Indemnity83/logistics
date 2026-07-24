package com.logistics.power.screen;

import com.logistics.LogisticsMod;
import com.logistics.core.lib.client.render.FluidBoxRenderer;
import com.logistics.core.lib.fluids.FluidDisplay;
import com.logistics.core.lib.power.HeatStage;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.power.engine.ui.MagmaticEngineScreenHandler;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Client-side GUI for the Magmatic Engine: a lava tank, an RF-buffer gauge, and a status readout, with
 * numeric detail shown as hover tooltips.
 */
public class MagmaticEngineScreen extends AbstractContainerScreen<MagmaticEngineScreenHandler> {

    private static final ResourceLocation TEXTURE =
            LogisticsMod.modId("textures/gui/power/magmatic_engine.png").toIdentifier();
    private static final ResourceLocation CHARGE = LogisticsMod.modId("automation/charge").toIdentifier();
    private static final ResourceLocation FLAME =
            ResourceId.in("minecraft", "container/furnace/lit_progress").toIdentifier();

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
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        hoverTooltip(graphics, mouseX, mouseY,
                TANK_FRAME_LEFT, TANK_FRAME_TOP, TANK_FRAME_WIDTH, TANK_FRAME_HEIGHT, tankTooltip());
        hoverTooltip(graphics, mouseX, mouseY,
                ENERGY_FRAME_LEFT, ENERGY_FRAME_TOP, 14, 32, statusTooltip());
    }

    private void hoverTooltip(
            GuiGraphics graphics, int mouseX, int mouseY, int x, int y, int w, int h, List<Component> lines) {
        if (isHovering(x, y, w, h, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
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
    protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        renderTank(graphics, TANK_LEFT, menu.getLavaId(), menu.getLavaFillFraction());

        int energyHeight = menu.getEnergyBarHeight(ENERGY_HEIGHT);
        if (energyHeight > 0) {
            graphics.blitSprite(
                    CHARGE, ENERGY_WIDTH, ENERGY_HEIGHT, 0, ENERGY_HEIGHT - energyHeight,
                    leftPos + ENERGY_LEFT, topPos + ENERGY_TOP + (ENERGY_HEIGHT - energyHeight), ENERGY_WIDTH, energyHeight);
        }

        renderFlame(graphics);
    }

    /** Vanilla flame overlaid into the baked silhouette while the engine is lit (burning a batch). */
    private void renderFlame(GuiGraphics graphics) {
        if (!menu.isLit()) {
            return;
        }
        graphics.blitSprite(
                FLAME, FLAME_SIZE, FLAME_SIZE, 0, 0,
                leftPos + FLAME_X, topPos + FLAME_Y, FLAME_SIZE, FLAME_SIZE);
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
        int tint = FluidBoxRenderer.opaque(appearance.tint());
        float r = ((tint >> 16) & 0xFF) / 255f;
        float g = ((tint >> 8) & 0xFF) / 255f;
        float b = (tint & 0xFF) / 255f;
        int x0 = leftPos + left;
        int bottomY = topPos + TANK_BOTTOM;
        int fillPixels = Math.round(fraction * TANK_HEIGHT);
        graphics.enableScissor(x0, bottomY - fillPixels, x0 + TANK_WIDTH, bottomY);
        graphics.setColor(r, g, b, 1.0f);
        for (int drawn = 0; drawn < fillPixels; drawn += 16) {
            graphics.blit(x0, bottomY - drawn - 16, 0, 16, 16, sprite);
        }
        graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        graphics.disableScissor();
    }
}
