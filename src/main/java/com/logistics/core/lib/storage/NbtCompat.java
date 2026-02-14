package com.logistics.core.lib.storage;

import net.minecraft.nbt.CompoundTag;

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
}