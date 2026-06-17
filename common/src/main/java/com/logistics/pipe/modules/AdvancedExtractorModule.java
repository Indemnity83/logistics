package com.logistics.pipe.modules;

import com.logistics.LogisticsPipe;
import com.logistics.core.LogisticsConfig;
import com.logistics.pipe.network.NetDbg;
import com.logistics.core.lib.pipe.Module;
import com.logistics.core.lib.pipe.TickingModule;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.lib.serialization.DirectionSerializer;
import com.logistics.core.lib.filter.FilterSlots;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.core.lib.pipe.PipeHud;
import com.logistics.core.lib.storage.IItemKey;
import com.logistics.core.lib.storage.IItemStorage;
import com.logistics.core.lib.storage.IItemView;
import com.logistics.core.lib.storage.ItemStorageLookup;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.core.lib.pipe.TravelingItem;
import com.logistics.pipe.ui.AdvancedExtractorScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Advanced extractor module — same extraction behavior as {@link BasicExtractorModule} but with
 * a configurable item filter (include/exclude) accessible via wrench GUI.
 *
 * <p>Parameterised at construction so the same class serves all marks:
 * <ul>
 *   <li>MkI  — 1 item every 100 ticks</li>
 *   <li>MkII — 1 item every 20 ticks</li>
 *   <li>MkIII — 64 items every 6 ticks</li>
 * </ul>
 */
public class AdvancedExtractorModule implements Module, TickingModule {
    private static final String EXTRACT_DIRECTION = "extract_direction";
    private static final String TICKS_SINCE_PULL = "ticks_since_pull";
    public static final String FILTER_ITEMS = "filter_items";
    public static final String FILTER_INVERTED = "filter_inverted";
    public static final int MAX_FILTER_SLOTS = 9;

    private final int itemsPerPull;
    private final int ticksBetweenPulls;

    public AdvancedExtractorModule(int itemsPerPull, int ticksBetweenPulls) {
        this.itemsPerPull = itemsPerPull;
        this.ticksBetweenPulls = ticksBetweenPulls;
    }

    // ==================== Filter Configuration ====================

    /** Returns filter slots in slot order; empty string means the slot is empty. */
    public FilterSlots getFilterItems(PipeContext ctx) {
        return FilterSlots.load(ctx.getCompoundTag(this, FILTER_ITEMS), MAX_FILTER_SLOTS);
    }

    public void setFilterItem(PipeContext ctx, int slot, String itemId) {
        if (slot < 0 || slot >= MAX_FILTER_SLOTS) return;
        FilterSlots updated = getFilterItems(ctx).with(slot, itemId);
        if (updated.isEmpty()) {
            ctx.remove(this, FILTER_ITEMS);
        } else {
            ctx.putCompoundTag(this, FILTER_ITEMS, updated.toTag());
        }
        ctx.markDirtyAndSync();
    }

    public boolean isFilterInverted(PipeContext ctx) {
        return ctx.getInt(this, FILTER_INVERTED, 0) == 1;
    }

    @Override
    public void appendHud(PipeContext ctx, PipeHud hud) {
        if (isFilterInverted(ctx)) {
            hud.line(ModuleHud.detail(Component.translatable("jade.logistics.pipe.filter.none")));
            return;
        }
        List<ItemStack> stacks = getFilterItems(ctx).asList().stream()
                .map(id -> ModuleHud.stack(id, 1))
                .filter(stack -> !stack.isEmpty())
                .toList();
        if (stacks.isEmpty()) {
            hud.line(ModuleHud.detail(Component.translatable("jade.logistics.pipe.filter.any")));
        } else {
            hud.items(stacks);
        }
    }

    public void setFilterInverted(PipeContext ctx, boolean inverted) {
        ctx.saveInt(this, FILTER_INVERTED, inverted ? 1 : 0);
        ctx.markDirtyAndSync();
    }

    // ==================== Module Interface ====================

    @Override
    public void onConnectionsChanged(PipeContext ctx, List<Direction> options) {
        List<Direction> inventoryFaces = ctx.getInventoryConnections();
        if (inventoryFaces.isEmpty()) {
            setExtractDirection(ctx, null);
            return;
        }
        Direction current = getExtractDirection(ctx);
        if (current == null || !inventoryFaces.contains(current)) {
            setExtractDirection(ctx, inventoryFaces.getFirst());
        }
    }

    @Override
    public void onTick(PipeContext ctx) {
        int ticks = ctx.getInt(this, TICKS_SINCE_PULL, 0) + 1;
        if (ticks < ticksBetweenPulls) {
            ctx.saveInt(this, TICKS_SINCE_PULL, ticks);
            return;
        }

        ctx.saveInt(this, TICKS_SINCE_PULL, 0);

        Direction dir = getExtractDirection(ctx);
        if (dir == null) return;

        extract(ctx, dir);
    }

    @Override
    public InteractionResult onWrench(PipeContext ctx, Player player) {
        if (ctx.world().isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

        String moduleStateKey = ctx.moduleStateKey(this);
        serverPlayer.openMenu(new SimpleMenuProvider(
                (syncId, playerInventory, p) -> new AdvancedExtractorScreenHandler(
                        syncId, playerInventory, ((PipeBlockEntity) ctx.blockEntity()), moduleStateKey),
                Component.translatable("screen.logistics.advanced_extractor")));
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult openItemConfig(ServerPlayer player, InteractionHand hand, ItemStack stack) {
        player.openMenu(new SimpleMenuProvider(
                (syncId, inv, p) -> new AdvancedExtractorScreenHandler(syncId, inv, player, hand),
                Component.translatable("screen.logistics.advanced_extractor")));
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable ResourceId getPipeArm(PipeContext ctx, Direction direction) {
        if (getExtractDirection(ctx) != direction) return null;
        String suffix = ctx.isInventoryConnection(direction) ? "_feature_extended" : "_feature";
        return LogisticsPipe.model("item_extractor_pipe" + suffix);
    }

    private void extract(PipeContext ctx, Direction dir) {
        int occupied = ctx.pipeAccess().getTravelingItems().stream()
                .mapToInt(i -> i.getStack().getCount())
                .sum();
        if (PipeBlockEntity.VIRTUAL_CAPACITY - occupied < itemsPerPull) return;

        BlockPos targetPos = ctx.pos().relative(dir);
        IItemStorage storage = ItemStorageLookup.find(ctx.world(), targetPos, dir.getOpposite());
        if (storage == null) return;

        FilterSlots filter = getFilterItems(ctx);
        boolean inverted = isFilterInverted(ctx);
        for (IItemView view : storage.contents()) {
            IItemKey key = view.resource();
            if (view.amount() <= 0) continue;
            ItemStack stack1 = key.toStack(1);
            if (inverted ? filter.included(stack1) : filter.excluded(stack1)) {
                NetDbg.out("[AdvancedExtractor @ {}] Filtered out {} (mode={})", ctx.pos(), key.toStack(1).getItem(), inverted ? "exclude" : "include");
                continue;
            }

            long extracted = storage.extract(key, itemsPerPull, false);
            if (extracted > 0) {
                NetDbg.out("[AdvancedExtractor @ {}] Extracted {}x{} via {}", ctx.pos(), extracted, key.toStack(1).getItem(), dir);
                ItemStack stack = key.toStack((int) extracted);
                TravelingItem item = new TravelingItem(
                        stack, dir.getOpposite(), LogisticsConfig.get().pipe.minSpeed);
                ctx.pipeAccess().forceAddItem(item, dir);
                return;
            }
        }
    }

    @Nullable
    private Direction getExtractDirection(PipeContext ctx) {
        return DirectionSerializer.load(ctx, this, EXTRACT_DIRECTION);
    }

    private void setExtractDirection(PipeContext ctx, @Nullable Direction direction) {
        DirectionSerializer.save(ctx, this, EXTRACT_DIRECTION, direction);
    }
}
