package com.logistics.pipe.modules;

import com.logistics.pipe.PipeContext;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.runtime.RoutePlan;
import com.logistics.pipe.runtime.TravelingItem;
import com.logistics.pipe.ui.ItemFilterScreenHandler;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Routes items based on per-side filters.
 * If the item matches any filtered sides, choose among those.
 * Otherwise, route to any side with no filters.
 */
public class ItemFilterModule implements Module {
    private static final String FILTERS = "filters"; // NBT key for save compatibility
    public static final int FILTER_SLOTS_PER_SIDE = 8;
    public static final Direction[] FILTER_ORDER = {
        Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.UP, Direction.DOWN
    };

    @Override
    public RoutePlan route(PipeContext ctx, TravelingItem item, List<Direction> options) {
        if (options == null) {
            options = List.of();
        }

        String itemId = BuiltInRegistries.ITEM.getKey(item.getStack().getItem()).toString();
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
    public InteractionResult onWrench(PipeContext ctx, Player player) {
        if (ctx.world().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        Level world = ctx.world();
        BlockPos pos = ctx.pos();
        serverPlayer.openMenu(new net.minecraft.world.SimpleMenuProvider(
                (syncId, inventory, playerEntity) -> {
                    PipeBlockEntity pipeEntity =
                            world.getBlockEntity(pos) instanceof PipeBlockEntity entity ? entity : null;
                    return new ItemFilterScreenHandler(syncId, inventory, pipeEntity);
                },
                Component.translatable("screen.logistics.item_filter")));
        return InteractionResult.SUCCESS;
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

    @Override
    public Integer getArmTint(PipeContext ctx, Direction direction) {
        return getFilterColor(direction);
    }

    public List<String> getFilterSlots(PipeContext ctx, Direction direction) {
        CompoundTag filters = ctx.getCompoundTag(this, FILTERS);
        List<String> slots = new ArrayList<>(FILTER_SLOTS_PER_SIDE);

        var listOpt = filters.getList(direction.getName());
        if (listOpt.isPresent()) {
            ListTag list = listOpt.get();
            for (int i = 0; i < FILTER_SLOTS_PER_SIDE; i++) {
                if (i < list.size()) {
                    slots.add(list.getString(i).orElse(""));
                } else {
                    slots.add("");
                }
            }
        } else {
            // No saved filters - fill with empty strings
            for (int i = 0; i < FILTER_SLOTS_PER_SIDE; i++) {
                slots.add("");
            }
        }
        return slots;
    }

    public void setFilterSlots(PipeContext ctx, Direction direction, List<String> slots) {
        CompoundTag filters = ctx.getCompoundTag(this, FILTERS);
        ListTag list = new ListTag();
        boolean hasAny = false;

        for (String slot : slots) {
            String value = slot == null ? "" : slot;
            if (!value.isEmpty()) {
                hasAny = true;
            }
            list.add(StringTag.valueOf(value));
        }

        if (hasAny) {
            filters.put(direction.getName(), list);
        } else {
            filters.remove(direction.getName());
        }

        if (!filters.isEmpty()) {
            ctx.putCompoundTag(this, FILTERS, filters);
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
