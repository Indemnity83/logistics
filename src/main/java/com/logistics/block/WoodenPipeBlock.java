package com.logistics.block;

import com.logistics.block.entity.PipeBlockEntity;
import com.logistics.item.TravelingItem;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

/**
 * Wooden pipes can extract items from adjacent inventories.
 * They are the entry point for items into the pipe network.
 * Items start slow in wooden pipes, then accelerate when entering regular pipes.
 * Visually, connections to inventories use opaque textures instead of transparent.
 */
public class WoodenPipeBlock extends PipeBlock {
    // Wooden pipes are slower - 3 seconds to traverse
    private static final float WOODEN_PIPE_SPEED = 1.0f / 60.0f; // Blocks per tick

    // Configuration: Extract one item every time the pipe empties
    private static final int EXTRACTION_INTERVAL = 60; // Ticks between extraction attempts (3 seconds)

    public WoodenPipeBlock(Settings settings) {
        super(settings);
    }

    @Override
    public float getPipeSpeed(World world, BlockPos pos, BlockState state) {
        return WOODEN_PIPE_SPEED;
    }

    @Override
    public boolean canAcceptFromPipe(World world, BlockPos pos, BlockState state, Direction fromDirection) {
        // Wooden pipes are extraction-only entry points - they cannot accept items from other pipes
        return false;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return validateTicker(type, com.logistics.block.entity.LogisticsBlockEntities.PIPE_BLOCK_ENTITY,
            (world1, pos, state1, blockEntity) -> {
                // Call parent tick for item movement
                PipeBlockEntity.tick(world1, pos, state1, blockEntity);
                // Add extraction logic for wooden pipes
                if (!world1.isClient) {
                    tryExtract(world1, pos, state1, blockEntity);
                }
            });
    }

    /**
     * Attempt to extract items from adjacent inventories
     */
    public static void tryExtract(World world, BlockPos pos, BlockState state, PipeBlockEntity blockEntity) {
        // Only extract once per second
        if (world.getTime() % EXTRACTION_INTERVAL != 0) {
            return;
        }

        // Try each connected direction
        for (Direction direction : Direction.values()) {
            BooleanProperty property = getPropertyForDirection(direction);
            if (property != null && state.get(property)) {
                if (extractFromDirection(world, pos, direction, blockEntity)) {
                    // Successfully extracted, don't try other directions this tick
                    return;
                }
            }
        }
    }

    /**
     * Try to extract an item from an inventory in the given direction
     */
    private static boolean extractFromDirection(World world, BlockPos pipePos, Direction direction, PipeBlockEntity blockEntity) {
        BlockPos targetPos = pipePos.offset(direction);

        // Get the item storage from the target block
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(world, targetPos, direction.getOpposite());
        if (storage == null) {
            return false;
        }

        // Try to extract one item
        try (Transaction transaction = Transaction.openOuter()) {
            for (StorageView<ItemVariant> view : storage) {
                ItemVariant variant = view.getResource();
                if (variant.isBlank()) {
                    continue;
                }

                // Try to extract 1 item
                long extracted = view.extract(variant, 1, transaction);
                if (extracted > 0) {
                    // Create traveling item with wooden pipe speed
                    ItemStack stack = variant.toStack((int) extracted);
                    TravelingItem item = new TravelingItem(stack, direction.getOpposite(), WOODEN_PIPE_SPEED);

                    blockEntity.addItem(item);
                    transaction.commit();
                    return true;
                }
            }
        }

        return false;
    }

    private static BooleanProperty getPropertyForDirection(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            case UP -> UP;
            case DOWN -> DOWN;
        };
    }
}
