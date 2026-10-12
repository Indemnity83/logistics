package com.logistics.core.lib.compat;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * The seam for land-claim and protection mods. Machines that change the world without a player
 * (farms break, plant and till through {@code setBlock}/{@code destroyBlock}, no fake player) ask
 * here before every write, passing the player who placed the machine.
 *
 * <p>Claim mods never see those writes as player actions, so this is where an integration would
 * check whether {@code owner} may build at {@code pos}. With no integration, everything is allowed.
 */
public final class ProtectionCompat {

    private ProtectionCompat() {}

    /**
     * Whether a machine owned by {@code owner} may modify the block at {@code pos}.
     *
     * @param owner the player who placed the machine, or {@code null} when unknown (placed by a
     *     non-player, or saved before owners were recorded)
     */
    public static boolean canModify(ServerLevel level, BlockPos pos, @Nullable UUID owner) {
        return true;
    }
}
