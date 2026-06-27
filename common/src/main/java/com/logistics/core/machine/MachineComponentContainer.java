package com.logistics.core.machine;

import com.logistics.core.LogisticsProfiler;
import com.logistics.core.lib.compat.NbtCompat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

/**
 * Ordered registry of a machine's components.
 *
 * <p>Components tick in registration order — this is load-bearing: an energy component added
 * before a recipe processor resets its received-this-tick counter before the processor spends.
 * Each component persists under {@code components/<id>}; on load, an absent {@code components}
 * tag triggers each component's {@link MachineComponent#loadLegacy} for pre-component saves.
 */
public final class MachineComponentContainer {

    private static final String COMPONENTS_KEY = "components";

    private final List<MachineComponent> ordered = new ArrayList<>();
    private final Map<String, MachineComponent> byId = new LinkedHashMap<>();

    /** Registers a component and returns it for direct reference in {@code configure(...)}. */
    public <T extends MachineComponent> T add(T component) {
        if (byId.putIfAbsent(component.id(), component) != null) {
            throw new IllegalArgumentException("Duplicate machine component id: " + component.id());
        }
        ordered.add(component);
        return component;
    }

    /** First registered component assignable to {@code type}, if any. */
    public <T> Optional<T> find(Class<T> type) {
        for (MachineComponent c : ordered) {
            if (type.isInstance(c)) {
                return Optional.of(type.cast(c));
            }
        }
        return Optional.empty();
    }

    public void onLoad(MachineContext ctx) {
        for (MachineComponent c : ordered) {
            c.onLoad(ctx);
        }
    }

    public void serverTick(MachineContext ctx) {
        LogisticsProfiler.push("machines");
        try {
            for (MachineComponent c : ordered) {
                c.serverTick(ctx);
            }
        } finally {
            LogisticsProfiler.pop();
        }
    }

    public void save(CompoundTag root, HolderLookup.Provider registries) {
        CompoundTag components = new CompoundTag();
        for (MachineComponent c : ordered) {
            CompoundTag tag = new CompoundTag();
            c.save(tag, registries);
            if (!tag.isEmpty()) {
                components.put(c.id(), tag);
            }
        }
        if (!components.isEmpty()) {
            root.put(COMPONENTS_KEY, components);
        }
    }

    public void load(CompoundTag root, HolderLookup.Provider registries) {
        if (NbtCompat.hasCompound(root, COMPONENTS_KEY)) {
            CompoundTag components = NbtCompat.getCompoundOrEmpty(root, COMPONENTS_KEY);
            for (MachineComponent c : ordered) {
                c.load(NbtCompat.getCompoundOrEmpty(components, c.id()), registries);
            }
        } else {
            for (MachineComponent c : ordered) {
                c.loadLegacy(root, registries);
            }
        }
    }
}
