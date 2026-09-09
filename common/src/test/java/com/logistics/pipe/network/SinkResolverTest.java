package com.logistics.pipe.network;

import com.logistics.core.lib.network.IWorldView;
import com.logistics.core.lib.network.NetworkGraph;
import com.logistics.core.lib.storage.IItemKey;
import com.logistics.pipe.modules.SinkPriority;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SinkResolver}, focusing on sink registration, resolution,
 * and the merge behavior when two networks join.
 */
class SinkResolverTest extends MinecraftTestEnvironment {

    private static final BlockPos POS_A = new BlockPos(0, 0, 0);
    private static final BlockPos POS_B = new BlockPos(10, 0, 0);
    private static final BlockPos POS_C = new BlockPos(20, 0, 0);

    // Tracks which positions the stub world view reports as accepting all items
    private final Set<BlockPos> accepting = new HashSet<>();

    private final IWorldView stubView = new IWorldView() {
        @Override public boolean isPipe(BlockPos pos) { return false; }
        @Override public List<BlockPos> getConnectedNeighbors(BlockPos pos) { return List.of(); }
        @Override public boolean matchesSinkFilter(BlockPos pos, ItemStack stack) { return accepting.contains(pos); }
        @Override public long dispatch(BlockPos p, BlockPos r, IItemKey i, long a, UUID d) { return 0; }
        @Override public boolean isClientSide() { return false; }
        @Override public void broadcastAlert(BlockPos pos, Component message) {}
        @Override public long gameTime() { return 0L; }
    };

    private NetworkGraph graphA;
    private NetworkGraph graphB;
    private SinkResolver resolverA;
    private SinkResolver resolverB;

    @BeforeEach
    void setUp() {
        graphA = new NetworkGraph();
        graphB = new NetworkGraph();
        resolverA = new SinkResolver(graphA, stubView);
        resolverB = new SinkResolver(graphB, stubView);
        accepting.clear();
    }

    // -------------------------------------------------------------------------
    // Basic resolution
    // -------------------------------------------------------------------------

    @Test
    void findSinkFor_returnsHighestPrioritySink() {
        graphA.addNode(POS_A);
        graphA.addNode(POS_B);
        accepting.add(POS_A);
        accepting.add(POS_B);
        resolverA.registerSink(POS_A, 3);
        resolverA.registerSink(POS_B, 7);
        resolverA.registerSinkInterest(POS_A, Items.IRON_INGOT);
        resolverA.registerSinkInterest(POS_B, Items.IRON_INGOT);

        assertEquals(POS_B, resolverA.findSinkFor(new ItemStack(Items.IRON_INGOT)));
    }

    @Test
    void findFilteredSinkFor_skipsCatchAllSinks() {
        graphA.addNode(POS_A);
        accepting.add(POS_A);
        resolverA.registerSink(POS_A, 0); // priority 0 = catch-all
        resolverA.registerGenericSinkInterest(POS_A);

        assertNull(resolverA.findFilteredSinkFor(new ItemStack(Items.IRON_INGOT)));
        assertNotNull(resolverA.findSinkFor(new ItemStack(Items.IRON_INGOT)));
    }

    // -------------------------------------------------------------------------
    // Tie resolution: equal priorities must resolve deterministically
    // -------------------------------------------------------------------------

    // Two Item Sink modules in separate chassis — a genuine tie, since both draw the same
    // rung. Chosen because the pre-fix hash-iteration winner was TIED_HIGH, i.e. not the
    // positionally-lowest one.
    private static final BlockPos TIED_LOW = new BlockPos(6, 64, 7);
    private static final BlockPos TIED_HIGH = new BlockPos(8, 64, 8);

    /**
     * Builds a network holding the tied sink pair plus {@code unrelatedSinks} unrelated
     * lower-priority sinks, and returns the destination it picks for an iron ingot.
     */
    private BlockPos tiedPairDestination(int unrelatedSinks) {
        NetworkGraph graph = new NetworkGraph();
        SinkResolver resolver = new SinkResolver(graph, stubView);
        for (BlockPos pos : List.of(TIED_LOW, TIED_HIGH)) {
            graph.addNode(pos);
            accepting.add(pos);
            resolver.registerSink(pos, SinkPriority.ITEM_SINK);
            resolver.registerSinkInterest(pos, Items.IRON_INGOT);
        }
        for (int i = 1; i <= unrelatedSinks; i++) {
            BlockPos pos = new BlockPos(100 + i, 70, i);
            graph.addNode(pos);
            accepting.add(pos);
            resolver.registerSink(pos, SinkPriority.ENCHANTMENT_SINK);
            resolver.registerSinkInterest(pos, Items.IRON_INGOT);
        }
        return resolver.findSinkFor(new ItemStack(Items.IRON_INGOT));
    }

    @Test
    void findSinkFor_tieIsBrokenByPositionNotIterationOrder() {
        assertTrue(TIED_LOW.asLong() < TIED_HIGH.asLong());
        assertEquals(TIED_LOW, tiedPairDestination(0));
    }

    @Test
    void findSinkFor_tieWinnerSurvivesUnrelatedSinksJoiningTheNetwork() {
        // A tied sink pair must keep its destination when the network grows: absorbing
        // unrelated, lower-priority sinks reorders the candidate set but must not
        // redirect items that were already going somewhere.
        assertEquals(tiedPairDestination(0), tiedPairDestination(11));
    }

    @Test
    void findSinkFor_tiedSinkThatRejectsTheItemDoesNotShadowOneThatAcceptsIt() {
        graphA.addNode(TIED_LOW);
        graphA.addNode(TIED_HIGH);
        accepting.add(TIED_HIGH); // TIED_LOW is positionally preferred but refuses this item
        resolverA.registerSink(TIED_LOW, SinkPriority.ITEM_SINK);
        resolverA.registerSink(TIED_HIGH, SinkPriority.ITEM_SINK);
        resolverA.registerSinkInterest(TIED_LOW, Items.IRON_INGOT);
        resolverA.registerSinkInterest(TIED_HIGH, Items.IRON_INGOT);

        assertEquals(TIED_HIGH, resolverA.findSinkFor(new ItemStack(Items.IRON_INGOT)));
    }

    // -------------------------------------------------------------------------
    // Ladder: a specific Item Sink outranks a Polymorphic Sink
    // -------------------------------------------------------------------------

    @Test
    void findSinkFor_itemSinkOutranksPolymorphicSinkRegardlessOfPosition() {
        // The Polymorphic sink is placed at the LOWER position, so the positional tiebreak
        // would hand it the item if the two still shared a rung. Only the priority
        // difference can send the item to the configured Item Sink.
        BlockPos polymorphic = TIED_LOW;
        BlockPos itemSink = TIED_HIGH;
        assertTrue(polymorphic.asLong() < itemSink.asLong());

        graphA.addNode(polymorphic);
        graphA.addNode(itemSink);
        accepting.add(polymorphic);
        accepting.add(itemSink);
        resolverA.registerSink(polymorphic, SinkPriority.POLYMORPHIC_SINK);
        resolverA.registerGenericSinkInterest(polymorphic); // polymorphic checks live inventory
        resolverA.registerSink(itemSink, SinkPriority.ITEM_SINK);
        resolverA.registerSinkInterest(itemSink, Items.IRON_INGOT);

        assertEquals(itemSink, resolverA.findSinkFor(new ItemStack(Items.IRON_INGOT)));
    }

    // -------------------------------------------------------------------------
    // Merge: specific interests from absorbed network
    // -------------------------------------------------------------------------

    @Test
    void merge_specificInterestFromAbsorbedNetworkIsFound() {
        // POS_B registered only in network B with a specific item interest
        graphB.addNode(POS_B);
        accepting.add(POS_B);
        resolverB.registerSink(POS_B, 5);
        resolverB.registerSinkInterest(POS_B, Items.IRON_INGOT);

        // Simulate graph merge so resolverA's graph contains POS_B
        graphA.addNode(POS_B);
        resolverA.merge(resolverB);

        assertEquals(POS_B, resolverA.findFilteredSinkFor(new ItemStack(Items.IRON_INGOT)));
    }

    @Test
    void merge_genericInterestFromAbsorbedNetworkIsFound() {
        // POS_C registered in network B with generic interest
        graphB.addNode(POS_C);
        accepting.add(POS_C);
        resolverB.registerSink(POS_C, 0);
        resolverB.registerGenericSinkInterest(POS_C);

        graphA.addNode(POS_C);
        resolverA.merge(resolverB);

        // findSinkFor (unfiltered) must find POS_C via the merged generic interest
        assertEquals(POS_C, resolverA.findSinkFor(new ItemStack(Items.GOLD_INGOT)));
    }

    // -------------------------------------------------------------------------
    // Merge: priority ordering across networks
    // -------------------------------------------------------------------------

    @Test
    void merge_absorbedHighPrioritySinkBeatsOriginalLowerPriority() {
        // Network A: POS_A at priority 3
        graphA.addNode(POS_A);
        accepting.add(POS_A);
        resolverA.registerSink(POS_A, 3);
        resolverA.registerSinkInterest(POS_A, Items.IRON_INGOT);

        // Network B: POS_B at priority 10
        graphB.addNode(POS_B);
        accepting.add(POS_B);
        resolverB.registerSink(POS_B, 10);
        resolverB.registerSinkInterest(POS_B, Items.IRON_INGOT);

        graphA.addNode(POS_B);
        resolverA.merge(resolverB);

        // POS_B (priority 10) must win over POS_A (priority 3)
        assertEquals(POS_B, resolverA.findFilteredSinkFor(new ItemStack(Items.IRON_INGOT)));
    }

    @Test
    void merge_conflictingPriorityUsesMaximum() {
        // Both networks register POS_A with different priorities
        graphA.addNode(POS_A);
        accepting.add(POS_A);
        resolverA.registerSink(POS_A, 3);
        resolverA.registerSinkInterest(POS_A, Items.IRON_INGOT);

        resolverB.registerSink(POS_A, 8); // same position, higher priority in B
        resolverB.registerSinkInterest(POS_A, Items.IRON_INGOT);

        resolverA.merge(resolverB);

        // Also add a lower-priority sink to verify POS_A wins with merged priority 8
        graphA.addNode(POS_B);
        accepting.add(POS_B);
        resolverA.registerSink(POS_B, 5);
        resolverA.registerSinkInterest(POS_B, Items.IRON_INGOT);

        assertEquals(POS_A, resolverA.findFilteredSinkFor(new ItemStack(Items.IRON_INGOT)));
    }

    // -------------------------------------------------------------------------
    // Cleanup: empty bucket pruning
    // -------------------------------------------------------------------------

    @Test
    void unregisterSink_prunesEmptySpecificInterestBuckets() {
        graphA.addNode(POS_A);
        accepting.add(POS_A);
        resolverA.registerSink(POS_A, 5);
        resolverA.registerSinkInterest(POS_A, Items.IRON_INGOT);

        resolverA.unregisterSink(POS_A);

        // After unregister the sink must not be found
        assertNull(resolverA.findSinkFor(new ItemStack(Items.IRON_INGOT)));
    }

    @Test
    void remove_clearsSinkFromAllIndices() {
        graphA.addNode(POS_A);
        accepting.add(POS_A);
        resolverA.registerSink(POS_A, 5);
        resolverA.registerSinkInterest(POS_A, Items.IRON_INGOT);
        resolverA.registerGenericSinkInterest(POS_A);

        resolverA.remove(POS_A);

        assertNull(resolverA.findSinkFor(new ItemStack(Items.IRON_INGOT)));
        assertNull(resolverA.findSinkFor(new ItemStack(Items.GOLD_INGOT)));
    }
}
