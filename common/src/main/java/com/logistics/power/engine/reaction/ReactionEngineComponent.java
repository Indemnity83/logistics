package com.logistics.power.engine.reaction;

import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.lib.power.EngineComponent;
import com.logistics.core.machine.MachineComponent;
import com.logistics.core.machine.MachineContext;
import com.logistics.core.machine.component.FluidStoreComponent;
import java.util.function.BooleanSupplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/**
 * The Reaction Engine simulation — the smallest engine in the mod. It consumes a datapack-defined liquid
 * reactant + solid reagent in one committed reaction, then generates a fixed RF/t for a fixed duration,
 * pushing it <b>directly to the network with no buffer</b> and discarding whatever the network can't take.
 * There is no heat, no coolant, no pressure, no stored energy — and no hardcoded materials, amounts,
 * energy, or duration: every value comes from the matched {@link ReactionRecipe}.
 *
 * <p>Once committed, a reaction <b>cannot pause, throttle, or cancel</b> — it runs to completion regardless
 * of redstone or network acceptance. Redstone only gates <em>starting</em> a new reaction. The injected
 * {@link ReactionOutput} + {@link ReactionLookup} seams make it unit-testable without a live level.
 */
public final class ReactionEngineComponent implements MachineComponent, EngineComponent.PistonState {

    /** Fastest piston of any engine — a violent reaction, not a steady burn. */
    private static final float PISTON_SPEED = 0.12f;

    private final String id;
    private final ReactionOutput output;
    private final FluidStoreComponent reactantStore;
    private final Container reagentInventory;
    private final ReactionLookup reactionLookup;
    private final BooleanSupplier powered;
    private final Runnable onChanged;

    // Persisted: the reaction countdown + the rate/total committed at ignition from the recipe (so a
    // mid-reaction reload resumes at the right rate and drives the progress bar). Never any stored RF.
    private int remainingReactionTicks;
    private int committedTotalTicks;
    private long committedOutputPerTick;

    // Transient.
    private long lastAttempted;
    private long lastAccepted;
    private boolean syncedReacting;

    public ReactionEngineComponent(
            String id,
            ReactionOutput output,
            FluidStoreComponent reactantStore,
            Container reagentInventory,
            ReactionLookup reactionLookup,
            BooleanSupplier powered,
            Runnable onChanged) {
        this.id = id;
        this.output = output;
        this.reactantStore = reactantStore;
        this.reagentInventory = reagentInventory;
        this.reactionLookup = reactionLookup;
        this.powered = powered;
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
            if (!tryIgnite(ctx)) {
                return; // idle: redstone off, no matching recipe, or not a full batch
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

    /** Attempt to commit a new reaction from the datapack recipes; returns whether one started. */
    private boolean tryIgnite(MachineContext ctx) {
        if (!powered.getAsBoolean()) {
            return false; // redstone only blocks STARTING a reaction
        }
        Fluid reactant = currentReactant();
        ReactionRecipe recipe = reactionLookup.find(ctx, reactant, currentReagent());
        if (recipe == null) {
            return false;
        }
        long batch = recipe.reactant().nativeAmount();
        if (reactantStore.tank().extract(recipe.reactant().key(), batch, true) != batch) {
            return false; // not a full batch present — atomic
        }
        reactantStore.tank().extract(recipe.reactant().key(), batch, false); // commit reactant
        reagentInventory.removeItem(0, recipe.reagentCount()); // commit the reagent(s)
        remainingReactionTicks = recipe.time();
        committedTotalTicks = recipe.time();
        committedOutputPerTick = recipe.outputPerTick();
        return true;
    }

    private Fluid currentReactant() {
        return reactantStore.tank().getFluidKey().getFluid();
    }

    private ItemStack currentReagent() {
        return reagentInventory.getItem(0);
    }

    public boolean isReacting() {
        return remainingReactionTicks > 0;
    }

    /** Whether a fresh reaction could start right now (powered + a matching recipe + a full reactant batch). */
    public boolean canStartReaction(MachineContext ctx) {
        if (!powered.getAsBoolean()) {
            return false;
        }
        ReactionRecipe recipe = reactionLookup.find(ctx, currentReactant(), currentReagent());
        if (recipe == null) {
            return false;
        }
        long batch = recipe.reactant().nativeAmount();
        return reactantStore.tank().extract(recipe.reactant().key(), batch, true) == batch;
    }

    @Override
    public float pistonSpeed() {
        return isReacting() ? PISTON_SPEED : 0f;
    }

    // ==================== GUI/HUD getters ====================

    public int remainingReactionTicks() {
        return remainingReactionTicks;
    }

    /** Total ticks of the current/last reaction for the progress bar (committed at ignition per recipe). */
    public int reactionDurationTicks() {
        return committedTotalTicks;
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
        tag.putInt("CommittedTotalTicks", committedTotalTicks);
        tag.putLong("CommittedOutputPerTick", committedOutputPerTick);
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        remainingReactionTicks = Math.max(0, NbtCompat.getInt(tag, "RemainingReactionTicks", 0));
        committedTotalTicks = Math.max(0, NbtCompat.getInt(tag, "CommittedTotalTicks", 0));
        committedOutputPerTick = NbtCompat.getLong(tag, "CommittedOutputPerTick", 0L);
        lastAttempted = 0;
        lastAccepted = 0;
        syncedReacting = isReacting();
    }
}
