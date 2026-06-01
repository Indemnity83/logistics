package com.logistics.pipe.modules;

import com.logistics.core.lib.pipe.RoutePlan;
import com.logistics.core.lib.pipe.TravelingItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

final class InsertionRoutingPlanner {
    private InsertionRoutingPlanner() {}

    static RoutePlan route(
            TravelingItem item,
            List<InventoryOption> inventoryOptions,
            List<Direction> pipeDirections,
            long routeSeed) {
        if (inventoryOptions.isEmpty() && pipeDirections.isEmpty()) {
            return RoutePlan.drop();
        }

        List<Direction> inventoryWithSpace = new ArrayList<>();
        List<Direction> inventoryWithPartialSpace = new ArrayList<>();
        List<Long> partialAmounts = new ArrayList<>();
        long amount = item.getStack().getCount();

        for (InventoryOption option : inventoryOptions) {
            long available = option.available();
            if (available >= amount) {
                inventoryWithSpace.add(option.direction());
            } else if (available > 0) {
                inventoryWithPartialSpace.add(option.direction());
                partialAmounts.add(available);
            }
        }

        if (!inventoryWithSpace.isEmpty()) {
            return RoutePlan.reroute(inventoryWithSpace);
        }

        if (!inventoryWithPartialSpace.isEmpty() && !pipeDirections.isEmpty()) {
            return splitToInventoryAndPipes(item, inventoryWithPartialSpace, partialAmounts, pipeDirections, routeSeed);
        }

        if (!inventoryWithPartialSpace.isEmpty()) {
            return RoutePlan.reroute(inventoryWithPartialSpace);
        }

        if (!pipeDirections.isEmpty()) {
            return RoutePlan.reroute(pipeDirections);
        }

        return RoutePlan.drop();
    }

    static long routeSeed(long pos, long gameTime, Direction currentDirection) {
        return mixHash(pos, gameTime, currentDirection.get3DDataValue());
    }

    private static RoutePlan splitToInventoryAndPipes(
            TravelingItem item,
            List<Direction> inventoryDirections,
            List<Long> amounts,
            List<Direction> pipeDirections,
            long routeSeed) {
        long remaining = item.getStack().getCount();
        List<TravelingItem> split = new ArrayList<>();

        for (int i = 0; i < inventoryDirections.size() && remaining > 0; i++) {
            long capacity = amounts.get(i);
            if (capacity <= 0) {
                continue;
            }
            long take = Math.min(capacity, remaining);
            if (take <= 0) {
                continue;
            }
            ItemStack stack = item.getStack().copy();
            stack.setCount((int) take);
            split.add(new TravelingItem(stack, inventoryDirections.get(i), item.getSpeed()));
            remaining -= take;
        }

        if (remaining > 0) {
            Direction chosen = chooseRandomDirection(pipeDirections, routeSeed);
            ItemStack stack = item.getStack().copy();
            stack.setCount((int) remaining);
            split.add(new TravelingItem(stack, chosen, item.getSpeed()));
        }

        return RoutePlan.split(split);
    }

    private static Direction chooseRandomDirection(List<Direction> options, long routeSeed) {
        Random random = new Random(routeSeed);
        return options.get(random.nextInt(options.size()));
    }

    private static long mixHash(long a, long b, long c) {
        long hash = a;
        hash = hash * 31 + b;
        hash = hash * 31 + c;
        return hash;
    }

    record InventoryOption(Direction direction, long available) {}
}
