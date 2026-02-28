package com.logistics.pipe.network;

import static org.junit.jupiter.api.Assertions.*;

import com.logistics.test.MinecraftTestEnvironment;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for RequestMatcher.
 * Tests request/provider matching logic with minimal Minecraft dependencies.
 */
class RequestMatcherTest extends MinecraftTestEnvironment {
    private RequestMatcher matcher;

    @BeforeEach
    void setUp() {
        matcher = new RequestMatcher();
    }

    @Test
    void testUpdateProviderCache() {
        BlockPos provider = new BlockPos(10, 64, 10);
        Map<ItemStack, Long> items = new HashMap<>();
        items.put(new ItemStack(Items.DIAMOND), 64L);

        matcher.updateProviderCache(provider, items, 0);

        assertEquals(64L, matcher.getAvailableAmount(new ItemStack(Items.DIAMOND)));
    }

    @Test
    void testGetAvailableAmountMultipleProviders() {
        BlockPos provider1 = new BlockPos(10, 64, 10);
        BlockPos provider2 = new BlockPos(20, 64, 20);

        Map<ItemStack, Long> items1 = new HashMap<>();
        items1.put(new ItemStack(Items.DIAMOND), 32L);

        Map<ItemStack, Long> items2 = new HashMap<>();
        items2.put(new ItemStack(Items.DIAMOND), 16L);

        matcher.updateProviderCache(provider1, items1, 0);
        matcher.updateProviderCache(provider2, items2, 0);

        assertEquals(48L, matcher.getAvailableAmount(new ItemStack(Items.DIAMOND)));
    }

    @Test
    void testGetAvailableAmountNoProviders() {
        assertEquals(0L, matcher.getAvailableAmount(new ItemStack(Items.DIAMOND)));
    }

    @Test
    void testGetAllAvailableItems() {
        BlockPos provider = new BlockPos(10, 64, 10);
        Map<ItemStack, Long> items = new HashMap<>();
        items.put(new ItemStack(Items.DIAMOND), 64L);
        items.put(new ItemStack(Items.EMERALD), 32L);

        matcher.updateProviderCache(provider, items, 0);

        Map<ItemStack, Long> available = matcher.getAllAvailableItems();
        assertEquals(2, available.size());
    }

    @Test
    void testFindProviderForSufficientQuantity() {
        BlockPos provider = new BlockPos(10, 64, 10);
        Map<ItemStack, Long> items = new HashMap<>();
        items.put(new ItemStack(Items.DIAMOND), 64L);

        matcher.updateProviderCache(provider, items, 0);

        BlockPos result = matcher.findProviderFor(new ItemStack(Items.DIAMOND), 32L);

        assertEquals(provider, result);
    }

    @Test
    void testFindProviderForInsufficientQuantity() {
        BlockPos provider = new BlockPos(10, 64, 10);
        Map<ItemStack, Long> items = new HashMap<>();
        items.put(new ItemStack(Items.DIAMOND), 16L);

        matcher.updateProviderCache(provider, items, 0);

        BlockPos result = matcher.findProviderFor(new ItemStack(Items.DIAMOND), 32L);

        assertNull(result, "Should return null when no provider has sufficient quantity");
    }

    @Test
    void testFindProviderForNoProviders() {
        BlockPos result = matcher.findProviderFor(new ItemStack(Items.DIAMOND), 32L);

        assertNull(result, "Should return null when no providers exist");
    }

    @Test
    void testAddRequest() {
        BlockPos requester = new BlockPos(5, 64, 5);
        ItemRequest request = new ItemRequest(requester, new ItemStack(Items.DIAMOND), 32L, 0L, 0);

        matcher.addRequest(request);

        assertEquals(1, matcher.getPendingRequests().size());
        assertEquals(request, matcher.getPendingRequests().get(0));
    }

    @Test
    void testAddOrder() {
        BlockPos provider = new BlockPos(10, 64, 10);
        BlockPos requester = new BlockPos(5, 64, 5);
        LogisticsOrder order = new LogisticsOrder(
            provider,
            requester,
            new ItemStack(Items.DIAMOND),
            32L,
            0L
        );

        matcher.addOrder(order);

        List<LogisticsOrder> orders = matcher.getOrdersFor(provider);
        assertEquals(1, orders.size());
        assertEquals(order, orders.get(0));
    }

    @Test
    void testGetOrdersForNoOrders() {
        BlockPos provider = new BlockPos(10, 64, 10);

        List<LogisticsOrder> orders = matcher.getOrdersFor(provider);

        assertTrue(orders.isEmpty());
    }

    @Test
    void testRemoveOrder() {
        BlockPos provider = new BlockPos(10, 64, 10);
        BlockPos requester = new BlockPos(5, 64, 5);
        LogisticsOrder order = new LogisticsOrder(
            provider,
            requester,
            new ItemStack(Items.DIAMOND),
            32L,
            0L
        );

        matcher.addOrder(order);
        matcher.removeOrder(order);

        assertTrue(matcher.getOrdersFor(provider).isEmpty());
    }

    @Test
    void testProcessRequestsSuccessfulMatch() {
        // Setup provider
        BlockPos provider = new BlockPos(10, 64, 10);
        Map<ItemStack, Long> items = new HashMap<>();
        items.put(new ItemStack(Items.DIAMOND), 64L);
        matcher.updateProviderCache(provider, items, 0);

        // Add request
        BlockPos requester = new BlockPos(5, 64, 5);
        ItemRequest request = new ItemRequest(requester, new ItemStack(Items.DIAMOND), 32L, 0L, 0);
        matcher.addRequest(request);

        // Process
        int matched = matcher.processRequests(100L);

        assertEquals(1, matched, "Should match 1 request");
        assertTrue(matcher.getPendingRequests().isEmpty(), "Request should be removed after matching");
        assertEquals(1, matcher.getOrdersFor(provider).size(), "Order should be created");
    }

    @Test
    void testProcessRequestsNoMatch() {
        // Add request without any providers
        BlockPos requester = new BlockPos(5, 64, 5);
        ItemRequest request = new ItemRequest(requester, new ItemStack(Items.DIAMOND), 32L, 0L, 0);
        matcher.addRequest(request);

        // Process
        int matched = matcher.processRequests(100L);

        assertEquals(0, matched, "Should match 0 requests");
        assertEquals(1, matcher.getPendingRequests().size(), "Request should remain pending");
    }

    @Test
    void testProcessRequestsMultipleRequests() {
        // Setup providers
        BlockPos provider1 = new BlockPos(10, 64, 10);
        Map<ItemStack, Long> items1 = new HashMap<>();
        items1.put(new ItemStack(Items.DIAMOND), 64L);
        matcher.updateProviderCache(provider1, items1, 0);

        BlockPos provider2 = new BlockPos(20, 64, 20);
        Map<ItemStack, Long> items2 = new HashMap<>();
        items2.put(new ItemStack(Items.EMERALD), 32L);
        matcher.updateProviderCache(provider2, items2, 0);

        // Add requests
        BlockPos requester = new BlockPos(5, 64, 5);
        matcher.addRequest(new ItemRequest(requester, new ItemStack(Items.DIAMOND), 32L, 0L, 0));
        matcher.addRequest(new ItemRequest(requester, new ItemStack(Items.EMERALD), 16L, 0L, 0));

        // Process
        int matched = matcher.processRequests(100L);

        assertEquals(2, matched, "Should match both requests");
        assertTrue(matcher.getPendingRequests().isEmpty());
    }

    @Test
    void testRemoveProviderCache() {
        BlockPos provider = new BlockPos(10, 64, 10);
        Map<ItemStack, Long> items = new HashMap<>();
        items.put(new ItemStack(Items.DIAMOND), 64L);

        matcher.updateProviderCache(provider, items, 0);
        matcher.removeProviderCache(provider);

        assertEquals(0L, matcher.getAvailableAmount(new ItemStack(Items.DIAMOND)));
    }

    @Test
    void testRemoveOrdersFor() {
        BlockPos provider = new BlockPos(10, 64, 10);
        BlockPos requester = new BlockPos(5, 64, 5);
        LogisticsOrder order = new LogisticsOrder(
            provider,
            requester,
            new ItemStack(Items.DIAMOND),
            32L,
            0L
        );

        matcher.addOrder(order);
        matcher.removeOrdersFor(provider);

        assertTrue(matcher.getOrdersFor(provider).isEmpty());
    }

    @Test
    void testMerge() {
        // Setup first matcher
        BlockPos provider1 = new BlockPos(10, 64, 10);
        Map<ItemStack, Long> items1 = new HashMap<>();
        items1.put(new ItemStack(Items.DIAMOND), 32L);
        matcher.updateProviderCache(provider1, items1, 0);

        BlockPos requester1 = new BlockPos(5, 64, 5);
        ItemRequest request1 = new ItemRequest(requester1, new ItemStack(Items.EMERALD), 16L, 0L, 0);
        matcher.addRequest(request1);

        // Setup second matcher
        RequestMatcher other = new RequestMatcher();
        BlockPos provider2 = new BlockPos(20, 64, 20);
        Map<ItemStack, Long> items2 = new HashMap<>();
        items2.put(new ItemStack(Items.DIAMOND), 16L);
        other.updateProviderCache(provider2, items2, 0);

        BlockPos requester2 = new BlockPos(15, 64, 15);
        ItemRequest request2 = new ItemRequest(requester2, new ItemStack(Items.GOLD_INGOT), 8L, 0L, 0);
        other.addRequest(request2);

        // Merge
        matcher.merge(other);

        // Verify merged state
        assertEquals(48L, matcher.getAvailableAmount(new ItemStack(Items.DIAMOND)));
        assertEquals(2, matcher.getPendingRequests().size());
    }
}
