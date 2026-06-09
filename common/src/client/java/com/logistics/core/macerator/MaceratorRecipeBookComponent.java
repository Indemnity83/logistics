package com.logistics.core.macerator;
import com.logistics.core.lib.resource.ResourceId;

import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.inventory.Slot;

import java.util.List;

/**
 * Recipe book component for the Macerator screen.
 * Shows all macerator recipes in a single custom tab.
 */
public class MaceratorRecipeBookComponent extends RecipeBookComponent {

    private static final WidgetSprites FILTER_SPRITES = new WidgetSprites(
        ResourceId.in("minecraft", "recipe_book/furnace_filter_enabled").toIdentifier(),
        ResourceId.in("minecraft", "recipe_book/furnace_filter_disabled").toIdentifier(),
        ResourceId.in("minecraft", "recipe_book/furnace_filter_enabled_highlighted").toIdentifier(),
        ResourceId.in("minecraft", "recipe_book/furnace_filter_disabled_highlighted").toIdentifier()
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
