package com.logistics.pipe.modules;

import com.logistics.pipe.PipeContext;
import com.logistics.pipe.runtime.RoutePlan;
import com.logistics.pipe.runtime.TravelingItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

public class InsertionModule implements Module {
    @Override
    public RoutePlan route(PipeContext ctx, TravelingItem item, List<Direction> options) {
        if (options == null || options.isEmpty()) {
            return RoutePlan.drop();
        }

        List<Direction> inventoryWithSpace = new ArrayList<>();
        List<Direction> inventoryWithPartialSpace = new ArrayList<>();
        List<Long> partialAmounts = new ArrayList<>();
        List<Direction> pipeDirections = new ArrayList<>();
        long amount = item.getStack().getCount();

        for (Direction direction : options) {
            if (ctx.isInventoryConnection(direction)) {
                long available = getInsertSpace(ctx, item, direction);
                if (available >= amount) {
                    inventoryWithSpace.add(direction);
                } else if (available > 0) {
                    inventoryWithPartialSpace.add(direction);
                    partialAmounts.add(available);
                }
                continue;
            }

            if (ctx.isNeighborPipe(direction)) {
                pipeDirections.add(direction);
            }
        }

        if (!inventoryWithSpace.isEmpty()) {
            return RoutePlan.reroute(inventoryWithSpace);
        }

        if (!inventoryWithPartialSpace.isEmpty() && !pipeDirections.isEmpty()) {
            return splitToInventoryAndPipes(ctx, item, inventoryWithPartialSpace, partialAmounts, pipeDirections);
        }

        if (!inventoryWithPartialSpace.isEmpty()) {
            return RoutePlan.reroute(inventoryWithPartialSpace);
        }

        if (!pipeDirections.isEmpty()) {
            return RoutePlan.reroute(pipeDirections);
        }

        return RoutePlan.drop();
    }

    private long getInsertSpace(PipeContext ctx, TravelingItem item, Direction direction) {
        BlockPos targetPos = ctx.pos().relative(direction);
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(ctx.world(), targetPos, direction.getOpposite());
        if (storage == null) {
            return 0;
        }

        ItemVariant variant = ItemVariant.of(item.getStack());
        long amount = item.getStack().getCount();
        if (variant.isBlank() || amount <= 0) {
            return 0;
        }

        long reserved = getRoutedAmount(ctx, direction, variant);
        if (reserved < 0) {
            reserved = 0;
        }

        try (Transaction transaction = Transaction.openOuter()) {
            long requested = amount + reserved;
            long accepted = storage.insert(variant, requested, transaction);
            long available = accepted - reserved;
            return Math.max(0, available);
        }
    }

    private long getRoutedAmount(PipeContext ctx, Direction direction, ItemVariant variant) {
        long total = 0;
        for (TravelingItem other : ctx.blockEntity().getTravelingItems()) {
            if (!other.isRouted()) {
                continue;
            }

            if (other.getDirection() != direction) {
                continue;
            }

            ItemVariant otherVariant = ItemVariant.of(other.getStack());
            if (!variant.equals(otherVariant)) {
                continue;
            }

            total += other.getStack().getCount();
        }
        return total;
    }

    private RoutePlan splitToInventoryAndPipes(
            PipeContext ctx,
            TravelingItem item,
            List<Direction> inventoryDirections,
            List<Long> amounts,
            List<Direction> pipeDirections) {
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
            Direction chosen = chooseRandomDirection(ctx, item, pipeDirections);
            ItemStack stack = item.getStack().copy();
            stack.setCount((int) remaining);
            split.add(new TravelingItem(stack, chosen, item.getSpeed()));
        }

        return RoutePlan.split(split);
    }

    private Direction chooseRandomDirection(PipeContext ctx, TravelingItem item, List<Direction> options) {
        long seed = mixHash(
                ctx.pos().asLong(),
                ctx.world().getGameTime(),
                item.getDirection().ordinal());
        Random random = new Random(seed);
        return options.get(random.nextInt(options.size()));
    }

    private long mixHash(long a, long b, long c) {
        long hash = a;
        hash = hash * 31 + b;
        hash = hash * 31 + c;
        return hash;
    }
}
