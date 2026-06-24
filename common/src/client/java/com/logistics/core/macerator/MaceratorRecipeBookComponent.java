package com.logistics.core.macerator;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsCore;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.recipebook.GhostSlots;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.network.chat.Component;
import com.logistics.core.lib.resource.ResourceId;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.display.RecipeDisplay;

import java.util.List;

/**
 * Recipe book component for the Macerator screen.
 * Shows all macerator recipes in a single custom tab.
 */
public class MaceratorRecipeBookComponent extends RecipeBookComponent<MaceratorScreenHandler> {

    private static final WidgetSprites FILTER_SPRITES = new WidgetSprites(
        ResourceId.in("minecraft", "recipe_book/furnace_filter_enabled").toIdentifier(),
        ResourceId.in("minecraft", "recipe_book/furnace_filter_disabled").toIdentifier(),
        ResourceId.in("minecraft", "recipe_book/furnace_filter_enabled_highlighted").toIdentifier(),
        ResourceId.in("minecraft", "recipe_book/furnace_filter_disabled_highlighted").toIdentifier()
    );

    private static final Component FILTER_NAME = Component.translatable("gui.recipebook.toggleRecipes.smeltable");

    private static final List<TabInfo> TABS = List.of(
        new TabInfo(LogisticsAutomation.BLOCK.MACERATOR.asItem(), LogisticsCore.RECIPE.MACERATOR_CATEGORY)
    );

    public MaceratorRecipeBookComponent(MaceratorScreenHandler handler) {
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
        collection.selectRecipes(contents, d -> d instanceof MaceratorRecipeDisplay);
    }

    @Override
    protected Component getRecipeFilterName() {
        return FILTER_NAME;
    }

    @Override
    protected void fillGhostRecipe(GhostSlots ghostSlots, RecipeDisplay display, ContextMap context) {
        ghostSlots.setResult(this.menu.getSlot(1), context, display.result());
        if (display instanceof MaceratorRecipeDisplay md) {
            ghostSlots.setInput(this.menu.getSlot(0), context, md.ingredient());
        }
    }
}
