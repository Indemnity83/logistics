package com.logistics.core.macerator;

import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.inventory.Slot;

import java.util.List;

/**
 * Recipe book component for the Macerator screen.
 * Shows all macerator recipes in a single custom tab.
 */
public class MaceratorRecipeBookComponent extends RecipeBookComponent {

    private static final WidgetSprites FILTER_SPRITES = new WidgetSprites(
        ResourceLocation.withDefaultNamespace("recipe_book/furnace_filter_enabled"),
        ResourceLocation.withDefaultNamespace("recipe_book/furnace_filter_disabled"),
        ResourceLocation.withDefaultNamespace("recipe_book/furnace_filter_enabled_highlighted"),
        ResourceLocation.withDefaultNamespace("recipe_book/furnace_filter_disabled_highlighted")
    );

    private static final Component FILTER_NAME = Component.translatable("gui.recipebook.toggleRecipes.smeltable");

    @Override
    protected void initFilterButtonTextures() {
        this.filterButton.initTextureValues(FILTER_SPRITES);
    }

    @Override
    protected Component getRecipeFilterName() {
        return FILTER_NAME;
    }

    @Override
    public void setupGhostRecipe(RecipeHolder<?> recipe, List<Slot> slots) {
        // No-op: Macerator recipe book is not integrated with vanilla recipe placement in MC 1.21.1
    }
}
