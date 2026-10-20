package com.logistics.core.machine.tier;

import com.logistics.LogisticsCore;
import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.machine.MachineComponent;
import com.logistics.core.machine.MachineHudModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;

/**
 * Holds a machine's {@link MachineTier}. Saved as the string id under {@code Tier}, carried on the
 * dropped item as {@code logistics:machine_tier} (non-basic tiers only, so basic drops stack with
 * crafted items), and shown on the look-at HUD.
 *
 * <p>Listeners registered with {@link #onChange} fire whenever the tier changes, including on load,
 * so a machine can resize its buffers from one place.
 */
public final class TierComponent
        implements MachineComponent, MachineComponent.HudContributor, MachineComponent.ItemComponents {

    public static final String NBT_KEY = "Tier";
    public static final String HUD_TIER = "jade.logistics.machine.tier";

    private final String id;
    private final Runnable onSync;
    private final List<Consumer<MachineTier>> listeners = new ArrayList<>();
    private MachineTier tier = MachineTier.BASIC;

    public TierComponent(String id, Runnable onSync) {
        this.id = id;
        this.onSync = onSync;
    }

    @Override
    public String id() {
        return id;
    }

    public MachineTier tier() {
        return tier;
    }

    /**
     * Changes the tier, notifies listeners, and syncs the host. Returns {@code false} without doing
     * anything when the tier is unchanged.
     */
    public boolean setTier(MachineTier tier) {
        if (!apply(tier)) {
            return false;
        }
        onSync.run();
        return true;
    }

    /** Registers a listener fired with the new tier on every change. */
    public void onChange(Consumer<MachineTier> listener) {
        listeners.add(Objects.requireNonNull(listener));
    }

    private boolean apply(MachineTier tier) {
        Objects.requireNonNull(tier);
        if (tier == this.tier) {
            return false;
        }
        this.tier = tier;
        for (Consumer<MachineTier> listener : listeners) {
            listener.accept(tier);
        }
        return true;
    }

    @Override
    public void contributeHud(MachineHudModel hud) {
        // Hidden while nothing can change a tier.
        // hud.line(HUD_TIER, tier.translationKey());
    }

    @Override
    public void addItemComponents(DataComponentMap.Builder builder) {
        if (tier != MachineTier.BASIC) {
            builder.set(LogisticsCore.DATA.MACHINE_TIER, tier);
        }
    }

    @Override
    public void readItemComponents(ComponentReader components) {
        MachineTier fromItem = components.get(LogisticsCore.DATA.MACHINE_TIER);
        if (fromItem != null) {
            setTier(fromItem);
        }
    }

    @Override
    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putString(NBT_KEY, tier.id());
    }

    /** Never throws: a missing or unknown id loads as {@link MachineTier#BASIC}. Does not sync. */
    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        apply(MachineTier.byIdOrBasic(NbtCompat.getString(tag, NBT_KEY, "")));
    }
}
