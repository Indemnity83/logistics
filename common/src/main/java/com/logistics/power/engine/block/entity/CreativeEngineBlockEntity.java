package com.logistics.power.engine.block.entity;

import com.logistics.core.lib.power.AbstractEngineBlockEntity;
import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.power.engine.block.CreativeEngineBlock;
import com.logistics.LogisticsPower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity for the Creative Engine.
 * A special engine for Creative Mode that generates configurable amounts of energy.
 */
public class CreativeEngineBlockEntity extends AbstractEngineBlockEntity {

    // ==================== Constants ====================

    /** Output levels that double with each wrench click. */
    public static final long[] OUTPUT_LEVELS = CreativeOutputLevels.DEFAULT_LEVELS.clone();

    private static final long MAX_ENERGY = 10_000L;

    // ==================== State ====================

    private final CreativeOutputLevels outputLevels = new CreativeOutputLevels();

    // ==================== Constructor & Ticker ====================

    public CreativeEngineBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsPower.ENTITY.CREATIVE_ENGINE_BLOCK_ENTITY, pos, state);
    }

    public static void tick(Level world, BlockPos pos, BlockState state, CreativeEngineBlockEntity entity) {
        entity.tickEngine(world, pos, state);
    }

    // ==================== Subclass Configuration ====================

    @Override
    protected long getEnergyBufferCapacity() {
        return MAX_ENERGY;
    }

    @Override
    protected long getOutputPower() {
        return outputLevels.outputRate();
    }

    @Override
    public boolean canOverheat() {
        return false;
    }

    @Override
    protected Direction getOutputDirection() {
        return CreativeEngineBlock.getOutputDirection(getBlockState());
    }

    @Override
    protected boolean isRedstonePowered() {
        return getBlockState().getValue(CreativeEngineBlock.POWERED);
    }

    @Override
    protected boolean sendsEnergyContinuously() {
        return true;
    }

    @Override
    protected HeatStage computeStage() {
        return HeatStage.COLD;
    }

    @Override
    public float getPistonSpeed() {
        return outputLevels.pistonSpeed();
    }

    // ==================== Lifecycle Hooks ====================

    /**
     * Creative engine generates infinite energy - buffer is always full when running.
     */
    @Override
    protected void produceEnergy() {
        if (!isRedstonePowered()) {
            return;
        }

        // Infinite energy generation - always fill buffer to max
        energyBuffer.setAmount(getEnergyBufferCapacity());
    }

    // ==================== Output Level Control ====================

    /**
     * Cycles to the next output level (doubles the output rate).
     * Wraps around to minimum when maximum is exceeded.
     *
     * @return the new output rate in RF/t
     */
    public long cycleOutputLevel() {
        long outputRate = outputLevels.cycle();
        markDirtyAndSync(); // Sync to clients so renderer can update piston speed
        return outputRate;
    }

    /**
     * Gets the current output level index.
     */
    public int getOutputLevelIndex() {
        return outputLevels.index();
    }

    /**
     * Gets the current output rate in RF/t.
     */
    public long getOutputRate() {
        return outputLevels.outputRate();
    }

    // ==================== NBT Serialization ====================

    @Override
    protected void saveLogisticsData(CompoundTag nbt, HolderLookup.Provider registries) {
        super.saveLogisticsData(nbt, registries);
        nbt.putInt("OutputLevelIndex", outputLevels.index());
    }

    @Override
    protected void loadLogisticsData(CompoundTag nbt, HolderLookup.Provider registries) {
        super.loadLogisticsData(nbt, registries);
        outputLevels.restore(NbtCompat.getInt(nbt, "OutputLevelIndex", 0));
    }

    @Override
    protected void loadLegacyData(CompoundTag nbt, HolderLookup.Provider registries) {
        super.loadLegacyData(nbt, registries); // Loads engine data from "Engine" tag

        // Load Creative-specific data from old "CreativeData" tag
        if (nbt.contains("CreativeData")) {
            CompoundTag creativeData = nbt.getCompound("CreativeData");
            outputLevels.restore(NbtCompat.getInt(creativeData, "outputLevelIndex", 0));
        }
    }
}
