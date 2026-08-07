package com.logistics.automation.transposer;

import com.logistics.LogisticsMod;
import com.logistics.core.lib.client.render.FluidBoxRenderer;
import com.logistics.core.lib.fluids.FluidDisplay;
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
import org.jetbrains.annotations.NotNull;

/**
 * Client-side GUI screen for the Transposer: a small input slot over a larger output slot, a droplet
 * progress gauge, an energy bar, and the tank.
 */
public class TransposerScreen extends AbstractContainerScreen<TransposerScreenHandler> {

    private static final ResourceLocation TEXTURE =
            LogisticsMod.modId("textures/gui/automation/transposer.png").toIdentifier();

    // Shared static energy-gauge bar sprite, drawn dark for empty + bright for fill.
    private static final ResourceLocation CHARGE = LogisticsMod.modId("automation/charge").toIdentifier();

    // Droplet progress gauge: gray "empty" frame always drawn, white "filled" frame revealed on top as
    // progress advances. Mirrored in place (both frames) for an Empty recipe, so it points left instead
    // of right.
    private static final int GAUGE_X = 82, GAUGE_Y = 24;
    // Sprite is 23x17: 23px wide, not 24 (the extra column is background padding); 17px tall, not 16
    // (the filled frame's anti-aliased tip bleeds one row past the flat-colored empty frame).
    private static final int GAUGE_WIDTH = 23, GAUGE_HEIGHT = 17;
    private static final int GAUGE_U = 199;
    private static final int GAUGE_EMPTY_V = 24;
    private static final int GAUGE_FILLED_V = 42;

    // Tank rectangle in the GUI (screen-local), matching the Crucible placeholder texture.
    private static final int TANK_LEFT = 116;
    private static final int TANK_TOP = 18;
    private static final int TANK_BOTTOM = 76;
    private static final int TANK_WIDTH = 16;
    private static final int TANK_HEIGHT = TANK_BOTTOM - TANK_TOP;
    private static final int OVERLAY_U = TANK_LEFT + 64;

    public TransposerScreen(TransposerScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        hoverTooltip(graphics, mouseX, mouseY, TANK_LEFT, TANK_TOP, TANK_WIDTH, TANK_HEIGHT, tankTooltip());
    }

    private void hoverTooltip(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, int w, int h, List<Component> lines) {
        if (mouseX >= leftPos + x && mouseX < leftPos + x + w && mouseY >= topPos + y && mouseY < topPos + y + h) {
            graphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
        }
    }

    private List<Component> tankTooltip() {
        int fluidId = menu.getTankFluidId();
        int amountMb = menu.getTankAmountMb();
        if (fluidId < 0 || amountMb <= 0) {
            return List.of(Component.translatable("tooltip.logistics.fluid.tank_empty"));
        }
        Fluid fluid = BuiltInRegistries.FLUID.byId(fluidId);
        var id = BuiltInRegistries.FLUID.getKey(fluid);
        Component amount = Component.translatable("tooltip.logistics.fluid.tank_amount", amountMb)
                .withStyle(ChatFormatting.GRAY);
        Component mod = Component.literal(modName(id.getNamespace()))
                .withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC);
        return List.of(FluidDisplay.name(fluid), amount, mod);
    }

    private static String modName(String namespace) {
        return namespace.isEmpty()
                ? namespace
                : Character.toUpperCase(namespace.charAt(0)) + namespace.substring(1);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        renderProgressGauge(graphics);

        // Energy gauge: dark "empty" bar full height, then the bright fill over the bottom `energyHeight` px.
        graphics.setColor(0.25f, 0.25f, 0.25f, 1.0f);
        graphics.blitSprite(CHARGE, 12, 30, 0, 0, leftPos + 10, topPos + 19, 12, 30);
        graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        int energyHeight = menu.getEnergyBarHeight();
        if (energyHeight > 0) {
            graphics.blitSprite(CHARGE, 12, 30, 0, 30 - energyHeight,
                leftPos + 10, topPos + 19 + (30 - energyHeight), 12, energyHeight);
        }

        renderTank(graphics);
    }

    /**
     * Draws the droplet gauge: the gray empty frame, then the white filled frame clipped to the current
     * progress width. Mirrored in place (both frames) for an Empty recipe, so the droplet points left
     * instead of right — 1.21.1's {@code blit} has no independent source/destination sizing, so unlike
     * the newer branches this uses a pose-stack flip around the gauge's own footprint instead of reversed
     * source sampling. The fill always reveals from the droplet's point toward its round end: cropped
     * from the sprite's right edge growing left, which the pose flip then mirrors when appropriate.
     */
    private void renderProgressGauge(GuiGraphics graphics) {
        int x = leftPos + GAUGE_X;
        int y = topPos + GAUGE_Y;
        boolean mirrored = !menu.isFillMode();

        if (mirrored) {
            graphics.pose().pushPose();
            graphics.pose().translate(2 * x + GAUGE_WIDTH, 0, 0);
            graphics.pose().scale(-1, 1, 1);
        }

        graphics.blit(TEXTURE, x, y, GAUGE_U, GAUGE_EMPTY_V, GAUGE_WIDTH, GAUGE_HEIGHT);

        int fillWidth = menu.getProgressFillWidth();
        if (fillWidth > 0) {
            int destX = x + GAUGE_WIDTH - fillWidth;
            int srcU = GAUGE_U + GAUGE_WIDTH - fillWidth;
            graphics.blit(TEXTURE, destX, y, srcU, GAUGE_FILLED_V, fillWidth, GAUGE_HEIGHT);
        }

        if (mirrored) {
            graphics.pose().popPose();
        }
    }

    private void renderTank(GuiGraphics graphics) {
        int fluidId = menu.getTankFluidId();
        float fraction = menu.getTankFillFraction();
        if (fluidId >= 0 && fraction > 0f) {
            Fluid fluid = BuiltInRegistries.FLUID.byId(fluidId);
            if (fluid != Fluids.EMPTY) {
                FluidBoxRenderer.Appearance appearance = FluidBoxRenderer.resolveForGui(fluid);
                if (appearance != null) {
                    TextureAtlasSprite sprite = appearance.sprite();
                    int tint = FluidBoxRenderer.opaque(appearance.tint());
                    float r = ((tint >> 16) & 0xFF) / 255f;
                    float g = ((tint >> 8) & 0xFF) / 255f;
                    float b = (tint & 0xFF) / 255f;
                    int fillPixels = Math.round(fraction * TANK_HEIGHT);
                    int x0 = leftPos + TANK_LEFT;
                    int bottomY = topPos + TANK_BOTTOM;
                    graphics.enableScissor(x0, bottomY - fillPixels, x0 + TANK_WIDTH, bottomY);
                    graphics.setColor(r, g, b, 1.0f);
                    for (int drawn = 0; drawn < fillPixels; drawn += 16) {
                        graphics.blit(x0, bottomY - drawn - 16, 0, 16, 16, sprite);
                    }
                    graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
                    graphics.disableScissor();
                }
            }
        }

        graphics.blit(TEXTURE, leftPos + TANK_LEFT, topPos + TANK_TOP, OVERLAY_U, TANK_TOP, TANK_WIDTH, TANK_HEIGHT);
    }
}
