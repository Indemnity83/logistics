package com.logistics.forestry.farm;

import com.logistics.core.lib.compat.NbtCompat;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A tend farm's Plots: every position it has harvested, with the germling that replants it, and
 * whether it is an Empty Plot waiting for that germling. Saved with the block entity, never copied
 * to the dropped item.
 */
final class FarmPlots {

    private static final class Plot {
        final ItemStack germling;
        boolean empty;

        Plot(ItemStack germling, boolean empty) {
            this.germling = germling;
            this.empty = empty;
        }
    }

    private final Map<BlockPos, Plot> plots = new LinkedHashMap<>();
    private int emptyCount;

    /** Remembers {@code pos} as a Plot of {@code germling}, empty or planted. */
    void remember(BlockPos pos, ItemStack germling, boolean empty) {
        forget(pos);
        plots.put(pos.immutable(), new Plot(germling.copyWithCount(1), empty));
        if (empty) {
            emptyCount++;
        }
    }

    /** The germling that replants the Plot at {@code pos}, or {@code null} when it is no Plot. */
    @Nullable
    ItemStack germling(BlockPos pos) {
        Plot plot = plots.get(pos);
        return plot != null ? plot.germling : null;
    }

    void markEmpty(BlockPos pos, boolean empty) {
        Plot plot = plots.get(pos);
        if (plot != null && plot.empty != empty) {
            plot.empty = empty;
            emptyCount += empty ? 1 : -1;
        }
    }

    void forget(BlockPos pos) {
        Plot plot = plots.remove(pos);
        if (plot != null && plot.empty) {
            emptyCount--;
        }
    }

    int size() {
        return plots.size();
    }

    int emptyCount() {
        return emptyCount;
    }

    void save(CompoundTag tag, String key, HolderLookup.Provider registries) {
        if (plots.isEmpty()) {
            return;
        }
        RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        ListTag list = new ListTag();
        plots.forEach((pos, plot) -> ItemStack.CODEC.encodeStart(ops, plot.germling).result().ifPresent(germling -> {
            CompoundTag entry = new CompoundTag();
            entry.putLong("Pos", pos.asLong());
            entry.put("Germling", germling);
            entry.putBoolean("Empty", plot.empty);
            list.add(entry);
        }));
        tag.put(key, list);
    }

    void load(CompoundTag tag, String key, HolderLookup.Provider registries) {
        plots.clear();
        emptyCount = 0;
        RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        ListTag list = NbtCompat.getListOrEmpty(tag, key);
        for (int i = 0; i < list.size(); i++) {
            NbtCompat.ifHasCompoundAt(list, i, entry -> ItemStack.CODEC
                    .parse(ops, NbtCompat.getCompoundOrEmpty(entry, "Germling"))
                    .result()
                    .filter(germling -> !germling.isEmpty())
                    .ifPresent(germling -> remember(
                            BlockPos.of(NbtCompat.getLong(entry, "Pos", 0L)),
                            germling,
                            NbtCompat.getBoolean(entry, "Empty", false))));
        }
    }
}
