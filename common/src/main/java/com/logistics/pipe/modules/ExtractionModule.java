package com.logistics.pipe.modules;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsConfigHost.Configs;

import com.logistics.LogisticsPipe;
import com.logistics.core.lib.pipe.Module;
import com.logistics.core.lib.pipe.TickingModule;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.core.lib.storage.IItemKey;
import com.logistics.core.lib.storage.IItemStorage;
import com.logistics.core.lib.storage.IItemView;
import com.logistics.core.lib.storage.ItemStorageLookup;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.core.lib.pipe.TravelingItem;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class ExtractionModule implements Module, TickingModule {
    private static final String EXTRACT_FROM = "extract_direction"; // NBT key for save compatibility
    private static final String TICKS_SINCE_PULL = "ticks_since_pull";
    private static final int RF_PER_ITEM = 10;
    private static final long ENERGY_CAPACITY = 2560L;

    @Override
    public void onTick(PipeContext ctx) {
        // Increment tick counter
        int ticks = ctx.getInt(this, TICKS_SINCE_PULL, 0);
        ticks++;
        ctx.saveInt(this, TICKS_SINCE_PULL, ticks);

        // Check if we should extract
        if (!shouldExtract(ctx, ticks)) {
            return;
        }

        // Get extraction direction
        Direction direction = getExtractionDirection(ctx);
        if (direction == null) {
            direction = autoSelectDirection(ctx);
            if (direction == null) {
                return;
            }
            setExtractionDirection(ctx, direction);
        }

        // Extract items based on available energy
        extractFromDirection(ctx, direction);

        // Always reset tick counter and zero energy buffer
        ctx.saveInt(this, TICKS_SINCE_PULL, 0);
        ctx.setEnergy(0);
    }

    @Override
    public void onConnectionsChanged(PipeContext ctx, List<Direction> options) {
        List<Direction> inventoryFaces = ctx.getInventoryConnections();
        if (inventoryFaces.isEmpty()) {
            setExtractionDirection(ctx, null);
            return;
        }

        Direction current = getExtractionDirection(ctx);
        if (current == null || !inventoryFaces.contains(current)) {
            setExtractionDirection(ctx, inventoryFaces.getFirst());
        }
    }

    @Override
    public InteractionResult onWrench(PipeContext ctx, Player player) {
        if (ctx.world().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        List<Direction> connected = ctx.getInventoryConnections();

        // No valid outputs: clear config.
        if (connected.isEmpty()) {
            setExtractionDirection(ctx, null);
            return InteractionResult.SUCCESS;
        }

        Direction current = getExtractionDirection(ctx);
        Direction next = nextInCycle(connected, current);

        setExtractionDirection(ctx, next);
        return InteractionResult.SUCCESS;
    }

    private @Nullable Direction getExtractionDirection(PipeContext ctx) {
        CompoundTag state = ctx.moduleState(this);
        String directionStr = NbtCompat.getString(state, EXTRACT_FROM, "");
        if (directionStr.isEmpty()) {
            return null;
        }
        try {
            return Direction.from3DDataValue(Integer.parseInt(directionStr));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void setExtractionDirection(PipeContext ctx, @Nullable Direction direction) {
        Direction current = getExtractionDirection(ctx);
        if (current == direction) {
            return;
        }

        if (direction == null) {
            ctx.remove(this, EXTRACT_FROM);
        } else {
            ctx.saveString(this, EXTRACT_FROM, String.valueOf(direction.get3DDataValue()));
        }

        ctx.markDirtyAndSync();
    }

    private Direction nextInCycle(List<Direction> ordered, @Nullable Direction current) {
        if (ordered.isEmpty()) {
            throw new IllegalArgumentException("ordered directions must not be empty");
        }

        int idx = (current == null) ? -1 : ordered.indexOf(current);
        return (idx < 0) ? ordered.getFirst() : ordered.get((idx + 1) % ordered.size());
    }

    private boolean shouldExtract(PipeContext ctx, int ticks) {
        long energy = ctx.getEnergy();

        // 0-7 ticks: cooldown, never extract
        if (ticks < 8) {
            return false;
        }

        // 8-15 ticks: only if energy covers full stack
        if (ticks < 16) {
            Direction direction = getExtractionDirection(ctx);
            long maxExtractable = getMaxExtractableCount(ctx, direction);
            return energy >= maxExtractable * RF_PER_ITEM;
        }

        // 16+ ticks: extract if ≥10 RF available
        return energy >= RF_PER_ITEM;
    }

    private long getMaxExtractableCount(PipeContext ctx, @Nullable Direction direction) {
        if (direction == null) {
            return 0;
        }

        BlockPos targetPos = ctx.pos().relative(direction);
        IItemStorage storage = ItemStorageLookup.find(ctx.world(), targetPos, direction.getOpposite());

        if (storage == null) {
            return 0;
        }

        // Find first non-empty slot and check stack size
        for (IItemView view : storage.contents()) {
            IItemKey key = view.resource();
            if (view.amount() > 0) {
                return Math.min(64, key.toStack(1).getItem().getDefaultMaxStackSize());
            }
        }
        return 0;
    }

    @Nullable private Direction autoSelectDirection(PipeContext ctx) {
        List<Direction> inventoryFaces = ctx.getInventoryConnections();
        return inventoryFaces.isEmpty() ? null : inventoryFaces.getFirst();
    }

    private boolean extractFromDirection(PipeContext ctx, Direction direction) {
        long energy = ctx.getEnergy();
        long maxItems = Math.min(64, energy / RF_PER_ITEM);

        if (maxItems <= 0) {
            return false;
        }

        // Check if pipe has space for the full extraction
        int totalItems = ctx.pipeAccess().getTravelingItems().stream()
                .mapToInt(item -> item.getStack().getCount())
                .sum();
        int remaining = PipeBlockEntity.VIRTUAL_CAPACITY - totalItems;
        if (remaining < maxItems) {
            return false; // Not enough space for full extraction, skip to preserve full stacks
        }

        BlockPos targetPos = ctx.pos().relative(direction);
        IItemStorage storage = ItemStorageLookup.find(ctx.world(), targetPos, direction.getOpposite());

        if (storage == null) {
            return false;
        }

        for (IItemView view : storage.contents()) {
            IItemKey key = view.resource();
            if (view.amount() <= 0) {
                continue;
            }

            long extracted = storage.extract(key, maxItems, false);
            if (extracted > 0) {
                ItemStack stack = key.toStack((int) extracted);
                TravelingItem item = new TravelingItem(stack, direction.getOpposite(), LogisticsConfigHost.get(Configs.PIPE_MIN_SPEED));
                ctx.pipeAccess().forceAddItem(item, direction);
                return true;
            }
        }

        return false;
    }

    @Override
    public @Nullable ResourceId getPipeArm(PipeContext ctx, Direction direction) {
        if (!isExtractionFace(ctx, direction)) {
            return null;
        }
        String suffix = ctx.isInventoryConnection(direction) ? "_feature_extended" : "_feature";
        return LogisticsPipe.model("item_extractor_pipe" + suffix);
    }

    private boolean isExtractionFace(PipeContext ctx, Direction direction) {
        return getExtractionDirection(ctx) == direction;
    }

    @Override
    public boolean acceptsLowTierEnergyFrom(PipeContext ctx, Direction from) {
        return true; // Accept low-tier energy from all directions
    }
}
