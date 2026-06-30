package com.logistics.automation.alloysmelter;

import com.logistics.LogisticsAutomation;
import com.logistics.core.lib.resource.ResourceId;
import java.util.List;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.recipebook.GhostSlots;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.network.chat.Component;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.display.RecipeDisplay;

/**
 * Recipe book component for the Alloy Smelter screen.
 * Shows all alloy-smelter recipes in a single custom tab.
 */
public class AlloySmelterRecipeBookComponent extends RecipeBookComponent<AlloySmelterScreenHandler> {

    private static final WidgetSprites FILTER_SPRITES = new WidgetSprites(
        ResourceId.in("minecraft", "recipe_book/furnace_filter_enabled").toIdentifier(),
        ResourceId.in("minecraft", "recipe_book/furnace_filter_disabled").toIdentifier(),
        ResourceId.in("minecraft", "recipe_book/furnace_filter_enabled_highlighted").toIdentifier(),
        ResourceId.in("minecraft", "recipe_book/furnace_filter_disabled_highlighted").toIdentifier()
    );

    private static final Component FILTER_NAME = Component.translatable("gui.recipebook.toggleRecipes.smeltable");

    private static final List<TabInfo> TABS = List.of(
        new TabInfo(LogisticsAutomation.BLOCK.ALLOY_SMELTER.asItem(), LogisticsAutomation.RECIPE.ALLOY_SMELTER_CATEGORY)
    );

    public AlloySmelterRecipeBookComponent(AlloySmelterScreenHandler handler) {
        super(handler, TABS);
    }

    @Override
    protected WidgetSprites getFilterButtonTextures() {
        return FILTER_SPRITES;
    }

    @Override
    protected boolean isCraftingSlot(Slot slot) {
        return slot.index == AlloySmelterBlockEntity.INPUT_A_SLOT
                || slot.index == AlloySmelterBlockEntity.INPUT_B_SLOT
                || slot.index == AlloySmelterBlockEntity.PRIMARY_OUTPUT_SLOT;
    }

    @Override
    protected void selectMatchingRecipes(RecipeCollection collection, StackedItemContents contents) {
        collection.selectRecipes(contents, d -> d instanceof AlloySmelterRecipeDisplay);
    }

    @Override
    protected Component getRecipeFilterName() {
        return FILTER_NAME;
    }

    @Override
    protected void fillGhostRecipe(GhostSlots ghostSlots, RecipeDisplay display, ContextMap context) {
        ghostSlots.setResult(this.menu.getSlot(AlloySmelterBlockEntity.PRIMARY_OUTPUT_SLOT), context, display.result());
        if (display instanceof AlloySmelterRecipeDisplay d) {
            ghostSlots.setInput(this.menu.getSlot(AlloySmelterBlockEntity.INPUT_A_SLOT), context, d.inputA());
            ghostSlots.setInput(this.menu.getSlot(AlloySmelterBlockEntity.INPUT_B_SLOT), context, d.inputB());
        }
    }
}
