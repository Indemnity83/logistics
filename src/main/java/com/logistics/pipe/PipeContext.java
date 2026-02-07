package com.logistics.pipe;

import com.logistics.core.lib.pipe.PipeConnection;
import com.logistics.pipe.block.PipeBlock;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.modules.Module;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public record PipeContext(Level world, BlockPos pos, BlockState state, PipeBlockEntity blockEntity) {

    /**
     * Get the Pipe instance for this pipe block.
     * @return the Pipe, or null if the block is not a PipeBlock or has no pipe
     */
    @Nullable public Pipe pipe() {
        if (state.getBlock() instanceof PipeBlock pipeBlock) {
            return pipeBlock.getPipe();
        }
        return null;
    }

    public CompoundTag moduleState(String key) {
        return blockEntity.getOrCreateModuleState(key);
    }

    // Convenience methods for module state access (with Module instance)
    public String getString(Module module, String key, String defaultValue) {
        return moduleState(module.getStateKey()).getString(key).orElse(defaultValue);
    }

    public void saveString(Module module, String key, String value) {
        moduleState(module.getStateKey()).putString(key, value);
    }

    public int getInt(Module module, String key, int defaultValue) {
        return moduleState(module.getStateKey()).getInt(key).orElse(defaultValue);
    }

    public void saveInt(Module module, String key, int value) {
        moduleState(module.getStateKey()).putInt(key, value);
    }

    public long getLong(Module module, String key, long defaultValue) {
        return moduleState(module.getStateKey()).getLong(key).orElse(defaultValue);
    }

    public void saveLong(Module module, String key, long value) {
        moduleState(module.getStateKey()).putLong(key, value);
    }

    public void remove(Module module, String key) {
        moduleState(module.getStateKey()).remove(key);
    }

    // Convenience methods for energy access
    public long getEnergy() {
        return blockEntity().energyStorage != null ? blockEntity().energyStorage.amount : 0;
    }

    public void setEnergy(long amount) {
        if (blockEntity().energyStorage != null) {
            blockEntity().energyStorage.amount = amount;
        }
    }

    public CompoundTag getCompoundTag(Module module, String key) {
        return moduleState(module.getStateKey()).getCompoundOrEmpty(key);
    }

    public void putCompoundTag(Module module, String key, CompoundTag value) {
        moduleState(module.getStateKey()).put(key, value);
    }

    public void markDirty() {
        blockEntity.setChanged();
    }

    public void markDirtyAndSync() {
        markDirty();

        if (!world.isClientSide()) {
            world.sendBlockUpdated(pos, state, state, 3);
        }
    }

    /**
     * Check if this pipe is receiving redstone power.
     * Used by modules like BoostModule to conditionally enable behaviors.
     *
     * @return true if the pipe is powered by redstone
     */
    public boolean isPowered() {
        return state.hasProperty(PipeBlock.POWERED) && state.getValue(PipeBlock.POWERED);
    }

    /**
     * Get the blockstate of a neighboring block in the given direction.
     * Convenience method to avoid repeatedly writing world().getBlockState(pos().offset(direction)).
     *
     * @param direction The direction of the neighbor
     * @return The BlockState of the neighboring block
     */
    public BlockState getNeighborState(Direction direction) {
        return world.getBlockState(pos.relative(direction));
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
        if (!(state.getBlock() instanceof PipeBlock pipeBlock)) {
            return connected;
        }

        for (Direction direction : Direction.values()) {
            PipeConnection.Type type = pipeBlock.getConnectionType(world, pos, direction);
            if (type != PipeConnection.Type.NONE) {
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
        if (!(state.getBlock() instanceof PipeBlock pipeBlock)) {
            return false;
        }
        PipeConnection.Type type = pipeBlock.getConnectionType(world, pos, direction);
        return type != PipeConnection.Type.NONE;
    }

    /**
     * Get the connection type for a specific direction.
     *
     * @param direction The direction to check
     * @return The connection type (NONE, PIPE, or INVENTORY)
     */
    public PipeConnection.Type getConnectionType(Direction direction) {
        if (!(state.getBlock() instanceof PipeBlock pipeBlock)) {
            return PipeConnection.Type.NONE;
        }
        return pipeBlock.getConnectionType(world, pos, direction);
    }

    /**
     * Get the cached connection type for the given direction.
     * Always uses the cached value from the block entity.
     */
    public PipeConnection.Type getCachedConnectionType(Direction direction) {
        return blockEntity.getCachedConnectionType(direction);
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
     * Get connected directions that lead to inventories.
     *
     * This relies on the pipe's authoritative connection type logic (including any module filtering)
     * and avoids duplicating ItemStorage probing here.
     */
    public List<Direction> getInventoryConnections() {
        List<Direction> faces = new java.util.ArrayList<>();
        for (Direction direction : Direction.values()) {
            if (isInventoryConnection(direction)) {
                faces.add(direction);
            }
        }
        return faces;
    }

    public boolean isInventoryConnection(Direction direction) {
        return getConnectionType(direction) == PipeConnection.Type.INVENTORY;
    }
}
