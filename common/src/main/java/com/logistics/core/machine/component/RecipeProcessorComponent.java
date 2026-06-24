package com.logistics.core.machine.component;

import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.machine.MachineComponent;
import com.logistics.core.machine.MachineContext;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * RF-cost recipe processor: each tick it resolves the active recipe for the current input, spends
 * a fixed {@code rfPerTick} (all-or-nothing) toward the recipe's {@code energyRequired}, and emits
 * the output once enough energy has been spent. Progress is the energy spent so far.
 *
 * <p>The pure spend/complete decision lives in {@link RecipeProcessPlan} for unit-testing; this
 * class wires it to sibling item/energy components and toggles the machine's "lit" state.
 */
public final class RecipeProcessorComponent implements MachineComponent, MachineComponent.ProcessState {

    /** Toggles the machine's working/lit block state. */
    @FunctionalInterface
    public interface LitController {
        void setLit(MachineContext ctx, boolean lit);
    }

    private final String id;
    private final RecipeResolver resolver;
    private final long rfPerTick;
    private final ProcessIO io;
    private final LitController lit;
    private final Runnable onChanged;

    private long energySpent;
    @Nullable
    private RecipePlan activePlan;

    public RecipeProcessorComponent(
            String id,
            RecipeResolver resolver,
            long rfPerTick,
            ItemStoreComponent items,
            EnergyStorageComponent energy,
            LitController lit,
            Runnable onChanged) {
        this.id = id;
        this.resolver = resolver;
        this.rfPerTick = rfPerTick;
        this.lit = lit;
        this.onChanged = onChanged;
        this.io = new ComponentProcessIO(items, energy);
    }

    /** Constructor accepting a custom {@link ProcessIO}, primarily for tests. */
    public RecipeProcessorComponent(
            String id, RecipeResolver resolver, long rfPerTick, ProcessIO io, LitController lit, Runnable onChanged) {
        this.id = id;
        this.resolver = resolver;
        this.rfPerTick = rfPerTick;
        this.io = io;
        this.lit = lit;
        this.onChanged = onChanged;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public void serverTick(MachineContext ctx) {
        RecipePlan plan = resolver.resolve(io, ctx);
        if (plan == null) {
            reset(ctx);
            return;
        }
        if (activePlan == null || !sameRecipe(activePlan, plan)) {
            activePlan = plan;
            energySpent = 0;
        }

        RecipeProcessPlan.Result result = RecipeProcessPlan.advance(
                energySpent, plan.energyRequired(), io.energyStored(), rfPerTick, io.canAcceptOutput(plan.result()));

        if (!result.consumedEnergy()) {
            lit.setLit(ctx, false);
            return;
        }

        io.consumeEnergy(rfPerTick);
        energySpent = result.energySpent();
        lit.setLit(ctx, true);
        onChanged.run();

        if (result.complete()) {
            io.consumeInput();
            io.produceOutput(plan.result());
            awardExperience(ctx, plan.experience());
            energySpent = 0;
            activePlan = null;
        }
    }

    private void reset(MachineContext ctx) {
        if (energySpent != 0 || activePlan != null) {
            energySpent = 0;
            activePlan = null;
            onChanged.run();
        }
        lit.setLit(ctx, false);
    }

    private static boolean sameRecipe(RecipePlan a, RecipePlan b) {
        return a.energyRequired() == b.energyRequired()
                && ItemStack.isSameItemSameComponents(a.result(), b.result());
    }

    private void awardExperience(MachineContext ctx, float experience) {
        if (experience <= 0 || !(ctx.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        int xp = (int) experience;
        if (serverLevel.getRandom().nextFloat() < (experience - xp)) {
            xp++;
        }
        if (xp > 0) {
            ExperienceOrb.award(serverLevel, Vec3.atCenterOf(ctx.pos()), xp);
        }
    }

    // ----- ProcessState -----

    @Override
    public float progress() {
        if (activePlan == null || activePlan.energyRequired() <= 0) {
            return 0f;
        }
        return Math.min(1f, (float) energySpent / activePlan.energyRequired());
    }

    @Override
    public boolean isProcessing() {
        return activePlan != null;
    }

    /** Energy spent toward the active recipe; drives the menu progress bar. */
    public long energySpent() {
        return energySpent;
    }

    /** Total energy the active recipe requires, or 0 when idle. */
    public long energyRequired() {
        return activePlan != null ? activePlan.energyRequired() : 0;
    }

    // ----- persistence -----

    @Override
    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        if (energySpent > 0) {
            tag.putLong("energySpent", energySpent);
        }
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        energySpent = NbtCompat.getLong(tag, "energySpent", 0L);
        // activePlan is re-resolved on the next tick.
    }

    /** Default {@link ProcessIO} bridging the processor to sibling item and energy components. */
    private static final class ComponentProcessIO implements ProcessIO {
        private final ItemStoreComponent items;
        private final EnergyStorageComponent energy;

        ComponentProcessIO(ItemStoreComponent items, EnergyStorageComponent energy) {
            this.items = items;
            this.energy = energy;
        }

        @Override
        public ItemStack input() {
            return items.input();
        }

        @Override
        public boolean canAcceptOutput(ItemStack result) {
            return items.canAcceptOutput(result);
        }

        @Override
        public void consumeInput() {
            items.shrinkInput();
        }

        @Override
        public void produceOutput(ItemStack result) {
            items.produceOutput(result);
        }

        @Override
        public long energyStored() {
            return energy.amount();
        }

        @Override
        public void consumeEnergy(long rf) {
            energy.consume(rf);
        }
    }
}
