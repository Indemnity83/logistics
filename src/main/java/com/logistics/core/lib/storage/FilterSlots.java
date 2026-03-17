package com.logistics.core.lib.storage;

import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable, fixed-size list of item-ID strings representing a filter configuration.
 *
 * <p>Empty slots are represented as {@code ""} (empty string). The list always has exactly
 * {@code size} entries so slot indices are stable across load/save cycles.
 *
 * <p>Usage pattern in modules:
 * <pre>{@code
 * // Load
 * FilterSlots filters = FilterSlots.load(ctx.getCompoundTag(this, KEY), MAX_SLOTS);
 *
 * // Modify (returns new instance)
 * FilterSlots updated = filters.with(slot, itemId);
 *
 * // Save
 * if (updated.isEmpty()) ctx.remove(this, KEY);
 * else ctx.putCompoundTag(this, KEY, updated.toTag());
 * }</pre>
 */
public final class FilterSlots {
    private final List<String> slots;

    private FilterSlots(List<String> slots) {
        this.slots = slots;
    }

    /**
     * Load filter slots from a CompoundTag. Keys are slot indices as decimal strings ("0", "1", …).
     * Missing keys default to {@code ""}.
     */
    public static FilterSlots load(CompoundTag tag, int size) {
        List<String> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(NbtCompat.getString(tag, String.valueOf(i), ""));
        }
        return new FilterSlots(List.copyOf(list));
    }

    /**
     * Serialize to a CompoundTag. Only non-empty slots are written to keep the tag compact.
     */
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        for (int i = 0; i < slots.size(); i++) {
            String s = slots.get(i);
            if (!s.isEmpty()) tag.putString(String.valueOf(i), s);
        }
        return tag;
    }

    /** The item ID at {@code index}, or {@code ""} if the slot is empty. */
    public String get(int index) {
        return slots.get(index);
    }

    /** Number of slots (always equal to the {@code size} passed to {@link #load}). */
    public int size() {
        return slots.size();
    }

    /** Returns {@code true} when all slots are empty. */
    public boolean isEmpty() {
        return slots.stream().allMatch(String::isEmpty);
    }

    /**
     * Returns a new {@code FilterSlots} with {@code itemId} written at {@code index}.
     * Pass {@code null} or {@code ""} to clear the slot.
     */
    public FilterSlots with(int index, String itemId) {
        List<String> copy = new ArrayList<>(slots);
        copy.set(index, itemId == null ? "" : itemId);
        return new FilterSlots(List.copyOf(copy));
    }

    /** All slot values (including empty strings). Size equals {@link #size()}. */
    public List<String> asList() {
        return slots;
    }

    /** Only the non-empty slot values (order preserved, indices lost). */
    public List<String> nonEmpty() {
        return slots.stream().filter(s -> !s.isEmpty()).toList();
    }
}
