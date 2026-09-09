package com.logistics.pipe.modules;

import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.test.FakePipeAccess;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Save-format guards for {@link ItemFilterModule} that can run without a live level.
 *
 * <h2>What this class can and cannot cover</h2>
 * Both {@link ItemFilterModule#getFilterStacks} and {@link ItemFilterModule#setFilterStacks} derive
 * their {@link RegistryOps} from {@code ctx.world().registryAccess()} and short-circuit to
 * "everything empty" when there is no level. A unit test cannot build a {@link
 * net.minecraft.world.level.Level}, so <em>no stored payload can be decoded here</em> — a fixture
 * injected into module state is inert whatever shape it has, and a test that reads one back is
 * asserting the absence of a level rather than anything about the format.
 *
 * <p>So the value-level round-trip — write a filter, reload it, still get the same item with its
 * components — is a GameTest, where a real {@code ServerLevel} exists: see
 * {@code ModuleGameTestBody#testFilterModuleRoutesMatchingItems} and
 * {@code #testFilterModuleMultipleSideFilters}, which write filters through
 * {@code setFilterStacks} on a placed pipe and then route real items by them. That is the guard
 * against the 0.5.4 filter-data-loss regression. What stays here is the part that needs no
 * registry: the shape of the list {@code getFilterStacks} hands back, and the refusal to
 * half-write without one.
 *
 * <p>Do not add a fixture-decoding test to this class. It will pass on an empty result no matter
 * what the fixture says.
 */
@DisplayName("ItemFilterModule — save-format guards")
class ItemFilterModuleSerializationGoldenTest extends MinecraftTestEnvironment {

    // The module state key used by ItemFilterModule (getStateKey() = simpleName().toLowerCase())
    // DO NOT change this string — if ItemFilterModule is renamed, it must keep returning "itemfiltermodule"
    private static final String MODULE_STATE_KEY = "itemfiltermodule";

    private ItemFilterModule module;
    private FakePipeAccess access;
    private PipeContext ctx;

    @BeforeEach
    void setUp() {
        module = new ItemFilterModule();
        access = new FakePipeAccess();
        ctx = new PipeContext(null, BlockPos.ZERO, null, access);
    }

    @Test
    @DisplayName("state key is stable — module rename would break existing saves")
    void moduleStateKeyIsStable() {
        // This test intentionally checks the key string. If ItemFilterModule is ever
        // renamed, getStateKey() MUST be overridden to keep returning "itemfiltermodule".
        assertThat(module.getStateKey()).isEqualTo(MODULE_STATE_KEY);
    }

    @Test
    @DisplayName("getFilterStacks always returns one entry per slot, whatever is stored")
    void getFilterStacksIsAlwaysOneEntryPerSlot() {
        // Nothing stored at all.
        assertThat(module.getFilterStacks(ctx, Direction.NORTH)).hasSize(ItemFilterModule.FILTER_SLOTS_PER_SIDE);

        // A stored side, but a list shorter than the slot count — the tail must still be padded,
        // or a screen reading slot 7 off a three-entry save walks off the end.
        injectNorthFilters(encodedList(Items.DIAMOND, Items.EMERALD, Items.GOLD_INGOT));
        assertThat(module.getFilterStacks(ctx, Direction.NORTH)).hasSize(ItemFilterModule.FILTER_SLOTS_PER_SIDE);

        // A side with no entry of its own still gets a full-width, all-empty list rather than a
        // short one, so "no filter configured" and "filter of nothing" are the same shape.
        List<ItemStack> south = module.getFilterStacks(ctx, Direction.SOUTH);
        assertThat(south).hasSize(ItemFilterModule.FILTER_SLOTS_PER_SIDE);
        assertThat(south).allMatch(ItemStack::isEmpty);
    }

    @Test
    @DisplayName("setFilterStacks writes nothing at all when there is nothing to write")
    void setFilterStacksWritesNothingForAnEmptyConfiguration() {
        List<ItemStack> allEmpty = Collections.nCopies(ItemFilterModule.FILTER_SLOTS_PER_SIDE, ItemStack.EMPTY);

        module.setFilterStacks(ctx, Direction.NORTH, allEmpty);

        // An empty configuration must not leave a "filters" husk behind: the module state is what
        // decides whether a saved pipe carries filter data at all.
        assertThat(access.getRawState(MODULE_STATE_KEY).getCompound(ItemFilterModule.FILTERS)).isEmpty();
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private void injectNorthFilters(ListTag list) {
        CompoundTag filters = new CompoundTag();
        filters.put(Direction.NORTH.getName(), list);
        access.getRawState(MODULE_STATE_KEY).put(ItemFilterModule.FILTERS, filters);
    }

    /** A list in the live on-disk shape: one ItemStack.CODEC compound per configured slot. */
    private static ListTag encodedList(net.minecraft.world.item.Item... items) {
        RegistryOps<Tag> ops = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)
                .createSerializationContext(NbtOps.INSTANCE);
        ListTag list = new ListTag();
        for (net.minecraft.world.item.Item item : items) {
            list.add(ItemStack.CODEC.encodeStart(ops, new ItemStack(item))
                    .result()
                    .orElseThrow(() -> new AssertionError("Encode failed for " + item)));
        }
        return list;
    }
}
