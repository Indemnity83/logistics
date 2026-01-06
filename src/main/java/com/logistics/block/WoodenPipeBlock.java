package com.logistics.block;

import com.logistics.block.entity.PipeBlockEntity;
import com.logistics.item.TravelingItem;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.BlockState;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Wooden pipes can extract items from adjacent inventories.
 * They are the entry point for items into the pipe network.
 * Items start slow in wooden pipes, then accelerate when entering regular pipes.
 * Visually, connections to inventories use opaque textures instead of transparent.
 */
public class WoodenPipeBlock extends PipeBlock {
    public static final EnumProperty<ActiveFace> ACTIVE_FACE = EnumProperty.of("active_face", ActiveFace.class);

    // Wooden pipes are slower - 3 seconds to traverse
    private static final float WOODEN_PIPE_SPEED = 1.0f / 60.0f; // Blocks per tick

    // Configuration: Extract one item every time the pipe empties
    private static final int EXTRACTION_INTERVAL = 60; // Ticks between extraction attempts (3 seconds)

    public WoodenPipeBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(ACTIVE_FACE, ActiveFace.NONE));
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

    @Override
    public boolean canAcceptFromInventory(World world, BlockPos pos, BlockState state, Direction fromDirection) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (!(blockEntity instanceof PipeBlockEntity pipeEntity)) {
            return false;
        }

        Direction activeFace = pipeEntity.getActiveInputFace();
        return activeFace != null && activeFace == fromDirection;
    }

    @Override
    protected BlockState updateInventoryConnections(World world, BlockPos pos, BlockState state) {
        return super.updateInventoryConnections(world, pos, state);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockState state = super.getPlacementState(ctx);
        if (state == null) {
            return null;
        }
        return state.with(ACTIVE_FACE, ActiveFace.NONE);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(ACTIVE_FACE);
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

    public void cycleActiveFace(World world, BlockPos pos) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (!(blockEntity instanceof PipeBlockEntity pipeEntity)) {
            return;
        }

        BlockState state = world.getBlockState(pos);
        List<Direction> inventoryFaces = findInventoryFaces(world, pos, state);
        if (inventoryFaces.isEmpty()) {
            pipeEntity.setActiveInputFace(null);
            return;
        }

        Direction activeFace = pipeEntity.getActiveInputFace();
        int index = inventoryFaces.indexOf(activeFace);
        int nextIndex = index == -1 ? 0 : (index + 1) % inventoryFaces.size();
        pipeEntity.setActiveInputFace(inventoryFaces.get(nextIndex));
    }

    /**
     * Attempt to extract items from adjacent inventories
     */
    public static void tryExtract(World world, BlockPos pos, BlockState state, PipeBlockEntity blockEntity) {
        // Only extract once per second
        if (world.getTime() % EXTRACTION_INTERVAL != 0) {
            return;
        }

        Direction activeFace = blockEntity.getActiveInputFace();
        if (activeFace != null && isInventoryFace(world, pos, activeFace)) {
            extractFromDirection(world, pos, activeFace, blockEntity);
            return;
        }

        List<Direction> inventoryFaces = findInventoryFaces(world, pos, state);
        if (inventoryFaces.size() == 1) {
            Direction selected = inventoryFaces.get(0);
            blockEntity.setActiveInputFace(selected);
            extractFromDirection(world, pos, selected, blockEntity);
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

                    // Add item from the direction of the inventory
                    blockEntity.addItem(item, direction);
                    transaction.commit();
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean isInventoryFace(World world, BlockPos pos, Direction direction) {
        BlockPos targetPos = pos.offset(direction);
        BlockState targetState = world.getBlockState(targetPos);
        if (targetState.getBlock() instanceof PipeBlock) {
            return false;
        }

        Storage<ItemVariant> storage = ItemStorage.SIDED.find(world, targetPos, direction.getOpposite());
        return storage != null;
    }

    private static List<Direction> findInventoryFaces(World world, BlockPos pos, BlockState state) {
        List<Direction> faces = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            BooleanProperty property = getPropertyForDirection(direction);
            if (property == null || !state.get(property)) {
                continue;
            }

            if (isInventoryFace(world, pos, direction)) {
                faces.add(direction);
            }
        }
        return faces;
    }

    public static ActiveFace toActiveFace(@Nullable Direction direction) {
        if (direction == null) {
            return ActiveFace.NONE;
        }

        return switch (direction) {
            case NORTH -> ActiveFace.NORTH;
            case SOUTH -> ActiveFace.SOUTH;
            case EAST -> ActiveFace.EAST;
            case WEST -> ActiveFace.WEST;
            case UP -> ActiveFace.UP;
            case DOWN -> ActiveFace.DOWN;
        };
    }

    public enum ActiveFace implements net.minecraft.util.StringIdentifiable {
        NONE("none"),
        NORTH("north"),
        SOUTH("south"),
        EAST("east"),
        WEST("west"),
        UP("up"),
        DOWN("down");

        private final String name;

        ActiveFace(String name) {
            this.name = name;
        }

        @Override
        public String asString() {
            return name;
        }
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
