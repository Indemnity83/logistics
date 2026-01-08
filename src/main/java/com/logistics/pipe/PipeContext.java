package com.logistics.pipe;

import com.logistics.block.PipeBlock;
import com.logistics.block.entity.PipeBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record PipeContext(World world,
                          BlockPos pos,
                          BlockState state,
                          PipeBlockEntity blockEntity) {
    public NbtCompound moduleState(String key) {
        return blockEntity.getOrCreateModuleState(key);
    }

    /**
     * Update the FEATURE_FACE blockstate property to visually indicate a direction.
     * This is used by modules like MergerModule and ExtractionModule to show their active face.
     *
     * @param direction The direction to set, or null to clear
     */
    public void setFeatureFace(@Nullable Direction direction) {
        if (world.isClient) {
            return;
        }

        PipeBlock.FeatureFace featureFace = PipeBlock.toFeatureFace(direction);
        if (state.contains(PipeBlock.FEATURE_FACE) && state.get(PipeBlock.FEATURE_FACE) != featureFace) {
            world.setBlockState(pos, state.with(PipeBlock.FEATURE_FACE, featureFace), 3);
        }
    }

    /**
     * Check if this pipe is receiving redstone power.
     * Used by modules like BoostModule to conditionally enable behaviors.
     *
     * @return true if the pipe is powered by redstone
     */
    public boolean isPowered() {
        return state.contains(PipeBlock.POWERED) && state.get(PipeBlock.POWERED);
    }

    /**
     * Get the blockstate of a neighboring block in the given direction.
     * Convenience method to avoid repeatedly writing world().getBlockState(pos().offset(direction)).
     *
     * @param direction The direction of the neighbor
     * @return The BlockState of the neighboring block
     */
    public BlockState getNeighborState(Direction direction) {
        return world.getBlockState(pos.offset(direction));
    }

    /**
     * Check if the neighboring block in the given direction is a pipe.
     * Used by ingress modules to determine if items are coming from another pipe.
     *
     * @param direction The direction to check
     * @return true if the neighbor is a PipeBlock
     */
    public boolean isNeighborPipe(Direction direction) {
        return getNeighborState(direction).getBlock() instanceof PipeBlock;
    }

    /**
     * Get all directions that this pipe has connections to (pipes or inventories).
     * Returns directions where the connection type is not NONE.
     *
     * @return List of connected directions
     */
    public List<Direction> getConnectedDirections() {
        List<Direction> connected = new java.util.ArrayList<>();
        for (Direction direction : Direction.values()) {
            if (state.get(PipeBlock.getPropertyForDirection(direction)) != PipeBlock.ConnectionType.NONE) {
                connected.add(direction);
            }
        }
        return connected;
    }

    /**
     * Check if this pipe has a connection in the given direction.
     * A connection can be to another pipe or to an inventory.
     *
     * @param direction The direction to check
     * @return true if there is a connection in that direction
     */
    public boolean hasConnection(Direction direction) {
        return state.get(PipeBlock.getPropertyForDirection(direction)) != PipeBlock.ConnectionType.NONE;
    }

    /**
     * Get connected directions that lead to other pipes.
     */
    public List<Direction> getPipeConnections() {
        List<Direction> outputs = new java.util.ArrayList<>();
        for (Direction direction : getConnectedDirections()) {
            if (isNeighborPipe(direction)) {
                outputs.add(direction);
            }
        }
        return outputs;
    }

    /**
     * Get connected directions that lead to inventories (non-pipes with ItemStorage).
     */
    public List<Direction> getInventoryConnections() {
        List<Direction> faces = new java.util.ArrayList<>();
        for (Direction direction : Direction.values()) {
            if (!hasConnection(direction)) {
                continue;
            }

            if (!isNeighborPipe(direction)) {
                BlockPos targetPos = pos.offset(direction);
                if (net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.find(world, targetPos, direction.getOpposite()) != null) {
                    faces.add(direction);
                }
            }
        }
        return faces;
    }
}
