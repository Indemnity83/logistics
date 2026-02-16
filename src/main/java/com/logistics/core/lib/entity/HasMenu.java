package com.logistics.core.lib.entity;

import net.minecraft.world.MenuProvider;

/**
 * Marker interface for block entities that provide a GUI/menu.
 * Block entities implementing this interface can be opened by players.
 */
public interface HasMenu {
    /**
     * Create the menu provider for this block entity.
     * The menu provider contains the display name and creates the menu when opened.
     *
     * @return The menu provider
     */
    MenuProvider createMenuProvider();
}
