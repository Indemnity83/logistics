package com.logistics.pipe.modules;

import java.util.ArrayList;
import java.util.List;

import com.logistics.pipe.PipeContext;
import com.logistics.pipe.runtime.RoutePlan;
import com.logistics.pipe.runtime.TravelingItem;

import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class InsertionModule implements Module {
    @Override
    public RoutePlan route(PipeContext ctx, TravelingItem item, List<Direction> options) {
        if (options == null || options.isEmpty()) {
            return RoutePlan.drop();
        }

        List<Direction> inventoryWithSpace = new ArrayList<>();
        List<Direction> pipeDirections = new ArrayList<>();

        for (Direction direction : options) {
            if (ctx.isInventoryConnection(direction)) {
                if (hasInsertSpace(ctx, item, direction)) {
                    inventoryWithSpace.add(direction);
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

        if (!pipeDirections.isEmpty()) {
            return RoutePlan.reroute(pipeDirections);
        }

        return RoutePlan.drop();
    }

    private boolean hasInsertSpace(PipeContext ctx, TravelingItem item, Direction direction) {
        BlockPos targetPos = ctx.pos().offset(direction);
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(ctx.world(), targetPos, direction.getOpposite());
        if (storage == null) {
            return false;
        }

        ItemVariant variant = ItemVariant.of(item.getStack());
        long amount = item.getStack().getCount();
        if (variant.isBlank() || amount <= 0) {
            return false;
        }

        try (Transaction transaction = Transaction.openOuter()) {
            return storage.insert(variant, amount, transaction) > 0;
        }
    }
}
