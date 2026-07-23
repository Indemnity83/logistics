package com.logistics.power.engine.reaction;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.core.machine.FakeMachineContext;
import com.logistics.core.machine.MachineContext;
import com.logistics.core.machine.component.FluidStoreComponent;
import com.logistics.test.MinecraftTestEnvironment;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * Behavior of the bufferless Reaction Engine simulation. Uses vanilla lava as the "reactant" and blaze
 * powder as the "catalyst" via an injected recipe lookup (so no mod fluids need registering), and injected
 * {@link ReactionOutput} fakes for the network.
 */
class ReactionEngineComponentTest extends MinecraftTestEnvironment {

    private static final ReactionEngineProfile PROFILE = ReactionEngineProfile.of(200, 500, 4_000, 250);

    private final HolderLookup.Provider registries =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    private final BiFunction<Fluid, ItemStack, ReactionRecipe> lookup = (f, i) ->
            (f == Fluids.LAVA && i.is(Items.BLAZE_POWDER)) ? new ReactionRecipe(f, Items.BLAZE_POWDER, null, null) : null;

    private static final ReactionOutput NONE = (ctx, amount) -> 0;
    private static final ReactionOutput FULL = (ctx, amount) -> amount;

    /** A network that accepts up to a mutable cap and records the amount offered each tick. */
    private static final class CapOutput implements ReactionOutput {
        long cap;
        long lastOffered;

        CapOutput(long cap) {
            this.cap = cap;
        }

        @Override
        public long push(MachineContext ctx, long amount) {
            lastOffered = amount;
            return Math.min(amount, Math.max(0, cap));
        }
    }

    private FluidStoreComponent tank(@Nullable Fluid fluid, long mb) {
        FluidStoreComponent s = new FluidStoreComponent("reactant", FluidUnits.mb(1_000_000), () -> {});
        if (fluid != null && mb > 0) {
            s.tank().setContents(SimpleFluidKey.of(fluid), FluidUnits.mb(mb));
        }
        return s;
    }

    private SimpleContainer catalyst(@Nullable Item item, int count) {
        SimpleContainer c = new SimpleContainer(1);
        if (item != null) {
            c.setItem(0, new ItemStack(item, count));
        }
        return c;
    }

    private ReactionEngineComponent engine(
            ReactionOutput out, FluidStoreComponent tank, Container catalyst, BooleanSupplier powered) {
        return new ReactionEngineComponent("reaction", out, tank, catalyst, lookup, powered, PROFILE, () -> {});
    }

    private void seed(ReactionEngineComponent c, int remaining, long committedOutput) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("RemainingReactionTicks", remaining);
        tag.putLong("CommittedOutputPerTick", committedOutput);
        c.load(tag, registries);
    }

    private void tick(ReactionEngineComponent c) {
        c.serverTick(new FakeMachineContext());
    }

    private void tick(ReactionEngineComponent c, int n) {
        for (int i = 0; i < n; i++) {
            tick(c);
        }
    }

    private long mb(long millibuckets) {
        return FluidUnits.mb(millibuckets);
    }

    // ==================== Ignition ====================

    @Test
    void validReactantAndCatalystStartsReaction() {
        FluidStoreComponent tank = tank(Fluids.LAVA, 1000);
        SimpleContainer cat = catalyst(Items.BLAZE_POWDER, 1);
        ReactionEngineComponent r = engine(FULL, tank, cat, () -> true);
        tick(r);
        assertThat(r.isReacting()).isTrue();
        assertThat(r.remainingReactionTicks()).isEqualTo(499); // 500 committed, minus this tick's generation
        assertThat(tank.tank().getAmount()).isEqualTo(mb(750)); // exactly 250 mB consumed
        assertThat(cat.getItem(0).isEmpty()).isTrue(); // exactly 1 catalyst consumed
        assertThat(r.lastAttempted()).isEqualTo(200);
        assertThat(r.lastAccepted()).isEqualTo(200);
    }

    @Test
    void redstoneOffBlocksNewReaction() {
        FluidStoreComponent tank = tank(Fluids.LAVA, 1000);
        SimpleContainer cat = catalyst(Items.BLAZE_POWDER, 1);
        ReactionEngineComponent r = engine(FULL, tank, cat, () -> false);
        tick(r);
        assertThat(r.isReacting()).isFalse();
        assertThat(tank.tank().getAmount()).isEqualTo(mb(1000)); // nothing consumed
        assertThat(cat.getItem(0).getCount()).isEqualTo(1);
    }

    @Test
    void invalidCatalystDoesNotReact() {
        FluidStoreComponent tank = tank(Fluids.LAVA, 1000);
        SimpleContainer cat = catalyst(Items.DIAMOND, 1); // not a valid catalyst
        ReactionEngineComponent r = engine(FULL, tank, cat, () -> true);
        tick(r);
        assertThat(r.isReacting()).isFalse();
        assertThat(tank.tank().getAmount()).isEqualTo(mb(1000));
    }

    @Test
    void notAFullBatchDoesNotReact() {
        FluidStoreComponent tank = tank(Fluids.LAVA, 100); // < 250 mB batch
        SimpleContainer cat = catalyst(Items.BLAZE_POWDER, 1);
        ReactionEngineComponent r = engine(FULL, tank, cat, () -> true);
        tick(r);
        assertThat(r.isReacting()).isFalse();
        assertThat(tank.tank().getAmount()).isEqualTo(mb(100)); // untouched
        assertThat(cat.getItem(0).getCount()).isEqualTo(1);
    }

    @Test
    void canStartReactionReflectsPowerAndValidPair() {
        assertThat(engine(FULL, tank(Fluids.LAVA, 1000), catalyst(Items.BLAZE_POWDER, 1), () -> true)
                .canStartReaction()).isTrue();
        assertThat(engine(FULL, tank(Fluids.LAVA, 1000), catalyst(Items.BLAZE_POWDER, 1), () -> false)
                .canStartReaction()).isFalse();
        assertThat(engine(FULL, tank(Fluids.LAVA, 1000), catalyst(Items.DIAMOND, 1), () -> true)
                .canStartReaction()).isFalse();
    }

    // ==================== Committed reaction ====================

    @Test
    void committedReactionRunsToCompletion() {
        ReactionEngineComponent r = engine(FULL, tank(null, 0), catalyst(null, 0), () -> true);
        seed(r, 500, 200);
        tick(r, 499);
        assertThat(r.isReacting()).isTrue();
        assertThat(r.remainingReactionTicks()).isEqualTo(1);
        tick(r);
        assertThat(r.isReacting()).isFalse();
        assertThat(r.remainingReactionTicks()).isZero();
    }

    @Test
    void runningReactionIgnoresRedstone() {
        CapOutput out = new CapOutput(Long.MAX_VALUE);
        ReactionEngineComponent r = engine(out, tank(null, 0), catalyst(null, 0), () -> false); // unpowered
        seed(r, 100, 200);
        tick(r, 10);
        assertThat(r.remainingReactionTicks()).isEqualTo(90); // still counting down while unpowered
        assertThat(r.lastAccepted()).isEqualTo(200); // still pushing
        assertThat(out.lastOffered).isEqualTo(200);
    }

    @Test
    void noNewReactionCommittedWhileReacting() {
        FluidStoreComponent tank = tank(Fluids.LAVA, 4000);
        SimpleContainer cat = catalyst(Items.BLAZE_POWDER, 8);
        ReactionEngineComponent r = engine(FULL, tank, cat, () -> true);
        seed(r, 300, 200);
        tick(r, 100);
        assertThat(tank.tank().getAmount()).isEqualTo(mb(4000)); // no reactant consumed mid-reaction
        assertThat(cat.getItem(0).getCount()).isEqualTo(8); // no catalyst consumed mid-reaction
    }

    // ==================== Direct output / discard ====================

    @Test
    void generationIsLimitedByNetworkAcceptance() {
        ReactionEngineComponent r = engine((ctx, a) -> Math.min(a, 150), tank(null, 0), catalyst(null, 0), () -> true);
        seed(r, 100, 200);
        tick(r);
        assertThat(r.lastAttempted()).isEqualTo(200);
        assertThat(r.lastAccepted()).isEqualTo(150); // network only took 150
    }

    @Test
    void excessRfIsDiscardedNotCarriedOver() {
        CapOutput out = new CapOutput(50); // network accepts only 50 this tick
        ReactionEngineComponent r = engine(out, tank(null, 0), catalyst(null, 0), () -> true);
        seed(r, 5, 200);
        tick(r);
        assertThat(out.lastOffered).isEqualTo(200);
        assertThat(r.lastAccepted()).isEqualTo(50); // 150 discarded
        out.cap = Long.MAX_VALUE; // network frees up
        tick(r);
        assertThat(out.lastOffered).isEqualTo(200); // offers exactly outputPerTick, NOT 200 + 150 leftover
        assertThat(r.lastAccepted()).isEqualTo(200);
    }

    @Test
    void fullyBlockedNetworkStillReactsAndWastesEverything() {
        ReactionEngineComponent r = engine(NONE, tank(null, 0), catalyst(null, 0), () -> true);
        seed(r, 10, 200);
        tick(r, 5);
        assertThat(r.remainingReactionTicks()).isEqualTo(5); // burns regardless of acceptance
        assertThat(r.lastAttempted()).isEqualTo(200);
        assertThat(r.lastAccepted()).isZero(); // all wasted
    }

    // ==================== Persistence ====================

    @Test
    void savePersistsOnlyTimerAndRateNoStoredRf() {
        ReactionEngineComponent r = engine(FULL, tank(null, 0), catalyst(null, 0), () -> true);
        seed(r, 300, 200);
        CompoundTag tag = new CompoundTag();
        r.save(tag, registries);
        assertThat(tag.contains("RemainingReactionTicks")).isTrue();
        assertThat(tag.contains("CommittedOutputPerTick")).isTrue();
        assertThat(tag.contains("StoredEnergy")).isFalse();
        assertThat(tag.contains("Energy")).isFalse();
    }

    @Test
    void saveLoadDuringActiveReactionResumesAtCommittedRate() {
        ReactionEngineComponent writer = engine(FULL, tank(null, 0), catalyst(null, 0), () -> true);
        seed(writer, 300, 200);
        CompoundTag tag = new CompoundTag();
        writer.save(tag, registries);

        CapOutput out = new CapOutput(Long.MAX_VALUE);
        ReactionEngineComponent reader = engine(out, tank(null, 0), catalyst(null, 0), () -> false); // unpowered
        reader.load(tag, registries);
        assertThat(reader.remainingReactionTicks()).isEqualTo(300);
        assertThat(reader.isReacting()).isTrue();
        tick(reader);
        assertThat(reader.remainingReactionTicks()).isEqualTo(299); // resumes and keeps reacting
        assertThat(out.lastOffered).isEqualTo(200); // at the committed rate
    }

    // ==================== Piston / animation ====================

    @Test
    void pistonRunsFastWhileReactingStopsWhenIdle() {
        ReactionEngineComponent r = engine(FULL, tank(null, 0), catalyst(null, 0), () -> true);
        seed(r, 100, 200);
        assertThat(r.pistonSpeed()).isEqualTo(0.12f);
        assertThat(r.isReacting()).isTrue();

        seed(r, 0, 200);
        assertThat(r.pistonSpeed()).isZero();
        assertThat(r.isReacting()).isFalse();
    }
}
