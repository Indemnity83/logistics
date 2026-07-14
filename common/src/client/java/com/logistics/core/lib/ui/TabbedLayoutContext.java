package com.logistics.core.lib.ui;

import net.minecraft.client.gui.components.Button;
import java.util.function.Consumer;

/**
 * A context provided to the {@link Tab#contentApplier()} lambda during screen initialization.
 * This allows defining which slots and buttons are part of this tab's visual layout.
 * 
 * @param <H> The type of the ScreenHandler (Menu) this context belongs to.
 */
public interface TabbedLayoutContext<H> {

    /** 
     * Maps a specific slot index from the Menu to this tab's content area.
     * 
     * @param slotIndex The index of the slot in the Menu.
     * @param x The X coordinate relative to the screen's left position.
     * @param y The Y coordinate relative to the screen's top position.
     */
    void addSlot(int slotIndex, int x, int y);

    /** 
     * Adds a button to this tab's content area.
     * 


     * @param text The display text for the button.
     * @param x The X coordinate relative to the screen's left position.
     * @param y The Y coordinate relative to the screen's top position.
     * @param action The callback for when the button is clicked.
     */
    void addButton(String text, int x, int y, Consumer<Button> action);

    /** 
     * @return The active Menu (ScreenHandler) associated with this screen.
     */
    H getMenu();
}
