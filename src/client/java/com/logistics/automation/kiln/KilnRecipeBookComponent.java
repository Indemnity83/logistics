package com.logistics.automation.kiln;

import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.recipebook.GhostSlots;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.gui.screens.recipebook.SearchRecipeBookCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.display.FurnaceRecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplay;

import java.util.List;

/**
 * Recipe book component for the Kiln screen.
 * Shows all vanilla smelting recipes using furnace tabs and filter sprites.
 */
public class KilnRecipeBookComponent extends RecipeBookComponent<KilnScreenHandler> {

    private static final WidgetSprites FILTER_SPRITES = new WidgetSprites(
        Identifier.withDefaultNamespace("recipe_book/furnace_filter_enabled"),
        Identifier.withDefaultNamespace("recipe_book/furnace_filter_disabled"),
        Identifier.withDefaultNamespace("recipe_book/furnace_filter_enabled_highlighted"),
        Identifier.withDefaultNamespace("recipe_book/furnace_filter_disabled_highlighted")
    );

    private static final Component FILTER_NAME = Component.translatable("gui.recipebook.toggleRecipes.smeltable");

    private static final List<TabInfo> TABS = List.of(
        new TabInfo(SearchRecipeBookCategory.FURNACE),
        new TabInfo(Items.PORKCHOP, RecipeBookCategories.FURNACE_FOOD),
        new TabInfo(Items.STONE, RecipeBookCategories.FURNACE_BLOCKS),
        new TabInfo(Items.LAVA_BUCKET, Items.EMERALD, RecipeBookCategories.FURNACE_MISC)
    );

    public KilnRecipeBookComponent(KilnScreenHandler handler) {
        super(handler, TABS);
    }

    @Override
    protected WidgetSprites getFilterButtonTextures() {
        return FILTER_SPRITES;
    }

    @Override
    protected boolean isCraftingSlot(Slot slot) {
        return slot.index == 0 || slot.index == 1;
    }

    @Override
    protected void selectMatchingRecipes(RecipeCollection collection, StackedItemContents contents) {
        collection.selectRecipes(contents, d -> d instanceof FurnaceRecipeDisplay);
    }

    @Override
    protected Component getRecipeFilterName() {
        return FILTER_NAME;
    }

    @Override
    protected void fillGhostRecipe(GhostSlots ghostSlots, RecipeDisplay display, ContextMap context) {
        ghostSlots.setResult(this.menu.getSlot(1), context, display.result());
        if (display instanceof FurnaceRecipeDisplay furnaceDisplay) {
            ghostSlots.setInput(this.menu.getSlot(0), context, furnaceDisplay.ingredient());
        }
    }
}
