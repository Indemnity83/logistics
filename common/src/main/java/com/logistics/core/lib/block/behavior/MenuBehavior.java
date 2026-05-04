package com.logistics.core.lib.block.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Behavior module for blocks that provide GUI/menu interactions.
 */
public final class MenuBehavior {

    private MenuBehavior() {}

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

    /**
     * Try to open a menu/GUI for the block entity at the given position.
     * Only works if the block entity implements HasMenu.
     *
     * <p>This is intended for use in Block.useWithoutItem() or Block.useItemOn()
     * to conditionally open GUIs only for blocks that support them.
     *
     * @param level The world
     * @param pos The block position
     * @param player The player trying to open the menu
     * @return SUCCESS if menu was opened, PASS if no menu available
     */
    public static InteractionResult tryOpenMenu(Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof HasMenu hasMenu) {
            player.openMenu(hasMenu.createMenuProvider());
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }
}
