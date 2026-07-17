package com.logistics.automation.fabricator.jei;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsMod;
import com.logistics.automation.fabricator.FabricatorRecipe;
import com.logistics.automation.fabricator.SizedIngredient;
import com.logistics.core.lib.resource.ResourceId;
import java.util.Arrays;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * JEI recipe category for the Sequential Fabricator: a variable number of item ingredients combined into
 * one output. Ingredients are packed from the left, so the arrow and output sit immediately after the
 * last input; the category is sized for up to {@value #MAX_INPUTS} inputs (chipset recipes use one or two).
 */
public class FabricatorRecipeCategory implements IRecipeCategory<FabricatorRecipe> {

    public static final RecipeType<FabricatorRecipe> RECIPE_TYPE =
        RecipeType.create(LogisticsMod.MOD_ID, "fabricator", FabricatorRecipe.class);

    // Matches SequentialFabricatorScreen.TEXTURE; the filled progress arrow sprite lives at UV (199,35), 24x16.
    private static final ResourceId TEXTURE =
        LogisticsMod.modId("textures/gui/automation/sequential_fabricator.png");

    private static final int MAX_INPUTS = 3;
    private static final int INPUT_X0 = 8, INPUT_Y = 9, INPUT_STEP = 18;
    private static final int ARROW_Y = 10, ARROW_W = 24, ARROW_GAP = 2;
    private static final int OUTPUT_Y = 9, OUTPUT_GAP = 6;

    private static final int WIDTH = outputX(MAX_INPUTS) + 16;
    private static final int HEIGHT = 28;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;

    public FabricatorRecipeCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawableItemStack(
            new ItemStack(LogisticsAutomation.BLOCK.SEQUENTIAL_FABRICATOR));
        this.arrow = guiHelper.createDrawable(TEXTURE.toIdentifier(), 199, 35, ARROW_W, 16);
    }

    /** X of the progress arrow: immediately to the right of the packed inputs. */
    private static int arrowX(int inputCount) {
        return INPUT_X0 + inputCount * INPUT_STEP + ARROW_GAP;
    }

    /** X of the output slot: to the right of the arrow. */
    private static int outputX(int inputCount) {
        return arrowX(inputCount) + ARROW_W + OUTPUT_GAP;
    }

    @Override
    public RecipeType<FabricatorRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.logistics.automation.sequential_fabricator");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void draw(FabricatorRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        arrow.draw(guiGraphics, arrowX(recipe.ingredients().size()), ARROW_Y);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, FabricatorRecipe recipe, IFocusGroup focuses) {
        List<SizedIngredient> ingredients = recipe.ingredients();
        for (int i = 0; i < ingredients.size(); i++) {
            SizedIngredient input = ingredients.get(i);
            // Show each accepted item at the per-craft count, not a bare count-of-1 ingredient.
            List<ItemStack> stacks = Arrays.stream(input.ingredient().getItems())
                .map(stack -> stack.copyWithCount(input.count()))
                .toList();
            builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X0 + i * INPUT_STEP, INPUT_Y)
                .addItemStacks(stacks);
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, outputX(ingredients.size()), OUTPUT_Y)
            .addItemStack(recipe.getResultItem());
    }
}
