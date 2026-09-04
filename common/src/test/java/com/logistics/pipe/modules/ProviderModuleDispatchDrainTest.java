package com.logistics.pipe.modules;

import com.logistics.core.lib.block.capability.PipeConnection;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.core.lib.pipe.TravelingItem;
import com.logistics.core.lib.storage.ContainerItemStorage;
import com.logistics.core.lib.storage.ItemStorageLookup;
import com.logistics.test.FakePipeAccess;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link ProviderModule#processDispatchQueue} ships the right item for every queue
 * entry it drains in a single cycle.
 *
 * <h2>Regression context</h2>
 * <p>The drain loop resolved the {@code IItemKey} once, from the head entry captured before the
 * loop, then re-peeked the head on every iteration without re-deriving the item. A Provider MkII
 * ({@code stackLimit = 4}) that finished one order and started the next in the same cycle therefore
 * extracted and shipped the <em>first</em> entry's item while crediting the <em>second</em> entry's
 * order as fulfilled — the second requester silently received the wrong item.
 *
 * <p>Only the MkII module can iterate more than once; the base provider is {@code (8, 1)}.
 */
@DisplayName("ProviderModule dispatch queue drain")
class ProviderModuleDispatchDrainTest extends MinecraftTestEnvironment {

    private static final BlockPos PIPE_POS = BlockPos.ZERO;
    private static final BlockPos CHEST_POS = PIPE_POS.relative(Direction.NORTH);
    private static final BlockPos REQUESTER_A = new BlockPos(10, 1, 0);
    private static final BlockPos REQUESTER_B = new BlockPos(20, 1, 0);
    private static final UUID DELIVERY_A = UUID.randomUUID();
    private static final UUID DELIVERY_B = UUID.randomUUID();

    private SimpleContainer chest;
    private FakePipeAccess access;
    private PipeContext ctx;

    @BeforeEach
    void setUp() {
        chest = new SimpleContainer(27);
        ItemStorageLookup.register((world, pos, dir) ->
                CHEST_POS.equals(pos) ? new ContainerItemStorage(chest) : null);

        access = new FakePipeAccess().setConnection(Direction.NORTH, PipeConnection.Type.INVENTORY);
        ctx = new PipeContext(null, PIPE_POS, null, access);
    }

    @AfterEach
    void tearDown() {
        ItemStorageLookup.register((world, pos, dir) -> null);
    }

    /** A Provider MkII: 64 items and 4 stacks per drain cycle. */
    private static ProviderModule providerMkII() {
        return new ProviderModule(64, 4);
    }

    private void seedQueue(ProviderModule provider, ProviderDispatchQueue queue) {
        provider.saveQueue(ctx, queue);
    }

    @Test
    @DisplayName("two orders drained in one cycle each ship their own item")
    void twoOrdersInOneCycleEachShipTheirOwnItem() {
        chest.setItem(0, new ItemStack(Items.DIAMOND, 64));
        chest.setItem(1, new ItemStack(Items.EMERALD, 64));

        ProviderModule provider = providerMkII();
        ProviderDispatchQueue queue = new ProviderDispatchQueue();
        queue.enqueue("minecraft:diamond", 4, REQUESTER_A, DELIVERY_A);
        queue.enqueue("minecraft:emerald", 8, REQUESTER_B, DELIVERY_B);
        seedQueue(provider, queue);

        provider.processDispatchQueue(ctx);

        List<TravelingItem> shipped = access.getInjectedItems();
        assertThat(shipped).hasSize(2);

        assertThat(shipped.get(0).getStack().getItem()).isEqualTo(Items.DIAMOND);
        assertThat(shipped.get(0).getStack().getCount()).isEqualTo(4);
        assertThat(shipped.get(0).getDestination()).isEqualTo(REQUESTER_A);
        assertThat(shipped.get(0).getDeliveryId()).isEqualTo(DELIVERY_A);

        assertThat(shipped.get(1).getStack().getItem())
                .as("second order is for emeralds, so emeralds must be shipped")
                .isEqualTo(Items.EMERALD);
        assertThat(shipped.get(1).getStack().getCount()).isEqualTo(8);
        assertThat(shipped.get(1).getDestination()).isEqualTo(REQUESTER_B);
        assertThat(shipped.get(1).getDeliveryId()).isEqualTo(DELIVERY_B);

        assertThat(chest.countItem(Items.DIAMOND)).isEqualTo(60);
        assertThat(chest.countItem(Items.EMERALD)).isEqualTo(56);
    }

    @Test
    @DisplayName("consecutive orders for the same item still drain in one cycle")
    void consecutiveOrdersForTheSameItemStillDrainInOneCycle() {
        chest.setItem(0, new ItemStack(Items.DIAMOND, 64));

        ProviderModule provider = providerMkII();
        ProviderDispatchQueue queue = new ProviderDispatchQueue();
        queue.enqueue("minecraft:diamond", 4, REQUESTER_A, DELIVERY_A);
        queue.enqueue("minecraft:diamond", 8, REQUESTER_B, DELIVERY_B);
        seedQueue(provider, queue);

        provider.processDispatchQueue(ctx);

        List<TravelingItem> shipped = access.getInjectedItems();
        assertThat(shipped).hasSize(2);
        assertThat(shipped.get(0).getStack().getCount()).isEqualTo(4);
        assertThat(shipped.get(0).getDestination()).isEqualTo(REQUESTER_A);
        assertThat(shipped.get(1).getStack().getCount()).isEqualTo(8);
        assertThat(shipped.get(1).getDestination()).isEqualTo(REQUESTER_B);
        assertThat(chest.countItem(Items.DIAMOND)).isEqualTo(52);
    }

    @Test
    @DisplayName("a single-stack provider is unaffected — it never reaches the second entry")
    void singleStackProviderShipsOnlyTheHeadEntry() {
        chest.setItem(0, new ItemStack(Items.DIAMOND, 64));
        chest.setItem(1, new ItemStack(Items.EMERALD, 64));

        ProviderModule provider = new ProviderModule(8, 1); // base provider
        ProviderDispatchQueue queue = new ProviderDispatchQueue();
        queue.enqueue("minecraft:diamond", 4, REQUESTER_A, DELIVERY_A);
        queue.enqueue("minecraft:emerald", 8, REQUESTER_B, DELIVERY_B);
        seedQueue(provider, queue);

        provider.processDispatchQueue(ctx);

        List<TravelingItem> shipped = access.getInjectedItems();
        assertThat(shipped).hasSize(1);
        assertThat(shipped.get(0).getStack().getItem()).isEqualTo(Items.DIAMOND);
        assertThat(chest.countItem(Items.EMERALD)).isEqualTo(64);
    }
}
