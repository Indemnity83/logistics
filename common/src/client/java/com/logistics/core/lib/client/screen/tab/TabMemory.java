package com.logistics.core.lib.client.screen.tab;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * The player's intended open tab per side, keyed by screen type, for the rest of the client
 * session. Holds intent only: whether the tab can actually open is decided from live geometry.
 */
public final class TabMemory {
    private static final Map<String, EnumMap<TabSide, String>> OPEN = new HashMap<>();

    private TabMemory() {
    }

    public static @Nullable String get(String screenKey, TabSide side) {
        EnumMap<TabSide, String> sides = OPEN.get(screenKey);
        return sides == null ? null : sides.get(side);
    }

    /** Records {@code tabId} as the intended open tab, or clears the side when it is null. */
    public static void set(String screenKey, TabSide side, @Nullable String tabId) {
        if (tabId == null) {
            EnumMap<TabSide, String> sides = OPEN.get(screenKey);
            if (sides != null) {
                sides.remove(side);
                if (sides.isEmpty()) {
                    OPEN.remove(screenKey);
                }
            }
            return;
        }
        OPEN.computeIfAbsent(screenKey, key -> new EnumMap<>(TabSide.class)).put(side, tabId);
    }

    public static void clear() {
        OPEN.clear();
    }
}
