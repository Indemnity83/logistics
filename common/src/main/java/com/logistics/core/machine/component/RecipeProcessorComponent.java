package com.logistics.core.machine.component;

import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.lib.recipe.FluidResult;
import com.logistics.core.machine.MachineComponent;
import com.logistics.core.machine.MachineContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * RF-cost recipe processor: each tick it resolves the active recipe for the current input, spends
 * a fixed {@code rfPerTick} (all-or-nothing) toward the recipe's {@code energyRequired}, and emits
 * the output once enough energy has been spent. Progress is the energy spent so far.
 *
 * <p>The pure spend/complete decision lives in {@link RecipeProcessPlan} for unit-testing; this
 * class wires it to sibling item/energy components and toggles the machine's "lit" state.
 */
public final class RecipeProcessorComponent
        implements MachineComponent, MachineComponent.ProcessState, MachineComponent.ExperienceStore {

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
    private float storedExperience;
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
        this(id, resolver, rfPerTick, items, energy, null, lit, onChanged);
    }

    /** Constructor for a machine with a fluid output: the processor deposits into {@code fluids}. */
    public RecipeProcessorComponent(
            String id,
            RecipeResolver resolver,
            long rfPerTick,
            ItemStoreComponent items,
            EnergyStorageComponent energy,
            @Nullable FluidStoreComponent fluids,
            LitController lit,
            Runnable onChanged) {
        this.id = id;
        this.resolver = resolver;
        this.rfPerTick = rfPerTick;
        this.lit = lit;
        this.onChanged = onChanged;
        this.io = new ComponentProcessIO(items, energy, fluids);
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
        syncActivePlan(plan);

        RecipeProcessPlan.Result result = RecipeProcessPlan.advance(
                energySpent, plan.energyRequired(), io.energyStored(), rfPerTick, canRun(plan));
        if (!result.consumedEnergy()) {
            lit.setLit(ctx, false);
            return;
        }

        io.consumeEnergy(rfPerTick);
        energySpent = result.energySpent();
        lit.setLit(ctx, true);
        onChanged.run();

        if (result.complete()) {
            completeRun(plan, ctx);
        }
    }

    /** Adopts the resolved plan, restoring NBT progress on first sight and resetting it on a recipe change. */
    private void syncActivePlan(RecipePlan plan) {
        if (activePlan == null) {
            activePlan = plan;
            energySpent = Math.min(energySpent, plan.energyRequired()); // keep progress restored from NBT
        } else if (!sameRecipe(activePlan, plan)) {
            activePlan = plan;
            energySpent = 0;
        }
    }

    /** Whether inputs are present and there's room for every item and fluid output the plan will emit. */
    private boolean canRun(RecipePlan plan) {
        FluidResult fluidResult = plan.fluidResult();
        return hasInputs(plan)
                && io.canAcceptOutputs(plan.result(), plan.byproducts())
                && (fluidResult == null || io.canAcceptFluid(fluidResult));
    }

    /** Consumes inputs, emits outputs/byproducts/fluid, banks experience, and clears the active run. */
    private void completeRun(RecipePlan plan, MachineContext ctx) {
        consumeInputs(plan);
        io.produceOutputs(plan.result(), rollByproducts(plan.byproducts(), ctx.random()));
        FluidResult fluidResult = plan.fluidResult();
        if (fluidResult != null) {
            io.produceFluid(fluidResult);
        }
        if (plan.experience() > 0) {
            storedExperience += plan.experience();
        }
        energySpent = 0;
        activePlan = null;
    }

    /** Whether every input slot holds at least the count the plan consumes from it. */
    private boolean hasInputs(RecipePlan plan) {
        int[] counts = plan.inputCounts();
        for (int i = 0; i < counts.length; i++) {
            if (io.input(i).getCount() < counts[i]) {
                return false;
            }
        }
        return true;
    }

    /** Consumes each input slot's per-run count. */
    private void consumeInputs(RecipePlan plan) {
        int[] counts = plan.inputCounts();
        for (int i = 0; i < counts.length; i++) {
            io.consumeInput(i, counts[i]);
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
                && a.experience() == b.experience()
                && Arrays.equals(a.inputCounts(), b.inputCounts())
                && ItemStack.isSameItemSameComponents(a.result(), b.result())
                && sameByproducts(a.byproducts(), b.byproducts())
                && sameFluid(a.fluidResult(), b.fluidResult());
    }

    private static boolean sameFluid(@Nullable FluidResult a, @Nullable FluidResult b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.fluid() == b.fluid() && a.millibuckets() == b.millibuckets();
    }

    // Compared element-wise on item + chance; ItemStack has no value equals, so List.equals is unusable.
    private static boolean sameByproducts(List<ChanceOutput> a, List<ChanceOutput> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            ChanceOutput x = a.get(i);
            ChanceOutput y = b.get(i);
            if (x.chance() != y.chance() || !ItemStack.isSameItemSameComponents(x.template(), y.template())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int drainExperience(RandomSource random) {
        int xp = (int) storedExperience;
        float fraction = storedExperience - xp;
        if (fraction > 0 && random.nextFloat() < fraction) {
            xp++;
        }
        storedExperience = 0;
        return xp;
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
        if (storedExperience > 0) {
            tag.putFloat("storedExperience", storedExperience);
        }
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        energySpent = NbtCompat.getLong(tag, "energySpent", 0L);
        storedExperience = NbtCompat.getFloat(tag, "storedExperience", 0f);
        // activePlan is re-resolved on the next tick.
    }

    /** Default {@link ProcessIO} bridging the processor to sibling item, energy, and fluid components. */
    private static final class ComponentProcessIO implements ProcessIO {
        private final ItemStoreComponent items;
        private final EnergyStorageComponent energy;

        @Nullable
        private final FluidStoreComponent fluids;

        ComponentProcessIO(ItemStoreComponent items, EnergyStorageComponent energy, @Nullable FluidStoreComponent fluids) {
            this.items = items;
            this.energy = energy;
            this.fluids = fluids;
        }

        @Override
        public int inputSlotCount() {
            return items.inputCount();
        }

        @Override
        public ItemStack input() {
            return items.input();
        }

        @Override
        public ItemStack input(int index) {
            return items.input(index);
        }

        @Override
        public void consumeInput(int count) {
            items.consumeInput(count);
        }

        @Override
        public void consumeInput(int index, int count) {
            items.consumeInput(index, count);
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
            // A fluid-only machine has no item result and no output slot; skip the slot-0 check for it.
            if (!result.isEmpty() && !items.canAcceptInto(0, result)) {
                return false;
            }
            for (int i = 0; i < byproducts.size(); i++) {
                ChanceOutput byproduct = byproducts.get(i);
                // Validate the worst-case roll so a fractional bonus is never silently dropped on completion.
                int maxCount = byproduct.maxCount();
                if (maxCount > 0 && !items.canAcceptInto(i + 1, byproduct.stack(maxCount))) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void produceOutputs(ItemStack result, List<ItemStack> rolledByproducts) {
            if (!result.isEmpty()) {
                items.produceInto(0, result);
            }
            for (int i = 0; i < rolledByproducts.size(); i++) {
                items.produceInto(i + 1, rolledByproducts.get(i));
            }
        }

        @Override
        public boolean canAcceptFluid(FluidResult fluid) {
            if (fluids == null) {
                return false;
            }
            long amount = fluid.nativeAmount();
            return fluids.tank().insert(fluid.key(), amount, true) == amount;
        }

        @Override
        public void produceFluid(FluidResult fluid) {
            if (fluids != null) {
                fluids.tank().insert(fluid.key(), fluid.nativeAmount(), false);
            }
        }
    }
}
