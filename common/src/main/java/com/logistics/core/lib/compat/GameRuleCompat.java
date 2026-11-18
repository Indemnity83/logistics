package com.logistics.core.lib.compat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;

/**
 * The cross-version seam for reading game rules: mc/26.x reads typed {@code GameRule}s through
 * {@code GameRules#get}, mc/1.21.x reads {@code GameRules.Key}s through {@code getInt}/{@code getBoolean}.
 */
public final class GameRuleCompat {

    private GameRuleCompat() {}

    /** The {@code randomTickSpeed} game rule: random ticks per chunk section per tick. */
    public static int randomTickSpeed(ServerLevel level) {
        return level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
    }
}
