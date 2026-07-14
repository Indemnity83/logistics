package com.logistics.core.lib.ui;

import net.minecraft.resources.ResourceLocation;
import java.util.function.Consumer;

/**
 * Represents a single tab within a {@link TabbedContainerScreen}.
 * 
 * @param <H> The type of the ScreenHandler (Menu) this tab belongs to.
 */
public record Tab<H>(
        String name,
        ResourceLocation icon,
        TabSide side,
        Consumer<TabbedLayoutContext<H>> contentApplier
) {
    /** Defines which side of the screen the tab belongs to. */
    public enum TabSide {
        LEFT, RIGHT
    }
}
