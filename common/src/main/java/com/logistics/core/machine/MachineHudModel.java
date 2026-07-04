package com.logistics.core.machine;

import com.logistics.core.lib.compat.NbtCompat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.material.Fluid;

/**
 * Loader-agnostic model of a machine's look-at (Jade) HUD, assembled server-side from every
 * {@link MachineComponent.HudContributor} the machine hosts and synced to the client as NBT.
 *
 * <p>Entries persist under a single {@code hud} sub-tag as {@code hud/<index>} compounds (a count plus
 * numbered keys — never a {@code ListTag}, so reads avoid the {@code ListTag.getCompound(i)}
 * cross-version delta). The client rebuilds the typed entry list with {@link #entries(CompoundTag)};
 * text-representable entries render in {@code MachineHudLines}, graphical ones (fluid) in the
 * loader-specific Jade provider.
 */
public final class MachineHudModel {

    /** A single HUD contribution. */
    public sealed interface Entry permits ProgressEntry, FluidEntry {}

    /** Active-recipe progress as a 0..1 fraction. */
    public record ProgressEntry(float fraction) implements Entry {}

    /** A fluid tank's fill: fluid id, optional components, and amount/capacity in millibuckets. */
    public record FluidEntry(String fluidId, DataComponentPatch components, long amountMb, long capacityMb)
            implements Entry {}

    private static final String HUD_KEY = "hud";
    private static final String COUNT = "n";
    private static final String TYPE = "type";
    private static final String TYPE_PROGRESS = "progress";
    private static final String TYPE_FLUID = "fluid";

    private final List<Entry> entries = new ArrayList<>();

    public void progress(float fraction) {
        entries.add(new ProgressEntry(fraction));
    }

    public void fluid(Fluid fluid, DataComponentPatch components, long amountMb, long capacityMb) {
        entries.add(new FluidEntry(BuiltInRegistries.FLUID.getKey(fluid).toString(), components, amountMb, capacityMb));
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /** Serialize the collected entries into the synced HUD tag. Writes nothing when empty. */
    public void save(CompoundTag data) {
        if (entries.isEmpty()) {
            return;
        }
        CompoundTag hud = new CompoundTag();
        hud.putInt(COUNT, entries.size());
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag e = new CompoundTag();
            switch (entries.get(i)) {
                case ProgressEntry p -> {
                    e.putString(TYPE, TYPE_PROGRESS);
                    e.putFloat("fraction", p.fraction());
                }
                case FluidEntry f -> {
                    e.putString(TYPE, TYPE_FLUID);
                    e.putString("fluid", f.fluidId());
                    e.putLong("amount", f.amountMb());
                    e.putLong("capacity", f.capacityMb());
                    if (!f.components().isEmpty()) {
                        DataComponentPatch.CODEC
                                .encodeStart(NbtOps.INSTANCE, f.components())
                                .result()
                                .ifPresent(tag -> e.put("components", tag));
                    }
                }
            }
            hud.put(Integer.toString(i), e);
        }
        data.put(HUD_KEY, hud);
    }

    /** Rebuild the typed entry list synced into {@code data}; empty when no HUD tag was written. */
    public static List<Entry> entries(CompoundTag data) {
        CompoundTag hud = NbtCompat.getCompoundOrEmpty(data, HUD_KEY);
        int count = NbtCompat.getInt(hud, COUNT, 0);
        if (count <= 0) {
            return List.of();
        }
        List<Entry> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            CompoundTag e = NbtCompat.getCompoundOrEmpty(hud, Integer.toString(i));
            switch (NbtCompat.getString(e, TYPE, "")) {
                case TYPE_PROGRESS -> result.add(new ProgressEntry(NbtCompat.getFloat(e, "fraction", 0f)));
                case TYPE_FLUID -> {
                    DataComponentPatch components = DataComponentPatch.EMPTY;
                    if (NbtCompat.hasCompound(e, "components")) {
                        components = DataComponentPatch.CODEC
                                .parse(NbtOps.INSTANCE, NbtCompat.getCompoundOrEmpty(e, "components"))
                                .result()
                                .orElse(DataComponentPatch.EMPTY);
                    }
                    result.add(new FluidEntry(
                            NbtCompat.getString(e, "fluid", ""),
                            components,
                            NbtCompat.getLong(e, "amount", 0L),
                            NbtCompat.getLong(e, "capacity", 0L)));
                }
                default -> {}
            }
        }
        return result;
    }
}
