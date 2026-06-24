package com.logistics.core.machine.component;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.core.machine.FakeMachineContext;
import com.logistics.test.MinecraftTestEnvironment;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

class RecipeProcessorComponentTest extends MinecraftTestEnvironment {

    /** In-memory IO so the processor's tick flow can be exercised without a live machine. */
    private static final class FakeProcessIO implements ProcessIO {
        long energy;
        boolean hasInput = true;
        int inputCount = 1;
        boolean outputAccepts = true;
        int inputsConsumed;
        int outputsProduced;

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
}
