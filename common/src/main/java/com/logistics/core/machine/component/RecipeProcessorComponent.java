package com.logistics.core.machine.component;

import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.machine.MachineComponent;
import com.logistics.core.machine.MachineContext;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
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
        if (activePlan == null) {
            activePlan = plan;
            energySpent = Math.min(energySpent, plan.energyRequired()); // keep progress restored from NBT
        } else if (!sameRecipe(activePlan, plan)) {
            activePlan = plan;
            energySpent = 0;
        }

        boolean canRun = io.input().getCount() >= plan.inputCount()
                && io.canAcceptOutputs(plan.result(), plan.byproducts());
        RecipeProcessPlan.Result result =
                RecipeProcessPlan.advance(energySpent, plan.energyRequired(), io.energyStored(), rfPerTick, canRun);

        if (!result.consumedEnergy()) {
            lit.setLit(ctx, false);
            return;
        }

        io.consumeEnergy(rfPerTick);
        energySpent = result.energySpent();
        lit.setLit(ctx, true);
        onChanged.run();

        if (result.complete()) {
            io.consumeInput(plan.inputCount());
            io.produceOutputs(plan.result(), rollByproducts(plan.byproducts(), ctx.random()));
            awardExperience(ctx, plan.experience());
            energySpent = 0;
            activePlan = null;
        }
    }

    /** Rolls each byproduct's yield into a stack (empty when the roll is zero), parallel to {@code byproducts}. */
    private static List<ItemStack> rollByproducts(List<ChanceOutput> byproducts, RandomSource random) {
        List<ItemStack> rolled = new ArrayList<>(byproducts.size());
        for (ChanceOutput byproduct : byproducts) {
            rolled.add(byproduct.stack(byproduct.roll(random)));
        }
        return rolled;
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
        if (ctx.random().nextFloat() < (experience - xp)) {
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
        public void consumeInput(int count) {
            items.consumeInput(count);
        }

        @Override
        public long energyStored() {
            return energy.amount();
        }

        @Override
        public void consumeEnergy(long rf) {
            energy.consume(rf);
        }

        @Override
        public boolean canAcceptOutputs(ItemStack result, List<ChanceOutput> byproducts) {
            if (!items.canAcceptInto(0, result)) {
                return false;
            }
            for (int i = 0; i < byproducts.size(); i++) {
                ChanceOutput byproduct = byproducts.get(i);
                if (byproduct.guaranteedCount() > 0 && !items.canAcceptInto(i + 1, byproduct.guaranteedStack())) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void produceOutputs(ItemStack result, List<ItemStack> rolledByproducts) {
            items.produceInto(0, result);
            for (int i = 0; i < rolledByproducts.size(); i++) {
                items.produceInto(i + 1, rolledByproducts.get(i));
            }
        }
    }
}
