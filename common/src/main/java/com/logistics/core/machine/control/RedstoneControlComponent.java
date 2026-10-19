package com.logistics.core.machine.control;

import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.machine.MachineComponent;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

/**
 * Holds a host's {@link RedstoneMode}. Persists only a non-default mode, and a load always fully
 * determines the mode (a missing or unknown key means the default), since clients re-load update tags.
 */
public final class RedstoneControlComponent implements MachineComponent {

    private static final String MODE_KEY = "Mode";

    private final String id;
    private final RedstoneMode defaultMode;
    private final Runnable onChanged;
    private RedstoneMode mode;

    public RedstoneControlComponent(String id, RedstoneMode defaultMode, Runnable onChanged) {
        this.id = id;
        this.defaultMode = Objects.requireNonNull(defaultMode);
        this.onChanged = onChanged;
        this.mode = defaultMode;
    }

    @Override
    public String id() {
        return id;
    }

    public RedstoneMode mode() {
        return mode;
    }

    public RedstoneMode defaultMode() {
        return defaultMode;
    }

    public void setMode(RedstoneMode mode) {
        Objects.requireNonNull(mode);
        if (this.mode == mode) {
            return;
        }
        this.mode = mode;
        onChanged.run();
    }

    /** Whether work is permitted; {@code signal} is not evaluated when the mode is {@link RedstoneMode#IGNORED}. */
    public boolean allows(BooleanSupplier signal) {
        return mode == RedstoneMode.IGNORED || mode.allows(signal.getAsBoolean());
    }

    @Override
    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        if (mode != defaultMode) {
            tag.putString(MODE_KEY, mode.serializedName());
        }
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        mode = RedstoneMode.byName(NbtCompat.getString(tag, MODE_KEY, defaultMode.serializedName()), defaultMode);
    }

    @Override
    public void loadLegacy(CompoundTag root, HolderLookup.Provider registries) {
        mode = defaultMode;
    }
}
