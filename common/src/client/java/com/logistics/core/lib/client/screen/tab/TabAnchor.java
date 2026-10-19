package com.logistics.core.lib.client.screen.tab;

import java.util.function.IntSupplier;

/**
 * Live view of a container screen's geometry. Every supplier is read again on each use: the
 * recipe book moves {@code left} without re-initializing the screen, and a resize changes all of
 * them on the same screen instance.
 *
 * @param left the GUI image's left edge ({@code leftPos})
 * @param top the GUI image's top edge ({@code topPos})
 * @param imageWidth the GUI image's width
 * @param screenWidth the screen's scaled width
 * @param screenHeight the screen's scaled height
 */
public record TabAnchor(
        IntSupplier left, IntSupplier top, IntSupplier imageWidth, IntSupplier screenWidth, IntSupplier screenHeight) {
}
