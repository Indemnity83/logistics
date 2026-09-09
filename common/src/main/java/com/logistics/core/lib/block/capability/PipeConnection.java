package com.logistics.core.lib.block.capability;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;

/**
 * Interface for blocks that can connect to pipes.
 *
 * <p>Blocks implement this interface and register with the loader-specific capability system
 * to declare their pipe connectivity and item transfer behavior.
 */
public interface PipeConnection {

    /**
     * Represents the type of connection a block provides to pipes.
     */
    enum Type implements StringRepresentable {
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
        INVENTORY("inventory"),

        /**
         * Connection to a power source (e.g. a Battery). Rendered as a connection arm, but items
         * are never routed toward it — it is a power link, not an item route or inventory.
         */
        POWER("power");

        /**
         * Packs the six per-direction connection types into one value, so a change of *type* on a
         * side that stays connected is as visible as a side connecting or disconnecting.
         *
         * @param byDirection one type per {@link net.minecraft.core.Direction} ordinal
         */
        public static int signature(Type[] byDirection) {
            int signature = 0;
            for (int i = 0; i < byDirection.length; i++) {
                Type type = byDirection[i] == null ? NONE : byDirection[i];
                signature |= type.ordinal() << (i * 2);
            }
            return signature;
        }

        private static final Map<String, Type> BY_NAME =
                Stream.of(values()).collect(Collectors.toMap(Type::getSerializedName, type -> type));

        private final String name;

        Type(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        /**
         * Get the Type by its serialized name.
         *
         * @param name the serialized name
         * @return the Type, or NONE if not found
         */
        public static Type fromSerializedName(String name) {
            return BY_NAME.getOrDefault(name, NONE);
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
