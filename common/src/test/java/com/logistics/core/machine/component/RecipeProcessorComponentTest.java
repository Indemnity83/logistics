package com.logistics.core.machine.component;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.core.lib.recipe.FluidResult;
import com.logistics.core.machine.FakeMachineContext;
import com.logistics.test.MinecraftTestEnvironment;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.Test;

class RecipeProcessorComponentTest extends MinecraftTestEnvironment {

    /** In-memory IO so the processor's tick flow can be exercised without a live machine. */
    private static final class FakeProcessIO implements ProcessIO {
        long energy;
        boolean hasInput = true;
        int inputCount = 1;
        boolean outputAccepts = true;
        boolean fluidAccepts = true;
        int inputsConsumed;
        int outputsProduced;
        FluidResult producedFluid;

        @Override
        public ItemStack input() {
            return hasInput ? new ItemStack(Items.RAW_IRON, inputCount) : ItemStack.EMPTY;
        }

        @Override
        public void consumeInput(int count) {
            inputsConsumed += count;
        }

        @Override
        public long energyStored() {
            return energy;
        }

        @Override
        public void consumeEnergy(long rf) {
            energy -= rf;
        }

        @Override
        public boolean canAcceptOutputs(ItemStack result, List<ChanceOutput> byproducts) {
            return outputAccepts;
        }

        @Override
        public void produceOutputs(ItemStack result, List<ItemStack> rolledByproducts) {
            outputsProduced++;
            outputsProduced += (int) rolledByproducts.stream().filter(s -> !s.isEmpty()).count();
        }

        @Override
        public boolean canAcceptFluid(FluidResult fluid) {
            return fluidAccepts;
        }

        @Override
        public void produceFluid(FluidResult fluid) {
            producedFluid = fluid;
        }
    }

    private static RecipeProcessorComponent processor(FakeProcessIO io, long required, long rfPerTick) {
        return processor(io, new RecipePlan(required, new ItemStack(Items.IRON_INGOT), 0f), rfPerTick);
    }

    private static RecipeProcessorComponent processor(FakeProcessIO io, RecipePlan plan, long rfPerTick) {
        RecipeResolver resolver = (i, ctx) -> io.hasInput ? plan : null;
        return new RecipeProcessorComponent("processor", resolver, rfPerTick, io, (ctx, lit) -> {}, () -> {});
    }

    @Test
    void consumesInputsOnlyOnceEnoughEnergyIsSpent() {
        FakeProcessIO io = new FakeProcessIO();
        io.energy = 1_000;
        RecipeProcessorComponent processor = processor(io, 30, 10);
        FakeMachineContext ctx = new FakeMachineContext();

        processor.serverTick(ctx); // 10 spent
        processor.serverTick(ctx); // 20 spent
        assertThat(io.inputsConsumed).isZero();
        assertThat(io.outputsProduced).isZero();
        assertThat(processor.isProcessing()).isTrue();

        processor.serverTick(ctx); // 30 spent -> complete
        assertThat(io.inputsConsumed).isEqualTo(1);
        assertThat(io.outputsProduced).isEqualTo(1);
        assertThat(processor.energySpent()).isZero();
    }

    @Test
    void stallsWithoutEnoughEnergy() {
        FakeProcessIO io = new FakeProcessIO();
        io.energy = 5;
        RecipeProcessorComponent processor = processor(io, 30, 10);

        processor.serverTick(new FakeMachineContext());

        assertThat(processor.energySpent()).isZero();
        assertThat(io.energy).isEqualTo(5);
    }

    @Test
    void doesNotProcessWhenOutputBlocked() {
        FakeProcessIO io = new FakeProcessIO();
        io.energy = 1_000;
        io.outputAccepts = false;
        RecipeProcessorComponent processor = processor(io, 30, 10);

        processor.serverTick(new FakeMachineContext());

        assertThat(processor.energySpent()).isZero();
        assertThat(io.energy).isEqualTo(1_000);
    }

    @Test
    void depositsFluidResultOnCompletion() {
        FakeProcessIO io = new FakeProcessIO();
        io.energy = 1_000;
        FluidResult fluid = new FluidResult(Fluids.WATER, 250);
        RecipeProcessorComponent processor = processor(io, new RecipePlan(30, 1, fluid, 0f), 10);
        FakeMachineContext ctx = new FakeMachineContext();

        processor.serverTick(ctx); // 10 spent
        processor.serverTick(ctx); // 20 spent
        assertThat(io.producedFluid).isNull();

        processor.serverTick(ctx); // 30 spent -> complete
        assertThat(io.producedFluid).isSameAs(fluid);
        assertThat(io.inputsConsumed).isEqualTo(1);
    }

    @Test
    void doesNotProcessWhenFluidOutputFull() {
        FakeProcessIO io = new FakeProcessIO();
        io.energy = 1_000;
        io.fluidAccepts = false;
        FluidResult fluid = new FluidResult(Fluids.WATER, 250);
        RecipeProcessorComponent processor = processor(io, new RecipePlan(30, 1, fluid, 0f), 10);

        processor.serverTick(new FakeMachineContext());

        assertThat(processor.energySpent()).isZero();
        assertThat(io.energy).isEqualTo(1_000);
        assertThat(io.producedFluid).isNull();
    }

    @Test
    void runsFluidOnlyRecipeWithNoOutputSlot() {
        // A crucible-style machine: input-only inventory, no output slot, empty item result.
        Runnable noop = () -> {};
        ItemStoreComponent items = new ItemStoreComponent(
                "items",
                new SlotRole[] {SlotRole.INPUT},
                SidedLayout.furnace(new int[] {0}, new int[] {}, stack -> true),
                noop);
        EnergyStorageComponent energy = new EnergyStorageComponent("energy", 100_000, 100_000, 0, noop);
        energy.energy(null).insert(100_000, false);
        items.container().setItem(0, new ItemStack(Items.REDSTONE, 1));

        RecipePlan plan = new RecipePlan(20, new int[] {1}, ItemStack.EMPTY, List.of(), 0f);
        RecipeProcessorComponent processor = new RecipeProcessorComponent(
                "processor", (io, ctx) -> io.input().isEmpty() ? null : plan, 10, items, energy, (ctx, lit) -> {}, noop);

        FakeMachineContext ctx = new FakeMachineContext();
        processor.serverTick(ctx); // 10
        processor.serverTick(ctx); // 20 -> complete

        // Input consumed proves the recipe ran despite having no output slot to accept an item result.
        assertThat(items.input().isEmpty()).isTrue();
    }

    @Test
    void depositsFluidIntoTankWhenThereIsRoom() {
        Runnable noop = () -> {};
        ItemStoreComponent items = new ItemStoreComponent(
                "items",
                new SlotRole[] {SlotRole.INPUT},
                SidedLayout.furnace(new int[] {0}, new int[] {}, stack -> true),
                noop);
        EnergyStorageComponent energy = new EnergyStorageComponent("energy", 100_000, 100_000, 0, noop);
        energy.energy(null).insert(100_000, false);
        items.container().setItem(0, new ItemStack(Items.REDSTONE, 1));

        FluidStoreComponent fluids = new FluidStoreComponent("tank", FluidUnits.mb(100), noop);
        RecipePlan plan = new RecipePlan(20, 1, new FluidResult(Fluids.WATER, 50), 0f);
        RecipeProcessorComponent processor = new RecipeProcessorComponent(
                "processor", (io, ctx) -> io.input().isEmpty() ? null : plan, 10, items, energy, fluids, (ctx, lit) -> {}, noop);

        FakeMachineContext ctx = new FakeMachineContext();
        processor.serverTick(ctx); // 10
        processor.serverTick(ctx); // 20 -> complete

        assertThat(items.input().isEmpty()).isTrue();
        assertThat(FluidUnits.toMillibuckets(fluids.tank().getAmount())).isEqualTo(50);
    }

    @Test
    void doesNotRunWhenTankCannotHoldFullFluidOutput() {
        Runnable noop = () -> {};
        ItemStoreComponent items = new ItemStoreComponent(
                "items",
                new SlotRole[] {SlotRole.INPUT},
                SidedLayout.furnace(new int[] {0}, new int[] {}, stack -> true),
                noop);
        EnergyStorageComponent energy = new EnergyStorageComponent("energy", 100_000, 100_000, 0, noop);
        energy.energy(null).insert(100_000, false);
        items.container().setItem(0, new ItemStack(Items.REDSTONE, 1));

        // Tank holds 80 of 100 mB; the recipe would add 50 mB but only 20 fits, so it must not run.
        FluidStoreComponent fluids = new FluidStoreComponent("tank", FluidUnits.mb(100), noop);
        fluids.tank().insert(SimpleFluidKey.of(Fluids.WATER), FluidUnits.mb(80), false);

        RecipePlan plan = new RecipePlan(20, 1, new FluidResult(Fluids.WATER, 50), 0f);
        RecipeProcessorComponent processor = new RecipeProcessorComponent(
                "processor", (io, ctx) -> io.input().isEmpty() ? null : plan, 10, items, energy, fluids, (ctx, lit) -> {}, noop);

        FakeMachineContext ctx = new FakeMachineContext();
        for (int i = 0; i < 5; i++) {
            processor.serverTick(ctx); // 50 RF spent >> 20 required: would complete if the tank had room
        }

        assertThat(items.input().getCount()).isEqualTo(1); // input untouched -> the recipe never ran
        assertThat(FluidUnits.toMillibuckets(fluids.tank().getAmount())).isEqualTo(80); // tank unchanged
    }

    @Test
    void resetsWhenInputRemoved() {
        FakeProcessIO io = new FakeProcessIO();
        io.energy = 1_000;
        RecipeProcessorComponent processor = processor(io, 30, 10);
        FakeMachineContext ctx = new FakeMachineContext();

        processor.serverTick(ctx); // 10 spent
        assertThat(processor.energySpent()).isEqualTo(10);

        io.hasInput = false;
        processor.serverTick(ctx);
        assertThat(processor.isProcessing()).isFalse();
        assertThat(processor.energySpent()).isZero();
    }

    @Test
    void reportsProgressFraction() {
        FakeProcessIO io = new FakeProcessIO();
        io.energy = 1_000;
        RecipeProcessorComponent processor = processor(io, 40, 10);

        processor.serverTick(new FakeMachineContext()); // 10 of 40
        assertThat(processor.progress()).isEqualTo(0.25f);
    }

    @Test
    void producesPrimaryAndGuaranteedByproduct() {
        FakeProcessIO io = new FakeProcessIO();
        io.energy = 1_000;
        RecipePlan plan = new RecipePlan(
                20, 1, new ItemStack(Items.IRON_INGOT),
                List.of(new ChanceOutput(new ItemStack(Items.GUNPOWDER), 1.0f)), 0f);
        RecipeProcessorComponent processor = processor(io, plan, 10);
        FakeMachineContext ctx = new FakeMachineContext();

        processor.serverTick(ctx); // 10
        processor.serverTick(ctx); // 20 -> complete

        assertThat(io.inputsConsumed).isEqualTo(1);
        assertThat(io.outputsProduced).isEqualTo(2); // primary + one guaranteed byproduct
    }

    @Test
    void preservesRestoredProgressOnFirstTickAfterReload() {
        FakeProcessIO io = new FakeProcessIO();
        io.energy = 1_000;
        RecipeProcessorComponent processor = processor(io, 100, 10);

        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.putLong("energySpent", 20);
        processor.load(tag, null);
        assertThat(processor.energySpent()).isEqualTo(20);

        // First tick after reload re-resolves the recipe; restored progress must not be wiped.
        processor.serverTick(new FakeMachineContext());
        assertThat(processor.energySpent()).isEqualTo(30);
    }

    @Test
    void blocksCompletionWhenFractionalByproductSlotIsFull() {
        Runnable noop = () -> {};
        ItemStoreComponent items = new ItemStoreComponent(
                "items",
                new SlotRole[] {SlotRole.INPUT, SlotRole.OUTPUT, SlotRole.OUTPUT},
                SidedLayout.furnace(new int[] {0}, new int[] {1, 2}, stack -> true),
                noop);
        EnergyStorageComponent energy = new EnergyStorageComponent("energy", 100_000, 100_000, 0, noop);
        energy.energy(null).insert(100_000, false);

        items.container().setItem(0, new ItemStack(Items.RAW_IRON, 1));
        items.produceInto(1, new ItemStack(Items.GUNPOWDER, 64)); // fill the byproduct output slot

        RecipePlan plan = new RecipePlan(
                20,
                1,
                new ItemStack(Items.IRON_INGOT),
                List.of(new ChanceOutput(new ItemStack(Items.GUNPOWDER), 0.5f)),
                0f);
        RecipeProcessorComponent processor = new RecipeProcessorComponent(
                "processor", (io, ctx) -> io.input().isEmpty() ? null : plan, 10, items, energy, (ctx, lit) -> {}, noop);

        FakeMachineContext ctx = new FakeMachineContext();
        for (int i = 0; i < 5; i++) {
            processor.serverTick(ctx); // 5 x 10 RF >> 20 required: would complete if the bonus slot weren't full
        }

        // The 0.5 bonus could roll one gunpowder but its slot is full, so the recipe must not complete.
        assertThat(items.input().getCount()).isEqualTo(1);
        assertThat(processor.energySpent()).isZero();
    }

    @Test
    void banksExperienceOnCompletionAndDrainsItOnce() {
        FakeProcessIO io = new FakeProcessIO();
        io.energy = 1_000;
        RecipePlan plan = new RecipePlan(20, 1, new ItemStack(Items.IRON_INGOT), List.of(), 2f);
        RecipeProcessorComponent processor = processor(io, plan, 10);
        FakeMachineContext ctx = new FakeMachineContext();

        processor.serverTick(ctx); // 10
        processor.serverTick(ctx); // 20 -> complete, banks 2 XP

        net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create();
        assertThat(processor.drainExperience(random)).isEqualTo(2);
        assertThat(processor.drainExperience(random)).isZero(); // drained, not re-awarded
    }

    @Test
    void accumulatesFractionalExperienceIntoWholePoints() {
        FakeProcessIO io = new FakeProcessIO();
        io.energy = 1_000;
        RecipePlan plan = new RecipePlan(20, 1, new ItemStack(Items.IRON_INGOT), List.of(), 0.5f);
        RecipeProcessorComponent processor = processor(io, plan, 10);
        FakeMachineContext ctx = new FakeMachineContext();

        for (int i = 0; i < 4; i++) {
            processor.serverTick(ctx); // two completions of 0.5 XP -> 1.0 banked
        }

        // 0.5 + 0.5 = 1.0 exactly, so the whole point drops regardless of the rounding roll.
        assertThat(processor.drainExperience(net.minecraft.util.RandomSource.create())).isEqualTo(1);
    }

    @Test
    void persistsBankedExperienceAcrossReload() {
        FakeProcessIO io = new FakeProcessIO();
        io.energy = 1_000;
        RecipePlan plan = new RecipePlan(20, 1, new ItemStack(Items.IRON_INGOT), List.of(), 3f);
        RecipeProcessorComponent processor = processor(io, plan, 10);
        FakeMachineContext ctx = new FakeMachineContext();

        processor.serverTick(ctx);
        processor.serverTick(ctx); // complete, banks 3 XP

        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        processor.save(tag, null);

        RecipeProcessorComponent reloaded = processor(new FakeProcessIO(), plan, 10);
        reloaded.load(tag, null);
        assertThat(reloaded.drainExperience(net.minecraft.util.RandomSource.create())).isEqualTo(3);
    }

    @Test
    void roundsFractionalExperienceUpAfterReload() {
        // Probe the seeded RNG's first roll so we can bank a fraction guaranteed to round up,
        // making the rounding branch in drainExperience deterministic.
        final long seed = 42L;
        float roll = net.minecraft.util.RandomSource.create(seed).nextFloat();
        float fraction = (roll + 1.0f) / 2.0f; // strictly between roll and 1 -> always rounds up
        float experience = 2.0f + fraction;

        FakeProcessIO io = new FakeProcessIO();
        io.energy = 1_000;
        RecipePlan plan = new RecipePlan(20, 1, new ItemStack(Items.IRON_INGOT), List.of(), experience);
        RecipeProcessorComponent processor = processor(io, plan, 10);
        FakeMachineContext ctx = new FakeMachineContext();

        processor.serverTick(ctx);
        processor.serverTick(ctx); // complete, banks a fractional XP amount

        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        processor.save(tag, null);
        assertThat(tag.contains("storedExperience")).isTrue();

        RecipeProcessorComponent reloaded = processor(new FakeProcessIO(), plan, 10);
        reloaded.load(tag, null);

        // 2 whole points + a fraction that beats the seeded roll -> 3 (rounding branch, not truncation).
        assertThat(reloaded.drainExperience(net.minecraft.util.RandomSource.create(seed))).isEqualTo(3);
        assertThat(reloaded.drainExperience(net.minecraft.util.RandomSource.create(seed))).isZero();
    }

    @Test
    void stallsUntilInputCountIsAvailable() {
        FakeProcessIO io = new FakeProcessIO();
        io.energy = 1_000;
        io.inputCount = 1; // only one in the slot
        RecipePlan plan = new RecipePlan(20, 2, new ItemStack(Items.IRON_INGOT), List.of(), 0f); // needs two
        RecipeProcessorComponent processor = processor(io, plan, 10);

        processor.serverTick(new FakeMachineContext());

        assertThat(processor.energySpent()).isZero();
        assertThat(io.energy).isEqualTo(1_000);
    }

    /** Two-input store: slots 0,1 are inputs and 2,3 are outputs (the alloy-smelter shape). */
    private static ItemStoreComponent twoInputStore(Runnable onChanged) {
        return new ItemStoreComponent(
                "items",
                new SlotRole[] {SlotRole.INPUT, SlotRole.INPUT, SlotRole.OUTPUT, SlotRole.OUTPUT},
                SidedLayout.bottomOut(new int[] {0, 1}, new int[] {2, 3}, stack -> true),
                onChanged);
    }

    @Test
    void consumesPerSlotCountsForMultipleInputs() {
        Runnable noop = () -> {};
        ItemStoreComponent items = twoInputStore(noop);
        EnergyStorageComponent energy = new EnergyStorageComponent("energy", 100_000, 100_000, 0, noop);
        energy.energy(null).insert(100_000, false);

        items.container().setItem(0, new ItemStack(Items.COPPER_INGOT, 5));
        items.container().setItem(1, new ItemStack(Items.RAW_IRON, 2));

        // Consumes 3 from slot 0 and 1 from slot 1, producing the primary plus a guaranteed byproduct.
        RecipePlan plan = new RecipePlan(
                20,
                new int[] {3, 1},
                new ItemStack(Items.IRON_INGOT, 4),
                List.of(new ChanceOutput(new ItemStack(Items.GUNPOWDER), 1.0f)),
                0f);
        RecipeProcessorComponent processor = new RecipeProcessorComponent(
                "processor", (io, ctx) -> plan, 10, items, energy, (ctx, lit) -> {}, noop);

        FakeMachineContext ctx = new FakeMachineContext();
        processor.serverTick(ctx); // 10
        processor.serverTick(ctx); // 20 -> complete

        assertThat(items.container().getItem(0).getCount()).isEqualTo(2); // 5 - 3
        assertThat(items.container().getItem(1).getCount()).isEqualTo(1); // 2 - 1
        assertThat(items.container().getItem(2).getItem()).isEqualTo(Items.IRON_INGOT);
        assertThat(items.container().getItem(2).getCount()).isEqualTo(4);
        assertThat(items.container().getItem(3).getItem()).isEqualTo(Items.GUNPOWDER);
    }

    @Test
    void doesNotCarryProgressWhenASecondaryInputCostChanges() {
        Runnable noop = () -> {};
        ItemStoreComponent items = twoInputStore(noop);
        EnergyStorageComponent energy = new EnergyStorageComponent("energy", 100_000, 100_000, 0, noop);
        energy.energy(null).insert(100_000, false);
        items.container().setItem(0, new ItemStack(Items.COPPER_INGOT, 16));
        items.container().setItem(1, new ItemStack(Items.RAW_IRON, 16));

        // Identical energy, result, and PRIMARY count; only the secondary slot's cost differs, so the
        // two must still count as different recipes (secondary inputs affect recipe identity).
        RecipePlan layoutA = new RecipePlan(40, new int[] {1, 1}, new ItemStack(Items.IRON_INGOT), List.of(), 0f);
        RecipePlan layoutB = new RecipePlan(40, new int[] {1, 2}, new ItemStack(Items.IRON_INGOT), List.of(), 0f);
        RecipePlan[] active = {layoutA};
        RecipeProcessorComponent processor = new RecipeProcessorComponent(
                "processor", (io, ctx) -> active[0], 10, items, energy, (ctx, lit) -> {}, noop);
        FakeMachineContext ctx = new FakeMachineContext();

        processor.serverTick(ctx); // 10
        processor.serverTick(ctx); // 20 on layout A
        assertThat(processor.energySpent()).isEqualTo(20);

        active[0] = layoutB; // only the secondary slot's cost changed
        processor.serverTick(ctx);
        // Progress reset to 0 then spent 10 this tick — it did NOT inherit the 20 (which would give 30).
        assertThat(processor.energySpent()).isEqualTo(10);
    }

    @Test
    void stallsWhenAnyInputSlotIsShort() {
        Runnable noop = () -> {};
        ItemStoreComponent items = twoInputStore(noop);
        EnergyStorageComponent energy = new EnergyStorageComponent("energy", 100_000, 100_000, 0, noop);
        energy.energy(null).insert(100_000, false);

        items.container().setItem(0, new ItemStack(Items.COPPER_INGOT, 5));
        // slot 1 left empty -> the recipe needs one there and must not run

        RecipePlan plan = new RecipePlan(
                20, new int[] {3, 1}, new ItemStack(Items.IRON_INGOT, 4), List.of(), 0f);
        RecipeProcessorComponent processor = new RecipeProcessorComponent(
                "processor", (io, ctx) -> plan, 10, items, energy, (ctx, lit) -> {}, noop);

        processor.serverTick(new FakeMachineContext());

        assertThat(processor.energySpent()).isZero();
        assertThat(items.container().getItem(0).getCount()).isEqualTo(5); // untouched
    }
}
