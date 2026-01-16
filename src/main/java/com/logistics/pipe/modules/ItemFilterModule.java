package com.logistics.pipe.modules;

import com.logistics.block.entity.PipeBlockEntity;
import com.logistics.item.LogisticsItems;
import com.logistics.pipe.PipeContext;
import com.logistics.pipe.runtime.RoutePlan;
import com.logistics.pipe.runtime.TravelingItem;
import com.logistics.pipe.ui.ItemFilterScreenHandler;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Routes items based on per-side filters.
 * If the item matches any filtered sides, choose among those.
 * Otherwise, route to any side with no filters.
 */
public class ItemFilterModule implements Module {
    private static final String FILTERS = "filters"; // NBT key for save compatibility
    public static final int FILTER_SLOTS_PER_SIDE = 8;
    public static final Direction[] FILTER_ORDER = {
        Direction.NORTH,
        Direction.SOUTH,
        Direction.WEST,
        Direction.EAST,
        Direction.UP,
        Direction.DOWN
    };

    @Override
    public RoutePlan route(PipeContext ctx, TravelingItem item, List<Direction> options) {
        if (options == null) {
            options = List.of();
        }

        String itemId = Registries.ITEM.getId(item.getStack().getItem()).toString();
        List<Direction> matches = new ArrayList<>();
        List<Direction> fallbacks = new ArrayList<>();

        for (Direction direction : options) {
            List<String> filters = getFiltersForSide(ctx, direction);
            if (filters.isEmpty()) {
                fallbacks.add(direction);
                continue;
            }

            if (filters.contains(itemId)) {
                matches.add(direction);
            }
        }

        List<Direction> candidates = !matches.isEmpty() ? matches : fallbacks;
        return RoutePlan.reroute(candidates);
    }

    @Override
    public ActionResult onUseWithItem(PipeContext ctx, ItemUsageContext usage) {
        if (!LogisticsItems.isWrench(usage.getStack())) {
            return ActionResult.PASS;
        }

        if (ctx.world().isClient()) {
            return ActionResult.SUCCESS;
        }

        if (!(usage.getPlayer() instanceof ServerPlayerEntity player)) {
            return ActionResult.PASS;
        }

        World world = ctx.world();
        BlockPos pos = ctx.pos();
        player.openHandledScreen(new net.minecraft.screen.SimpleNamedScreenHandlerFactory(
            (syncId, inventory, playerEntity) -> {
                PipeBlockEntity pipeEntity = world.getBlockEntity(pos) instanceof PipeBlockEntity entity ? entity : null;
                return new ItemFilterScreenHandler(syncId, inventory, pipeEntity);
            },
            Text.translatable("screen.logistics.item_filter")
        ));
        return ActionResult.SUCCESS;
    }

    public static int getFilterColor(Direction direction) {
        return switch (direction) {
            case NORTH -> 0x3F76E4;
            case SOUTH -> 0xD93F3F;
            case WEST -> 0xC9B33A;
            case EAST -> 0x5AAE4D;
            case UP -> 0xE5E5E5;
            case DOWN -> 0x4A4A4A;
        };
    }

    public List<String> getFilterSlots(PipeContext ctx, Direction direction) {
        NbtCompound filters = ctx.getNbtCompound(this, FILTERS);
        NbtList list = filters.getListOrEmpty(direction.getId());
        List<String> slots = new ArrayList<>(FILTER_SLOTS_PER_SIDE);
        for (int i = 0; i < FILTER_SLOTS_PER_SIDE; i++) {
            if (i < list.size()) {
                slots.add(list.getString(i).orElse(""));
            } else {
                slots.add("");
            }
        }
        return slots;
    }

    public void setFilterSlots(PipeContext ctx, Direction direction, List<String> slots) {
        NbtCompound filters = ctx.getNbtCompound(this, FILTERS);
        NbtList list = new NbtList();
        boolean hasAny = false;

        for (String slot : slots) {
            String value = slot == null ? "" : slot;
            if (!value.isEmpty()) {
                hasAny = true;
            }
            list.add(NbtString.of(value));
        }

        if (hasAny) {
            filters.put(direction.getId(), list);
        } else {
            filters.remove(direction.getId());
        }

        if (!filters.isEmpty()) {
            ctx.putNbtCompound(this, FILTERS, filters);
        } else {
            ctx.remove(this, FILTERS);
        }
    }

    private List<String> getFiltersForSide(PipeContext ctx, Direction direction) {
        List<String> slots = getFilterSlots(ctx, direction);
        List<String> ids = new ArrayList<>(slots.size());
        for (String id : slots) {
            if (!id.isEmpty()) {
                ids.add(id);
            }
        }
        return ids;
    }
}
