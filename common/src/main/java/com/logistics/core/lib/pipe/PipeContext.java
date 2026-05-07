package com.logistics.core.lib.pipe;

import com.logistics.core.lib.block.capability.PipeConnection;
import com.logistics.core.lib.energy.EnergyComponent;
import com.logistics.core.lib.network.ILogisticsNetwork;
import com.logistics.core.lib.compat.NbtCompat;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public record PipeContext(
        Level world,
        BlockPos pos,
        BlockState state,
        IPipeAccess blockEntity,
        Map<Module, String> moduleStateKeys) {

    public PipeContext(Level world, BlockPos pos, BlockState state, IPipeAccess blockEntity) {
        this(world, pos, state, blockEntity, Map.of());
    }

    /**
     * Returns a new context that routes {@code module}'s state through {@code stateKey} instead
     * of the class-level default. Uses {@link IdentityHashMap} intentionally: the lookup relies on
     * object identity, not equality. The exact instance passed here must be the same instance that
     * later calls {@code ctx.moduleState(this)} — if {@link com.logistics.pipe.ChassisPipe#getDynamicModuleEntries}
     * is ever called twice for the same slot it would produce a different instance and the scoped
     * key would silently fall back to the class-level default.
     */
    public PipeContext withModuleStateKey(Module module, String stateKey) {
        IdentityHashMap<Module, String> keys = new IdentityHashMap<>(moduleStateKeys);
        keys.put(module, stateKey);
        return new PipeContext(world, pos, state, blockEntity, keys);
    }

    public String moduleStateKey(Module module) {
        return moduleStateKeys.getOrDefault(module, module.getStateKey());
    }

    public CompoundTag moduleState(String key) {
        return blockEntity.moduleState(key);
    }

    public CompoundTag moduleState(Module module) {
        String stateKey = moduleStateKey(module);
        CompoundTag existingScopedState = blockEntity.existingModuleState(stateKey);
        CompoundTag state = moduleState(stateKey);

        String defaultStateKey = module.getStateKey();
        if (!stateKey.equals(defaultStateKey) && existingScopedState == null) {
            CompoundTag legacyState = blockEntity.existingModuleState(defaultStateKey);
            if (legacyState != null && !legacyState.isEmpty()) {
                for (String key : legacyState.keySet()) {
                    state.put(key, legacyState.get(key).copy());
                }
            }
        }

        return state;
    }

    // Convenience methods for module state access (with Module instance)
    public String getString(Module module, String key, String defaultValue) {
        return NbtCompat.getString(moduleState(module), key, defaultValue);
    }

    public void saveString(Module module, String key, String value) {
        moduleState(module).putString(key, value);
    }

    public int getInt(Module module, String key, int defaultValue) {
        return NbtCompat.getInt(moduleState(module), key, defaultValue);
    }

    public void saveInt(Module module, String key, int value) {
        moduleState(module).putInt(key, value);
    }

    public long getLong(Module module, String key, long defaultValue) {
        return NbtCompat.getLong(moduleState(module), key, defaultValue);
    }

    public void saveLong(Module module, String key, long value) {
        moduleState(module).putLong(key, value);
    }

    public void remove(Module module, String key) {
        moduleState(module).remove(key);
    }

    public void clearModuleState(Module module) {
        blockEntity.clearModuleState(moduleStateKey(module));
        markDirtyAndSync();
    }

    // Convenience methods for energy access
    public long getEnergy() {
        EnergyComponent energy = blockEntity.getEnergy();
        return energy != null ? energy.getAmount() : 0;
    }

    public void setEnergy(long amount) {
        EnergyComponent energy = blockEntity.getEnergy();
        if (energy != null) {
            energy.setAmount(amount);
            markDirtyAndSync();
        }
    }

    public CompoundTag getCompoundTag(Module module, String key) {
        return NbtCompat.getCompoundOrEmpty(moduleState(module), key);
    }

    public void putCompoundTag(Module module, String key, CompoundTag value) {
        moduleState(module).put(key, value);
    }

    public void markDirty() {
        blockEntity.markDirty();
    }

    public void markDirtyAndSync() {
        markDirty();

        if (world != null && !world.isClientSide()) {
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
        return blockEntity.isPowered();
    }

    /**
     * Return the logistics network this pipe belongs to, or {@code null} if not yet formed.
     */
    public @Nullable ILogisticsNetwork network() {
        return blockEntity.getNetwork();
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
        return blockEntity.isNeighborPipe(world, pos, direction);
    }

    /**
     * Get all directions that this pipe has connections to (pipes or inventories).
     * Returns directions where the connection type is not NONE.
     *
     * @return List of connected directions
     */
    public List<Direction> getConnectedDirections() {
        List<Direction> connected = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            PipeConnection.Type type = blockEntity.getConnectionType(world, pos, direction);
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
        PipeConnection.Type type = blockEntity.getConnectionType(world, pos, direction);
        return type != PipeConnection.Type.NONE;
    }

    /**
     * Get the connection type for a specific direction.
     *
     * @param direction The direction to check
     * @return The connection type (NONE, PIPE, or INVENTORY)
     */
    public PipeConnection.Type getConnectionType(Direction direction) {
        return blockEntity.getConnectionType(world, pos, direction);
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
        List<Direction> outputs = new ArrayList<>();
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
        List<Direction> faces = new ArrayList<>();
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
