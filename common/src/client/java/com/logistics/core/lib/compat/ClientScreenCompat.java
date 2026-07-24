package com.logistics.core.lib.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

/**
 * Hides the per-version accessor for the client's currently open screen.
 *
 * <p><b>26.2:</b> the screen sits behind the GUI object — {@code Minecraft.getInstance().gui.screen()}.
 * <p><b>26.1 / 1.21.11 / 1.21.1:</b> the public {@code Minecraft.getInstance().screen} field.
 */
public final class ClientScreenCompat {

    private ClientScreenCompat() {}

    /** The client's currently open screen, or {@code null} when none is open. */
    @Nullable
    public static Screen currentScreen() {
        return Minecraft.getInstance().gui.screen();
    }
}
