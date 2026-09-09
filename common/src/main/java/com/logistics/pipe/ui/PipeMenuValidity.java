package com.logistics.pipe.ui;

import com.logistics.core.lib.menu.MenuValidity;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import org.jetbrains.annotations.Nullable;

/**
 * The rule for how long a pipe-backed menu stays open: the pipe it edits must still be there, and
 * the player must still be close enough to it.
 *
 * <p>Shared by every pipe screen handler and by packet validation so the two cannot disagree —
 * a menu that stays open past what a packet accepts, or vice versa, is how an edit ends up
 * applied to a block the player can no longer see.
 *
 * <p>The reach itself comes from {@link MenuValidity}, so pipe menus and machine menus agree.
 */
public final class PipeMenuValidity {

    /** Interaction reach in blocks, measured from the block centre. See {@link MenuValidity#MAX_REACH}. */
    public static final double MAX_REACH = MenuValidity.MAX_REACH;

    private PipeMenuValidity() {}

    /** True while {@code distanceSqr} from a block centre is inside {@link #MAX_REACH}. */
    public static boolean isWithinReach(double distanceSqr) {
        return MenuValidity.isWithinReach(distanceSqr);
    }

    /** True while {@code player} is close enough to {@code pos} to interact with it. */
    public static boolean isWithinReach(Player player, BlockPos pos) {
        return MenuValidity.isWithinReach(player, pos);
    }

    /**
     * True while the menu should stay open, for handlers that hold their pipe directly rather than
     * through a {@link ContainerLevelAccess}.
     *
     * <p>A {@code null} pipe means the menu was built by the client-side factory and is not bound
     * to a block; only the server's instance is polled, so this closes it rather than guessing.
     */
    public static boolean stillValid(@Nullable PipeBlockEntity pipeEntity, Player player) {
        return pipeEntity != null
                && !pipeEntity.isRemoved()
                && isWithinReach(player, pipeEntity.getBlockPos());
    }

    /**
     * True while the menu should stay open: a live pipe still stands where it was opened and the
     * player is still in reach.
     *
     * <p>Returns {@code true} when the context has no level — the item-configuration mode, which
     * has no block to validate against and whose handlers gate on the held stack instead.
     *
     * <p>{@link net.minecraft.server.level.ServerPlayer} re-checks this every tick, so a broken
     * pipe closes the menu long before anything could be built in its place.
     */
    public static boolean stillValid(ContainerLevelAccess context, Player player) {
        return context.evaluate(
                (level, pos) -> level.getBlockEntity(pos) instanceof PipeBlockEntity pipe
                        && !pipe.isRemoved()
                        && isWithinReach(player, pos),
                true);
    }
}
