package com.logistics.core.lib.storage;

import com.logistics.pipe.PipeContext;
import com.logistics.pipe.modules.Module;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

/**
 * Utility for serializing/deserializing Direction values in module storage.
 * Handles conversion between Direction and NBT string representation.
 */
public class DirectionSerializer {

    /**
     * Load a Direction from module storage.
     *
     * @param ctx The pipe context
     * @param module The module storing the direction
     * @param key The NBT key for the direction value
     * @return The stored direction, or null if not set
     */
    @Nullable
    public static Direction load(PipeContext ctx, Module module, String key) {
        CompoundTag state = ctx.moduleState(module.getStateKey());
        String directionStr = NbtCompat.getString(state, key, "");
        if (directionStr.isEmpty()) {
            return null;
        }
        try {
            return Direction.from3DDataValue(Integer.parseInt(directionStr));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Save a Direction to module storage.
     * Only saves if the direction has changed from current value.
     *
     * @param ctx The pipe context
     * @param module The module storing the direction
     * @param key The NBT key for the direction value
     * @param direction The direction to save, or null to clear
     */
    public static void save(PipeContext ctx, Module module, String key, @Nullable Direction direction) {
        Direction current = load(ctx, module, key);
        if (current == direction) {
            return;
        }

        if (direction == null) {
            ctx.remove(module, key);
        } else {
            ctx.saveString(module, key, String.valueOf(direction.get3DDataValue()));
        }

        ctx.markDirtyAndSync();
    }
}
