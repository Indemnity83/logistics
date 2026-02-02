package com.logistics.core.lib.pipe;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.Direction;

/**
 * Interface for blocks that can connect to pipes.
 *
 * <p>Blocks implement this interface and register with {@link PipeConnectionRegistry#SIDED}
 * to declare their pipe connectivity and item transfer behavior.
 *
 * <p>Example registration for a quarry that accepts pipes from above but rejects items:
 * <pre>{@code
 * PipeConnectionRegistry.SIDED.registerForBlockEntity(
 *     (quarry, direction) -> direction == Direction.UP ? quarry : null,
 *     QUARRY_BLOCK_ENTITY);
 * }</pre>
 */
public interface PipeConnection {

    /**
     * Represents the type of connection a block provides to pipes.
     */
    enum Type implements StringIdentifiable {
        /**
         * No connection allowed.
         */
        NONE("none"),

        /**
         * Connection to another pipe.
         */
        PIPE("pipe"),

        /**
         * Connection to an inventory (chest, furnace, etc.) or similar storage.
         */
        INVENTORY("inventory");

        private final String name;

        Type(String name) {
            this.name = name;
        }

        @Override
        public String asString() {
            return name;
        }
    }

    /**
     * Get the connection type for the given direction.
     *
     * @param direction the direction the pipe is connecting from
     * @return the connection type; never null (use {@link Type#NONE} if no connection allowed)
     */
    Type getConnectionType(Direction direction);

    /**
     * Attempt to add an item to this block from the given direction.
     *
     * @param from the direction the item is coming from
     * @param stack the item stack to add
     * @return true if the item was accepted, false otherwise
     */
    boolean addItem(Direction from, ItemStack stack);

    /**
     * Check if this block can accept an item from the given direction.
     *
     * @param from the direction the item would come from
     * @param stack the item stack to check
     * @return true if the item could be accepted, false otherwise
     */
    default boolean canAcceptFrom(Direction from, ItemStack stack) {
        return false;
    }
}
