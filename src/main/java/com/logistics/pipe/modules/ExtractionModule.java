package com.logistics.pipe.modules;

import com.logistics.LogisticsMod;
import com.logistics.pipe.PipeContext;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.runtime.PipeConfig;
import com.logistics.pipe.runtime.TravelingItem;
import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class ExtractionModule implements Module {
    private static final String EXTRACT_FROM = "extract_direction"; // NBT key for save compatibility
    private static final String TICKS_SINCE_PULL = "ticks_since_pull";
    private static final int RF_PER_ITEM = 10;
    private static final long ENERGY_CAPACITY = 2560L;

    @Override
    public void onTick(PipeContext ctx) {
        Module.super.onTick(ctx);
        if (ctx.world().isClientSide()) {
            return;
        }

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
        ctx.setEnergy(this, 0);
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
        CompoundTag state = ctx.moduleState(getStateKey());
        if (!state.contains(EXTRACT_FROM)) {
            return null;
        }
        String directionStr = state.getString(EXTRACT_FROM).orElse("");
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
        long energy = ctx.getEnergy(this);

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
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(ctx.world(), targetPos, direction.getOpposite());

        if (storage == null) {
            return 0;
        }

        // Find first non-empty slot and check stack size
        for (StorageView<ItemVariant> view : storage) {
            ItemVariant variant = view.getResource();
            if (!variant.isBlank()) {
                return Math.min(64, variant.getItem().getDefaultMaxStackSize());
            }
        }
        return 0;
    }

    @Nullable private Direction autoSelectDirection(PipeContext ctx) {
        List<Direction> inventoryFaces = ctx.getInventoryConnections();
        return inventoryFaces.isEmpty() ? null : inventoryFaces.getFirst();
    }

    private boolean extractFromDirection(PipeContext ctx, Direction direction) {
        long energy = ctx.getEnergy(this);
        long maxItems = Math.min(64, energy / RF_PER_ITEM);

        if (maxItems <= 0) {
            return false;
        }

        // Check if pipe has space for the full extraction
        int totalItems = ctx.blockEntity().getTravelingItems().stream()
                .mapToInt(item -> item.getStack().getCount())
                .sum();
        int remaining = PipeBlockEntity.VIRTUAL_CAPACITY - totalItems;
        if (remaining < maxItems) {
            return false; // Not enough space for full extraction, skip to preserve full stacks
        }

        BlockPos targetPos = ctx.pos().relative(direction);
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(ctx.world(), targetPos, direction.getOpposite());

        if (storage == null) {
            return false;
        }

        try (Transaction transaction = Transaction.openOuter()) {
            for (StorageView<ItemVariant> view : storage) {
                ItemVariant variant = view.getResource();
                if (variant.isBlank()) {
                    continue;
                }

                long extracted = view.extract(variant, maxItems, transaction);
                if (extracted > 0) {
                    ItemStack stack = variant.toStack((int) extracted);
                    TravelingItem item = new TravelingItem(stack, direction.getOpposite(), PipeConfig.ITEM_MIN_SPEED);
                    ctx.blockEntity().forceAddItem(item, direction);
                    transaction.commit();
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public @Nullable Identifier getPipeArm(PipeContext ctx, Direction direction) {
        if (!isExtractionFace(ctx, direction)) {
            return null;
        }
        String suffix = ctx.isInventoryConnection(direction) ? "_feature_extended" : "_feature";
        return Identifier.fromNamespaceAndPath(LogisticsMod.MOD_ID, "block/pipe/item_extractor_pipe" + suffix);
    }

    private boolean isExtractionFace(PipeContext ctx, Direction direction) {
        return getExtractionDirection(ctx) == direction;
    }

    // --- Energy methods ---

    @Override
    public long getEnergyAmount(PipeContext ctx) {
        return ctx.getEnergy(this);
    }

    @Override
    public long getEnergyCapacity(PipeContext ctx) {
        return ENERGY_CAPACITY;
    }

    @Override
    public long insertEnergy(PipeContext ctx, long maxAmount, boolean simulate) {
        long current = getEnergyAmount(ctx);
        long space = Math.max(0, ENERGY_CAPACITY - current);
        long accepted = Math.min(maxAmount, space);

        if (accepted > 0 && !simulate) {
            ctx.setEnergy(this, current + accepted);
        }

        return accepted;
    }

    @Override
    public long extractEnergy(PipeContext ctx, long maxAmount, boolean simulate) {
        long current = getEnergyAmount(ctx);
        long extracted = Math.min(maxAmount, current);

        if (extracted > 0 && !simulate) {
            long remaining = current - extracted;
            ctx.setEnergy(this, remaining);
        }

        return extracted;
    }

    @Override
    public boolean canInsertEnergy(PipeContext ctx) {
        return true;
    }

    @Override
    public boolean canExtractEnergy(PipeContext ctx) {
        return false; // Module extracts internally, not exposed externally
    }

    @Override
    public boolean acceptsLowTierEnergyFrom(PipeContext ctx, Direction from) {
        return true; // Accept low-tier energy from all directions
    }
}
