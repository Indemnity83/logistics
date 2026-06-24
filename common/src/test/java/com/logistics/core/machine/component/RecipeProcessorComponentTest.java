package com.logistics.core.machine.component;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.core.machine.FakeMachineContext;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

class RecipeProcessorComponentTest extends MinecraftTestEnvironment {

    /** In-memory IO so the processor's tick flow can be exercised without a live machine. */
    private static final class FakeProcessIO implements ProcessIO {
        long energy;
        boolean hasInput = true;
        boolean outputAccepts = true;
        int inputsConsumed;
        int outputsProduced;

        @Override
        public ItemStack input() {
            return hasInput ? new ItemStack(Items.RAW_IRON) : ItemStack.EMPTY;
        }

        @Override
        public boolean canAcceptOutput(ItemStack result) {
            return outputAccepts;
        }

        @Override
        public void consumeInput() {
            inputsConsumed++;
        }

        @Override
        public void produceOutput(ItemStack result) {
            outputsProduced++;
        }

        @Override
        public long energyStored() {
            return energy;
        }

        @Override
        public void consumeEnergy(long rf) {
            energy -= rf;
        }
    }

    private static RecipeProcessorComponent processor(FakeProcessIO io, long required, long rfPerTick) {
        RecipeResolver resolver = (i, ctx) ->
                io.hasInput ? new RecipePlan(required, new ItemStack(Items.IRON_INGOT), 0f) : null;
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
}
