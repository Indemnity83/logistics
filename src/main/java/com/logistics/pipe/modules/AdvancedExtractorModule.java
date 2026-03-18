package com.logistics.pipe.modules;

import com.logistics.LogisticsPipe;
import com.logistics.core.lib.pipe.Module;
import com.logistics.core.lib.pipe.TickingModule;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.lib.storage.DirectionSerializer;
import com.logistics.core.lib.storage.FilterSlots;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.core.lib.pipe.TravelingItem;
import com.logistics.pipe.ui.AdvancedExtractorScreenHandler;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

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

    public void setFilterInverted(PipeContext ctx, boolean inverted) {
        ctx.saveInt(this, FILTER_INVERTED, inverted ? 1 : 0);
        ctx.markDirtyAndSync();
    }

    /**
     * Returns true if the item should be skipped during extraction.
     * With an empty filter, nothing is filtered out.
     * Include mode (default): only items IN the filter are extracted.
     * Exclude mode (inverted): items IN the filter are skipped.
     */
    private boolean isFilteredOut(PipeContext ctx, ItemStack stack) {
        Set<String> resolvable = getFilterItems(ctx).resolveItemIds();
        if (resolvable.isEmpty()) return false;
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        boolean itemInFilter = resolvable.contains(itemId);
        return isFilterInverted(ctx) == itemInFilter;
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

        serverPlayer.openMenu(new SimpleMenuProvider(
                (syncId, playerInventory, p) -> new AdvancedExtractorScreenHandler(
                        syncId, playerInventory, ((PipeBlockEntity) ctx.blockEntity())),
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
        int occupied = ctx.blockEntity().getTravelingItems().stream()
                .mapToInt(i -> i.getStack().getCount())
                .sum();
        if (PipeBlockEntity.VIRTUAL_CAPACITY - occupied < itemsPerPull) return;

        BlockPos targetPos = ctx.pos().relative(dir);
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(ctx.world(), targetPos, dir.getOpposite());
        if (storage == null) return;

        try (Transaction tx = Transaction.openOuter()) {
            for (StorageView<ItemVariant> view : storage) {
                ItemVariant variant = view.getResource();
                if (variant.isBlank()) continue;
                if (isFilteredOut(ctx, variant.toStack())) continue;

                long extracted = view.extract(variant, itemsPerPull, tx);
                if (extracted > 0) {
                    ItemStack stack = variant.toStack((int) extracted);
                    TravelingItem item = new TravelingItem(
                            stack, dir.getOpposite(), LogisticsPipe.CONFIG.ITEM_MIN_SPEED);
                    ctx.blockEntity().forceAddItem(item, dir);
                    tx.commit();
                    return;
                }
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
