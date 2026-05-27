package com.logistics.pipe.modules;

import com.logistics.core.lib.pipe.RoutingModule;

import com.logistics.core.lib.resource.ResourceId;
import com.logistics.pipe.network.NetDbg;
import com.logistics.core.lib.pipe.Module;
import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.core.lib.pipe.RoutePlan;
import com.logistics.core.lib.pipe.TravelingItem;
import com.logistics.pipe.ui.ItemFilterScreenHandler;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Routes items based on per-side filters.
 * If the item matches any filtered sides, choose among those.
 * Otherwise, route to any side with no filters.
 */
public class ItemFilterModule implements Module, RoutingModule {
    public static final String FILTERS = "filters"; // NBT key for save compatibility
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

        if (!matches.isEmpty()) {
            NetDbg.out("[ItemFilter @ {}] {} matches filter, routing to {}", ctx.pos(), item.getStack().getItem(), matches);
            return RoutePlan.reroute(matches);
        }
        if (!fallbacks.isEmpty()) {
            NetDbg.out("[ItemFilter @ {}] No filter match for {}, using fallback(s): {}", ctx.pos(), item.getStack().getItem(), fallbacks);
            return RoutePlan.reroute(fallbacks);
        }
        NetDbg.out("[ItemFilter @ {}] No route for {}, dropping", ctx.pos(), item.getStack().getItem());
        return RoutePlan.reroute(List.of());
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

    @Override
    public InteractionResult openItemConfig(ServerPlayer player, InteractionHand hand, ItemStack stack) {
        player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                (syncId, inv, p) -> new ItemFilterScreenHandler(syncId, inv, player, hand),
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

        ListTag list = NbtCompat.getListOrEmpty(filters, direction.getName());
        for (int i = 0; i < FILTER_SLOTS_PER_SIDE; i++) {
            slots.add(NbtCompat.getStringAt(list, i, ""));
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

    /**
     * Returns the filter stacks for {@code direction}, preserving full component data.
     * Handles both old format (ListTag of StringTag item IDs) and new format
     * (ListTag of CompoundTag full ItemStack). Always returns exactly
     * {@link #FILTER_SLOTS_PER_SIDE} entries, padding with {@link ItemStack#EMPTY}.
     */
    public List<ItemStack> getFilterStacks(PipeContext ctx, Direction direction) {
        CompoundTag filters = ctx.getCompoundTag(this, FILTERS);
        List<ItemStack> result = new ArrayList<>(FILTER_SLOTS_PER_SIDE);

        ListTag list = NbtCompat.getListOrEmpty(filters, direction.getName());
        RegistryOps<Tag> ops = (ctx.world() != null)
                ? ctx.world().registryAccess().createSerializationContext(NbtOps.INSTANCE)
                : null;

        for (int i = 0; i < FILTER_SLOTS_PER_SIDE; i++) {
            ItemStack stack = ItemStack.EMPTY;
            if (i < list.size()) {
                var compoundOpt = list.getCompound(i);
                if (compoundOpt.isPresent()) {
                    // New format: full ItemStack encoded with ItemStack.CODEC
                    CompoundTag tag = compoundOpt.get();
                    if (!tag.isEmpty() && ops != null) {
                        stack = ItemStack.CODEC.parse(ops, tag).result().orElse(ItemStack.EMPTY);
                    }
                } else {
                    // Old format: item registry ID string
                    String id = NbtCompat.getStringAt(list, i, "");
                    if (!id.isEmpty()) {
                        ResourceId resource = ResourceId.tryParse(id);
                        if (resource != null) {
                            var itemOpt = BuiltInRegistries.ITEM.get(resource.toIdentifier());
                            if (itemOpt.isPresent() && itemOpt.get().value() != Items.AIR) {
                                stack = new ItemStack(itemOpt.get().value());
                            }
                        }
                    }
                }
            }
            result.add(stack);
        }

        return result;
    }

    /**
     * Persists {@code stacks} for {@code direction} using full {@link ItemStack.CODEC} encoding,
     * so component data (custom names, colors, NBT) survives save/reload.
     */
    public void setFilterStacks(PipeContext ctx, Direction direction, List<ItemStack> stacks) {
        CompoundTag filters = ctx.getCompoundTag(this, FILTERS);
        ListTag list = new ListTag();
        boolean hasAny = false;

        RegistryOps<Tag> ops = (ctx.world() != null)
                ? ctx.world().registryAccess().createSerializationContext(NbtOps.INSTANCE)
                : null;

        for (int i = 0; i < FILTER_SLOTS_PER_SIDE; i++) {
            ItemStack stack = (i < stacks.size() && stacks.get(i) != null) ? stacks.get(i) : ItemStack.EMPTY;
            if (!stack.isEmpty() && ops != null) {
                hasAny = true;
                CompoundTag encoded = ItemStack.CODEC.encodeStart(ops, stack.copyWithCount(1))
                        .result()
                        .filter(t -> t instanceof CompoundTag)
                        .map(t -> (CompoundTag) t)
                        .orElse(new CompoundTag());
                list.add(encoded);
            } else {
                list.add(new CompoundTag());
            }
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
        List<ItemStack> filterStacks = getFilterStacks(ctx, direction);
        List<String> ids = new ArrayList<>(filterStacks.size());
        for (ItemStack stack : filterStacks) {
            if (!stack.isEmpty()) {
                ids.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            }
        }
        return ids;
    }
}
