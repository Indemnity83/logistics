package com.logistics.core.lib.filter;

import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.lib.items.ItemMatcher;
import com.logistics.core.lib.resource.ResourceId;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private volatile Set<String> resolvedIds;

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

    /** Returns true if this item IS in the filter list. */
    public boolean included(ItemStack stack) {
        Set<String> ids = resolveItemIds();
        return !ids.isEmpty() && ids.contains(ItemMatcher.itemId(stack));
    }

    /** Returns true if this item is NOT in the filter list (and the filter is non-empty). */
    public boolean excluded(ItemStack stack) {
        Set<String> ids = resolveItemIds();
        return !ids.isEmpty() && !ids.contains(ItemMatcher.itemId(stack));
    }

    /**
     * Returns the subset of non-empty slot values that successfully resolve to a registered item.
     * Stale IDs (e.g. from an unloaded mod) are silently excluded.
     */
    public Set<String> resolveItemIds() {
        Set<String> cached = resolvedIds;
        if (cached != null) return cached;
        Set<String> result = new HashSet<>();
        for (String id : nonEmpty()) {
            ResourceId rid = ResourceId.tryParse(id);
            if (rid != null && BuiltInRegistries.ITEM.get(rid.toIdentifier()).isPresent()) {
                result.add(id);
            }
        }
        resolvedIds = cached = Set.copyOf(result);
        return cached;
    }
}
