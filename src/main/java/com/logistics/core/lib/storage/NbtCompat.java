package com.logistics.core.lib.storage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.function.Consumer;

/**
 * NBT compatibility layer to abstract API differences between Minecraft versions.
 *
 * <p><b>Minecraft 1.21.5+ (current mc/1.21.11):</b>
 * NBT getters return {@code Optional<T>}, requiring {@code .orElse(default)}.
 *
 * <p><b>Minecraft 1.21.1-1.21.4:</b>
 * NBT getters return primitives directly, with implicit defaults when keys are missing.
 *
 * <p>This abstraction allows code to work across versions - when backporting to mc/1.21.1,
 * simply change the implementation in this class to use the old API.
 *
 * <p><b>Usage:</b>
 * <pre>{@code
 * // Instead of:
 * int value = nbt.getInt("key").orElse(42);  // mc/1.21.5+
 *
 * // Write:
 * int value = NbtCompat.getInt(nbt, "key", 42);  // Works on all versions
 * }</pre>
 */
public final class NbtCompat {
    private NbtCompat() {}

    // ==================== Primitive Getters ====================

    /**
     * Read an int from NBT with a default value.
     *
     * <p><b>mc/1.21.11 implementation:</b> Uses Optional-based API.
     * <p><b>mc/1.21.1 backport:</b> Replace with {@code tag.contains(key, Tag.TAG_INT) ? tag.getInt(key) : defaultValue}
     */
    public static int getInt(CompoundTag tag, String key, int defaultValue) {
        return tag.getInt(key).orElse(defaultValue);
    }

    /**
     * Read a long from NBT with a default value.
     *
     * <p><b>mc/1.21.11 implementation:</b> Uses Optional-based API.
     * <p><b>mc/1.21.1 backport:</b> Replace with {@code tag.contains(key, Tag.TAG_LONG) ? tag.getLong(key) : defaultValue}
     */
    public static long getLong(CompoundTag tag, String key, long defaultValue) {
        return tag.getLong(key).orElse(defaultValue);
    }

    /**
     * Read a double from NBT with a default value.
     *
     * <p><b>mc/1.21.11 implementation:</b> Uses Optional-based API.
     * <p><b>mc/1.21.1 backport:</b> Replace with {@code tag.contains(key, Tag.TAG_DOUBLE) ? tag.getDouble(key) : defaultValue}
     */
    public static double getDouble(CompoundTag tag, String key, double defaultValue) {
        return tag.getDouble(key).orElse(defaultValue);
    }

    /**
     * Read a float from NBT with a default value.
     *
     * <p><b>mc/1.21.11 implementation:</b> Uses Optional-based API.
     * <p><b>mc/1.21.1 backport:</b> Replace with {@code tag.contains(key, Tag.TAG_FLOAT) ? tag.getFloat(key) : defaultValue}
     */
    public static float getFloat(CompoundTag tag, String key, float defaultValue) {
        return tag.getFloat(key).orElse(defaultValue);
    }

    /**
     * Read a boolean from NBT with a default value.
     *
     * <p><b>mc/1.21.11 implementation:</b> Uses Optional-based API.
     * <p><b>mc/1.21.1 backport:</b> Replace with {@code tag.contains(key, Tag.TAG_BYTE) ? tag.getBoolean(key) : defaultValue}
     */
    public static boolean getBoolean(CompoundTag tag, String key, boolean defaultValue) {
        return tag.getBoolean(key).orElse(defaultValue);
    }

    /**
     * Read a string from NBT with a default value.
     *
     * <p><b>mc/1.21.11 implementation:</b> Uses Optional-based API.
     * <p><b>mc/1.21.1 backport:</b> Replace with {@code tag.contains(key, Tag.TAG_STRING) ? tag.getString(key) : defaultValue}
     */
    public static String getString(CompoundTag tag, String key, String defaultValue) {
        return tag.getString(key).orElse(defaultValue);
    }

    // ==================== Array Getters ====================

    /**
     * Read an int array from NBT with a default value.
     *
     * <p><b>mc/1.21.11 implementation:</b> Uses Optional-based API.
     * <p><b>mc/1.21.1 backport:</b> Replace with {@code tag.contains(key, Tag.TAG_INT_ARRAY) ? tag.getIntArray(key) : defaultValue}
     */
    public static int[] getIntArray(CompoundTag tag, String key, int[] defaultValue) {
        return tag.getIntArray(key).orElse(defaultValue);
    }

    // ==================== Compound Tag Helpers ====================

    /**
     * Read a compound tag from NBT, or return an empty tag if missing.
     *
     * <p><b>mc/1.21.11 implementation:</b> {@code tag.getCompound(key).orElse(new CompoundTag())}
     * <p><b>mc/1.21.1 backport:</b> Same code works! (getCompound returns CompoundTag directly)
     */
    public static CompoundTag getCompoundOrEmpty(CompoundTag tag, String key) {
        return tag.getCompound(key).orElse(new CompoundTag());
    }

    /**
     * Execute action if compound tag exists at key.
     *
     * <p><b>mc/1.21.11 implementation:</b> {@code tag.getCompound(key).ifPresent(action)}
     * <p><b>mc/1.21.1 backport:</b> Same code works!
     */
    public static void ifHasCompound(CompoundTag tag, String key, Consumer<CompoundTag> action) {
        tag.getCompound(key).ifPresent(action);
    }

    // ==================== List Tag Helpers ====================

    /**
     * Execute action if list tag exists at key.
     *
     * <p><b>mc/1.21.11 implementation:</b> {@code tag.getList(key).ifPresent(action)}
     * <p><b>mc/1.21.1 backport:</b> {@code if (tag.contains(key)) action.accept(tag.getList(key, 10));}
     */
    public static void ifHasList(CompoundTag tag, String key, Consumer<ListTag> action) {
        tag.getList(key).ifPresent(action);
    }

    /**
     * Get list tag or return empty list if missing.
     *
     * <p><b>mc/1.21.11 implementation:</b> {@code tag.getList(key).orElseGet(ListTag::new)}
     * <p><b>mc/1.21.1 backport:</b> {@code tag.contains(key) ? tag.getList(key, 10) : new ListTag()}
     */
    public static ListTag getListOrEmpty(CompoundTag tag, String key) {
        return tag.getList(key).orElseGet(ListTag::new);
    }

    /**
     * Execute action if compound tag exists at index in list.
     *
     * <p><b>mc/1.21.11 implementation:</b> {@code list.getCompound(index).ifPresent(action)}
     * <p><b>mc/1.21.1 backport:</b> Same code works!
     */
    public static void ifHasCompoundAt(ListTag list, int index, Consumer<CompoundTag> action) {
        if (index >= 0 && index < list.size()) {
            list.getCompound(index).ifPresent(action);
        }
    }

    /**
     * Get string at index in list with a default value.
     *
     * <p><b>mc/1.21.11 implementation:</b> {@code list.getString(index).orElse(defaultValue)}
     * <p><b>mc/1.21.1 backport:</b> {@code index < list.size() ? list.getString(index) : defaultValue}
     */
    public static String getStringAt(ListTag list, int index, String defaultValue) {
        if (index >= 0 && index < list.size()) {
            return list.getString(index).orElse(defaultValue);
        }
        return defaultValue;
    }

    // ==================== Int Array Helper ====================

    /**
     * Execute action if int array exists at key.
     *
     * <p><b>mc/1.21.11 implementation:</b> {@code tag.getIntArray(key).ifPresent(action)}
     * <p><b>mc/1.21.1 backport:</b> Same code works!
     */
    public static void ifHasIntArray(CompoundTag tag, String key, Consumer<int[]> action) {
        tag.getIntArray(key).ifPresent(action);
    }
}