package com.logistics.core.lib.storage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

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
        return tag.contains(key, Tag.TAG_INT) ? tag.getInt(key) : defaultValue;
    }

    /**
     * Read a long from NBT with a default value.
     *
     * <p><b>mc/1.21.11 implementation:</b> Uses Optional-based API.
     * <p><b>mc/1.21.1 backport:</b> Replace with {@code tag.contains(key, Tag.TAG_LONG) ? tag.getLong(key) : defaultValue}
     */
    public static long getLong(CompoundTag tag, String key, long defaultValue) {
        return tag.contains(key, Tag.TAG_LONG) ? tag.getLong(key) : defaultValue;
    }

    /**
     * Read a double from NBT with a default value.
     *
     * <p><b>mc/1.21.11 implementation:</b> Uses Optional-based API.
     * <p><b>mc/1.21.1 backport:</b> Replace with {@code tag.contains(key, Tag.TAG_DOUBLE) ? tag.getDouble(key) : defaultValue}
     */
    public static double getDouble(CompoundTag tag, String key, double defaultValue) {
        return tag.contains(key, Tag.TAG_DOUBLE) ? tag.getDouble(key) : defaultValue;
    }

    /**
     * Read a float from NBT with a default value.
     *
     * <p><b>mc/1.21.11 implementation:</b> Uses Optional-based API.
     * <p><b>mc/1.21.1 backport:</b> Replace with {@code tag.contains(key, Tag.TAG_FLOAT) ? tag.getFloat(key) : defaultValue}
     */
    public static float getFloat(CompoundTag tag, String key, float defaultValue) {
        return tag.contains(key, Tag.TAG_FLOAT) ? tag.getFloat(key) : defaultValue;
    }

    /**
     * Read a boolean from NBT with a default value.
     *
     * <p><b>mc/1.21.11 implementation:</b> Uses Optional-based API.
     * <p><b>mc/1.21.1 backport:</b> Replace with {@code tag.contains(key, Tag.TAG_BYTE) ? tag.getBoolean(key) : defaultValue}
     */
    public static boolean getBoolean(CompoundTag tag, String key, boolean defaultValue) {
        return tag.contains(key, Tag.TAG_BYTE) ? tag.getBoolean(key) : defaultValue;
    }

    /**
     * Read a string from NBT with a default value.
     *
     * <p><b>mc/1.21.11 implementation:</b> Uses Optional-based API.
     * <p><b>mc/1.21.1 backport:</b> Replace with {@code tag.contains(key, Tag.TAG_STRING) ? tag.getString(key) : defaultValue}
     */
    public static String getString(CompoundTag tag, String key, String defaultValue) {
        return tag.contains(key, Tag.TAG_STRING) ? tag.getString(key) : defaultValue;
    }

    // ==================== Array Getters ====================

    /**
     * Read an int array from NBT with a default value.
     *
     * <p><b>mc/1.21.11 implementation:</b> Uses Optional-based API.
     * <p><b>mc/1.21.1 backport:</b> Replace with {@code tag.contains(key, Tag.TAG_INT_ARRAY) ? tag.getIntArray(key) : defaultValue}
     */
    public static int[] getIntArray(CompoundTag tag, String key, int[] defaultValue) {
        return tag.contains(key, Tag.TAG_INT_ARRAY) ? tag.getIntArray(key) : defaultValue;
    }

    // ==================== Compound Tag Helpers ====================

    /**
     * Read a compound tag from NBT, or return an empty tag if missing.
     *
     * <p><b>mc/1.21.11 implementation:</b> {@code tag.getCompound(key).orElse(new CompoundTag())}
     * <p><b>mc/1.21.1 backport:</b> Same code works! (getCompound returns CompoundTag directly)
     */
    public static CompoundTag getCompoundOrEmpty(CompoundTag tag, String key) {
        // MC 1.21.1: getCompound() returns CompoundTag directly (empty if missing)
        return tag.contains(key, Tag.TAG_COMPOUND) ? tag.getCompound(key) : new CompoundTag();
    }

    /**
     * Execute action if compound tag exists at key.
     *
     * <p><b>mc/1.21.11 implementation:</b> {@code tag.getCompound(key).ifPresent(action)}
     * <p><b>mc/1.21.1 backport:</b> Same code works!
     */
    public static void ifHasCompound(CompoundTag tag, String key, Consumer<CompoundTag> action) {
        // MC 1.21.1: Check if exists, then get directly
        if (tag.contains(key, Tag.TAG_COMPOUND)) {
            action.accept(tag.getCompound(key));
        }
    }

    // ==================== List Tag Helpers ====================

    /**
     * Execute action if list tag exists at key.
     *
     * <p><b>mc/1.21.11 implementation:</b> {@code tag.getList(key).ifPresent(action)}
     * <p><b>mc/1.21.1 backport:</b> {@code if (tag.contains(key)) action.accept(tag.getList(key, 10));}
     */
    public static void ifHasList(CompoundTag tag, String key, Consumer<ListTag> action) {
        // MC 1.21.1: getList(key, type) requires type parameter (10 = TAG_COMPOUND)
        if (tag.contains(key, Tag.TAG_LIST)) {
            action.accept(tag.getList(key, Tag.TAG_COMPOUND));
        }
    }

    /**
     * Get list tag or return empty list if missing.
     *
     * <p><b>mc/1.21.11 implementation:</b> {@code tag.getList(key).orElseGet(ListTag::new)}
     * <p><b>mc/1.21.1 backport:</b> Access via tag.get() to avoid element-type filtering in getList(key, type).
     * MC 1.21.1's getList(key, type) returns empty if the stored list element type doesn't match,
     * which would break reading string lists (e.g. filter slots) using TAG_COMPOUND.
     */
    public static ListTag getListOrEmpty(CompoundTag tag, String key) {
        if (!tag.contains(key, Tag.TAG_LIST)) {
            return new ListTag();
        }
        Tag t = tag.get(key);
        return t instanceof ListTag listTag ? listTag : new ListTag();
    }

    /**
     * Execute action if compound tag exists at index in list.
     *
     * <p><b>mc/1.21.11 implementation:</b> {@code list.getCompound(index).ifPresent(action)}
     * <p><b>mc/1.21.1 backport:</b> Same code works!
     */
    public static void ifHasCompoundAt(ListTag list, int index, Consumer<CompoundTag> action) {
        // MC 1.21.1: getCompound(index) returns CompoundTag directly
        if (index >= 0 && index < list.size()) {
            action.accept(list.getCompound(index));
        }
    }

    /**
     * Get string at index in list with a default value.
     *
     * <p><b>mc/1.21.11 implementation:</b> {@code list.getString(index).orElse(defaultValue)}
     * <p><b>mc/1.21.1 backport:</b> {@code index < list.size() ? list.getString(index) : defaultValue}
     */
    public static String getStringAt(ListTag list, int index, String defaultValue) {
        // MC 1.21.1: getString(index) returns String directly
        if (index >= 0 && index < list.size()) {
            return list.getString(index);
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
        // MC 1.21.1: getIntArray() returns int[] directly
        if (tag.contains(key, Tag.TAG_INT_ARRAY)) {
            action.accept(tag.getIntArray(key));
        }
    }
}