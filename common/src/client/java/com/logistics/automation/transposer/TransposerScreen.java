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
 * Client-side GUI screen for the Transposer. No progress arrow or energy bar — the Transposer has no RF.
 */
public class TransposerScreen extends AbstractContainerScreen<TransposerScreenHandler> {

    private static final ResourceLocation TEXTURE =
            LogisticsMod.modId("textures/gui/automation/transposer.png").toIdentifier();

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

        renderTank(graphics);
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
