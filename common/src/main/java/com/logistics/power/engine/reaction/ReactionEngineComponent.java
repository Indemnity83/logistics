package com.logistics.power.engine.reaction;

import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.IFluidKey;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.core.lib.power.EngineComponent;
import com.logistics.core.machine.MachineComponent;
import com.logistics.core.machine.MachineContext;
import com.logistics.core.machine.component.FluidStoreComponent;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/**
 * The Reaction Engine simulation — the smallest engine in the mod. It consumes a liquid reactant + a solid
 * catalyst in one committed reaction, then generates a fixed RF/t for a fixed duration, pushing it
 * <b>directly to the network with no buffer</b> and discarding whatever the network can't take. There is
 * no heat, no coolant, no pressure, no stored energy.
 *
 * <p>Once committed, a reaction <b>cannot pause, throttle, or cancel</b> — it runs to completion regardless
 * of redstone or network acceptance. Redstone only gates <em>starting</em> a new reaction. The injected
 * {@link ReactionOutput} + reaction-lookup seams make it unit-testable without a live level.
 */
public final class ReactionEngineComponent implements MachineComponent, EngineComponent.PistonState {

    /** Fastest piston of any engine — a violent reaction, not a steady burn. */
    private static final float PISTON_SPEED = 0.12f;

    private final String id;
    private final ReactionOutput output;
    private final FluidStoreComponent reactantStore;
    private final Container catalystInventory;
    private final BiFunction<Fluid, ItemStack, ReactionRecipe> reactionLookup;
    private final BooleanSupplier powered;
    private final ReactionEngineProfile profile;
    private final Runnable onChanged;

    // Persisted: the reaction countdown + the rate committed at ignition (so a mid-reaction reload resumes
    // at the right rate once recipes vary). Never any stored RF — the engine is bufferless.
    private int remainingReactionTicks;
    private long committedOutputPerTick;

    // Transient.
    private long lastAttempted;
    private long lastAccepted;
    private boolean syncedReacting;

    public ReactionEngineComponent(
            String id,
            ReactionOutput output,
            FluidStoreComponent reactantStore,
            Container catalystInventory,
            BiFunction<Fluid, ItemStack, ReactionRecipe> reactionLookup,
            BooleanSupplier powered,
            ReactionEngineProfile profile,
            Runnable onChanged) {
        this.id = id;
        this.output = output;
        this.reactantStore = reactantStore;
        this.catalystInventory = catalystInventory;
        this.reactionLookup = reactionLookup;
        this.powered = powered;
        this.profile = profile;
        this.onChanged = onChanged;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public void serverTick(MachineContext ctx) {
        int r0 = remainingReactionTicks;
        tickSimulation(ctx);
        if (remainingReactionTicks != r0) {
            onChanged.run();
        }
        // Sync on the reacting flip so the client piston starts/stops with the reaction.
        boolean reacting = isReacting();
        if (reacting != syncedReacting) {
            syncedReacting = reacting;
            ctx.sync();
        }
    }

    private void tickSimulation(MachineContext ctx) {
        if (!isReacting()) {
            lastAttempted = 0;
            lastAccepted = 0;
            if (!tryIgnite()) {
                return; // idle: redstone off, no valid reactant+catalyst, or not a full batch
            }
            // fall through — a newly committed reaction generates its first tick immediately (matches the
            // Fuel/Magmatic commit-then-generate convention)
        }
        // Reacting: runs to completion regardless of redstone or network acceptance.
        long out = committedOutputPerTick;
        long accepted = output.push(ctx, out); // direct push; anything unaccepted is discarded, never stored
        lastAttempted = out;
        lastAccepted = accepted;
        remainingReactionTicks--;
    }

    /** Attempt to commit a new reaction; returns whether one started. Redstone-gated. */
    private boolean tryIgnite() {
        if (!powered.getAsBoolean()) {
            return false; // redstone only blocks STARTING a reaction
        }
        Fluid reactant = currentReactant();
        ReactionRecipe rxn = reactionLookup.apply(reactant, currentCatalyst());
        if (rxn == null) {
            return false;
        }
        IFluidKey key = SimpleFluidKey.of(reactant);
        long batch = FluidUnits.mb(profile.batchMb());
        if (reactantStore.tank().extract(key, batch, true) != batch) {
            return false; // not a full batch present — atomic
        }
        reactantStore.tank().extract(key, batch, false); // commit reactant
        catalystInventory.removeItem(0, 1); // commit 1 catalyst
        remainingReactionTicks = rxn.durationTicks(profile);
        committedOutputPerTick = rxn.outputPerTick(profile);
        return true;
    }

    private Fluid currentReactant() {
        return reactantStore.tank().getFluidKey().getFluid();
    }

    private ItemStack currentCatalyst() {
        return catalystInventory.getItem(0);
    }

    public boolean isReacting() {
        return remainingReactionTicks > 0;
    }

    /** Whether a fresh reaction could start right now (powered + a valid reactant+catalyst pair present). */
    public boolean canStartReaction() {
        return powered.getAsBoolean() && reactionLookup.apply(currentReactant(), currentCatalyst()) != null;
    }

    @Override
    public float pistonSpeed() {
        return isReacting() ? PISTON_SPEED : 0f;
    }

    // ==================== GUI/HUD getters ====================

    public int remainingReactionTicks() {
        return remainingReactionTicks;
    }

    /** Total ticks of the current/last reaction for the progress bar. (Per-recipe duration overrides would
     * need a committed-duration field; launch recipes all use the profile duration.) */
    public int reactionDurationTicks() {
        return profile.reactionDurationTicks();
    }

    public long lastAttempted() {
        return lastAttempted;
    }

    public long lastAccepted() {
        return lastAccepted;
    }

    // ==================== Persistence ====================

    @Override
    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("RemainingReactionTicks", remainingReactionTicks);
        tag.putLong("CommittedOutputPerTick", committedOutputPerTick);
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        remainingReactionTicks = Math.max(0, NbtCompat.getInt(tag, "RemainingReactionTicks", 0));
        committedOutputPerTick = NbtCompat.getLong(tag, "CommittedOutputPerTick", profile.outputPerTick());
        lastAttempted = 0;
        lastAccepted = 0;
        syncedReacting = isReacting();
    }
}
